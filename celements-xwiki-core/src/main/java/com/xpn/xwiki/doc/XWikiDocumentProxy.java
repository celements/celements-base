package com.xpn.xwiki.doc;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.UnaryOperator;
import java.util.zip.ZipOutputStream;

import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.DocumentName;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.internal.xml.XMLWriter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.EditForm;

public class XWikiDocumentProxy extends XWikiDocument {

  private static final UnaryOperator<Date> CLONE_DATE = date -> new Date(date.getTime());

  private final XWikiDocument readonly;
  private XWikiDocument editable;

  public XWikiDocumentProxy(XWikiDocument readonly) {
    super(null, null, null, null);
    this.readonly = checkNotNull(readonly);
  }

  // TODO
  // 1. set all methods setting sth to getEditDoc()
  // 2. set others to getReadDoc()
  // 3. clone all mutable return params
  // 4. check setAsContextDoc calls, may set the readonly into the context
  // 5. XWikiAttachment#setDoc may leak readonly, check where it is set
  // 5. check for setters/manipulation in read methods
  // 5. check not overrided methods -> override ALL, if calling super make comment why

  private XWikiDocument getReadDoc() {
    return (editable != null) ? editable : readonly;
  }

  private XWikiDocument getEditDoc() {
    if (editable == null) {
      editable = readonly.clone();
      editable.setFromCache(false);
    }
    return editable;
  }

  private <T> T clone(T instance, UnaryOperator<T> cloner) {
    if (instance == null) {
      return null;
    } else if (editable != null) {
      return instance; // no need to clone editable values
    } else {
      return cloner.apply(instance);
    }
  }

  @Override
  public long getId() {
    return getReadDoc().getId();
  }

  @Override
  public int calculateXWikiId() {
    return getReadDoc().calculateXWikiId();
  }

  @Override
  public void setId(long id, IdVersion idVersion) {
    getEditDoc().setId(id, idVersion);
  }

  @Override
  public boolean hasValidId() {
    return getReadDoc().hasValidId();
  }

  @Override
  public IdVersion getIdVersion() {
    return getReadDoc().getIdVersion();
  }

  @Override
  public String getSpace() {
    return getReadDoc().getSpace();
  }

  @Override
  public void setSpace(String space) {
    getEditDoc().setSpace(space);
  }

  @Override
  public String getWeb() {
    return getReadDoc().getWeb();
  }

  @Override
  public void setWeb(String space) {
    getEditDoc().setWeb(space);
  }

  @Override
  public String getVersion() {
    return getReadDoc().getVersion();
  }

  @Override
  public void setVersion(String version) {
    getEditDoc().setVersion(version);
  }

  @Override
  public Version getRCSVersion() {
    return getReadDoc().getRCSVersion();
  }

  @Override
  public void setRCSVersion(Version version) {
    getEditDoc().setRCSVersion(version);
  }

  @Override
  public XWikiDocument getOriginalDocument() {
    XWikiDocument origDoc = getReadDoc().getOriginalDocument();
    return (origDoc != null) ? new XWikiDocumentProxy(getReadDoc().getOriginalDocument()) : null;
  }

  @Override
  public void setOriginalDocument(XWikiDocument originalDocument) {
    getEditDoc().setOriginalDocument(originalDocument);
  }

  @Override
  public DocumentReference getParentReference() {
    return getReadDoc().getParentReference();
  }

  @Override
  public String getParent() {
    return getReadDoc().getParent();
  }

  @Override
  public void setParentReference(EntityReference parentReference) {
    getEditDoc().setParentReference(parentReference);
  }

  @Override
  public void setParentReference(DocumentReference parentReference) {
    getEditDoc().setParentReference(parentReference);
  }

  @Override
  public void setParent(String parent) {
    getEditDoc().setParent(parent);
  }

  @Override
  public String getContent() {
    return getReadDoc().getContent();
  }

  @Override
  public void setContent(String content) {
    getEditDoc().setContent(content);
  }

  @Override
  public void setContent(XDOM content) throws XWikiException {
    getEditDoc().setContent(content);
  }

  @Override
  public String getRenderedContent(Syntax targetSyntax, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getRenderedContent(targetSyntax, context);
  }

  @Override
  public String getRenderedContent(Syntax targetSyntax, boolean isolateVelocityMacros,
      XWikiContext context) throws XWikiException {
    return getReadDoc().getRenderedContent(targetSyntax, isolateVelocityMacros, context);
  }

  @Override
  public String getRenderedContent(XWikiContext context) throws XWikiException {
    return getReadDoc().getRenderedContent(context);
  }

  @Override
  public String getRenderedContent(String text, String syntaxId, XWikiContext context) {
    return getReadDoc().getRenderedContent(text, syntaxId, context);
  }

  @Override
  public String getRenderedContent(String text, String sourceSyntaxId, String targetSyntaxId,
      XWikiContext context) {
    return getReadDoc().getRenderedContent(text, sourceSyntaxId, targetSyntaxId, context);
  }

  @Override
  public String getEscapedContent(XWikiContext context) throws XWikiException {
    return getReadDoc().getEscapedContent(context);
  }

  @Override
  public String getName() {
    return getReadDoc().getName();
  }

  @Override
  public void setName(String name) {
    getEditDoc().setName(name);
  }

  @Override
  public DocumentReference getDocumentReference() {
    return getReadDoc().getDocumentReference();
  }

  @Override
  public DocumentReference getDocRef() {
    return getReadDoc().getDocRef();
  }

  @Override
  public String getFullName() {
    return getReadDoc().getFullName();
  }

  @Override
  public String getPrefixedFullName() {
    return getReadDoc().getPrefixedFullName();
  }

  @Override
  public void setDocumentReference(DocumentReference reference) {
    getEditDoc().setDocumentReference(reference);
  }

  @Override
  public void setFullName(String name) {
    getEditDoc().setFullName(name);
  }

  @Override
  public void setFullName(String fullName, XWikiContext context) {
    getEditDoc().setFullName(fullName, context);
  }

  @Override
  public DocumentName getDocumentName() {
    return getReadDoc().getDocumentName();
  }

  @Override
  public String getWikiName() {
    return getReadDoc().getWikiName();
  }

  @Override
  public WikiReference getWikiRef() {
    return getReadDoc().getWikiRef();
  }

  @Override
  public String getSpaceName() {
    return getReadDoc().getSpaceName();
  }

  @Override
  public SpaceReference getSpaceRef() {
    return getReadDoc().getSpaceRef();
  }

  @Override
  public String getPageName() {
    return getReadDoc().getPageName();
  }

  @Override
  public String getTitle() {
    return getReadDoc().getTitle();
  }

  @Override
  public String getDisplayTitle(XWikiContext context) {
    return getReadDoc().getDisplayTitle(context);
  }

  @Override
  public String getRenderedTitle(Syntax outputSyntax, XWikiContext context) {
    return getReadDoc().getRenderedTitle(outputSyntax, context);
  }

  @Override
  public String extractTitle() {
    return getReadDoc().extractTitle();
  }

  @Override
  public void setTitle(String title) {
    getEditDoc().setTitle(title);
  }

  @Override
  public String getFormat() {
    return getReadDoc().getFormat();
  }

  @Override
  public void setFormat(String format) {
    getEditDoc().setFormat(format);
  }

  @Override
  public String getAuthor() {
    return getReadDoc().getAuthor();
  }

  @Override
  public String getContentAuthor() {
    return getReadDoc().getContentAuthor();
  }

