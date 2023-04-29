package com.xpn.xwiki.objects;

import static com.google.common.base.Preconditions.*;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

public class BaseObjectProxy extends BaseObject {

  private final BaseObject readonly;
  private BaseObject editable;

  public BaseObjectProxy(BaseObject readonly) {
    super();
    this.readonly = checkNotNull(readonly);
  }

  private BaseObject getRead() {
    return (editable != null) ? editable : readonly;
  }

  private BaseObject getEdit() {
    if (editable == null) {
      editable = (BaseObject) readonly.clone();
    }
    return editable;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return super.getName();
  }

  @Override
  public void setName(String name) {
    // TODO Auto-generated method stub
    super.setName(name);
  }

  @Override
  public int hashCode() {
    // TODO Auto-generated method stub
    return super.hashCode();
  }

  @Override
  public void displayHidden(StringBuffer buffer, String name, String prefix, XWikiContext context) {
    // TODO Auto-generated method stub
    super.displayHidden(buffer, name, prefix, context);
  }

  @Override
  public void displayView(StringBuffer buffer, String name, String prefix, XWikiContext context) {
    // TODO Auto-generated method stub
    super.displayView(buffer, name, prefix, context);
  }

  @Override
  public void displayEdit(StringBuffer buffer, String name, String prefix, XWikiContext context) {
    // TODO Auto-generated method stub
    super.displayEdit(buffer, name, prefix, context);
  }

  @Override
  public String displayHidden(String name, String prefix, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.displayHidden(name, prefix, context);
  }

  @Override
  public String displayView(String name, String prefix, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.displayView(name, prefix, context);
  }

  @Override
  public String displayEdit(String name, String prefix, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.displayEdit(name, prefix, context);
  }

  @Override
  public String displayHidden(String name, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.displayHidden(name, context);
  }

  @Override
  public String displayView(String name, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.displayView(name, context);
  }

  @Override
  public String displayEdit(String name, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.displayEdit(name, context);
  }

  @Override
  public Object clone() {
    // TODO Auto-generated method stub
    return super.clone();
  }

  @Override
  protected BaseObject clone(boolean keepsIdentity) {
    // TODO Auto-generated method stub
    return super.clone(keepsIdentity);
  }

  @Override
  public BaseObject duplicate() {
    // TODO Auto-generated method stub
    return super.duplicate();
  }

  @Override
  public BaseObject duplicate(DocumentReference documentReference) {
    // TODO Auto-generated method stub
    return super.duplicate(documentReference);
  }

  @Override
  public boolean equals(Object obj) {
    // TODO Auto-generated method stub
    return super.equals(obj);
  }

  @Override
  public Element toXML(BaseClass bclass) {
    // TODO Auto-generated method stub
    return super.toXML(bclass);
  }

  @Override
  public void fromXML(Element oel) throws XWikiException {
    // TODO Auto-generated method stub
    super.fromXML(oel);
  }

  @Override
  public List<ObjectDiff> getDiff(Object oldEntity, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.getDiff(oldEntity, context);
  }

  @Override
  public com.xpn.xwiki.api.Object newObjectApi(BaseObject obj, XWikiContext context) {
    // TODO Auto-generated method stub
    return super.newObjectApi(obj, context);
  }

  @Override
  public void set(String fieldname, Object value, XWikiContext context) {
    // TODO Auto-generated method stub
    super.set(fieldname, value, context);
  }

  @Override
  public String getGuid() {
    // TODO Auto-generated method stub
    return super.getGuid();
  }

  @Override
  public void setGuid(String guid) {
    // TODO Auto-generated method stub
    super.setGuid(guid);
  }

  @Override
  public int getNumber() {
    // TODO Auto-generated method stub
    return super.getNumber();
  }

  @Override
  public void setNumber(int number) {
    // TODO Auto-generated method stub
    super.setNumber(number);
  }

  @Override
  public void addPropertyForRemoval(PropertyInterface field) {
    // TODO Auto-generated method stub
    super.addPropertyForRemoval(field);
  }

