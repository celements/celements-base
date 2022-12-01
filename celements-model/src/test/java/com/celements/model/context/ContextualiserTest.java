package com.celements.model.context;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class ContextualiserTest extends AbstractComponentTest {

  Map<String, Object> data;

  @Before
  public void prepare() {
    data = new HashMap<>();
    data.put("asdf", new Object());
    data.put("fdsa", new Object());
    data.put("null", null);
  }

  @Test
  public void test_withWiki() {
    Consumer<WikiReference> asserter = w -> assertEquals(w.getName(), getContext().getDatabase());
    WikiReference wiki = new WikiReference("wiki");
    WikiReference wikiInner = new WikiReference("other");
    getContext().setDatabase(wiki.getName());
    asserter.accept(wiki);
    new Contextualiser()
        .withWiki(wikiInner)
        .execute(() -> asserter.accept(wikiInner));
    asserter.accept(wiki);
  }

  @Test
  public void test_withDoc() {
    Consumer<XWikiDocument> asserter = d -> assertSame(d, getContext().getDoc());
    XWikiDocument doc = createMockAndAddToDefault(XWikiDocument.class);
    XWikiDocument docInner = createMockAndAddToDefault(XWikiDocument.class);
    getContext().setDoc(doc);
    asserter.accept(doc);
    new Contextualiser()
        .withDoc(docInner)
        .execute(() -> asserter.accept(docInner));
    asserter.accept(doc);
  }

  @Test
  public void test_withExecCtxValue() {
    data.keySet().forEach(k -> assertExecCtx(k, null));
    Contextualiser contextualiser = new Contextualiser();
    data.forEach(contextualiser::withExecContext);
    contextualiser.execute(() -> data.forEach(this::assertExecCtx));
    data.keySet().forEach(k -> assertExecCtx(k, null));
  }

  private void assertExecCtx(String key, Object value) {
    ExecutionContext ctx = Utils.getComponent(Execution.class).getContext();
    assertSame(value, ctx.getProperty(key));
  }

  @Test
  public void test_withXWikiCtxValue() {
    data.keySet().forEach(k -> assertXWikiCtx(k, null));
    Contextualiser contextualiser = new Contextualiser();
    data.forEach(contextualiser::withXWikiContext);
    contextualiser.execute(() -> data.forEach(this::assertXWikiCtx));
    data.keySet().forEach(k -> assertXWikiCtx(k, null));
  }

  private void assertXWikiCtx(String key, Object value) {
    XWikiContext ctx = getContext();
    assertSame(value, ctx.get(key));
  }

  @Test
  public void test_withVeloCtxValue() {
    getContext().put("vcontext", new VelocityContext());
    data.keySet().forEach(k -> assertVeloCtx(k, null));
    Contextualiser contextualiser = new Contextualiser();
    data.forEach(contextualiser::withVeloContext);
    contextualiser.execute(() -> data.forEach(this::assertVeloCtx));
    data.keySet().forEach(k -> assertVeloCtx(k, null));
  }

  private void assertVeloCtx(String key, Object value) {
    VelocityContext ctx = (VelocityContext) getContext().get("vcontext");
    assertSame(value, ctx.get(key));
  }

  @Test
  public void test_all() {
    getContext().put("vcontext", new VelocityContext());
    Contextualiser contextualiser = new Contextualiser();
    data.forEach(contextualiser::withExecContext);
    data.forEach(contextualiser::withXWikiContext);
    data.forEach(contextualiser::withVeloContext);
    contextualiser.execute(() -> {
      data.forEach(this::assertExecCtx);
      data.forEach(this::assertXWikiCtx);
      data.forEach(this::assertVeloCtx);
    });
    data.keySet().forEach(k -> {
      assertExecCtx(k, null);
      assertXWikiCtx(k, null);
      assertVeloCtx(k, null);
    });
  }

  @Test
  public void test_all_exc() {
    getContext().put("vcontext", new VelocityContext());
    Contextualiser contextualiser = new Contextualiser();
    data.forEach(contextualiser::withExecContext);
    data.forEach(contextualiser::withXWikiContext);
    data.forEach(contextualiser::withVeloContext);
    RuntimeException exc = new RuntimeException();
    try {
      contextualiser.execute(() -> {
        data.forEach(this::assertExecCtx);
        data.forEach(this::assertXWikiCtx);
        data.forEach(this::assertVeloCtx);
        throw exc;
      });
    } catch (RuntimeException e) {
      assertSame(exc, e);
    }
    data.keySet().forEach(k -> {
      assertExecCtx(k, null);
      assertXWikiCtx(k, null);
      assertVeloCtx(k, null);
    });
  }
}
