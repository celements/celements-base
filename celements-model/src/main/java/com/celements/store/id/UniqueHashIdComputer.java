package com.celements.store.id;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Verify.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.util.ModelUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.VerifyException;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.doc.XWikiDocument;

@Component(UniqueHashIdComputer.NAME)
public class UniqueHashIdComputer implements CelementsIdComputer {

  public static final String NAME = "uniqueHash";

  private static final String HASH_ALGO = "MD5";
  private static final byte BITS_COLLISION_COUNT = 2;
  private static final byte BITS_OBJECT_COUNT = 12;

  @Requirement
  private ModelUtils modelUtils;

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang)
      throws IdComputationException {
    return computeDocumentId(docRef, lang, 0);
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang, long collisionCount)
      throws IdComputationException {
    return computeId(docRef, lang, collisionCount, 0);
  }

  @Override
  public long computeDocumentId(XWikiDocument doc) throws IdComputationException {
    return computeId(doc, 0);
  }

  private long computeId(XWikiDocument doc, long objectCount) throws IdComputationException {
    checkNotNull(doc);
    long collisionCount = 0;
    return computeId(doc.getDocumentReference(), doc.getLanguage(), collisionCount, objectCount);
  }

  long computeId(DocumentReference docRef, String lang, long collisionCount, long objectCount)
      throws IdComputationException {
    verifyCount(collisionCount, BITS_COLLISION_COUNT);
    verifyCount(objectCount, BITS_OBJECT_COUNT);
    long docId = hashMD5(serializeLocalUid(docRef, lang));
    docId = andifyLeft(andifyRight(docId, BITS_OBJECT_COUNT), BITS_COLLISION_COUNT);
    byte bitsRight = 64 - BITS_COLLISION_COUNT;
    collisionCount = andifyRight(collisionCount << bitsRight, bitsRight);
    byte bitsLeft = 64 - BITS_OBJECT_COUNT;
    objectCount = andifyLeft(objectCount, bitsLeft);
    return collisionCount & docId & objectCount;
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
      verify(count < (1L << bits), "count '%s' outside of defined range '2^%s'", count, bits);
    } catch (VerifyException exc) {
      throw new IdComputationException(exc);
    }
  }

  /**
   * @return first 8 bytes of MD5 hash from given string
   */
  long hashMD5(String str) throws IdComputationException {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGO);
      digest.update(str.getBytes("utf-8"));
      return Longs.fromByteArray(digest.digest());
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException exc) {
      throw new IdComputationException("failed calculating hash", exc);
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