  @Override
  public DocumentReference getXClassReference() {
    // TODO Auto-generated method stub
    return super.getXClassReference();
  }

  @Override
  public String getClassName() {
    // TODO Auto-generated method stub
    return super.getClassName();
  }

  @Override
  public void setXClassReference(EntityReference xClassReference) {
    // TODO Auto-generated method stub
    super.setXClassReference(xClassReference);
  }

  @Override
  public void setClassName(String name) {
    // TODO Auto-generated method stub
    super.setClassName(name);
  }

  @Override
  public PropertyInterface safeget(String name) {
    // TODO Auto-generated method stub
    return super.safeget(name);
  }

  @Override
  public PropertyInterface get(String name) throws XWikiException {
    // TODO Auto-generated method stub
    return super.get(name);
  }

  @Override
  public void safeput(String name, PropertyInterface property) {
    // TODO Auto-generated method stub
    super.safeput(name, property);
  }

  @Override
  public void put(String name, PropertyInterface property) throws XWikiException {
    // TODO Auto-generated method stub
    super.put(name, property);
  }

  @Override
  public BaseClass getXClass(XWikiContext context) {
    // TODO Auto-generated method stub
    return super.getXClass(context);
  }

  @Override
  public BaseClass getxWikiClass(XWikiContext context) {
    // TODO Auto-generated method stub
    return super.getxWikiClass(context);
  }

  @Override
  public String getStringValue(String name) {
    // TODO Auto-generated method stub
    return super.getStringValue(name);
  }

  @Override
  public String getLargeStringValue(String name) {
    // TODO Auto-generated method stub
    return super.getLargeStringValue(name);
  }

  @Override
  public void setStringValue(String name, String value) {
    // TODO Auto-generated method stub
    super.setStringValue(name, value);
  }

  @Override
  public void setLargeStringValue(String name, String value) {
    // TODO Auto-generated method stub
    super.setLargeStringValue(name, value);
  }

  @Override
  public int getIntValue(String name) {
    // TODO Auto-generated method stub
    return super.getIntValue(name);
  }

  @Override
  public int getIntValue(String name, int default_value) {
    // TODO Auto-generated method stub
    return super.getIntValue(name, default_value);
  }

  @Override
  public void setIntValue(String name, int value) {
    // TODO Auto-generated method stub
    super.setIntValue(name, value);
  }

  @Override
  public long getLongValue(String name) {
    // TODO Auto-generated method stub
    return super.getLongValue(name);
  }

  @Override
  public void setLongValue(String name, long value) {
    // TODO Auto-generated method stub
    super.setLongValue(name, value);
  }

  @Override
  public float getFloatValue(String name) {
    // TODO Auto-generated method stub
    return super.getFloatValue(name);
  }

  @Override
  public void setFloatValue(String name, float value) {
    // TODO Auto-generated method stub
    super.setFloatValue(name, value);
  }

  @Override
  public double getDoubleValue(String name) {
    // TODO Auto-generated method stub
    return super.getDoubleValue(name);
  }

  @Override
  public void setDoubleValue(String name, double value) {
    // TODO Auto-generated method stub
    super.setDoubleValue(name, value);
  }

  @Override
  public Date getDateValue(String name) {
    // TODO Auto-generated method stub
    return super.getDateValue(name);
  }

  @Override
  public void setDateValue(String name, Date value) {
    // TODO Auto-generated method stub
    super.setDateValue(name, value);
  }

  @Override
  public Set<?> getSetValue(String name) {
    // TODO Auto-generated method stub
    return super.getSetValue(name);
  }

  @Override
  public void setSetValue(String name, Set<?> value) {
    // TODO Auto-generated method stub
    super.setSetValue(name, value);
  }

  @Override
  public List getListValue(String name) {
    // TODO Auto-generated method stub
    return super.getListValue(name);
  }

  @Override
  public void setStringListValue(String name, List value) {
    // TODO Auto-generated method stub
    super.setStringListValue(name, value);
  }

  @Override
  public void setDBStringListValue(String name, List value) {
    // TODO Auto-generated method stub
    super.setDBStringListValue(name, value);
  }