  @Override
  public void setAuthor(String author) {
    getEditDoc().setAuthor(author);
  }

  @Override
  public void setContentAuthor(String contentAuthor) {
    getEditDoc().setContentAuthor(contentAuthor);
  }

  @Override
  public String getCreator() {
    return getReadDoc().getCreator();
  }

  @Override
  public void setCreator(String creator) {
    getEditDoc().setCreator(creator);
  }

  @Override
  public Date getDate() {
    return clone(getReadDoc().getDate(), CLONE_DATE);
  }

  @Override
  public void setDate(Date date) {
    getEditDoc().setDate(date);
  }

  @Override
  public Date getCreationDate() {
    return clone(getReadDoc().getCreationDate(), CLONE_DATE);
  }

  @Override
  public void setCreationDate(Date date) {
    getEditDoc().setCreationDate(date);
  }

  @Override
  public Date getContentUpdateDate() {
    return clone(getReadDoc().getContentUpdateDate(), CLONE_DATE);
  }

  @Override
  public void setContentUpdateDate(Date date) {
    getEditDoc().setContentUpdateDate(date);
  }

  @Override
  public String getMeta() {
    return getReadDoc().getMeta();
  }

  @Override
  public void setMeta(String meta) {
    getEditDoc().setMeta(meta);
  }

  @Override
  public void appendMeta(String meta) {
    getEditDoc().appendMeta(meta);
  }

  @Override
  public boolean isContentDirty() {
    return getReadDoc().isContentDirty();
  }

  @Override
  public void incrementVersion() {
    getEditDoc().incrementVersion();
  }

  @Override
  public void setContentDirty(boolean contentDirty) {
    getEditDoc().setContentDirty(contentDirty);
  }

  @Override
  public boolean isMetaDataDirty() {
    return getReadDoc().isMetaDataDirty();
  }

  @Override
  public void setMetaDataDirty(boolean metaDataDirty) {
    getEditDoc().setMetaDataDirty(metaDataDirty);
  }

  @Override
  public String getAttachmentURL(String filename, XWikiContext context) {
    return getReadDoc().getAttachmentURL(filename, context);
  }

  @Override
  public String getAttachmentURL(String filename, String action, XWikiContext context) {
    return getReadDoc().getAttachmentURL(filename, action, context);
  }

  @Override
  public String getExternalAttachmentURL(String filename, String action, XWikiContext context) {
    return getReadDoc().getExternalAttachmentURL(filename, action, context);
  }

  @Override
  public String getAttachmentURL(String filename, String action, String querystring,
      XWikiContext context) {
    return getReadDoc().getAttachmentURL(filename, action, querystring, context);
  }

  @Override
  public String getAttachmentRevisionURL(String filename, String revision, XWikiContext context) {
    return getReadDoc().getAttachmentRevisionURL(filename, revision, context);
  }

  @Override
  public String getAttachmentRevisionURL(String filename, String revision, String querystring,
      XWikiContext context) {
    return getReadDoc().getAttachmentRevisionURL(filename, revision, querystring, context);
  }

  @Override
  public String getURL(String action, String params, boolean redirect, XWikiContext context) {
    return getReadDoc().getURL(action, params, redirect, context);
  }

  @Override
  public String getURL(String action, boolean redirect, XWikiContext context) {
    return getReadDoc().getURL(action, redirect, context);
  }

  @Override
  public String getURL(String action, XWikiContext context) {
    return getReadDoc().getURL(action, context);
  }

  @Override
  public String getURL(String action, String querystring, XWikiContext context) {
    return getReadDoc().getURL(action, querystring, context);
  }

  @Override
  public String getURL(String action, String querystring, String anchor, XWikiContext context) {
    return getReadDoc().getURL(action, querystring, anchor, context);
  }

  @Override
  public String getExternalURL(String action, XWikiContext context) {
    return getReadDoc().getExternalURL(action, context);
  }

  @Override
  public String getExternalURL(String action, String querystring, XWikiContext context) {
    return getReadDoc().getExternalURL(action, querystring, context);
  }

  @Override
  public String getParentURL(XWikiContext context) throws XWikiException {
    return getReadDoc().getParentURL(context);
  }

  @Override
  public Document newDocument(String customClassName, XWikiContext context) {
    return getReadDoc().newDocument(customClassName, context);
  }

  @Override
  public Document newDocument(Class<?> customClass, XWikiContext context) {
    return getReadDoc().newDocument(customClass, context);
  }

  @Override
  public Document newDocument(XWikiContext context) {
    return getReadDoc().newDocument(context);
  }

  @Override
  public XWikiDocumentArchive getDocumentArchive(XWikiContext context) throws XWikiException {
    return loadDocumentArchive();
  }

  @Override
  public void loadArchive(XWikiContext context) throws XWikiException {
    getReadDoc().loadArchive(context);
  }

  @Override
  public XWikiDocumentArchive getDocumentArchive() {
    XWikiDocumentArchive archive = getReadDoc().getDocumentArchive();
    return (archive != null) ? getEditDoc().getDocumentArchive() : null; // TODO proxy
  }

  @Override
  public XWikiDocumentArchive loadDocumentArchive() {
    getReadDoc().loadDocumentArchive();
    return getDocumentArchive();
  }

  @Override
  public void setDocumentArchive(XWikiDocumentArchive arch) {
    getEditDoc().setDocumentArchive(arch);
  }

  @Override
  public void setDocumentArchive(String sarch) throws XWikiException {
    getEditDoc().setDocumentArchive(sarch);
  }

  @Override
  public Version[] getRevisions(XWikiContext context) throws XWikiException {
    return getReadDoc().getRevisions(context);
  }

  @Override
  public String[] getRecentRevisions(int nb, XWikiContext context) throws XWikiException {
    return getReadDoc().getRecentRevisions(nb, context);
  }

  @Override
  public List<String> getRevisions(RevisionCriteria criteria, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getRevisions(criteria, context);
  }

  @Override
  public XWikiRCSNodeInfo getRevisionInfo(String version, XWikiContext context)
      throws XWikiException {
    return clone(getReadDoc().getRevisionInfo(version, context), XWikiRCSNodeInfo::new);
  }

  @Override
  public boolean isMostRecent() {
    return getReadDoc().isMostRecent();
  }

  @Override
  public void setMostRecent(boolean mostRecent) {
    getEditDoc().setMostRecent(mostRecent);
  }

  @Override
  public BaseClass getXClass() {
    return super.getXClass();
  }

  @Override
  public BaseClass getxWikiClass() {
    return super.getxWikiClass();
  }

  @Override
  public void setXClass(BaseClass xwikiClass) {
    getEditDoc().setXClass(xwikiClass);
  }

  @Override
  public void setxWikiClass(BaseClass xwikiClass) {
    getEditDoc().setxWikiClass(xwikiClass);
  }

  @Override
  public Map<DocumentReference, List<BaseObject>> getXObjects() {
    return super.getXObjects();
  }

  @Override
  public Set<DocumentReference> getXObjectClassRefs() {
    return super.getXObjectClassRefs();
  }

  @Override
  public Map<String, Vector<BaseObject>> getxWikiObjects() {
    return super.getxWikiObjects();
  }

  @Override
  public void setXObjects(Map<DocumentReference, List<BaseObject>> objects) {
    getEditDoc().setXObjects(objects);
  }

