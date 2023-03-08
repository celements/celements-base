package com.celements.model.context;

import static java.util.stream.Collectors.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.MoreOptional;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * {@link #execute} code within a modified {@link ExecutionContext}, {@link XWikiContext}
 * and/or {@link VelocityContext}. The modifications are reset after execution.
 */
@NotThreadSafe
public class Contextualiser {

  private class ContextModifier {

    private final Map<String, Optional<Object>> values = new HashMap<>();
    private final Function<String, Object> getter;
    private final BiConsumer<String, Object> setter;
    private final Consumer<String> remover;

    ContextModifier(Function<String, Object> getter,
        BiConsumer<String, Object> setter,
        Consumer<String> remover) {
      this.getter = getter;
      this.setter = setter;
      this.remover = remover;
    }

    void with(String key, Object val) {
      if (key != null) {
        values.put(key, Optional.ofNullable(val));
      }
    }

    boolean hasValues() {
      return !values.isEmpty();
    }

    Optional<Object> get(String key) {
      return Optional.ofNullable(getter.apply(key));
    }

    void setOrRemove(String key, Optional<Object> value) {
      if (value.isPresent()) {
        setter.accept(key, value.get());
      } else {
        remover.accept(key);
      }
    }

    <T> T execute(Supplier<T> supplier) {
      Map<String, Optional<Object>> prev = values.keySet().stream()
          .collect(toMap(key -> key, this::get));
      try {
        values.forEach(this::setOrRemove);
        return supplier.get();
      } finally {
        prev.forEach(this::setOrRemove);
      }
    }
  }

  private final Optional<ContextModifier> eCtxMod;
  private final Optional<ContextModifier> xCtxMod;
  private final Optional<ContextModifier> vCtxMod;

  public Contextualiser() {
    eCtxMod = getExecCtx().map(ctx -> new ContextModifier(ctx::getProperty, ctx::setProperty,
        ctx::removeProperty));
    xCtxMod = getXWikiCtx().map(ctx -> new ContextModifier(ctx::get, ctx::put, ctx::remove));
    vCtxMod = getVeloCtx().map(ctx -> new ContextModifier(ctx::get, ctx::put, ctx::remove));
  }

  public Contextualiser withExecContext(String key, Object val) {
    eCtxMod.ifPresent(mod -> mod.with(key, val));
    return this;
  }

  public Contextualiser withXWikiContext(String key, Object val) {
    xCtxMod.ifPresent(mod -> mod.with(key, val));
    return this;
  }

  public Contextualiser withVeloContext(String key, Object val) {
    vCtxMod.ifPresent(mod -> mod.with(key, val));
    return this;
  }

  public Contextualiser withWiki(WikiReference wiki) {
    return withXWikiContext("wiki", (wiki != null) ? wiki.getName() : null);
  }

  public Contextualiser withDoc(XWikiDocument doc) {
    return withXWikiContext("doc", doc);
  }

  public void execute(Runnable runnable) {
    execute(() -> {
      runnable.run();
      return null;
    });
  }

  public <T> T execute(Supplier<T> supplier) {
    return Stream.of(eCtxMod, xCtxMod, vCtxMod)
        .flatMap(MoreOptional::stream)
        .filter(ContextModifier::hasValues)
        .map(ctxMod -> (Function<Supplier<T>, T>) ctxMod::execute)
        .reduce(Supplier::get, (f1, f2) -> (s -> f1.apply(() -> f2.apply(s))))
        .apply(supplier);
  }

  private static Optional<VelocityContext> getVeloCtx() {
    return getXWikiCtx().map(ctx -> (VelocityContext) ctx.get("vcontext"));
  }

  private static Optional<XWikiContext> getXWikiCtx() {
    return getExecCtx()
        .map(ctx -> (XWikiContext) ctx.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
  }

  private static Optional<ExecutionContext> getExecCtx() {
    try {
      return Optional.ofNullable(Utils.getComponentManager()
          .lookup(Execution.class)
          .getContext());
    } catch (ComponentLookupException e) {
      return Optional.empty();
    }
  }

}
