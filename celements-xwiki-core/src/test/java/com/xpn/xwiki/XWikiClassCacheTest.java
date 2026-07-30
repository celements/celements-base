package com.xpn.xwiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiClassCacheTest extends AbstractComponentTest {

  @Test
  public void test_getXClass_fromCelDocumentAndContextCache() throws Exception {
    DocumentReference classReference = new DocumentReference("wiki", "space", "class");
    XWikiDocument document = new XWikiDocument(classReference);
    document.getXClass().addTextField("field", "Field", 42);
    CelDocument.Default celDocument = CelDocument.Default.from(document);
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    expect(store.loadCelDocument(classReference)).andReturn(Optional.of(celDocument));
    XWiki xwiki = new XWiki();
    xwiki.setStore(store);
    getContext().setWiki(xwiki);
    replayDefault();

    BaseClass first = xwiki.getXClass(classReference, getContext());
    BaseClass second = xwiki.getXClass(classReference, getContext());

    assertSame(first, second);
    assertNotSame(document.getXClass(), first);
    assertEquals(42, ((StringClass) first.safeget("field")).getSize());
    verifyDefault();
  }
}