  @Override
  public void setxWikiObjects(Map<String, Vector<BaseObject>> objects) {
    getEditDoc().setxWikiObjects(objects);
  }

  @Override
  public BaseObject getXObject() {
    return super.getXObject();
  }

  @Override
  public BaseObject getxWikiObject() {
    return super.getxWikiObject();
  }

  @Override
  public List<BaseClass> getXClasses(XWikiContext context) {
    return super.getXClasses(context);
  }

  @Override
  public List<BaseClass> getxWikiClasses(XWikiContext context) {
    return super.getxWikiClasses(context);
  }

  @Override
  public int createXObject(EntityReference classReference, XWikiContext context)
      throws XWikiException {
    return getEditDoc().createXObject(classReference, context);
  }

  @Override
  public int createNewObject(String className, XWikiContext context) throws XWikiException {
    return getEditDoc().createNewObject(className, context);
  }

  @Override
  public int getXObjectSize(DocumentReference classReference) {
    return getReadDoc().getXObjectSize(classReference);
  }

  @Override
  public int getObjectNumbers(String className) {
    return getReadDoc().getObjectNumbers(className);
  }

  @Override
  public List<BaseObject> getXObjects(DocumentReference classReference) {
    return super.getXObjects(classReference);
  }

  @Override
  public Vector<BaseObject> getObjects(String className) {
    return super.getObjects(className);
  }

  @Override
  public void setXObjects(DocumentReference classReference, List<BaseObject> objects) {
    getEditDoc().setXObjects(classReference, objects);
  }

  @Override
  public void setObjects(String className, Vector<BaseObject> objects) {
    getEditDoc().setObjects(className, objects);
  }

  @Override
  public BaseObject getXObject(DocumentReference classReference) {
    return super.getXObject(classReference);
  }

  @Override
  public BaseObject getObject(String className) {
    return super.getObject(className);
  }

  @Override
  public BaseObject getXObject(DocumentReference classReference, int nb) {
    return super.getXObject(classReference, nb);
  }

  @Override
  public BaseObject getObject(String className, int nb) {
    return super.getObject(className, nb);
  }

  @Override
  public BaseObject getXObject(DocumentReference classReference, String key, String value) {
    return super.getXObject(classReference, key, value);
  }

  @Override
  public BaseObject getObject(String className, String key, String value) {
    return super.getObject(className, key, value);
  }

  @Override
  public BaseObject getXObject(DocumentReference classReference, String key, String value,
      boolean failover) {
    return super.getXObject(classReference, key, value, failover);
  }

  @Override
  public BaseObject getObject(String className, String key, String value, boolean failover) {
    return super.getObject(className, key, value, failover);
  }

  @Override
  public void addXObject(DocumentReference classReference, BaseObject object) {
    getEditDoc().addXObject(classReference, object);
  }

  @Override
  public void addXObject(BaseObject object) {
    getEditDoc().addXObject(object);
  }

  @Override
  public void addObject(String className, BaseObject object) {
    getEditDoc().addObject(className, object);
  }

  @Override
  public void setXObject(DocumentReference classReference, int nb, BaseObject object) {
    getEditDoc().setXObject(classReference, nb, object);
  }

  @Override
  public void setXObject(int nb, BaseObject object) {
    getEditDoc().setXObject(nb, object);
  }

  @Override
  public void setObject(String className, int nb, BaseObject object) {
    getEditDoc().setObject(className, nb, object);
  }

  @Override
  public boolean isNew() {
    return getReadDoc().isNew();
  }

  @Override
  public void setNew(boolean aNew) {
    getEditDoc().setNew(aNew);
  }

  @Override
  public void mergeXClass(XWikiDocument templatedoc) {
    getEditDoc().mergeXClass(templatedoc);
  }

  @Override
  public void mergexWikiClass(XWikiDocument templatedoc) {
    getEditDoc().mergexWikiClass(templatedoc);
  }

  @Override
  public void mergeXObjects(XWikiDocument templatedoc) {
    getEditDoc().mergeXObjects(templatedoc);
  }

  @Override
  public void mergexWikiObjects(XWikiDocument templatedoc) {
    getEditDoc().mergexWikiObjects(templatedoc);
  }

  @Override
  public void cloneXObjects(XWikiDocument templatedoc) {
    getEditDoc().cloneXObjects(templatedoc);
  }

  @Override
  public void duplicateXObjects(XWikiDocument templatedoc) {
    getEditDoc().duplicateXObjects(templatedoc);
  }

  @Override
  public void clonexWikiObjects(XWikiDocument templatedoc) {
    getEditDoc().clonexWikiObjects(templatedoc);
  }

  @Override
  public DocumentReference getTemplateDocumentReference() {
    return getReadDoc().getTemplateDocumentReference();
  }

  @Override
  public String getTemplate() {
    return getReadDoc().getTemplate();
  }

  @Override
  public void setTemplateDocumentReference(DocumentReference templateDocumentReference) {
    getEditDoc().setTemplateDocumentReference(templateDocumentReference);
  }

  @Override
  public void setTemplate(String template) {
    getEditDoc().setTemplate(template);
  }

  @Override
  public String displayPrettyName(String fieldname, XWikiContext context) {
    return getReadDoc().displayPrettyName(fieldname, context);
  }

  @Override
  public String displayPrettyName(String fieldname, boolean showMandatory, XWikiContext context) {
    return getReadDoc().displayPrettyName(fieldname, showMandatory, context);
  }

  @Override
  public String displayPrettyName(String fieldname, boolean showMandatory, boolean before,
      XWikiContext context) {
    return getReadDoc().displayPrettyName(fieldname, showMandatory, before, context);
  }

  @Override
  public String displayPrettyName(String fieldname, BaseObject obj, XWikiContext context) {
    return getReadDoc().displayPrettyName(fieldname, obj, context);
  }

  @Override
  public String displayPrettyName(String fieldname, boolean showMandatory, BaseObject obj,
      XWikiContext context) {
    return getReadDoc().displayPrettyName(fieldname, showMandatory, obj, context);
  }

  @Override
  public String displayPrettyName(String fieldname, boolean showMandatory, boolean before,
      BaseObject obj, XWikiContext context) {
    return getReadDoc().displayPrettyName(fieldname, showMandatory, before, obj, context);
  }

  @Override
  public String displayTooltip(String fieldname, XWikiContext context) {
    return getReadDoc().displayTooltip(fieldname, context);
  }

  @Override
  public String displayTooltip(String fieldname, BaseObject obj, XWikiContext context) {
    return getReadDoc().displayTooltip(fieldname, obj, context);
  }

  @Override
  public String display(String fieldname, XWikiContext context) {
    return getReadDoc().display(fieldname, context);
  }

  @Override
  public String display(String fieldname, BaseObject obj, XWikiContext context) {
    return getReadDoc().display(fieldname, obj, context);
  }

  @Override
  public String display(String fieldname, String mode, XWikiContext context) {
    return getReadDoc().display(fieldname, mode, context);
  }

  @Override
  public String display(String fieldname, String type, BaseObject obj, XWikiContext context) {
    return getReadDoc().display(fieldname, type, obj, context);
  }

  @Override
  public String display(String fieldname, String mode, String prefix, XWikiContext context) {
    return getReadDoc().display(fieldname, mode, prefix, context);
  }

  @Override
  public String display(String fieldname, String type, BaseObject obj, String wrappingSyntaxId,
      XWikiContext context) {
    return getReadDoc().display(fieldname, type, obj, wrappingSyntaxId, context);
  }

