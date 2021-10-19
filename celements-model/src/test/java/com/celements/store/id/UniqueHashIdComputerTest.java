package com.celements.store.id;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.store.id.UniqueHashIdComputer.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class UniqueHashIdComputerTest extends AbstractComponentTest {

  private UniqueHashIdComputer idComputer;
  private DocumentReference docRef;
  private String lang;

  @Before
  public void prepareTest() {
    idComputer = (UniqueHashIdComputer) Utils.getComponent(CelementsIdComputer.class,
        UniqueHashIdComputer.NAME);
    docRef = new DocumentReference("db", "space", "page");
    lang = "es";
  }

  @Test
  public void test_getIdVersion() {
    assertSame(IdVersion.CELEMENTS_3, idComputer.getIdVersion());
  }

  @Test
  public void test_andifyRight() {
    long base = 0L;
    assertEquals(0x0000000000000000L, idComputer.andifyRight(base, (byte) 0));
    assertEquals(0x0000000000000001L, idComputer.andifyRight(base, (byte) 1));
    assertEquals(0x000000000000000fL, idComputer.andifyRight(base, (byte) 4));
    assertEquals(0x00000000ffffffffL, idComputer.andifyRight(base, (byte) 32));
    assertEquals(0x0fffffffffffffffL, idComputer.andifyRight(base, (byte) 60));
    assertEquals(0x7fffffffffffffffL, idComputer.andifyRight(base, (byte) 63));
  }

  @Test
  public void test_andifyLeft() {
    long base = 0L;
    assertEquals(0x0000000000000000L, idComputer.andifyLeft(base, (byte) 0));
    assertEquals(0x8000000000000000L, idComputer.andifyLeft(base, (byte) 1));
    assertEquals(0xf000000000000000L, idComputer.andifyLeft(base, (byte) 4));
    assertEquals(0xffffffff00000000L, idComputer.andifyLeft(base, (byte) 32));
    assertEquals(0xfffffffffffffff0L, idComputer.andifyLeft(base, (byte) 60));
    assertEquals(0xfffffffffffffffeL, idComputer.andifyLeft(base, (byte) 63));
  }

  @Test
  public void test_serializeLocalUid() {
    assertEquals("5:space4:page2:es", idComputer.serializeLocalUid(docRef, lang));
  }

  @Test
  public void test_hashMD5() throws Exception {
    assertEquals(0xf0da7f3f8545ded5L, idComputer.hashMD5(idComputer.serializeLocalUid(docRef,
        lang)));
  }

  @Test
  public void test_computeId_collisionCount() throws Exception {
    // full md5: 0xf0da7f3f8545ded5L
    assertEquals(0xf0da7f3f8545ced5L, idComputer.computeId(docRef, lang, (byte) 0b00, 0xed5));
    assertEquals(0xf0da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b01, 0xed5));
    assertEquals(0xf0da7f3f8545eed5L, idComputer.computeId(docRef, lang, (byte) 0b10, 0xed5));
    assertEquals(0xf0da7f3f8545fed5L, idComputer.computeId(docRef, lang, (byte) 0b11, 0xed5));
  }

  @Test
  public void test_computeId_collisionCount_negative() throws Exception {
    Throwable cause = assertThrows(IdComputationException.class,
        () -> idComputer.computeId(docRef, lang, (byte) -1, 0))
            .getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("negative"));
  }

  @Test
  public void test_computeId_collisionCount_overflow() throws Exception {
    Throwable cause = assertThrows(IdComputationException.class,
        () -> idComputer.computeId(docRef, lang, (byte) 0b100, 0))
            .getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("outside of defined range"));
  }

  @Test
  public void test_computeId_objectCount() throws Exception {
    // full md5: 0xf0da7f3f8545ded5L
    assertEquals(0xf0da7f3f8545d000L, idComputer.computeId(docRef, lang, (byte) 0b01, 0));
    assertEquals(0xf0da7f3f8545d005L, idComputer.computeId(docRef, lang, (byte) 0b01, 5));
    assertEquals(0xf0da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b01, 0xed5));
    assertEquals(0xf0da7f3f8545d0a0L, idComputer.computeId(docRef, lang, (byte) 0b01, 0xa0));
    assertEquals(0xf0da7f3f8545dfffL, idComputer.computeId(docRef, lang, (byte) 0b01, 0xfff));
  }

  @Test
  public void test_computeId_objectCount_negative() throws Exception {
    Throwable cause = assertThrows(IdComputationException.class,
        () -> idComputer.computeId(docRef, lang, (byte) 0, -1))
            .getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("negative"));
  }

  @Test
  public void test_computeId_objectCount_overflow() throws Exception {
    Throwable cause = assertThrows(IdComputationException.class,
        () -> idComputer.computeId(docRef, lang, (byte) 0, 1 << 12))
            .getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("outside of defined range"));
  }

  @Test
  public void test_computeId_zero() throws Exception {
    // all ids from 0 to 2^BITS_COUNTS will be shifted to 0
    long max = 1 << BITS_COUNTS;
    List<Long> zeroIds = Arrays.asList(0L, max / 4, max / 2, (max / 4) * 3, max - 1);
    MessageDigest digestMock = createMockAndAddToDefault(MessageDigest.class);
    idComputer.injectedDigest = digestMock;
    digestMock.update(isA(byte[].class));
    expectLastCall().times(zeroIds.size());
    for (long id : zeroIds) {
      expect(digestMock.digest()).andReturn(Longs.toByteArray(id)).once();
    }
    replayDefault();
    for (long id : zeroIds) {
      assertEquals("at " + id, nullCountBits(Long.MAX_VALUE - id),
          idComputer.computeId(docRef, lang, (byte) 0, 0));
    }
    verifyDefault();
  }

  private long nullCountBits(long id) {
    return (id >>> BITS_COUNTS) << BITS_COUNTS;
  }

  @Test
  public void test_computeId_illegalId_XWIKI_2() throws Exception {
    List<Long> illegalIds = Arrays.asList(123456L, (1L << BITS_COUNTS), -1L, -123456L,
        (long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE);
    MessageDigest digestMock = createMockAndAddToDefault(MessageDigest.class);
    idComputer.injectedDigest = digestMock;
    digestMock.update(isA(byte[].class));
    expectLastCall().times(illegalIds.size());
    for (long id : illegalIds) {
      expect(digestMock.digest()).andReturn(Longs.toByteArray(id)).once();
    }
    replayDefault();
    for (int i = 0; i < illegalIds.size(); i++) {
      final int objCount = i;
      Throwable cause = assertThrows(IdComputationException.class,
          () -> idComputer.computeId(docRef, lang, (byte) 0, objCount))
              .getCause();
      assertSame(VerifyException.class, cause.getClass());
      assertTrue(cause.getMessage(), cause.getMessage().contains(IdVersion.XWIKI_2.name()));
    }
    verifyDefault();
  }

  @Test
  public void test_computeDocumentId() throws Exception {
    assertEquals(0xf0da7f3f8545c000L, idComputer.computeDocumentId(docRef, lang));
    assertEquals(0xf0da7f3f8545d000L, idComputer.computeDocumentId(docRef, lang, (byte) 0b01));
  }

  @Test
  public void test_computeDocumentId_docRef() throws Exception {
    long exp = 0xf0da7f3f8545c000L;
    assertEquals(exp, idComputer.computeDocumentId(docRef, lang));
    docRef.getWikiReference().setName("asdf");
    assertEquals(exp, idComputer.computeDocumentId(docRef, lang));
    docRef.setName("asdf");
    assertNotEquals(exp, idComputer.computeDocumentId(docRef, lang));
    assertThrows(NullPointerException.class,
        () -> idComputer.computeDocumentId(null, lang));
  }

  @Test
  public void test_computeDocumentId_lang() throws Exception {
    long exp = 0x6ec3dd404a0a0000L;
    assertEquals(exp, idComputer.computeDocumentId(docRef, ""));
    assertEquals(exp, idComputer.computeDocumentId(docRef, " "));
    assertEquals(exp, idComputer.computeDocumentId(docRef, null));
    assertNotEquals(exp, idComputer.computeDocumentId(docRef, "de"));
  }

  @Test
  public void test_computeMaxDocumentId() throws Exception {
    assertEquals(0xf0da7f3f8545f000L, idComputer.computeMaxDocumentId(docRef, lang));
  }

  @Test
  public void test_computeDocumentId_doc() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    assertEquals(0xf0da7f3f8545c000L, idComputer.computeDocumentId(doc));
  }

  @Test
  public void test_getDocumentIdIterator() throws Exception {
    assertEquals(ImmutableList.of(
        0xf0da7f3f8545c000L,
        0xf0da7f3f8545d000L,
        0xf0da7f3f8545e000L,
        0xf0da7f3f8545f000L),
        ImmutableList.copyOf(idComputer.getDocumentIdIterator(docRef, lang)));
  }

  @Test
  public void test_computeNextObjectId_noObj() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    long docId = 0xf0da7f3f8545c000L;
    assertEquals(docId + 1, idComputer.computeNextObjectId(doc));
    assertEquals(docId + 1, idComputer.computeNextObjectId(doc));
  }

  @Test
  public void test_computeNextObjectId() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    long docId = 0xf0da7f3f8545c000L;
    assertEquals(docId + 1, addObjWithComputedId(doc).getId());
    assertEquals(docId + 2, addObjWithComputedId(doc).getId());
    assertEquals(docId + 3, addObjWithComputedId(doc).getId());
    assertEquals(docId + 4, addObjWithComputedId(doc).getId());
  }

  @Test
  public void test_computeNextObjectId_keepRemovedObjectsId() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    long docId = 0xf0da7f3f8545c000L;
    BaseObject removeObj;
    assertEquals(docId + 1, addObjWithComputedId(doc).getId());
    assertEquals(docId + 2, (removeObj = addObjWithComputedId(doc)).getId());
    assertEquals(docId + 3, addObjWithComputedId(doc).getId());
    doc.removeXObject(removeObj);
    assertEquals(docId + 4, addObjWithComputedId(doc).getId());
  }

  @Test
  public void test_computeNextObjectId_fillDeletedObjects() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    long docId = 0xf0da7f3f8545c000L;
    BaseObject removeObj;
    assertEquals(docId + 1, addObjWithComputedId(doc).getId());
    assertEquals(docId + 2, (removeObj = addObjWithComputedId(doc)).getId());
    assertEquals(docId + 3, addObjWithComputedId(doc).getId());
    doc.removeXObject(removeObj);
    doc.getXObjectsToRemove().clear();
    assertEquals(docId + 2, addObjWithComputedId(doc).getId());
    assertEquals(docId + 4, addObjWithComputedId(doc).getId());
  }

  @Test
  public void test_computeNextObjectId_ignoreOtherIdVersion() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    long docId = 0xf0da7f3f8545c000L;
    assertEquals(1, addObj(doc, 1, IdVersion.XWIKI_2).getId());
    assertEquals(docId + 1, addObjWithComputedId(doc).getId());
    assertEquals(2, addObj(doc, 2, IdVersion.XWIKI_2).getId());
    assertEquals(docId + 2, addObjWithComputedId(doc).getId());
  }

  private BaseObject addObjWithComputedId(XWikiDocument doc) throws IdComputationException {
    return addObj(doc, idComputer.computeNextObjectId(doc), idComputer.getIdVersion());
  }

  private BaseObject addObj(XWikiDocument doc, long id, IdVersion idVersion)
      throws IdComputationException {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(docRef);
    doc.addXObject(obj);
    obj.setId(id, idVersion);
    return obj;
  }

  @Test
  public void test_getMaxCollisionCount() throws Exception {
    assertEquals("only 2 collision bits", 3, idComputer.getMaxCollisionCount());
  }

  @Test
  public void test_extractCollisionCount() throws Exception {
    byte max = idComputer.getMaxCollisionCount();
    for (byte collisionCount = 0; collisionCount <= max; collisionCount++) {
      long docId = idComputer.computeDocumentId(docRef, lang, collisionCount);
      assertEquals(collisionCount, idComputer.extractCollisionCount(docId));
    }
  }

  @Test
  public void test_computeId_extractCollisionCount() throws Exception {
    byte max = idComputer.getMaxCollisionCount();
    for (byte collisionCount = 0; collisionCount <= max; collisionCount++) {
      XWikiDocument doc = new XWikiDocument(docRef);
      doc.setLanguage(lang);
      doc.setId(idComputer.computeDocumentId(docRef, lang, collisionCount), IdVersion.CELEMENTS_3);
      long docId = idComputer.computeId(doc, 0);
      assertEquals(collisionCount, idComputer.extractCollisionCount(docId));
    }
  }

}
