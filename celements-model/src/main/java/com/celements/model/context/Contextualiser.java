package com.celements.model.context;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * {@link #execute} code within a modified {@link ExecutionContext}, {@link XWikiContext}
 * and/or {@link VelocityContext}. The modifications are reset after execution.
 */
@NotThreadSafe
public class Contextualiser {

  private final Map<String, Optional<Object>> eValues = new HashMap<>();
  private final Map<String, Optional<Object>> xValues = new HashMap<>();
  private final Map<String, Optional<Object>> vValues = new HashMap<>();

  public Contextualiser withExecContext(String key, Object val) {
    return with(eValues, key, val);
  }

  public Contextualiser withXWikiContext(String key, Object val) {
    return with(xValues, key, val);
  }

  public Contextualiser withVeloContext(String key, Object val) {
    return with(vValues, key, val);
  }

  private Contextualiser with(Map<String, Optional<Object>> ctx, String key, Object val) {
    if (key != null) {
      ctx.put(key, Optional.ofNullable(val));
    }
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
    List<Function<Supplier<T>, T>> modifiers = new ArrayList<>();
    getExecCtx().ifPresent(ctx -> modifiers.add(s -> executeWith(s, eValues,
        ctx::getProperty, ctx::setProperty, ctx::removeProperty)));
    getXWikiCtx().ifPresent(ctx -> modifiers.add(s -> executeWith(s, xValues,
        ctx::get, ctx::put, ctx::remove)));
    getVeloCtx().ifPresent(ctx -> modifiers.add(s -> executeWith(s, vValues,
        ctx::get, ctx::put, ctx::remove)));
    return modifiers.stream()
        .reduce(Supplier::get, (f1, f2) -> (s -> f1.apply(() -> f2.apply(s))))
        .apply(supplier);
  }

  private <T> T executeWith(Supplier<T> supplier, Map<String, Optional<Object>> values,
      Function<String, Object> getter,
      BiConsumer<String, Object> setter,
      Consumer<String> remover) {
    return executeWith(supplier, values, key -> Optional.ofNullable(getter.apply(key)),
        (key, value) -> {
          if (value.isPresent()) {
            setter.accept(key, value.get());
          } else {
            remover.accept(key);
          }
        });
  }

  private <T> T executeWith(Supplier<T> supplier, Map<String, Optional<Object>> values,
      Function<String, Optional<Object>> getter,
      BiConsumer<String, Optional<Object>> setOrRemove) {
    Map<String, Optional<Object>> prev = values.keySet().stream()
        .collect(toMap(key -> key, getter));
    try {
      values.forEach(setOrRemove);
      return supplier.get();
    } finally {
      prev.forEach(setOrRemove);
    }
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
