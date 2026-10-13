package com.celements.init;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/** Real Package parsing/install with a mocked save boundary: S1 evidence, not store acceptance. */
public class DiskDocumentImporterTest extends AbstractComponentTest {

  private final WikiReference wiki = new WikiReference("test");
  private final Map<String, String> resources = new HashMap<>();
  private final Map<String, Set<String>> directories = new HashMap<>();
  private final List<String> opened = new ArrayList<>();
  private final List<XWikiDocument> saved = new ArrayList<>();
  private DiskDocumentImporter importer;
  private ServletContext servlet;
  private WikiService wikis;
  private IModelAccessFacade access;
  private Runnable onXmlRead = () -> {};
  private boolean translationExists;

  @Before
  public void prepareImporter() throws Exception {
    servlet = createNiceMock(ServletContext.class);
    expect(servlet.getResourcePaths(anyString())).andAnswer(() -> {
      String path = getCurrentArgument(0);
      return directories.get(path.endsWith("/") ? path : path + "/");
    }).anyTimes();
    expect(servlet.getResource(anyString())).andAnswer(() -> resources.containsKey(
        getCurrentArgument(0)) ? new URL("file:/present") : null).anyTimes();
    expect(servlet.getResourceAsStream(anyString())).andAnswer(() -> {
      String path = getCurrentArgument(0);
      opened.add(path);
      if (path.endsWith(".xml")) {
        onXmlRead.run();
      }
      return new ByteArrayInputStream(resources.get(path).getBytes(StandardCharsets.UTF_8));
    }).anyTimes();
    wikis = createNiceMock(WikiService.class);
    expect(wikis.hasWiki(anyObject(WikiReference.class))).andReturn(true).anyTimes();
    expect(wikis.streamAllWikis()).andAnswer(() -> Stream.of(wiki)).anyTimes();
    access = createNiceMock(IModelAccessFacade.class);
    expect(access.existsLang(anyObject(DocumentReference.class), anyString())).andAnswer(
        () -> !((DocumentReference) getCurrentArgument(0)).getName().equals("Missing")
            && (translationExists || !getCurrentArgument(1).equals("fr"))).anyTimes();
    XWikiRightService rights = createNiceMock(XWikiRightService.class);
    expect(rights.hasAdminRights(anyObject())).andReturn(true).anyTimes();
    expect(getWikiMock().getRightService()).andReturn(rights).anyTimes();
    XWikiStoreInterface store = createNiceMock(XWikiStoreInterface.class);
    expect(store.getTranslationList(anyObject(), same(getContext())))
        .andReturn(List.of()).anyTimes();
    expect(getWikiMock().getStore()).andReturn(store).anyTimes();
    expect(getWikiMock().getDefaultLanguage(same(getContext()))).andReturn("de").anyTimes();
    expect(getWikiMock().getXWikiPreference(eq("documentBundles"), same(getContext())))
        .andReturn("").anyTimes();
    expect(getWikiMock().Param("xwiki.documentBundles")).andReturn("").anyTimes();
    expect(getWikiMock().getDocument(anyString(), same(getContext()))).andAnswer(() -> {
      var doc = new XWikiDocument("Main", "Existing");
      doc.setNew(false);
      return doc;
    }).anyTimes();
    expect(getWikiMock().getVersioningStore()).andReturn(null).anyTimes();
    expect(getWikiMock().hasVersioning(same(getContext()))).andReturn(false).anyTimes();
    getWikiMock().saveDocument(anyObject(XWikiDocument.class), anyString(), same(getContext()));
    expectLastCall().andAnswer(() -> {
      saved.add(getCurrentArgument(0));
      return null;
    }).anyTimes();
    ObservationManager observations = registerComponentMock(ObservationManager.class);
    observations.notify(anyObject(), anyObject(), anyObject());
    expectLastCall().anyTimes();
    replay(servlet, wikis, access, rights, store);
    replayDefault();
    var modelUtils = Utils.getComponent(ModelUtils.class);
    importer = new DiskDocumentImporter(servlet, Utils.getComponent(Execution.class), wikis,
        modelUtils, access, new JsonConfigOverwriteFromDisk(servlet, modelUtils));
    directories.put("/docs/", Set.of("/docs/test/", "/docs/test.json"));
    directories.put("/docs/test/", Set.of("/docs/test/listed.xml", "/docs/test/control.xml",
        "/docs/test/missing.xml", "/docs/test/ignored.json"));
    resources.put("/docs/test/listed.xml", xml("Listed", ""));
    resources.put("/docs/test/control.xml", xml("Control", ""));
    resources.put("/docs/test/missing.xml", xml("Missing", ""));
    resources.put("/docs/test/ignored.json", "not XML and must never be opened");
  }

