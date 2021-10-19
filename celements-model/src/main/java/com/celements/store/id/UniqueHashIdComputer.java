package com.celements.store.id;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Verify.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.VerifyException;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import one.util.streamex.StreamEx;

/**
 * id format: 50bit | 2bit | 12bit
 * MSB: 50bit docId md5(fullname+lang)
 * center bits: 2bit collision count
 * LSB: 12bit object count
 */
@Component(UniqueHashIdComputer.NAME)
public class UniqueHashIdComputer implements CelementsIdComputer {

  public static final String NAME = "uniqueHash";

  private static final String HASH_ALGO = "MD5";

  static final byte BITS_COLLISION_COUNT = 2;
  static final byte BITS_OBJECT_COUNT = 12;
  static final byte BITS_COUNTS = BITS_COLLISION_COUNT + BITS_OBJECT_COUNT;
  static final byte BITS_DOCID = 64 - BITS_COUNTS;

  @Requirement
  private ModelUtils modelUtils;

  /**
   * intended for test purposes only
   */
  MessageDigest injectedDigest;

  @Override
  public IdVersion getIdVersion() {
    return IdVersion.CELEMENTS_3;
  }

  @Override
  public long compute(DocumentReference docRef, String lang) {
    try {
      return computeDocumentId(docRef, lang);
    } catch (IdComputationException exc) {
      throw new IllegalStateException("should not happend", exc);
    }
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang)
      throws IdComputationException {
    return computeDocumentId(docRef, lang, (byte) 0);
  }

  @Override
  public long computeMaxDocumentId(DocumentReference docRef, String lang)
      throws IdComputationException {
    return computeDocumentId(docRef, lang, getMaxCollisionCount());
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang, byte collisionCount)
      throws IdComputationException {
    return computeId(docRef, lang, collisionCount, 0);
  }

  @Override
  public Iterator<Long> getDocumentIdIterator(DocumentReference docRef, String lang) {
    return new Iterator<Long>() {

      private byte collisionCount = 0;
      private Long next = null;

      @Override
      public boolean hasNext() {
        while ((next == null) && ((collisionCount >>> BITS_COLLISION_COUNT) == 0)) {
          try {
            next = computeDocumentId(docRef, lang, collisionCount++);
          } catch (IdComputationException exc) {}
        }
        return (next != null);
      }

      @Override
      public Long next() {
        if (hasNext()) {
          long ret = next;
          next = null;
          return ret;
        }
        throw new NoSuchElementException(docRef + ", lang:" + lang);
      }
    };
  }

  @Override
  public long computeDocumentId(XWikiDocument doc) throws IdComputationException {
    return computeId(doc, 0);
  }

  @Override
  public long computeNextObjectId(XWikiDocument doc) throws IdComputationException {
    Set<Long> existingObjIds = collectVersionedObjectIds(doc);
    long nextObjectId;
    int objectCount = 1;
    do {
      nextObjectId = computeId(doc, objectCount++);
    } while (existingObjIds.contains(nextObjectId));
    return nextObjectId;
  }

  private Set<Long> collectVersionedObjectIds(XWikiDocument doc) {
    return StreamEx.of(XWikiObjectEditor.on(doc).fetch().stream())
        .append(doc.getXObjectsToRemove())
        .filter(Objects::nonNull)
        .filter(BaseObject::hasValidId)
        .filter(obj -> obj.getIdVersion() == getIdVersion())
        .map(BaseObject::getId)
        .toImmutableSet();
  }

  long computeId(XWikiDocument doc, int objectCount) throws IdComputationException {
    checkNotNull(doc);
    byte collisionCount = 0;
    if (doc.hasValidId() && (doc.getIdVersion() == getIdVersion())) {
      collisionCount = extractCollisionCount(doc.getId());
    }
    return computeId(doc.getDocumentReference(), doc.getLanguage(), collisionCount, objectCount);
  }

  byte extractCollisionCount(long id) {
    // & 0xff (255) to prevent accidental value conversions, see Sonar S3034
    return (byte) ((id >> BITS_OBJECT_COUNT) & (getMaxCollisionCount() & 0xff));
  }

  /**
   * inclusive
   */
  byte getMaxCollisionCount() {
    return ~(-1 << BITS_COLLISION_COUNT);
  }

  long computeId(DocumentReference docRef, String lang, byte collisionCount, int objectCount)
      throws IdComputationException {
    verifyCount(collisionCount, BITS_COLLISION_COUNT);
    verifyCount(objectCount, BITS_OBJECT_COUNT);
    long docId = hashMD5(serializeLocalUid(docRef, lang));
    // docId part mustn't be zero, thus map docIds >= 0 and < 2^BITS_COUNTS to distinct values
    if ((docId >>> BITS_COUNTS) == 0) {
      docId = Long.MAX_VALUE - docId;
    }
    long left = andifyRight(docId, BITS_COUNTS);
    long right = ((long) collisionCount << BITS_OBJECT_COUNT) + objectCount;
    right = andifyLeft(right, BITS_DOCID);
    return verifyId(left & right);
  }

  long andifyLeft(long base, byte bits) {
    return ~(~(base << bits) >>> bits);
  }

  long andifyRight(long base, byte bits) {
    return ~(~(base >>> bits) << bits);
  }

  private void verifyCount(long count, byte bits) throws IdComputationException {
    try {
      verify(count >= 0, "negative count '%s' not allowed", count);
      verify((count >>> bits) == 0, "count '%s' outside of defined range '2^%s'", count, bits);
    } catch (VerifyException exc) {
      throw new IdComputationException(exc);
    }
  }

  private long verifyId(long id) throws IdComputationException {
    try {
      // FIXME this verification can lead to unrecoverable production fast failures and has be
      // removed after completion of [CELDEV-605] XWikiDocument/BaseCollection id migration
      verify((id > Integer.MAX_VALUE) || (id < Integer.MIN_VALUE),
          "generated id '%s' may collide with '%s'", id, IdVersion.XWIKI_2);
      return id;
    } catch (VerifyException exc) {
      throw new IdComputationException(exc);
    }
  }

  /**
   * @return first 8 bytes of MD5 hash from given string
   */
  long hashMD5(String str) throws IdComputationException {
    MessageDigest digest = getMessageDigest();
    digest.update(str.getBytes(StandardCharsets.UTF_8));
    return Longs.fromByteArray(digest.digest());
  }

  private MessageDigest getMessageDigest() throws IdComputationException {
    try {
      if (injectedDigest == null) {
        return MessageDigest.getInstance(HASH_ALGO);
      } else {
        return injectedDigest;
      }
    } catch (NoSuchAlgorithmException exc) {
      throw new IdComputationException("illegal hash algorithm", exc);
    }
  }

  /**
   * @return calculated local uid like LocalUidStringEntityReferenceSerializer from XWiki 4.0+
   */
  String serializeLocalUid(DocumentReference docRef, String lang) {
    StringBuilder key = new StringBuilder();
    for (String name : Splitter.on('.').split(serialize(docRef, lang))) {
      if (!name.isEmpty()) {
        key.append(name.length()).append(':').append(name);
      }
    }
    return key.toString();
  }

  private String serialize(DocumentReference docRef, String lang) {
    return modelUtils.serializeRefLocal(docRef) + '.' + Strings.nullToEmpty(lang).trim();
  }

}
