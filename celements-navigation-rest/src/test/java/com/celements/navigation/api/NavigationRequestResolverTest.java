package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.web.service.IWebUtilsService;

public class NavigationRequestResolverTest extends AbstractComponentTest {

  private final WikiReference wikiRef = new WikiReference("xwiki");
  private final SpaceReference spaceRef = new SpaceReference("Content", wikiRef);
  private ModelUtils modelUtils;
  private ModelContext modelContext;
  private IWebUtilsService webUtilsService;
  private NavigationRequestResolver resolver;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(ModelUtils.class, ModelContext.class, IWebUtilsService.class);
    modelUtils = getMock(ModelUtils.class);
    modelContext = getMock(ModelContext.class);
    webUtilsService = getMock(IWebUtilsService.class);
    resolver = getBeanFactory().getBean(NavigationRequestResolver.class);
  }

  @Test
  public void test_resolve_acceptsCanonicalLocalReferencesAndRequestedLanguage() {
    var currentRef = new DocumentReference("MyPage", spaceRef);
    expectCanonicalSpace("Content");
    expectCanonicalDocument("Content.MyPage", currentRef);
    expect(modelUtils.normalizeLang("DE")).andReturn("de");
    expect(webUtilsService.getAllowedLanguages(spaceRef)).andReturn(List.of("en", "de"));
    replayDefault();
    var request = resolver.resolve("Content", "Content.MyPage", "DE", "main", 2);
    verifyDefault();
    assertEquals(spaceRef, request.nodeSpace());
    assertEquals(Optional.of(currentRef), request.currentNode());
    assertEquals("de", request.language());
    assertEquals(Optional.of("main"), request.partName());
    assertEquals(2, request.showInactiveToLevel());
  }

  @Test
  public void test_resolve_defaultsLanguageFromCurrentRequestAndNormalizesBlankPart() {
    expectCanonicalSpace("Content");
    expect(modelContext.getLanguage()).andReturn(Optional.of("fr"));
    replayDefault();
    var request = resolver.resolve("Content", null, null, "  ", 0);
    verifyDefault();
    assertEquals("fr", request.language());
    assertEquals(Optional.empty(), request.partName());
    assertEquals(Optional.empty(), request.serializedCurrentNode());
  }

  @Test
  public void test_resolve_defaultsLanguageFromWikiWhenRequestLanguageIsAbsent() {
    expectCanonicalSpace("Content");
    expect(modelContext.getLanguage()).andReturn(Optional.empty());
    expect(modelContext.getDefaultLanguage()).andReturn("en");
    replayDefault();
    assertEquals("en", resolver.resolve("Content", null, null, null, 0).language());
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsCurrentWikiQualifiedSpace() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("xwiki:Content", SpaceReference.class, wikiRef))
        .andReturn(spaceRef);
    expect(modelUtils.serializeRefLocal(spaceRef)).andReturn("Content");
    replayDefault();
    assertApiError("invalid_reference",
        () -> resolver.resolve("xwiki:Content", null, null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsOtherWikiQualifiedCurrentNode() {
    var otherRef = new DocumentReference("MyPage",
        new SpaceReference("Content", new WikiReference("other")));
    expectCanonicalSpace("Content");
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("other:Content.MyPage", DocumentReference.class, wikiRef))
        .andReturn(otherRef);
    expect(modelUtils.serializeRefLocal(otherRef)).andReturn("Content.MyPage");
    replayDefault();
    assertApiError("invalid_reference",
        () -> resolver.resolve("Content", "other:Content.MyPage", null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsCurrentWikiQualifiedCurrentNode() {
    var currentRef = new DocumentReference("MyPage", spaceRef);
    expectCanonicalSpace("Content");
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("xwiki:Content.MyPage", DocumentReference.class, wikiRef))
        .andReturn(currentRef);
    expect(modelUtils.serializeRefLocal(currentRef)).andReturn("Content.MyPage");
    replayDefault();
    assertApiError("invalid_reference",
        () -> resolver.resolve("Content", "xwiki:Content.MyPage", null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsBlankAndNoncanonicalCurrentNode() {
    expectCanonicalSpace("Content");
    replayDefault();
    assertApiError("invalid_reference", () -> resolver.resolve("Content", " ", null, null, 0));
    verifyDefault();
    resetDefault();
    expectCanonicalSpace("Content");
    replayDefault();
    assertApiError("invalid_reference",
        () -> resolver.resolve("Content", " Content.MyPage ", null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsNoncanonicalNodeSpace() {
    replayDefault();
    assertApiError("invalid_reference", () -> resolver.resolve(" Content ", null, null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsMalformedReference() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("[", SpaceReference.class, wikiRef))
        .andThrow(new IllegalArgumentException("malformed"));
    replayDefault();
    assertApiError("invalid_reference", () -> resolver.resolve("[", null, null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_doesNotMisclassifyUnexpectedNullPointerException() {
    expect(modelContext.getWikiRef()).andThrow(new NullPointerException("backend failure"));
    replayDefault();
    var exception = assertThrows(NullPointerException.class,
        () -> resolver.resolve("Content", null, null, null, 0));
    verifyDefault();
    assertEquals("backend failure", exception.getMessage());
  }

  @Test
  public void test_resolve_doesNotMisclassifyModelUtilsNullPointerException() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("Content", SpaceReference.class, wikiRef)).andReturn(spaceRef);
    expect(modelUtils.serializeRefLocal(spaceRef))
        .andThrow(new NullPointerException("serialization failure"));
    replayDefault();
    var exception = assertThrows(NullPointerException.class,
        () -> resolver.resolve("Content", null, null, null, 0));
    verifyDefault();
    assertEquals("serialization failure", exception.getMessage());
  }

  @Test
  public void test_resolve_rejectsNullParseResult() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("Content", SpaceReference.class, wikiRef)).andReturn(null);
    replayDefault();
    assertApiError("invalid_reference", () -> resolver.resolve("Content", null, null, null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsUnsupportedAndInvalidLanguage() {
    expectCanonicalSpace("Content");
    expect(modelUtils.normalizeLang("it")).andReturn("it");
    expect(webUtilsService.getAllowedLanguages(spaceRef)).andReturn(List.of("en", "de"));
    replayDefault();
    assertApiError("unsupported_language", () -> resolver.resolve("Content", null, "it", null, 0));
    verifyDefault();
    resetDefault();
    expectCanonicalSpace("Content");
    expect(modelUtils.normalizeLang("invalid"))
        .andThrow(new IllegalArgumentException("invalid language"));
    replayDefault();
    assertApiError("unsupported_language",
        () -> resolver.resolve("Content", null, "invalid", null, 0));
    verifyDefault();
  }

  @Test
  public void test_resolve_rejectsInactiveLevelsOutsideRange() {
    replayDefault();
    assertApiError("invalid_parameter", () -> resolver.resolve("Content", null, null, null, -1));
    assertApiError("invalid_parameter", () -> resolver.resolve("Content", null, null, null, 101));
    verifyDefault();
  }

  private void expectCanonicalSpace(String serialized) {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef(serialized, SpaceReference.class, wikiRef)).andReturn(spaceRef);
    expect(modelUtils.serializeRefLocal(spaceRef)).andReturn(serialized);
  }

  private void expectCanonicalDocument(String serialized, DocumentReference documentRef) {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef(serialized, DocumentReference.class, wikiRef))
        .andReturn(documentRef);
    expect(modelUtils.serializeRefLocal(documentRef)).andReturn(serialized);
  }

  private void assertApiError(String code, Runnable invocation) {
    var exception = assertThrows(NavigationApiException.class, invocation::run);
    assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    assertEquals(code, exception.code());
  }

}
