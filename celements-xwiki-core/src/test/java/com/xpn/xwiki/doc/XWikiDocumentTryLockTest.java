package com.xpn.xwiki.doc;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiDocumentTryLockTest extends AbstractComponentTest {

  private static final WikiReference DOCUMENT_WIKI = new WikiReference("other-wiki");

  private XWikiDocument document;
  private XWikiStoreInterface store;

  @Before
  public void prepareTest() throws Exception {
    document = new XWikiDocument(new DocumentReference("Page",
        new SpaceReference("Space", DOCUMENT_WIKI)));
    document.setId(42L, IdVersion.CELEMENTS_3);
    store = createDefaultMock(XWikiStoreInterface.class);
    expect(getWikiMock().getStore()).andReturn(store).anyTimes();
  }

  @Test
  public void test_tryLock_usesDocumentWikiAndTimeout() throws Exception {
    WikiReference callerWiki = getContext().getWikiRef();
    assertNotEquals(callerWiki, DOCUMENT_WIKI);
    Capture<XWikiLock> reservation = newCapture();
    Capture<Date> cutoff = newCapture();
    expect(getWikiMock().getXWikiPreferenceAsInt(DOCUMENT_WIKI, "lock_Timeout", 30 * 60))
        .andReturn(120);
    expect(store.tryLock(capture(reservation), capture(cutoff))).andReturn(true);

    replayDefault();
    long before = System.currentTimeMillis();
    assertTrue(document.tryLock("XWiki.user"));
    long after = System.currentTimeMillis();
    assertEquals(42L, reservation.getValue().getDocId());
    assertEquals("XWiki.user", reservation.getValue().getUserName());
    assertEquals(DOCUMENT_WIKI, reservation.getValue().getWikiRef().orElseThrow());
    assertTrue(cutoff.getValue().getTime() >= before - 120_000L);
    assertTrue(cutoff.getValue().getTime() <= after - 120_000L);
    assertEquals(callerWiki, getContext().getWikiRef());
    verifyDefault();
  }

  @Test
  public void test_tryLock_usesProviderForStoreAndExpiryPolicy() throws Exception {
    XWiki providedWiki = getWikiMock();
    XWiki legacyWiki = createDefaultMock(XWiki.class);
    getContext().setWiki(legacyWiki);
    expect(providedWiki.getXWikiPreferenceAsInt(DOCUMENT_WIKI, "lock_Timeout", 30 * 60))
        .andReturn(120);
    expect(store.tryLock(anyObject(XWikiLock.class), anyObject(Date.class))).andReturn(true);
    replayDefault();
    assertTrue(document.tryLock("XWiki.user"));
    verifyDefault();
  }

  @Test
  public void test_getLock_expiredLockIsHiddenWithoutDeletion() throws Exception {
    getContext().setDatabase(DOCUMENT_WIKI.getName());
    XWikiLock expired = new XWikiLock(42L, "alice");
    expired.setDate(new Date(System.currentTimeMillis() - 180_000L));
    expect(store.loadLock(42L, getContext(), true)).andReturn(expired);
    expect(getWikiMock().getXWikiPreferenceAsInt(DOCUMENT_WIKI, "lock_Timeout", 30 * 60))
        .andReturn(120);
    replayDefault();
    assertNull(document.getLock(getContext()));
    verifyDefault();
  }

  @Test
  public void test_getLock_activeLockIsReturned() throws Exception {
    getContext().setDatabase(DOCUMENT_WIKI.getName());
    XWikiLock active = new XWikiLock(42L, "alice");
    expect(store.loadLock(42L, getContext(), true)).andReturn(active);
    expect(getWikiMock().getXWikiPreferenceAsInt(DOCUMENT_WIKI, "lock_Timeout", 30 * 60))
        .andReturn(120);
    replayDefault();
    assertSame(active, document.getLock(getContext()));
    verifyDefault();
  }

  @Test
  public void test_getLock_noReservation() throws Exception {
    getContext().setDatabase(DOCUMENT_WIKI.getName());
    expect(store.loadLock(42L, getContext(), true)).andReturn(null);
    replayDefault();
    assertNull(document.getLock(getContext()));
    verifyDefault();
  }
}
