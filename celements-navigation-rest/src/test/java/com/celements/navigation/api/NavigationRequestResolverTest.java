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

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.web.service.IWebUtilsService;

public class NavigationRequestResolverTest {

  private final WikiReference wikiRef = new WikiReference("xwiki");
  private final SpaceReference spaceRef = new SpaceReference("Content", wikiRef);
  private ModelUtils modelUtils;
  private ModelContext modelContext;
  private IWebUtilsService webUtilsService;
  private NavigationRequestResolver resolver;

  @Before
  public void prepare() {
    modelUtils = createMock(ModelUtils.class);
    modelContext = createMock(ModelContext.class);
    webUtilsService = createMock(IWebUtilsService.class);
    resolver = new NavigationRequestResolver(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_acceptsCanonicalLocalReferencesAndRequestedLanguage() {
    var currentRef = new DocumentReference("MyPage", spaceRef);
    expectCanonicalSpace("Content");
    expectCanonicalDocument("Content.MyPage", currentRef);
    expect(modelUtils.normalizeLang("DE")).andReturn("de");
    expect(webUtilsService.getAllowedLanguages(spaceRef)).andReturn(List.of("en", "de"));
    replay(modelUtils, modelContext, webUtilsService);
    var request = resolver.resolve("Content", "Content.MyPage", "DE", "main", 2);
    verify(modelUtils, modelContext, webUtilsService);
    assertEquals(spaceRef, request.nodeSpace());
    assertEquals(Optional.of(currentRef), request.currentNode());
    assertEquals("de", request.language());
    assertEquals(Optional.of("main"), request.partName());
    assertEquals(2, request.showInactiveToLevel());
  }

  @Test
  public void resolve_defaultsLanguageFromCurrentRequestAndNormalizesBlankPart() {
    expectCanonicalSpace("Content");
    expect(modelContext.getLanguage()).andReturn(Optional.of("fr"));
    replay(modelUtils, modelContext, webUtilsService);
    var request = resolver.resolve("Content", null, null, "  ", 0);
    verify(modelUtils, modelContext, webUtilsService);
    assertEquals("fr", request.language());
    assertEquals(Optional.empty(), request.partName());
    assertEquals(Optional.empty(), request.serializedCurrentNode());
  }

  @Test
  public void resolve_defaultsLanguageFromWikiWhenRequestLanguageIsAbsent() {
    expectCanonicalSpace("Content");
    expect(modelContext.getLanguage()).andReturn(Optional.empty());
    expect(modelContext.getDefaultLanguage()).andReturn("en");
    replay(modelUtils, modelContext, webUtilsService);
    assertEquals("en", resolver.resolve("Content", null, null, null, 0).language());
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsCurrentWikiQualifiedSpace() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("xwiki:Content", SpaceReference.class, wikiRef))
        .andReturn(spaceRef);
    expect(modelUtils.serializeRefLocal(spaceRef)).andReturn("Content");
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference",
        () -> resolver.resolve("xwiki:Content", null, null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsOtherWikiQualifiedCurrentNode() {
    var otherRef = new DocumentReference("MyPage",
        new SpaceReference("Content", new WikiReference("other")));
    expectCanonicalSpace("Content");
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("other:Content.MyPage", DocumentReference.class, wikiRef))
        .andReturn(otherRef);
    expect(modelUtils.serializeRefLocal(otherRef)).andReturn("Content.MyPage");
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference",
        () -> resolver.resolve("Content", "other:Content.MyPage", null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsCurrentWikiQualifiedCurrentNode() {
    var currentRef = new DocumentReference("MyPage", spaceRef);
    expectCanonicalSpace("Content");
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("xwiki:Content.MyPage", DocumentReference.class, wikiRef))
        .andReturn(currentRef);
    expect(modelUtils.serializeRefLocal(currentRef)).andReturn("Content.MyPage");
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference",
        () -> resolver.resolve("Content", "xwiki:Content.MyPage", null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsBlankAndNoncanonicalCurrentNode() {
    expectCanonicalSpace("Content");
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference", () -> resolver.resolve("Content", " ", null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
    reset(modelUtils, modelContext, webUtilsService);
    expectCanonicalSpace("Content");
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference",
        () -> resolver.resolve("Content", " Content.MyPage ", null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsNoncanonicalNodeSpace() {
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference", () -> resolver.resolve(" Content ", null, null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsMalformedReference() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("[", SpaceReference.class, wikiRef))
        .andThrow(new IllegalArgumentException("malformed"));
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference", () -> resolver.resolve("[", null, null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_doesNotMisclassifyUnexpectedNullPointerException() {
    expect(modelContext.getWikiRef()).andThrow(new NullPointerException("backend failure"));
    replay(modelUtils, modelContext, webUtilsService);
    var exception = assertThrows(NullPointerException.class,
        () -> resolver.resolve("Content", null, null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
    assertEquals("backend failure", exception.getMessage());
  }

  @Test
  public void resolve_doesNotMisclassifyModelUtilsNullPointerException() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("Content", SpaceReference.class, wikiRef)).andReturn(spaceRef);
    expect(modelUtils.serializeRefLocal(spaceRef))
        .andThrow(new NullPointerException("serialization failure"));
    replay(modelUtils, modelContext, webUtilsService);
    var exception = assertThrows(NullPointerException.class,
        () -> resolver.resolve("Content", null, null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
    assertEquals("serialization failure", exception.getMessage());
  }

  @Test
  public void resolve_rejectsNullParseResult() {
    expect(modelContext.getWikiRef()).andReturn(wikiRef);
    expect(modelUtils.resolveRef("Content", SpaceReference.class, wikiRef)).andReturn(null);
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_reference", () -> resolver.resolve("Content", null, null, null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsUnsupportedAndInvalidLanguage() {
    expectCanonicalSpace("Content");
    expect(modelUtils.normalizeLang("it")).andReturn("it");
    expect(webUtilsService.getAllowedLanguages(spaceRef)).andReturn(List.of("en", "de"));
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("unsupported_language", () -> resolver.resolve("Content", null, "it", null, 0));
    verify(modelUtils, modelContext, webUtilsService);
    reset(modelUtils, modelContext, webUtilsService);
    expectCanonicalSpace("Content");
    expect(modelUtils.normalizeLang("invalid"))
        .andThrow(new IllegalArgumentException("invalid language"));
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("unsupported_language",
        () -> resolver.resolve("Content", null, "invalid", null, 0));
    verify(modelUtils, modelContext, webUtilsService);
  }

  @Test
  public void resolve_rejectsInactiveLevelsOutsideRange() {
    replay(modelUtils, modelContext, webUtilsService);
    assertApiError("invalid_parameter", () -> resolver.resolve("Content", null, null, null, -1));
    assertApiError("invalid_parameter", () -> resolver.resolve("Content", null, null, null, 101));
    verify(modelUtils, modelContext, webUtilsService);
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