  @Override
  public String display(String fieldname, String type, String pref, BaseObject obj,
      XWikiContext context) {
    return getReadDoc().display(fieldname, type, pref, obj, context);
  }

  @Override
  public String display(String fieldname, String type, String pref, BaseObject obj,
      String wrappingSyntaxId, XWikiContext context) {
    return getReadDoc().display(fieldname, type, pref, obj, wrappingSyntaxId, context);
  }

  @Override
  public String displayForm(DocumentReference classReference, String header, String format,
      XWikiContext context) {
    return getReadDoc().displayForm(classReference, header, format, context);
  }

  @Override
  public String displayForm(String className, String header, String format, XWikiContext context) {
    return getReadDoc().displayForm(className, header, format, context);
  }

  @Override
  public String displayForm(DocumentReference classReference, String header, String format,
      boolean linebreak, XWikiContext context) {
    return getReadDoc().displayForm(classReference, header, format, linebreak, context);
  }

  @Override
  public String displayForm(String className, String header, String format, boolean linebreak,
      XWikiContext context) {
    return getReadDoc().displayForm(className, header, format, linebreak, context);
  }

  @Override
  public String displayForm(DocumentReference classReference, XWikiContext context) {
    return getReadDoc().displayForm(classReference, context);
  }

  @Override
  public String displayForm(String className, XWikiContext context) {
    return getReadDoc().displayForm(className, context);
  }

  @Override
  public boolean isFromCache() {
    return getReadDoc().isFromCache();
  }

  @Override
  public void setFromCache(boolean fromCache) {
    getEditDoc().setFromCache(fromCache);
  }

  @Override
  public void readDocMetaFromForm(EditForm eform, XWikiContext context) throws XWikiException {
    getEditDoc().readDocMetaFromForm(eform, context);
  }

  @Override
  public void setTags(String tagsStr, XWikiContext context) throws XWikiException {
    getEditDoc().setTags(tagsStr, context);
  }

  @Override
  public String getTags(XWikiContext context) {
    return getReadDoc().getTags(context);
  }

  @Override
  public List<String> getTagsList(XWikiContext context) {
    return super.getTagsList(context);
  }

  @Override
  public List<String> getTagsPossibleValues(XWikiContext context) {
    return getReadDoc().getTagsPossibleValues(context);
  }

  @Override
  public void readTranslationMetaFromForm(EditForm eform, XWikiContext context)
      throws XWikiException {
    getEditDoc().readTranslationMetaFromForm(eform, context);
  }

  @Override
  public void readObjectsFromForm(EditForm eform, XWikiContext context) throws XWikiException {
    getEditDoc().readObjectsFromForm(eform, context);
  }

  @Override
  public void readFromForm(EditForm eform, XWikiContext context) throws XWikiException {
    getEditDoc().readFromForm(eform, context);
  }

  @Override
  public void readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException {
    getEditDoc().readFromTemplate(eform, context);
  }

  @Override
  public void readFromTemplate(DocumentReference templateDocumentReference, XWikiContext context)
      throws XWikiException {
    getEditDoc().readFromTemplate(templateDocumentReference, context);
  }

  @Override
  public void readFromTemplate(String template, XWikiContext context) throws XWikiException {
    getEditDoc().readFromTemplate(template, context);
  }

  @Override
  public XWikiDocument clone() {
    return getReadDoc().clone();
  }

  @Override
  public XWikiDocument duplicate(DocumentReference newDocumentReference) {
    return getReadDoc().duplicate(newDocumentReference);
  }

  @Override
  public void copyAttachments(XWikiDocument sourceDocument) {
    getEditDoc().copyAttachments(sourceDocument);
  }

  @Override
  public void loadAttachments(XWikiContext context) throws XWikiException {
    getEditDoc().loadAttachments(context);
  }

  @Override
  public String toXML(org.dom4j.Document doc, XWikiContext context) {
    return getReadDoc().toXML(doc, context);
  }

  @Override
  public String getXMLContent(XWikiContext context) throws XWikiException {
    return getReadDoc().getXMLContent(context);
  }

  @Override
  public String toXML(XWikiContext context) throws XWikiException {
    return getReadDoc().toXML(context);
  }

  @Override
  public String toFullXML(XWikiContext context) throws XWikiException {
    return getReadDoc().toFullXML(context);
  }

  @Override
  public void addToZip(ZipOutputStream zos, String zipname, boolean withVersions,
      XWikiContext context) throws XWikiException, IOException {
    getReadDoc().addToZip(zos, zipname, withVersions, context);
  }

  @Override
  public void addToZip(ZipOutputStream zos, boolean withVersions, XWikiContext context)
      throws XWikiException, IOException {
    getReadDoc().addToZip(zos, withVersions, context);
  }

  @Override
  public void addToZip(ZipOutputStream zos, XWikiContext context)
      throws XWikiException, IOException {
    getReadDoc().addToZip(zos, context);
  }

  @Override
  public String toXML(boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
      boolean bWithVersions, XWikiContext context) throws XWikiException {
    return getReadDoc().toXML(bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions,
        context);
  }

  @Override
  public org.dom4j.Document toXMLDocument(XWikiContext context) throws XWikiException {
    return getReadDoc().toXMLDocument(context);
  }

  @Override
  public org.dom4j.Document toXMLDocument(boolean bWithObjects, boolean bWithRendering,
      boolean bWithAttachmentContent, boolean bWithVersions, XWikiContext context)
      throws XWikiException {
    return getReadDoc().toXMLDocument(bWithObjects, bWithRendering, bWithAttachmentContent,
        bWithVersions,
        context);
  }

  @Override
  public void toXML(XMLWriter wr, boolean bWithObjects, boolean bWithRendering,
      boolean bWithAttachmentContent, boolean bWithVersions, XWikiContext context)
      throws XWikiException, IOException {
    getReadDoc().toXML(wr, bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions,
        context);
  }

  @Override
  public void toXML(OutputStream out, boolean bWithObjects, boolean bWithRendering,
      boolean bWithAttachmentContent, boolean bWithVersions, XWikiContext context)
      throws XWikiException, IOException {
    getReadDoc().toXML(out, bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions,
        context);
  }

  @Override
  public void fromXML(String xml) throws XWikiException {
    getEditDoc().fromXML(xml);
  }

  @Override
  public void fromXML(InputStream is) throws XWikiException {
    getEditDoc().fromXML(is);
  }

  @Override
  public void fromXML(String xml, boolean withArchive) throws XWikiException {
    getEditDoc().fromXML(xml, withArchive);
  }

  @Override
  public void fromXML(InputStream in, boolean withArchive) throws XWikiException {
    getEditDoc().fromXML(in, withArchive);
  }

  @Override
  public void fromXML(org.dom4j.Document domdoc, boolean withArchive) throws XWikiException {
    getEditDoc().fromXML(domdoc, withArchive);
  }

  @Override
  public void setAttachmentList(List<XWikiAttachment> list) {
    getEditDoc().setAttachmentList(list);
  }

  @Override
  public List<XWikiAttachment> getAttachmentList() {
    return super.getAttachmentList();
  }

  @Override
  public void saveAllAttachments(XWikiContext context) throws XWikiException {
    getEditDoc().saveAllAttachments(context);
  }

  @Override
  public void saveAllAttachments(boolean updateParent, boolean transaction, XWikiContext context)
      throws XWikiException {
    getEditDoc().saveAllAttachments(updateParent, transaction, context);
  }

