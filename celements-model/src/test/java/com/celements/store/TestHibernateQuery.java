package com.celements.store;

import static org.easymock.EasyMock.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.easymock.IAnswer;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.type.Type;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;

import one.util.streamex.StreamEx;

public abstract class TestHibernateQuery<T> extends AbstractQueryImpl {

  private Query theQueryMock;
  protected Map<String, Object> params;

  public TestHibernateQuery(String queryStr) {
    super(queryStr, FlushMode.AUTO, null, null);
    this.params = new HashMap<>();
    theQueryMock = createMock(Query.class);
    replay(theQueryMock);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Iterator iterate() throws HibernateException {
    return theQueryMock.iterate();
  }

  @Override
  public ScrollableResults scroll() throws HibernateException {
    return theQueryMock.scroll();
  }

  @Override
  public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
    return theQueryMock.scroll(scrollMode);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<T> list() throws HibernateException {
    return theQueryMock.list();
  }

  @Override
  public Query setText(String named, String val) {
    this.params.put(named, val);
    return this;
  }

  @Override
  public Query setString(String named, String val) {
    this.params.put(named, val);
    return this;
  }

  @Override
  public Query setLong(String named, long val) {
    this.params.put(named, Long.valueOf(val));
    return this;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Query setParameterList(String named, Collection vals) throws HibernateException {
    this.params.put(named, vals);
    return this;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Query setParameterList(String named, Collection vals, Type type)
      throws HibernateException {
    this.params.put(named, vals);
    return this;
  }

  @Override
  public int executeUpdate() throws HibernateException {
    return theQueryMock.executeUpdate();
  }

  @Override
  public Query setLockMode(String alias, LockMode lockMode) {
    return theQueryMock.setLockMode(alias, lockMode);
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected Map getLockModes() {
    throw new UnsupportedOperationException("getLockModes not supported");
  }

  public static void expectLoadExistingDocs(Session sessionMock, List<Object[]> existingDocs) {
    String hql = "select id, fullName, language from XWikiDocument where id in (:ids) order by id";
    Query query = new TestHibernateQuery<XWikiAttachment>(hql) {

      @Override
      public Iterator<Object[]> iterate() throws HibernateException {
        return StreamEx.of(existingDocs)
            .mapToEntry(row -> (long) row[0], row -> row)
            .filterKeys(((Collection<?>) params.get("ids"))::contains)
            .sortedBy(Map.Entry::getKey)
            .values().iterator();
      }
    };
    expect(sessionMock.createQuery(eq(hql))).andReturn(query).anyTimes();
  }

  public static void expectLoadAttachments(Session sessionMock,
      final List<XWikiAttachment> attList) {
    String hql = "from XWikiAttachment as attach where attach.docId=:docid";
    Query query = new TestHibernateQuery<XWikiAttachment>(hql) {

      @Override
      public List<XWikiAttachment> list() throws HibernateException {
        return attList;
      }
    };
    expect(sessionMock.createQuery(eq(hql))).andReturn(query).anyTimes();
  }

  public static void expectLoadObjects(Session sessionMock, final List<BaseObject> objList) {
    String hql = "from BaseObject as obj where obj.name = :name order by obj.className, obj.number";
    Query queryObj = new TestHibernateQuery<BaseObject>(hql) {

      @Override
      public List<BaseObject> list() throws HibernateException {
        return objList;
      }

      @Override
      public Iterator<BaseObject> iterate() throws HibernateException {
        return objList.iterator();
      }
    };
    expect(sessionMock.createQuery(eq(hql))).andReturn(queryObj).anyTimes();
  }

  public static void expectLoadProperties(Session sessionMock, final List<BaseObject> objList,
      final Map<Long, List<String[]>> propertiesMap) {
    String hql = "select prop.name, prop.classType from BaseProperty as prop where prop.id.id = :id";
    Query queryProp = new TestHibernateQuery<String[]>(hql) {

      @Override
      public List<String[]> list() throws HibernateException {
        return propertiesMap.get(params.get("id"));
      }
    };
    expect(sessionMock.createQuery(eq(hql))).andReturn(queryProp).atLeastOnce();
    sessionMock.load(isA(PropertyInterface.class), isA(Serializable.class));
    expectLastCall().andAnswer(new IAnswer<Object>() {

      @Override
      public Object answer() throws Throwable {
        BaseProperty property = (BaseProperty) getCurrentArguments()[0];
        Long objId = property.getObject().getId();
        for (BaseObject templBaseObject : objList) {
          if (objId.equals(templBaseObject.getId())) {
            for (Object theObj : templBaseObject.getFieldList()) {
              BaseProperty theField = (BaseProperty) theObj;
              if (theField.getName().equals(property.getName()) && theField.getClass().equals(
                  property.getClass())) {
                property.setValue(theField.getValue());
              }
            }
          }
        }
        return this;
      }
    }).atLeastOnce();
  }

}