  @Override
  public void setFields(Map fields) {
    // TODO Auto-generated method stub
    super.setFields(fields);
  }

  @Override
  public PropertyInterface getField(String name) {
    // TODO Auto-generated method stub
    return super.getField(name);
  }

  @Override
  public void addField(String name, PropertyInterface element) {
    // TODO Auto-generated method stub
    super.addField(name, element);
  }

  @Override
  public void removeField(String name) {
    // TODO Auto-generated method stub
    super.removeField(name);
  }

  @Override
  public Collection getFieldList() {
    // TODO Auto-generated method stub
    return super.getFieldList();
  }

  @Override
  public Set<String> getPropertyList() {
    // TODO Auto-generated method stub
    return super.getPropertyList();
  }

  @Override
  public Object[] getProperties() {
    // TODO Auto-generated method stub
    return super.getProperties();
  }

  @Override
  public String[] getPropertyNames() {
    // TODO Auto-generated method stub
    return super.getPropertyNames();
  }

  @Override
  public Iterator getSortedIterator() {
    // TODO Auto-generated method stub
    return super.getSortedIterator();
  }

  @Override
  public void merge(BaseObject object) {
    // TODO Auto-generated method stub
    super.merge(object);
  }

  @Override
  public List getFieldsToRemove() {
    // TODO Auto-generated method stub
    return super.getFieldsToRemove();
  }

  @Override
  public void setFieldsToRemove(List fieldsToRemove) {
    // TODO Auto-generated method stub
    super.setFieldsToRemove(fieldsToRemove);
  }

  @Override
  public String toXMLString() {
    // TODO Auto-generated method stub
    return super.toXMLString();
  }

  @Override
  public String toString(boolean withDefinition) {
    // TODO Auto-generated method stub
    return super.toString(withDefinition);
  }

  @Override
  public Map<String, Object> getCustomMappingMap() throws XWikiException {
    // TODO Auto-generated method stub
    return super.getCustomMappingMap();
  }

  @Override
  public void setDocumentReference(DocumentReference reference) {
    // TODO Auto-generated method stub
    super.setDocumentReference(reference);
  }

  @Override
  public void setWiki(String wiki) {
    // TODO Auto-generated method stub
    super.setWiki(wiki);
  }

  @Override
  public void setListValue(String name, List value) {
    // TODO Auto-generated method stub
    super.setListValue(name, value);
  }

  @Override
  public long getId() {
    // TODO Auto-generated method stub
    return super.getId();
  }

  @Override
  public void setId(long id, IdVersion idVersion) {
    // TODO Auto-generated method stub
    super.setId(id, idVersion);
  }

  @Override
  protected void setIdInternal(long id, IdVersion idVersion) {
    // TODO Auto-generated method stub
    super.setIdInternal(id, idVersion);
  }

  @Override
  public boolean hasValidId() {
    // TODO Auto-generated method stub
    return super.hasValidId();
  }

  @Override
  public IdVersion getIdVersion() {
    // TODO Auto-generated method stub
    return super.getIdVersion();
  }

  @Override
  public DocumentReference getDocumentReference() {
    // TODO Auto-generated method stub
    return super.getDocumentReference();
  }

  @Override
  public String getPrettyName() {
    // TODO Auto-generated method stub
    return super.getPrettyName();
  }

  @Override
  public void setPrettyName(String name) {
    // TODO Auto-generated method stub
    super.setPrettyName(name);
  }

  @Override
  public String getWiki() {
    // TODO Auto-generated method stub
    return super.getWiki();
  }

  @Override
  public XWikiDocument getDocument(XWikiContext context) throws XWikiException {
    // TODO Auto-generated method stub
    return super.getDocument(context);
  }

  @Override
  public String getDocumentSyntaxId(XWikiContext context) {
    // TODO Auto-generated method stub
    return super.getDocumentSyntaxId(context);
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return super.toString();
  }

  @Override
  protected void finalize() throws Throwable {
    // TODO Auto-generated method stub
    super.finalize();
  }

}