  @Override
  public void saveAttachmentsContent(List<XWikiAttachment> attachments, XWikiContext context)
      throws XWikiException {
    getEditDoc().saveAttachmentsContent(attachments, context);
  }

  @Override
  public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context)
      throws XWikiException {
    getEditDoc().saveAttachmentContent(attachment, context);
  }

  @Override
  public void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate,
      boolean bTransaction, XWikiContext context) throws XWikiException {
    getEditDoc().saveAttachmentContent(attachment, bParentUpdate, bTransaction, context);
  }

  @Override
  public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context)
      throws XWikiException {
    getEditDoc().loadAttachmentContent(attachment, context);
  }

  @Override
  public void deleteAttachment(XWikiAttachment attachment, XWikiContext context)
      throws XWikiException {
    getEditDoc().deleteAttachment(attachment, context);
  }

  @Override
  public void deleteAttachment(XWikiAttachment attachment, boolean toRecycleBin,
      XWikiContext context) throws XWikiException {
    getEditDoc().deleteAttachment(attachment, toRecycleBin, context);
  }

  @Override
  public List<DocumentReference> getBackLinkedReferences(XWikiContext context)
      throws XWikiException {
    return getReadDoc().getBackLinkedReferences(context);
  }

  @Override
  public List<String> getBackLinkedPages(XWikiContext context) throws XWikiException {
    return getReadDoc().getBackLinkedPages(context);
  }

  @Override
  public Set<XWikiLink> getUniqueWikiLinkedPages(XWikiContext context) throws XWikiException {
    return getReadDoc().getUniqueWikiLinkedPages(context);
  }

  @Override
  public Set<String> getUniqueLinkedPages(XWikiContext context) {
    return getReadDoc().getUniqueLinkedPages(context);
  }

  @Override
  public List<DocumentReference> getChildrenReferences(XWikiContext context) throws XWikiException {
    return getReadDoc().getChildrenReferences(context);
  }

  @Override
  public List<String> getChildren(XWikiContext context) throws XWikiException {
    return getReadDoc().getChildren(context);
  }

  @Override
  public List<DocumentReference> getChildrenReferences(int nb, int start, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getChildrenReferences(nb, start, context);
  }

  @Override
  public List<String> getChildren(int nb, int start, XWikiContext context) throws XWikiException {
    return getReadDoc().getChildren(nb, start, context);
  }

  @Override
  public void renameProperties(DocumentReference classReference,
      Map<String, String> fieldsToRename) {
    getEditDoc().renameProperties(classReference, fieldsToRename);
  }

  @Override
  public void renameProperties(String className, Map<String, String> fieldsToRename) {
    getEditDoc().renameProperties(className, fieldsToRename);
  }

  @Override
  public void addXObjectToRemove(BaseObject object) {
    getEditDoc().addXObjectToRemove(object);
  }

  @Override
  public void addObjectsToRemove(BaseObject object) {
    getEditDoc().addObjectsToRemove(object);
  }

  @Override
  public List<BaseObject> getXObjectsToRemove() {
    return super.getXObjectsToRemove();
  }

  @Override
  public ArrayList<BaseObject> getObjectsToRemove() {
    return super.getObjectsToRemove();
  }

  @Override
  public void setXObjectsToRemove(List<BaseObject> objectsToRemove) {
    getEditDoc().setXObjectsToRemove(objectsToRemove);
  }

  @Override
  public void setObjectsToRemove(ArrayList<BaseObject> objectsToRemove) {
    getEditDoc().setObjectsToRemove(objectsToRemove);
  }

  @Override
  public List<String> getIncludedPages(XWikiContext context) {
    return getReadDoc().getIncludedPages(context);
  }

  @Override
  public List<String> getIncludedMacros(XWikiContext context) {
    return getReadDoc().getIncludedMacros(context);
  }

  @Override
  public String displayRendered(PropertyClass pclass, String prefix, BaseCollection object,
      XWikiContext context) throws XWikiException {
    return getReadDoc().displayRendered(pclass, prefix, object, context);
  }

  @Override
  public String displayView(PropertyClass pclass, String prefix, BaseCollection object,
      XWikiContext context) {
    return getReadDoc().displayView(pclass, prefix, object, context);
  }

  @Override
  public String displayEdit(PropertyClass pclass, String prefix, BaseCollection object,
      XWikiContext context) {
    return getReadDoc().displayEdit(pclass, prefix, object, context);
  }

  @Override
  public String displayHidden(PropertyClass pclass, String prefix, BaseCollection object,
      XWikiContext context) {
    return getReadDoc().displayHidden(pclass, prefix, object, context);
  }

  @Override
  public XWikiAttachment getAttachment(String filename) {
    return super.getAttachment(filename);
  }

  @Override
  public XWikiAttachment addAttachment(String fileName, InputStream iStream, XWikiContext context)
      throws XWikiException, IOException {
    return getEditDoc().addAttachment(fileName, iStream, context);
  }

  @Override
  public XWikiAttachment addAttachment(String fileName, byte[] data, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addAttachment(fileName, data, context);
  }

  @Override
  public BaseObject getFirstObject(String fieldname) {
    return super.getFirstObject(fieldname);
  }

  @Override
  public BaseObject getFirstObject(String fieldname, XWikiContext context) {
    return super.getFirstObject(fieldname, context);
  }

  @Override
  public void setProperty(EntityReference classReference, String fieldName, BaseProperty value) {
    getEditDoc().setProperty(classReference, fieldName, value);
  }

  @Override
  public void setProperty(String className, String fieldName, BaseProperty value) {
    getEditDoc().setProperty(className, fieldName, value);
  }

  @Override
  public int getIntValue(DocumentReference classReference, String fieldName) {
    return getReadDoc().getIntValue(classReference, fieldName);
  }

  @Override
  public int getIntValue(String className, String fieldName) {
    return getReadDoc().getIntValue(className, fieldName);
  }

  @Override
  public long getLongValue(DocumentReference classReference, String fieldName) {
    return getReadDoc().getLongValue(classReference, fieldName);
  }

  @Override
  public long getLongValue(String className, String fieldName) {
    return getReadDoc().getLongValue(className, fieldName);
  }

  @Override
  public String getStringValue(DocumentReference classReference, String fieldName) {
    return getReadDoc().getStringValue(classReference, fieldName);
  }

  @Override
  public String getStringValue(String className, String fieldName) {
    return getReadDoc().getStringValue(className, fieldName);
  }

  @Override
  public int getIntValue(String fieldName) {
    return getReadDoc().getIntValue(fieldName);
  }

  @Override
  public long getLongValue(String fieldName) {
    return getReadDoc().getLongValue(fieldName);
  }

  @Override
  public String getStringValue(String fieldName) {
    return getReadDoc().getStringValue(fieldName);
  }

  @Override
  public void setStringValue(EntityReference classReference, String fieldName, String value) {
    getEditDoc().setStringValue(classReference, fieldName, value);
  }

  @Override
  public void setStringValue(String className, String fieldName, String value) {
    getEditDoc().setStringValue(className, fieldName, value);
  }

  @Override
  public List getListValue(DocumentReference classReference, String fieldName) {
    return super.getListValue(classReference, fieldName);
  }

  @Override
  public List getListValue(String className, String fieldName) {
    return super.getListValue(className, fieldName);
  }

  @Override
  public List getListValue(String fieldName) {
    return super.getListValue(fieldName);
  }

  @Override
  public void setStringListValue(EntityReference classReference, String fieldName, List value) {
    getEditDoc().setStringListValue(classReference, fieldName, value);
  }

  @Override
  public void setStringListValue(String className, String fieldName, List value) {
    getEditDoc().setStringListValue(className, fieldName, value);
  }

  @Override
  public void setDBStringListValue(EntityReference classReference, String fieldName, List value) {
    getEditDoc().setDBStringListValue(classReference, fieldName, value);
  }

  @Override
  public void setDBStringListValue(String className, String fieldName, List value) {
    getEditDoc().setDBStringListValue(className, fieldName, value);
  }

  @Override
  public void setLargeStringValue(EntityReference classReference, String fieldName, String value) {
    getEditDoc().setLargeStringValue(classReference, fieldName, value);
  }

  @Override
  public void setLargeStringValue(String className, String fieldName, String value) {
    getEditDoc().setLargeStringValue(className, fieldName, value);
  }

  @Override
  public void setIntValue(EntityReference classReference, String fieldName, int value) {
    getEditDoc().setIntValue(classReference, fieldName, value);
  }

  @Override
  public void setIntValue(String className, String fieldName, int value) {
    getEditDoc().setIntValue(className, fieldName, value);
  }

  @Override
  public String getDatabase() {
    return getReadDoc().getDatabase();
  }

  @Override
  public void setDatabase(String database) {
    getEditDoc().setDatabase(database);
  }

  @Override
  public String getLanguage() {
    return getReadDoc().getLanguage();
  }

  @Override
  public void setLanguage(String language) {
    getEditDoc().setLanguage(language);
  }

  @Override
  public String getDefaultLanguage() {
    return getReadDoc().getDefaultLanguage();
  }

  @Override
  public void setDefaultLanguage(String defaultLanguage) {
    getEditDoc().setDefaultLanguage(defaultLanguage);
  }

  @Override
  public int getTranslation() {
    return getReadDoc().getTranslation();
  }

  @Override
  public boolean isTrans() {
    return getReadDoc().isTrans();
  }

  @Override
  public void setTranslation(int translation) {
    getEditDoc().setTranslation(translation);
  }

  @Override
  public String getTranslatedContent(XWikiContext context) throws XWikiException {
    return getReadDoc().getTranslatedContent(context);
  }

  @Override
  public String getTranslatedContent(String language, XWikiContext context) throws XWikiException {
    return getReadDoc().getTranslatedContent(language, context);
  }

  @Override
  public XWikiDocument getTranslatedDocument(XWikiContext context) throws XWikiException {
    return getReadDoc().getTranslatedDocument(context);
  }

  @Override
  public XWikiDocument getTranslatedDocument(String language, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getTranslatedDocument(language, context);
  }

  @Override
  public String getRealLanguage(XWikiContext context) throws XWikiException {
    return getReadDoc().getRealLanguage(context);
  }

  @Override
  public String getRealLanguage() {
    return getReadDoc().getRealLanguage();
  }

  @Override
  public List<String> getTranslationList(XWikiContext context) throws XWikiException {
    return getReadDoc().getTranslationList(context);
  }

  @Override
  public List<Delta> getXMLDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
      throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getXMLDiff(fromDoc, toDoc, context);
  }

  @Override
  public List<Delta> getContentDiff(XWikiDocument fromDoc, XWikiDocument toDoc,
      XWikiContext context) throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getContentDiff(fromDoc, toDoc, context);
  }

  @Override
  public List<Delta> getContentDiff(String fromRev, String toRev, XWikiContext context)
      throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getContentDiff(fromRev, toRev, context);
  }

  @Override
  public List<Delta> getContentDiff(String fromRev, XWikiContext context)
      throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getContentDiff(fromRev, context);
  }

  @Override
  public List<Delta> getLastChanges(XWikiContext context)
      throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getLastChanges(context);
  }

  @Override
  public List<Delta> getRenderedContentDiff(XWikiDocument fromDoc, XWikiDocument toDoc,
      XWikiContext context) throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getRenderedContentDiff(fromDoc, toDoc, context);
  }

  @Override
  public List<Delta> getRenderedContentDiff(String fromRev, String toRev, XWikiContext context)
      throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getRenderedContentDiff(fromRev, toRev, context);
  }

  @Override
  public List<Delta> getRenderedContentDiff(String fromRev, XWikiContext context)
      throws XWikiException, DifferentiationFailedException {
    return getReadDoc().getRenderedContentDiff(fromRev, context);
  }

  @Override
  public List<MetaDataDiff> getMetaDataDiff(String fromRev, String toRev, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getMetaDataDiff(fromRev, toRev, context);
  }

  @Override
  public List<MetaDataDiff> getMetaDataDiff(String fromRev, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getMetaDataDiff(fromRev, context);
  }

  @Override
  public List<MetaDataDiff> getMetaDataDiff(XWikiDocument fromDoc, XWikiDocument toDoc,
      XWikiContext context) throws XWikiException {
    return getReadDoc().getMetaDataDiff(fromDoc, toDoc, context);
  }

  @Override
  public List<List<ObjectDiff>> getObjectDiff(String fromRev, String toRev, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getObjectDiff(fromRev, toRev, context);
  }

  @Override
  public List<List<ObjectDiff>> getObjectDiff(String fromRev, XWikiContext context)
      throws XWikiException {
    return getReadDoc().getObjectDiff(fromRev, context);
  }

  @Override
  public List<List<ObjectDiff>> getObjectDiff(XWikiDocument fromDoc, XWikiDocument toDoc,
      XWikiContext context) throws XWikiException {
    return getReadDoc().getObjectDiff(fromDoc, toDoc, context);
  }

  @Override
  public List<List<ObjectDiff>> getClassDiff(XWikiDocument fromDoc, XWikiDocument toDoc,
      XWikiContext context) throws XWikiException {
    return getReadDoc().getClassDiff(fromDoc, toDoc, context);
  }

  @Override
  public List<AttachmentDiff> getAttachmentDiff(XWikiDocument fromDoc, XWikiDocument toDoc,
      XWikiContext context) throws XWikiException {
    return getReadDoc().getAttachmentDiff(fromDoc, toDoc, context);
  }

  @Override
  public void rename(DocumentReference newDocumentReference, XWikiContext context)
      throws XWikiException {
    getEditDoc().rename(newDocumentReference, context);
  }

  @Override
  public void rename(String newDocumentName, XWikiContext context) throws XWikiException {
    getEditDoc().rename(newDocumentName, context);
  }

  @Override
  public void rename(DocumentReference newDocumentReference,
      List<DocumentReference> backlinkDocumentReferences, XWikiContext context)
      throws XWikiException {
    getEditDoc().rename(newDocumentReference, backlinkDocumentReferences, context);
  }

  @Override
  public void rename(String newDocumentName, List<String> backlinkDocumentNames,
      XWikiContext context) throws XWikiException {
    getEditDoc().rename(newDocumentName, backlinkDocumentNames, context);
  }

  @Override
  public void rename(DocumentReference newDocumentReference,
      List<DocumentReference> backlinkDocumentReferences,
      List<DocumentReference> childDocumentReferences, XWikiContext context) throws XWikiException {
    getEditDoc().rename(newDocumentReference, backlinkDocumentReferences, childDocumentReferences,
        context);
  }

  @Override
  public void rename(String newDocumentName, List<String> backlinkDocumentNames,
      List<String> childDocumentNames, XWikiContext context) throws XWikiException {
    getEditDoc().rename(newDocumentName, backlinkDocumentNames, childDocumentNames, context);
  }

  @Override
  public XWikiDocument copyDocument(DocumentReference newDocumentReference, XWikiContext context)
      throws XWikiException {
    return super.copyDocument(newDocumentReference, context);
  }

  @Override
  public XWikiDocument copyDocument(String newDocumentName, XWikiContext context)
      throws XWikiException {
    return super.copyDocument(newDocumentName, context);
  }

  @Override
  public XWikiLock getLock(XWikiContext context) throws XWikiException {
    return getReadDoc().getLock(context);
  }

  @Override
  public void setLock(String userName, XWikiContext context) throws XWikiException {
    getReadDoc().setLock(userName, context); // no doc state changes
  }

  @Override
  public void removeLock(XWikiContext context) throws XWikiException {
    getReadDoc().removeLock(context); // no doc state changes
  }

  @Override
  public void insertText(String text, String marker, XWikiContext context) throws XWikiException {
    getEditDoc().insertText(text, marker, context);
  }

  @Override
  public String getXClassXML() {
    return getReadDoc().getXClassXML();
  }

  @Override
  public String getxWikiClassXML() {
    return getReadDoc().getxWikiClassXML();
  }

  @Override
  public void setXClassXML(String xClassXML) {
    getEditDoc().setXClassXML(xClassXML);
  }

  @Override
  public void setxWikiClassXML(String xClassXML) {
    getEditDoc().setxWikiClassXML(xClassXML);
  }

  @Override
  public int getElements() {
    return getReadDoc().getElements();
  }

  @Override
  public void setElements(int elements) {
    getEditDoc().setElements(elements);
  }

  @Override
  public void setElement(int element, boolean toggle) {
    getEditDoc().setElement(element, toggle);
  }

  @Override
  public boolean hasElement(int element) {
    return getReadDoc().hasElement(element);
  }

  @Override
  public String getDefaultEditMode(XWikiContext context) throws XWikiException {
    return getReadDoc().getDefaultEditMode(context);
  }

  @Override
  public String getDefaultEditURL(XWikiContext context) throws XWikiException {
    return getReadDoc().getDefaultEditURL(context);
  }

  @Override
  public String getEditURL(String action, String mode, XWikiContext context) throws XWikiException {
    return getReadDoc().getEditURL(action, mode, context);
  }

  @Override
  public String getEditURL(String action, String mode, String language, XWikiContext context) {
    return getReadDoc().getEditURL(action, mode, language, context);
  }

  @Override
  public String getDefaultTemplate() {
    return getReadDoc().getDefaultTemplate();
  }

  @Override
  public void setDefaultTemplate(String defaultTemplate) {
    getEditDoc().setDefaultTemplate(defaultTemplate);
  }

  @Override
  public Vector<BaseObject> getComments() {
    return super.getComments();
  }

  @Override
  public Syntax getSyntax() {
    return getReadDoc().getSyntax();
  }

  @Override
  public String getSyntaxId() {
    return getReadDoc().getSyntaxId();
  }

  @Override
  public void setSyntax(Syntax syntax) {
    getEditDoc().setSyntax(syntax);
  }

  @Override
  public void setSyntaxId(String syntaxId) {
    getEditDoc().setSyntaxId(syntaxId);
  }

  @Override
  public Vector<BaseObject> getComments(boolean asc) {
    return super.getComments(asc);
  }

  @Override
  public boolean isCurrentUserCreator(XWikiContext context) {
    return getReadDoc().isCurrentUserCreator(context);
  }

  @Override
  public boolean isCreator(String username) {
    return getReadDoc().isCreator(username);
  }

  @Override
  public boolean isCurrentUserPage(XWikiContext context) {
    return getReadDoc().isCurrentUserPage(context);
  }

  @Override
  public boolean isCurrentLocalUserPage(XWikiContext context) {
    return getReadDoc().isCurrentLocalUserPage(context);
  }

  @Override
  public void resetArchive(XWikiContext context) throws XWikiException {
    getEditDoc().resetArchive(context);
  }

  @Override
  public BaseObject addXObjectFromRequest(XWikiContext context) throws XWikiException {
    return getEditDoc().addXObjectFromRequest(context);
  }

  @Override
  public BaseObject addObjectFromRequest(XWikiContext context) throws XWikiException {
    return getEditDoc().addObjectFromRequest(context);
  }

  @Override
  public BaseObject addXObjectFromRequest(EntityReference classReference, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addXObjectFromRequest(classReference, context);
  }

  @Override
  public BaseObject addObjectFromRequest(String className, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addObjectFromRequest(className, context);
  }

  @Override
  public BaseObject addXObjectFromRequest(DocumentReference classReference, String prefix,
      XWikiContext context) throws XWikiException {
    return getEditDoc().addXObjectFromRequest(classReference, prefix, context);
  }

  @Override
  public BaseObject addObjectFromRequest(String className, String prefix, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addObjectFromRequest(className, prefix, context);
  }

  @Override
  public List<BaseObject> addXObjectsFromRequest(DocumentReference classReference,
      XWikiContext context) throws XWikiException {
    return getEditDoc().addXObjectsFromRequest(classReference, context);
  }

  @Override
  public List<BaseObject> addObjectsFromRequest(String className, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addObjectsFromRequest(className, context);
  }

  @Override
  public List<BaseObject> addXObjectsFromRequest(DocumentReference classReference, String pref,
      XWikiContext context) throws XWikiException {
    return getEditDoc().addXObjectsFromRequest(classReference, pref, context);
  }

  @Override
  public List<BaseObject> addObjectsFromRequest(String className, String pref, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addObjectsFromRequest(className, pref, context);
  }

  @Override
  public BaseObject addXObjectFromRequest(DocumentReference classReference, int num,
      XWikiContext context) throws XWikiException {
    return getEditDoc().addXObjectFromRequest(classReference, num, context);
  }

  @Override
  public BaseObject addObjectFromRequest(String className, int num, XWikiContext context)
      throws XWikiException {
    return getEditDoc().addObjectFromRequest(className, num, context);
  }

  @Override
  public BaseObject addXObjectFromRequest(EntityReference classReference, String prefix, int num,
      XWikiContext context) throws XWikiException {
    return getEditDoc().addXObjectFromRequest(classReference, prefix, num, context);
  }

  @Override
  public BaseObject addObjectFromRequest(String className, String prefix, int num,
      XWikiContext context) throws XWikiException {
    return getEditDoc().addObjectFromRequest(className, prefix, num, context);
  }

  @Override
  public BaseObject updateXObjectFromRequest(EntityReference classReference, XWikiContext context)
      throws XWikiException {
    return getEditDoc().updateXObjectFromRequest(classReference, context);
  }

  @Override
  public BaseObject updateObjectFromRequest(String className, XWikiContext context)
      throws XWikiException {
    return getEditDoc().updateObjectFromRequest(className, context);
  }

  @Override
  public BaseObject updateXObjectFromRequest(EntityReference classReference, String prefix,
      XWikiContext context) throws XWikiException {
    return getEditDoc().updateXObjectFromRequest(classReference, prefix, context);
  }

  @Override
  public BaseObject updateObjectFromRequest(String className, String prefix, XWikiContext context)
      throws XWikiException {
    return getEditDoc().updateObjectFromRequest(className, prefix, context);
  }

  @Override
  public BaseObject updateXObjectFromRequest(EntityReference classReference, String prefix, int num,
      XWikiContext context) throws XWikiException {
    return getEditDoc().updateXObjectFromRequest(classReference, prefix, num, context);
  }

  @Override
  public BaseObject updateObjectFromRequest(String className, String prefix, int num,
      XWikiContext context) throws XWikiException {
    return getEditDoc().updateObjectFromRequest(className, prefix, num, context);
  }

  @Override
  public List<BaseObject> updateXObjectsFromRequest(EntityReference classReference,
      XWikiContext context) throws XWikiException {
    return getEditDoc().updateXObjectsFromRequest(classReference, context);
  }

  @Override
  public List<BaseObject> updateObjectsFromRequest(String className, XWikiContext context)
      throws XWikiException {
    return getEditDoc().updateObjectsFromRequest(className, context);
  }

  @Override
  public List<BaseObject> updateXObjectsFromRequest(EntityReference classReference, String pref,
      XWikiContext context) throws XWikiException {
    return getEditDoc().updateXObjectsFromRequest(classReference, pref, context);
  }

  @Override
  public List<BaseObject> updateObjectsFromRequest(String className, String pref,
      XWikiContext context) throws XWikiException {
    return getEditDoc().updateObjectsFromRequest(className, pref, context);
  }

  @Override
  public boolean isAdvancedContent() {
    return getReadDoc().isAdvancedContent();
  }

  @Override
  public boolean isProgrammaticContent() {
    return getReadDoc().isProgrammaticContent();
  }

  @Override
  public boolean removeXObject(BaseObject object) {
    return getEditDoc().removeXObject(object);
  }

  @Override
  public boolean removeObject(BaseObject object) {
    return getEditDoc().removeObject(object);
  }

  @Override
  public boolean removeXObjects(DocumentReference classReference) {
    return getEditDoc().removeXObjects(classReference);
  }

  @Override
  public boolean removeObjects(String className) {
    return getEditDoc().removeObjects(className);
  }

  @Override
  public List<DocumentSection> getSections() throws XWikiException {
    return super.getSections();
  }

  @Override
  public DocumentSection getDocumentSection(int sectionNumber) throws XWikiException {
    return super.getDocumentSection(sectionNumber);
  }

  @Override
  public String getContentOfSection(int sectionNumber) throws XWikiException {
    return getReadDoc().getContentOfSection(sectionNumber);
  }

  @Override
  public String updateDocumentSection(int sectionNumber, String newSectionContent)
      throws XWikiException {
    return getEditDoc().updateDocumentSection(sectionNumber, newSectionContent);
  }

  @Override
  public String getVersionHashCode(XWikiContext context) {
    return getReadDoc().getVersionHashCode(context);
  }

  @Override
  public String getInternalProperty(String propname) {
    return getReadDoc().getInternalProperty(propname);
  }

  @Override
  public String getCustomClass() {
    return getReadDoc().getCustomClass();
  }

  @Override
  public void setCustomClass(String customClass) {
    getEditDoc().setCustomClass(customClass);
  }

  @Override
  public void setValidationScript(String validationScript) {
    getEditDoc().setValidationScript(validationScript);
  }

  @Override
  public String getValidationScript() {
    return getReadDoc().getValidationScript();
  }

  @Override
  public String getComment() {
    return getReadDoc().getComment();
  }

  @Override
  public void setComment(String comment) {
    getEditDoc().setComment(comment);
  }

  @Override
  public boolean isMinorEdit() {
    return getReadDoc().isMinorEdit();
  }

  @Override
  public void setMinorEdit(boolean isMinor) {
    getEditDoc().setMinorEdit(isMinor);
  }

  @Override
  protected Boolean getMinorEdit1() {
    return getReadDoc().getMinorEdit1();
  }

  @Override
  protected void setMinorEdit1(Boolean isMinor) {
    getEditDoc().setMinorEdit1(isMinor);
  }

  @Override
  public BaseObject newXObject(EntityReference classReference, XWikiContext context)
      throws XWikiException {
    return getEditDoc().newXObject(classReference, context);
  }

  @Override
  public BaseObject newObject(String className, XWikiContext context) throws XWikiException {
    return getEditDoc().newObject(className, context);
  }

  @Override
  public BaseObject getXObject(DocumentReference classReference, boolean create,
      XWikiContext context) {
    return super.getXObject(classReference, create, context);
  }

  @Override
  public BaseObject getObject(String className, boolean create, XWikiContext context) {
    return super.getObject(className, create, context);
  }

  @Override
  public boolean validate(XWikiContext context) throws XWikiException {
    return getReadDoc().validate(context);
  }

  @Override
  public boolean validate(String[] classNames, XWikiContext context) throws XWikiException {
    return getReadDoc().validate(classNames, context);
  }

  @Override
  public String getPreviousVersion() {
    return getReadDoc().getPreviousVersion();
  }

  @Override
  public String serialize() {
    return getReadDoc().serialize();
  }

  @Override
  public String toString() {
    return getReadDoc().toString();
  }

  @Override
  public void setHidden(Boolean hidden) {
    getEditDoc().setHidden(hidden);
  }

  @Override
  public Boolean isHidden() {
    return getReadDoc().isHidden();
  }

  @Override
  public void convertSyntax(String targetSyntaxId, XWikiContext context) throws XWikiException {
    getEditDoc().convertSyntax(targetSyntaxId, context);
  }

  @Override
  public void convertSyntax(Syntax targetSyntax, XWikiContext context) throws XWikiException {
    getEditDoc().convertSyntax(targetSyntax, context);
  }

  @Override
  public XDOM getXDOM() {
    return getReadDoc().getXDOM();
  }

  @Override
  public boolean is10Syntax() {
    return getReadDoc().is10Syntax();
  }

  @Override
  public boolean is10Syntax(String syntaxId) {
    return getReadDoc().is10Syntax(syntaxId);
  }

  @Override
  public DocumentReference resolveClassReference(String documentName) {
    return getReadDoc().resolveClassReference(documentName);
  }

  @Override
  public void setListValue(String className, String fieldName, List value) {
    getEditDoc().setListValue(className, fieldName, value);
  }

  @Override
  public List<DocumentSection> getSplitSectionsAccordingToTitle() throws XWikiException {
    return super.getSplitSectionsAccordingToTitle();
  }

  @Override
  public List<String> getLinkedPages(XWikiContext context) {
    return getReadDoc().getLinkedPages(context);
  }

  @Override
  public List<XWikiLink> getLinks(XWikiContext context) throws XWikiException {
    return super.getLinks(context);
  }

  @Override
  public List<XWikiLink> getWikiLinkedPages(XWikiContext context) throws XWikiException {
    return super.getWikiLinkedPages(context);
  }

  @Override
  public List<String> getBacklinks(XWikiContext context) throws XWikiException {
    return super.getBacklinks(context);
  }

  @Override
  public String getRenderedContent(String text, XWikiContext context) {
    return getReadDoc().getRenderedContent(text, context);
  }

  @Override
  public boolean equals(Object object) {
    return getReadDoc().equals(object);
  }

  @Override
  public int hashCode() {
    return getReadDoc().hashCode();
  }

  // TODO remove the method?
  @Override
  public void setAsContextDoc(XWikiContext context) {
    // keep super here, to inject the wrapper into the context
    super.setAsContextDoc(context);
  }
}