  @Test
  public void existingListedAndMissingInstallButExistingControlSkips() {
    resources.put("/docs/test.json", "[\"Main.Listed\"]");
    importer.importForWiki(wiki, false);
    assertEquals(Set.of("Listed", "Missing"), names());
    assertFalse(opened.contains("/docs/test/ignored.json"));
    assertEquals(1, opened.stream().filter("/docs/test.json"::equals).count());
  }

  @Test
  public void forceInstallsAllAndMissingPolicyPreservesNormalBehavior() {
    importer.importForWiki(wiki, false);
    assertEquals(Set.of("Missing"), names());
    saved.clear();
    importer.importForWiki(wiki, true);
    assertEquals(Set.of("Listed", "Control", "Missing"), names());
  }

  @Test
  public void invalidPolicyPreventsAnyPackageReadsEvenWhenForced() {
    resources.put("/docs/test.json", "[\"Main.Listed\",null]");
    assertThrows(IllegalArgumentException.class, () -> importer.importForWiki(wiki, true));
    assertEquals(List.of("/docs/test.json"), opened);
    assertTrue(saved.isEmpty());
  }

  @Test
  public void validatesEvenWithNoInstallationPaths() {
    directories.clear();
    resources.put("/docs/test.json", "{}");
    assertThrows(IllegalArgumentException.class, () -> importer.importForWiki(wiki, true));
    assertTrue(saved.isEmpty());
  }

  @Test
  public void allPathUsesTargetWikiSnapshotAndNextInvocationReloads() {
    directories.put("/docs/", Set.of("/docs/all/", "/docs/test/", "/docs/unused.json"));
    directories.put("/docs/all/", Set.of("/docs/all/control.xml"));
    resources.put("/docs/all/control.xml", xml("Control", ""));
    resources.put("/docs/test.json", "[\"Main.Listed\"]");
    onXmlRead = () -> resources.put("/docs/test.json", "[\"Main.Control\"]");
    importer.importForWiki(wiki, false);
    assertEquals(Set.of("Listed", "Missing"), names());
    assertEquals(1, opened.stream().filter("/docs/test.json"::equals).count());
    saved.clear();
    importer.importForWiki(wiki, false);
    assertEquals(Set.of("Control", "Missing"), names());
    assertEquals(2, opened.stream().filter("/docs/test.json"::equals).count());
  }

  @Test
  public void missingTranslationInstallsWithoutAllowlist() {
    directories.put("/docs/test/", Set.of("/docs/test/translation.xml"));
    resources.put("/docs/test/translation.xml", xml("Listed", "fr"));
    importer.importForWiki(wiki, false);
    assertEquals(1, saved.size());
    assertEquals("fr", saved.get(0).getLanguage());
  }

  @Test
  public void existingTranslationRequiresSelection() {
    translationExists = true;
    directories.put("/docs/test/", Set.of("/docs/test/translation.xml"));
    resources.put("/docs/test/translation.xml", xml("Listed", "fr"));
    importer.importForWiki(wiki, false);
    assertTrue(saved.isEmpty());

    resources.put("/docs/test.json", "[\"Main.Listed\"]");
    importer.importForWiki(wiki, false);
    assertEquals(1, saved.size());
    assertEquals("fr", saved.get(0).getLanguage());
  }

  @Test
  public void jsonAloneDoesNotDiscoverWikiOrInstallUnusedSelection() {
    directories.put("/docs/", Set.of("/docs/unused.json"));
    resources.put("/docs/unused.json", "malformed but no wiki discovered from this file");
    resources.put("/docs/test.json", "[\"Main.Unused\"]");
    importer.importForWiki(wiki, false);
    assertTrue(saved.isEmpty());
    assertEquals(List.of("/docs/test.json"), opened);
  }

  @Test
  public void springCreatesImporterWithConcreteJsonPolicy() throws Exception {
    registerComponentMock(ServletContext.class, "default", servlet);
    registerComponentMock(WikiService.class, "default", wikis);
    registerComponentMock(IModelAccessFacade.class, "default", access);
    assertNotNull(getSpringContext().getBean(DiskDocumentImporter.class));
    assertNotNull(getSpringContext().getBean(JsonConfigOverwriteFromDisk.class));
  }

  private Set<String> names() {
    return saved.stream().map(XWikiDocument::getName).collect(Collectors.toSet());
  }

  private String xml(String name, String language) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <xwikidoc><web>Main</web><name>%s</name><language>%s</language>
        <translation>%s</translation><content>from disk</content><title>Disk title</title>
        <creator>XWiki.Creator</creator><author>XWiki.Author</author>
        <contentAuthor>XWiki.Author</contentAuthor><version>1.1</version></xwikidoc>
        """.formatted(name, language, language.isEmpty() ? "0" : "1");
  }
}
