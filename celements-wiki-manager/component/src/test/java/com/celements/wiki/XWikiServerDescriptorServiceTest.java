package com.celements.wiki;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.wiki.classes.XWikiServerClass.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiDescriptor.State;
import com.celements.wiki.WikiDescriptor.Visibility;
import com.celements.wiki.classes.XWikiServerClass;
import com.celements.wiki.exception.WikiDescriptorException;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class XWikiServerDescriptorServiceTest extends AbstractComponentTest {

  private XWikiServerClass classDef;
  private XWikiServerDescriptorService service;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(ModelUtils.class);
    registerComponentMock(IModelAccessFacade.class);
    registerComponentMock(QueryWikiService.class);
    registerComponentMock(XWikiConfigSource.class);
    classDef = getBeanFactory().getBean(XWikiServerClass.class);
    service = getBeanFactory().getBean(XWikiServerDescriptorService.class);
  }

  @Test
  public void test_loadingService() {
    replayDefault();
    assertNotNull(service);
    verifyDefault();
  }

  @Test
  public void test_getDescriptorDocRef_mainWiki() {
    expect(getMock(ModelUtils.class).getMainWikiRef()).andReturn(new WikiReference("unkownMain"));
    expect(getMock(ModelUtils.class).normalizeWikiRef(eq(XWikiConstant.MAIN_WIKI)))
        .andReturn(new WikiReference("main"));

    replayDefault();
    DocumentReference configDocRef = service.getDescriptorDocRef(XWikiConstant.MAIN_WIKI);
    verifyDefault();

    assertEquals("XWikiServerMain", configDocRef.getName());
  }

  @Test
  public void test_createDescriptor() throws Exception {
    WikiReference wikiRef = new WikiReference("mywiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    XWikiDocument doc = new XWikiDocument(docRef);
    expectClass(classDef, docRef.getWikiReference());
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(docRef)).andReturn(doc);
    expect(getMock(XWikiConfigSource.class).getProperty("xwiki.url.protocol", ""))
        .andReturn("https");
    getMock(IModelAccessFacade.class).saveDocument(same(doc),
        eq("createDescriptor mywiki.example"));

    replayDefault();
    service.createDescriptor(wikiRef, "mywiki.example");
    verifyDefault();

    XWikiObjectFetcher fetcher = XWikiObjectFetcher.on(doc).filter(CLASS_REF);
    assertEquals(1, fetcher.count());
    assertEquals("mywiki", fetcher.fetchField(FIELD_PRETTY_NAME).findFirst().get());
    assertEquals("mywiki.example", fetcher.fetchField(FIELD_SERVER).findFirst().get());
    assertEquals(Boolean.TRUE, fetcher.fetchField(FIELD_SECURE).findFirst().get());
    assertEquals(Visibility.PUBLIC, fetcher.fetchField(FIELD_VISIBILITY).findFirst().get());
    assertEquals(State.ACTIVE, fetcher.fetchField(FIELD_STATE).findFirst().get());
    assertEquals("en", fetcher.fetchField(FIELD_LANGUAGE).findFirst().get());
    assertEquals("Content.WebHome", fetcher.fetchField(FIELD_HOMEPAGE).findFirst().get());
    assertEquals(Boolean.FALSE, fetcher.fetchField(FIELD_IS_TEMPLATE).findFirst().get());
    assertEquals(Boolean.FALSE, fetcher.fetchField(FIELD_OICD_ACTIVE).findFirst().get());
    assertEquals("#includeForm('XWiki.XWikiServerClassSheet')", doc.getContent());
  }

  @Test
  public void test_getDescriptors_aliases() throws Exception {
    WikiReference wikiRef = new WikiReference("mywiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    XWikiDocument doc = new XWikiDocument(docRef);
    addDescriptorObj(doc, wikiRef, "mywiki.example", true, false);
    addDescriptorObj(doc, wikiRef, "alias.example", false, true);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef)).andReturn(doc);
    expect(getMock(QueryWikiService.class).toUri(1, "mywiki.example"))
        .andReturn(Optional.of(URI.create("https://mywiki.example")));
    expect(getMock(QueryWikiService.class).toUri(0, "alias.example"))
        .andReturn(Optional.of(URI.create("http://alias.example")));

    replayDefault();
    List<WikiDescriptor> ret = service.getDescriptors(wikiRef);
    verifyDefault();

    assertEquals(2, ret.size());
    assertDescriptor(ret.get(0), wikiRef, "mywiki.example", true, false,
        URI.create("https://mywiki.example"));
    assertDescriptor(ret.get(1), wikiRef, "alias.example", false, true,
        URI.create("http://alias.example"));
  }

  @Test
  public void test_isOicdEnabled_true() throws Exception {
    WikiReference wikiRef = new WikiReference("mywiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    XWikiDocument doc = new XWikiDocument(docRef);
    addDescriptorObj(doc, wikiRef, "mywiki.example", true, false);
    addDescriptorObj(doc, wikiRef, "alias.example", false, true);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef)).andReturn(doc);
    expect(getMock(QueryWikiService.class).toUri(anyInt(), anyString())).andReturn(Optional.empty())
        .times(2);

    replayDefault();
    assertTrue(service.isOicdEnabled(wikiRef));
    verifyDefault();
  }

  @Test
  public void test_isOicdEnabled_false() throws Exception {
    WikiReference wikiRef = new WikiReference("mywiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    XWikiDocument doc = new XWikiDocument(docRef);
    addDescriptorObj(doc, wikiRef, "mywiki.example", true, false);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef)).andReturn(doc);
    expect(getMock(QueryWikiService.class).toUri(anyInt(), anyString()))
        .andReturn(Optional.empty());

    replayDefault();
    assertFalse(service.isOicdEnabled(wikiRef));
    verifyDefault();
  }

  @Test
  public void test_isOicdEnabled_missingWiki() throws Exception {
    WikiReference wikiRef = new WikiReference("missingwiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef))
        .andThrow(new DocumentNotExistsException(docRef));

    replayDefault();
    assertFalse(service.isOicdEnabled(wikiRef));
    verifyDefault();
  }

  @Test
  public void test_deleteDescriptors() throws Exception {
    WikiReference wikiRef = new WikiReference("mywiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    getMock(IModelAccessFacade.class).deleteDocument(docRef, true);

    replayDefault();
    service.deleteDescriptors(wikiRef);
    verifyDefault();
  }

  @Test
  public void test_deleteDescriptors_exception() throws Exception {
    WikiReference wikiRef = new WikiReference("mywiki");
    DocumentReference docRef = expectDescriptorDocRef(wikiRef);
    getMock(IModelAccessFacade.class).deleteDocument(docRef, true);
    expectLastCall().andThrow(new DocumentDeleteException(docRef));

    replayDefault();
    WikiDescriptorException exp = assertThrows(WikiDescriptorException.class,
        () -> service.deleteDescriptors(wikiRef));
    verifyDefault();
    assertEquals(wikiRef, exp.getWikiRef());
  }

  private DocumentReference expectDescriptorDocRef(WikiReference wikiRef) {
    WikiReference mainWikiRef = new WikiReference("xwikidb");
    WikiReference normWikiRef = new WikiReference(wikiRef.getName());
    DocumentReference docRef = new DocumentReference(
        mainWikiRef.getName(), XWikiConstant.XWIKI_SPACE,
        XWikiServerDescriptorService.DOC_NAME_PREFIX
            + Character.toUpperCase(normWikiRef.getName().charAt(0))
            + normWikiRef.getName().substring(1));
    expect(getMock(ModelUtils.class).normalizeWikiRef(eq(wikiRef))).andReturn(normWikiRef);
    expect(getMock(ModelUtils.class).getMainWikiRef()).andReturn(mainWikiRef);
    return docRef;
  }

  private void addDescriptorObj(XWikiDocument doc, WikiReference wikiRef, String host,
      boolean secure, boolean oicd) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(CLASS_REF.getDocRef(doc.getDocumentReference().getWikiReference()));
    obj.setStringValue(FIELD_PRETTY_NAME.getName(), wikiRef.getName());
    obj.setStringValue(FIELD_SERVER.getName(), host);
    obj.setIntValue(FIELD_SECURE.getName(), secure ? 1 : 0);
    obj.setStringValue(FIELD_VISIBILITY.getName(), "public");
    obj.setStringValue(FIELD_STATE.getName(), "active");
    obj.setStringValue(FIELD_LANGUAGE.getName(), "en");
    obj.setStringValue(FIELD_HOMEPAGE.getName(), "Content.WebHome");
    obj.setIntValue(FIELD_IS_TEMPLATE.getName(), 0);
    obj.setIntValue(FIELD_OICD_ACTIVE.getName(), oicd ? 1 : 0);
    doc.addXObject(obj);
  }

  private void assertDescriptor(WikiDescriptor descriptor, WikiReference wikiRef, String server,
      boolean secure, boolean oicd, URI uri) {
    assertEquals(wikiRef, descriptor.wiki());
    assertEquals(wikiRef.getName(), descriptor.prettyName());
    assertEquals(server, descriptor.server());
    assertEquals(Visibility.PUBLIC, descriptor.visibility());
    assertEquals(State.ACTIVE, descriptor.state());
    assertEquals("en", descriptor.language());
    assertEquals(secure, descriptor.secure());
    assertEquals(oicd, descriptor.oicd());
    assertEquals(uri, descriptor.uri());
  }

  private BaseClass expectClass(ClassDefinition classDef, WikiReference wikiRef)
      throws XWikiException {
    BaseClass bClass = expectNewBaseObject(classDef.getDocRef(wikiRef));
    return expectPropertyClasses(bClass, classDef.getFields().stream()
        .collect(Collectors.toMap(ClassField::getName, ClassField::getXField)));
  }

}
