package com.xpn.xwiki.doc;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.easymock.Capture;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiDocumentTryLockTest extends AbstractComponentTest {

  @Test
  public void test_tryLock_usesDocumentWiki() throws Exception {
    WikiReference callerWiki = getContext().getWikiRef();
    DocumentReference docRef = new DocumentReference("other-wiki", "Space", "Page");
    assertNotEquals(callerWiki, docRef.getWikiReference());
    XWikiDocument document = new XWikiDocument(docRef);
    document.setId(42L, IdVersion.CELEMENTS_3);
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    Capture<XWikiLock> reservation = newCapture();
    expect(getWikiMock().getStore()).andReturn(store);
    expect(store.tryLock(capture(reservation), eq(docRef.getWikiReference()))).andReturn(true);

    replayDefault();
    assertTrue(document.tryLock("XWiki.user"));
    assertEquals(42L, reservation.getValue().getDocId());
    assertEquals("XWiki.user", reservation.getValue().getUserName());
    assertEquals(callerWiki, getContext().getWikiRef());
    verifyDefault();
  }
}
