/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package com.celements.store;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.celements.model.access.ContextExecutor;
import com.celements.model.access.XWikiDocumentCreator;
import com.celements.model.access.exception.MetaDataLoadException;
import com.celements.model.context.ModelContext;
import com.celements.model.metadata.DocumentMetaData;
import com.celements.model.metadata.ImmutableDocumentMetaData;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * A proxy store implementation that caches Documents when they are first fetched and subsequently
 * return them from a
 * cache. It delegates all write and search operations to an underlying store without doing any
 * caching on them.
 *
 * @version $Id$
 */
@Component(DocumentCacheStore.COMPONENT_NAME)
public class DocumentCacheStore extends DelegateStore implements XWikiCacheStoreInterface {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentCacheStore.class);
  private static final Logger LOGGER_DL = LoggerFactory.getLogger(DocumentLoader.class);

  public static final String COMPONENT_NAME = "docCache";

  private static final String PARAM_PREFIX = "celements.store." + COMPONENT_NAME;
  public static final String PARAM_DOC_CACHE_CAPACITY = PARAM_PREFIX + ".capacityDoc";
  public static final String PARAM_EXIST_CACHE_CAPACITY = PARAM_PREFIX + ".capacityExists";

  @Requirement
  private CacheManager cacheManager;

  @Requirement
  private XWikiDocumentCreator docCreator;

  @Requirement
  private ModelContext modelContext;

  @Requirement
  private ModelUtils modelUtils;

  /**
   * CAUTION: Lazy initialized of cache thus volatile is needed.
   */
  private volatile Cache<XWikiDocument> docCache;

  /**
   * CAUTION: Lazy initialized of cache thus volatile is needed.
   */
  private volatile Cache<Boolean> existCache;

  private final ConcurrentMap<String, DocumentLoader> documentLoaderMap = new ConcurrentHashMap<>();

  @Override
  protected String getName() {
    return COMPONENT_NAME;
  }

  // SonarLint Rule squid:S3064 - Assignment of lazy-initialized members should be
  // the last step with double-checked locking
  void initalize() {
    try {
      if (this.docCache == null) {
        synchronized (this) {
          if (this.docCache == null) {
            this.docCache = newDocCache();
          }
        }
      }
      if (this.existCache == null) {
        synchronized (this) {
          if (this.existCache == null) {
            this.existCache = newExistCache();
          }
        }
      }
    } catch (CacheException | ComponentLookupException exc) {
      throw new IllegalStateException("FATAL: Failed to initialize document cache.", exc);
    }
  }

  private Cache<Boolean> newExistCache() throws CacheException, ComponentLookupException {
    CacheConfiguration config = new CacheConfiguration();
    config.setConfigurationId("xwiki.store.pageexistcache");
    LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
    lru.setMaxEntries(getExistCacheCapacity());
    config.put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
    return cacheManager.getCacheFactory().newCache(config);
  }

  private int getExistCacheCapacity() {
    int existCacheCapacity = cfgSrc.getProperty(PARAM_DOC_CACHE_CAPACITY, 10000);
    int docCacheCapacity = getDocCacheCapacity();
    if (existCacheCapacity < docCacheCapacity) {
      LOGGER.warn("WARNING: document exists cache capacity is smaller configured than docCache "
          + "capacity. Ignoring exists cache configuration '{}' and using doc cache capacity '{}'"
          + " instead.", existCacheCapacity, docCacheCapacity);
      existCacheCapacity = docCacheCapacity;
    }
    return existCacheCapacity;
  }

  private Cache<XWikiDocument> newDocCache() throws CacheException, ComponentLookupException {
    CacheConfiguration config = new CacheConfiguration();
    config.setConfigurationId("xwiki.store.pagecache");
    LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
    lru.setMaxEntries(getDocCacheCapacity());
    config.put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
    return cacheManager.getCacheFactory().newCache(config);
  }

  private int getDocCacheCapacity() {
    return Math.max(0, cfgSrc.getProperty(PARAM_DOC_CACHE_CAPACITY, 100));
  }

  @Override
  public void initCache(int docCacheCapacity, int existCacheCapacity, XWikiContext context)
      throws XWikiException {
    LOGGER.info("initCache externally called. This is not supported. The document cache initializes"
        + " automatically on start.");
  }

  @Override
  public XWikiStoreInterface getStore() {
    return getBackingStore();
  }

  @Override
  public void setStore(XWikiStoreInterface store) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    saveXWikiDoc(doc, context, true);
  }

  @Override
  public void saveXWikiDoc(final XWikiDocument doc, final XWikiContext context,
      final boolean bTransaction) throws XWikiException {
    new ContextExecutor<Void, XWikiException>() {

      @Override
      protected Void call() throws XWikiException {
        getBackingStore().saveXWikiDoc(doc, context, bTransaction);
        doc.setStore(DocumentCacheStore.this.getBackingStore());
        removeDocFromCache(doc, true);
        return null;
      }
    }.inWiki(new WikiReference(context.getDatabase())).execute();
  }

  @Override
  public synchronized void flushCache() {
    LOGGER.warn("flushCache may lead to serious memory visibility problems.");
    if (this.docCache != null) {
      this.docCache.dispose();
      this.docCache = null;
    }
    if (this.existCache != null) {
      this.existCache.dispose();
      this.existCache = null;
    }
  }

  String getKey(DocumentReference docRef) {
    DocumentReference cacheDocRef = RefBuilder.from(docRef)
        .with(modelContext.getWikiRef())
        .build(DocumentReference.class);
    return modelUtils.serializeRef(cacheDocRef);
  }

  String getKeyWithLang(DocumentReference docRef, String language) {
    if (Strings.isNullOrEmpty(language)) {
      return getKey(docRef);
    } else {
      return getKey(docRef) + ":" + language;
    }
  }

  String getKeyWithLang(XWikiDocument doc) {
    String language = doc.getLanguage();
    if (language.equals(doc.getDefaultLanguage())) {
      language = "";
    }
    return getKeyWithLang(doc.getDocumentReference(), language);
  }

  private DocumentLoader getDocumentLoader(String key) {
    DocumentLoader docLoader = documentLoaderMap.get(key);
    if (docLoader == null) {
      LOGGER.debug("create document loader for '{}' in thread '{}'", key,
          Thread.currentThread().getId());
      docLoader = new DocumentLoader(key);
      DocumentLoader setDocLoader = documentLoaderMap.putIfAbsent(key, docLoader);
      if (setDocLoader != null) {
        docLoader = setDocLoader;
        LOGGER.info("replace with existing from map for key '{}' in thread '{}'", key,
            Thread.currentThread().getId());
      }
    }
    return docLoader;
  }

  public boolean remove(XWikiDocument doc) {
    return removeDocFromCache(doc, null) != InvalidateState.CACHE_MISS;
  }

  InvalidateState removeDocFromCache(XWikiDocument doc, Boolean docExists) {
    InvalidateState returnState = InvalidateState.CACHE_MISS;
    Set<String> docKeys = new HashSet<>();
    String key = getKey(doc.getDocumentReference());
    String origKey = "";
    if (doc.getOriginalDocument() != null) {
      origKey = getKey(doc.getOriginalDocument().getDocumentReference());
      docKeys.add(origKey);
      docKeys.add(getKeyWithLang(doc.getOriginalDocument()));
    }
    docKeys.add(key);
    docKeys.add(getKeyWithLang(doc));
    for (String k : docKeys) {
      InvalidateState invState = invalidateDocCache(k);
      if (invState == InvalidateState.REMOVED) {
        returnState = InvalidateState.REMOVED;
      } else if (returnState != InvalidateState.REMOVED) {
        returnState = invState;
      }
    }
    if (getExistCache() != null) {
      if ((doc.getTranslation() == 0) || (Boolean.TRUE.equals(docExists))) {
        setExistCache(origKey, null);
        setExistCache(key, docExists);
      }
      setExistCache(doc, docExists);
    }
    return returnState;
  }

  InvalidateState invalidateDocCache(String key) {
    InvalidateState invalidState = InvalidateState.CACHE_MISS;
    final DocumentLoader docLoader = documentLoaderMap.get(key);
    boolean invalidateDocLoader = (docLoader != null);
    if (invalidateDocLoader) {
      invalidState = docLoader.invalidate();
    }
    XWikiDocument oldCachedDoc = null;
    if (getDocCache() != null) {
      oldCachedDoc = getDocFromCache(key);
      if (oldCachedDoc != null) {
        synchronized (oldCachedDoc) {
          oldCachedDoc = getDocFromCache(key);
          if (oldCachedDoc != null) {
            setDocCache(key, null);
            invalidState = InvalidateState.REMOVED;
          }
        }
      }
    }
    return invalidState;
  }

  public synchronized void clearCache() {
    getDocCache().removeAll();
    getExistCache().removeAll();
    LOGGER.warn("cleared doc cache", new RuntimeException());
  }

  @Override
  public XWikiDocument loadXWikiDoc(final XWikiDocument doc, final XWikiContext context)
      throws XWikiException {
    return new ContextExecutor<XWikiDocument, XWikiException>() {

      @Override
      protected XWikiDocument call() throws XWikiException {
        return loadXWikiDocInternal(doc, context);
      }
    }.inWiki(new WikiReference(context.getDatabase())).execute();
  }

  private XWikiDocument loadXWikiDocInternal(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    LOGGER.trace("Cache: begin for docRef '{}' in cache", doc.getDocumentReference());
    XWikiDocument ret;
    String key = getKey(doc.getDocumentReference());
    String keyWithLang = getKeyWithLang(doc);
    if (doesNotExistsForKey(key) || doesNotExistsForKey(keyWithLang)) {
      LOGGER.debug("Cache: The document {} does not exist, return an empty one", keyWithLang);
      ret = createEmptyXWikiDoc(doc);
    } else {
      LOGGER.debug("Cache: Trying to get doc '{}' from cache", keyWithLang);
      XWikiDocument cachedoc = getDocFromCache(keyWithLang);
      if (cachedoc != null) {
        LOGGER.debug("Cache: got doc '{}' from cache", keyWithLang);
      } else {
        cachedoc = getDocumentLoader(keyWithLang).loadDocument(keyWithLang, doc, context);
      }
      LOGGER.trace("Cache: end for doc '{}' in cache", keyWithLang);
      ret = cachedoc;
    }
    return ret;
  }

  private boolean doesNotExistsForKey(String key) {
    return Boolean.FALSE.equals(getExistCache().get(key));
  }

  /**
   * getCache is private, thus for tests we need getDocFromCache to check the cache state
   */
  XWikiDocument getDocFromCache(String key) {
    return getDocCache().get(key);
  }

  /**
   * getCache is private, thus for tests we need getExistFromCache to check the cache state
   */
  Boolean getExistFromCache(String key) {
    return getExistCache().get(key);
  }

  @Override
  public void deleteXWikiDoc(final XWikiDocument doc, final XWikiContext context)
      throws XWikiException {
    new ContextExecutor<Void, XWikiException>() {

      @Override
      protected Void call() throws XWikiException {

        getBackingStore().deleteXWikiDoc(doc, context);
        removeDocFromCache(doc, false);
        return null;
      }
    }.inWiki(new WikiReference(context.getDatabase())).execute();
  }

  @Override
  public synchronized void deleteWiki(String wikiName, XWikiContext context) throws XWikiException {
    super.deleteWiki(wikiName, context);
    flushCache();
  }

  @Override
  public boolean exists(final XWikiDocument doc, final XWikiContext context) throws XWikiException {
    return new ContextExecutor<Boolean, XWikiException>() {

      @Override
      protected Boolean call() throws XWikiException {
        return existsInternal(doc, context);
      }
    }.inWiki(new WikiReference(context.getDatabase())).execute();
  }

  // FIXME [CELDEV-924] Store add lang support for exists check and cache
  private boolean existsInternal(XWikiDocument doc, XWikiContext context) throws XWikiException {
    String key = getKey(doc.getDocumentReference());
    Boolean result = getExistCache().get(key);
    if (result == null) {
      result = (getDocCache().get(key) != null);
      if (!result) {
        result = getBackingStore().exists(doc, context);
      }
      getExistCache().set(key, result);
    }
    LOGGER.trace("exists return '{}' for '{}'", result, key);
    return result;
  }

  private Cache<XWikiDocument> getDocCache() {
    initalize(); // make sure cache is initialized
    return this.docCache;
  }

  private void setDocCache(String key, XWikiDocument doc) {
    LOGGER.debug("setDocCache - {}{}", key, (doc == null ? " removed" : ""));
    if (doc == null) {
      getDocCache().remove(key);
    } else {
      getDocCache().set(key, doc);
    }
  }

  private Cache<Boolean> getExistCache() {
    initalize(); // make sure cache is initialized
    return this.existCache;
  }

  private void setExistCache(String key, Boolean exists) {
    LOGGER.debug("setExistCache - {} to {}", key, exists);
    if (exists == null) {
      getExistCache().remove(key);
    } else {
      getExistCache().set(key, exists);
    }
  }

  private void setExistCache(XWikiDocument doc, Boolean exists) {
    setExistCache(getKeyWithLang(doc), exists);
  }

  enum InvalidateState {

    CACHE_MISS, REMOVED, LOADING_CANCELED, LOADING_MULTI_CANCELED, LOADING_CANCEL_FAILED

  }

  private static final int DOCSTATE_LOADING = 0;
  private static final int DOCSTATE_FINISHED = Integer.MAX_VALUE;

  private class DocumentLoader {

    private volatile XWikiDocument loadedDoc;
    private final String key;

    /**
     * if loadingState equals _DOCSTATE_LOADING than a valid loading is about to start or in process
     * if loadingState is lower _DOCSTATE_LOADING than a loading has been successful canceled and a
     * reload will take place
     * if loadingState equals _DOCSTATE_FINISHED or is at least greater _DOCSTATE_LOADING loading
     * finished before any canceling happened
     */
    private final AtomicInteger loadingState = new AtomicInteger(DOCSTATE_LOADING);

    private DocumentLoader(String key) {
      this.key = key;
    }

    private InvalidateState invalidate() {
      InvalidateState invalidState;
      int beforeState = loadingState.getAndDecrement();
      if (beforeState < 0) {
        if (loadedDoc != null) {
          LOGGER_DL.warn("should not happen: possible lifelock! {}", this.key);
        }
        invalidState = InvalidateState.LOADING_MULTI_CANCELED;
      } else if (beforeState == 0) {
        invalidState = InvalidateState.LOADING_CANCELED;
      } else {
        invalidState = InvalidateState.LOADING_CANCEL_FAILED;
      }
      boolean succInvalidated = beforeState <= 0;
      LOGGER_DL.debug("invalidated cache during loading document. {}", succInvalidated);
      return invalidState;
    }

    /**
     * IMPORTANT: do not change anything on the synchronization of this method.
     * It is a very delicate case and very likely memory visibility breaks in less than 1 out of
     * 100'000 document loads. Thus it is difficult to test for correctness.
     */
    private XWikiDocument loadDocument(String key, XWikiDocument doc, XWikiContext context)
        throws XWikiException {
      checkArgument(key);
      if (loadedDoc == null) {
        synchronized (this) {
          if (loadedDoc == null) {
            // if a thread is just between the document cache miss and getting the documentLoader
            // when the documentLoader removes itself from the map, then a new documentLoader is
            // generated. Therefore we double check here that still no document is in cache.
            XWikiDocument loadingDoc = getDocCache().get(key);
            if (loadingDoc == null) {
              XWikiDocument newDoc = null;
              do {
                if ((loadingState.getAndSet(DOCSTATE_LOADING) < DOCSTATE_LOADING)
                    && (newDoc != null)) {
                  LOGGER_DL.info("DocumentLoader-{}: invalidated docloader '{}' reloading",
                      Thread.currentThread().getId(), key);
                }
                // use a further synchronized method call to prevent an unsafe publication of the
                // new document over the cache
                newDoc = new DocumentBuilder().buildDocument(key, doc, context);
              } while (!loadingState.compareAndSet(DOCSTATE_LOADING, DOCSTATE_FINISHED));
              LOGGER_DL.debug("DocumentLoader-{}: put doc '{}' in cache",
                  Thread.currentThread().getId(), key);
              final String keyWithLang = getKeyWithLang(newDoc);
              if (!newDoc.isNew()) {
                setDocCache(keyWithLang, newDoc);
                setExistCache(getKey(newDoc.getDocumentReference()), true);
                setExistCache(keyWithLang, true);
              } else {
                LOGGER_DL.debug("DocumentLoader-{}: loading '{}' failed. Setting exists"
                    + " to FALSE for '{}'", Thread.currentThread().getId(), key, keyWithLang);
                setExistCache(keyWithLang, false);
              }
              loadingDoc = newDoc;
            } else {
              LOGGER_DL.debug("DocumentLoader-{}: found in cache skip loding for '{}'",
                  Thread.currentThread().getId(), key);
            }
            documentLoaderMap.remove(key);
            // SonarLint Rule squid:S3064 - Assignment of lazy-initialized members should be
            // the last step with double-checked locking
            loadedDoc = loadingDoc;
          }
        }
      }
      return loadedDoc;
    }

    private void checkArgument(String key) {
      if (!this.key.equals(key)) {
        throw new RuntimeException(
            "DocumentLoader illegally used with a different key (registered key:" + this.key
                + ", loading doc key: " + key + ").");
      }
    }

    private class DocumentBuilder {

      private synchronized XWikiDocument buildDocument(String key, XWikiDocument doc,
          XWikiContext context) throws XWikiException {
        LOGGER_DL.trace("DocumentLoader-{}: Trying to get doc '{}' for real",
            Thread.currentThread().getId(), key);
        // IMPORTANT: do not clone here. Creating new document is much faster.
        XWikiDocument buildDoc = createEmptyXWikiDoc(doc);
        buildDoc.setLanguage(doc.getLanguage());
        buildDoc = getBackingStore().loadXWikiDoc(buildDoc, context);
        buildDoc.setStore(getBackingStore());
        buildDoc.setFromCache(!buildDoc.isNew());
        return buildDoc;
      }

    }
  }

  private XWikiDocument createEmptyXWikiDoc(XWikiDocument doc) {
    DocumentReference docRef = RefBuilder.from(doc.getDocumentReference())
        .with(modelContext.getWikiRef())
        .build(DocumentReference.class);
    XWikiDocument newDoc = docCreator.createWithoutDefaults(docRef, doc.getLanguage());
    newDoc.setDefaultLanguage(doc.getDefaultLanguage());
    newDoc.setStore(getBackingStore());
    return newDoc;
  }

  @Override
  public Set<DocumentMetaData> listDocumentMetaData(EntityReference filterRef) {
    Set<DocumentMetaData> ret = new LinkedHashSet<>();
    try {
      Map<String, SpaceReference> spaceRefMap = new HashMap<>();
      Query query = buildDocumentMetaDataQuery(filterRef);
      WikiReference wiki = new WikiReference(query.getWiki());
      for (Object[] docData : query.<Object[]>execute()) {
        Optional<DocumentMetaData> metaData = getMetaData(wiki, docData, spaceRefMap);
        if (metaData.isPresent()) {
          ret.add(metaData.get());
        }
      }
      LOGGER.info("listDocumentMetaData: found {} docs with hql '{}' for filterRef '{}'",
          ret.size(), query.getStatement(), filterRef);
    } catch (QueryException exc) {
      throw new MetaDataLoadException(filterRef, exc);
    }
    if (getBackingStore() instanceof MetaDataStoreExtension) {
      ret.addAll(((MetaDataStoreExtension) getBackingStore()).listDocumentMetaData(filterRef));
    }
    return ret;
  }

  private Query buildDocumentMetaDataQuery(EntityReference filterRef) throws QueryException {
    StringBuilder sb = new StringBuilder();
    sb.append("select distinct doc.space, doc.name, doc.language, doc.version "
        + "from XWikiDocument as doc");
    Map<String, String> bindValues = new HashMap<>();
    filterRef.extractRef(SpaceReference.class).ifPresent(spaceRef -> {
      sb.append(" where doc.space = :spaceName");
      bindValues.put("spaceName", spaceRef.getName());
      filterRef.extractRef(DocumentReference.class).ifPresent(docRef -> {
        sb.append(" and doc.name = :docName");
        bindValues.put("docName", docRef.getName());
      });
    });
    String hql = sb.toString();
    Query query = getQueryManager().createQuery(hql, Query.HQL);
    query.setWiki(filterRef.extractRef(WikiReference.class)
        .orElse(modelContext.getWikiRef()).getName());
    for (Entry<String, String> bind : bindValues.entrySet()) {
      query.bindValue(bind.getKey(), bind.getValue());
    }
    return query;
  }

  private Optional<DocumentMetaData> getMetaData(WikiReference wikiRef, Object[] docData,
      Map<String, SpaceReference> spaceRefMap) {
    DocumentMetaData metaData = null;
    try {
      String spaceName = (String) docData[0];
      if (!spaceRefMap.containsKey(spaceName)) {
        spaceRefMap.put(spaceName, new SpaceReference(spaceName, wikiRef));
      }
      metaData = new ImmutableDocumentMetaData.Builder(spaceRefMap.get(spaceName),
          (String) docData[1]).language((String) docData[2]).version((String) docData[3]).build();
    } catch (IllegalArgumentException iae) {
      LOGGER.warn("getMetaData: illegal docData '{}'", Arrays.asList(docData), iae);
    }
    return Optional.ofNullable(metaData);
  }

}
