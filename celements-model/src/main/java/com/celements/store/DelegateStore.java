package com.celements.store;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryManager;

import com.celements.model.metadata.DocumentMetaData;
import com.google.common.base.Suppliers;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public abstract class DelegateStore implements XWikiStoreInterface, MetaDataStoreExtension {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Requirement("xwikiproperties")
  private ConfigurationSource cfgSrc;

  private final Supplier<XWikiStoreInterface> backingStore = Suppliers.memoize(() -> {
    String hint = cfgSrc.getProperty(getBackingStoreConfigName(), "default");
    XWikiStoreInterface store = Utils.getComponent(XWikiStoreInterface.class, hint);
    if (store == this) {
      throw new IllegalArgumentException("circular backing store: " + hint);
    }
    logger.info("backing store initialized '{}' for hint '{}'", store, hint);
    return store;
  });

  public XWikiStoreInterface getBackingStore() {
    return backingStore.get();
  }

  protected abstract String getName();

  protected String getBackingStoreConfigName() {
    return "celements.store." + getName() + ".backingStore";
  }

  @Override
  public Set<DocumentMetaData> listDocumentMetaData(EntityReference filterRef) {
    if (getBackingStore() instanceof MetaDataStoreExtension) {
      return ((MetaDataStoreExtension) getBackingStore()).listDocumentMetaData(filterRef);
    } else {
      return new LinkedHashSet<>();
    }
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    getBackingStore().saveXWikiDoc(doc, context);
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().saveXWikiDoc(doc, context, bTransaction);
  }

  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    return getBackingStore().loadXWikiDoc(doc, context);
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    getBackingStore().deleteXWikiDoc(doc, context);
  }

  @Override
  public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
    return getBackingStore().exists(doc, context);
  }

  @Override
  public List<String> getClassList(XWikiContext context) throws XWikiException {
    return getBackingStore().getClassList(context);
  }

  @Override
  public int countDocuments(String wheresql, XWikiContext context) throws XWikiException {
    return getBackingStore().countDocuments(wheresql, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocumentReferences(wheresql, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocumentsNames(wheresql, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(wheresql, nb, start, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocumentsNames(wheresql, nb, start, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start,
      String selectColumns, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(wheresql, nb, start, selectColumns, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentsNames(wheresql, nb, start, selectColumns, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb,
      int start, List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(parametrizedSqlClause, nb, start,
        parameterValues, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentsNames(parametrizedSqlClause, nb, start, parameterValues,
        context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(parametrizedSqlClause, parameterValues,
        context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String parametrizedSqlClause, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentsNames(parametrizedSqlClause, parameterValues, context);
  }

  @Override
  public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
      throws XWikiException {
    return getBackingStore().isCustomMappingValid(bclass, custommapping1, context);
  }

  @Override
  public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context)
      throws XWikiException {
    return getBackingStore().injectCustomMapping(doc1class, context);
  }

  @Override
  public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    return getBackingStore().injectCustomMappings(doc, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      boolean customMapping, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, customMapping, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, int nb,
      int start, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, nb, start, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      boolean customMapping, int nb, int start, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, customMapping, nb, start,
        context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, nb, start, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      boolean customMapping, boolean checkRight, int nb, int start, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, customMapping, checkRight,
        nb, start, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb,
      int start, List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbylanguage, nb, start,
        parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping, int nb, int start, List<?> parameterValues, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start,
        parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, nb, start, parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping, boolean checkRight, int nb, int start, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbylanguage, customMapping,
        checkRight, nb, start, parameterValues, context);
  }

  @Override
  public int countDocuments(String parametrizedSqlClause, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().countDocuments(parametrizedSqlClause, parameterValues, context);
  }

  @Override
  public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    return getBackingStore().loadLock(docId, context, bTransaction);
  }

  @Override
  public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().saveLock(lock, context, bTransaction);
  }

  @Override
  public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().deleteLock(lock, context, bTransaction);
  }

  @Override
  public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    return getBackingStore().loadLinks(docId, context, bTransaction);
  }

  @Override
  public List<DocumentReference> loadBacklinks(DocumentReference documentReference,
      boolean bTransaction, XWikiContext context) throws XWikiException {
    return getBackingStore().loadBacklinks(documentReference, bTransaction, context);
  }

  @Override
  @Deprecated
  public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    return getBackingStore().loadBacklinks(fullName, context, bTransaction);
  }

  @Override
  public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().saveLinks(doc, context, bTransaction);
  }

  @Override
  public void deleteLinks(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().deleteLinks(docId, context, bTransaction);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, XWikiContext context)
      throws XWikiException {
    return getBackingStore().search(sql, nb, start, context);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams,
      XWikiContext context) throws XWikiException {
    return getBackingStore().search(sql, nb, start, whereParams, context);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().search(sql, nb, start, parameterValues, context);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().search(sql, nb, start, whereParams, parameterValues, context);
  }

  @Override
  public synchronized void cleanUp(XWikiContext context) {
    getBackingStore().cleanUp(context);
  }

  @Override
  public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException {
    return getBackingStore().isWikiNameAvailable(wikiName, context);
  }

  @Override
  public synchronized void createWiki(String wikiName, XWikiContext context) throws XWikiException {
    getBackingStore().createWiki(wikiName, context);
  }

  @Override
  public synchronized void deleteWiki(String wikiName, XWikiContext context) throws XWikiException {
    getBackingStore().deleteWiki(wikiName, context);
  }

  @Override
  public List<String> getCustomMappingPropertyList(BaseClass bclass) {
    return getBackingStore().getCustomMappingPropertyList(bclass);
  }

  @Override
  public synchronized void injectCustomMappings(XWikiContext context) throws XWikiException {
    getBackingStore().injectCustomMappings(context);
  }

  @Override
  public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException {
    getBackingStore().injectUpdatedCustomMappings(context);
  }

  @Override
  public List<String> getTranslationList(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    return getBackingStore().getTranslationList(doc, context).stream()
        .filter(lang -> !lang.isEmpty())
        .collect(Collectors.toList());
  }

  @Override
  public QueryManager getQueryManager() {
    return getBackingStore().getQueryManager();
  }
}
