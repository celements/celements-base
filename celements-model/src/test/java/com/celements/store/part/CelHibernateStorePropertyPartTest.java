package com.celements.store.part;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.store.CelHibernateStore;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringProperty;

public class CelHibernateStorePropertyPartTest extends AbstractComponentTest {

  private CelHibernateStorePropertyPart storePart;

  private CelHibernateStore storeMock;
  private Session sessionMock;

  @Before
  public void prepareTest() throws Exception {
    storeMock = createDefaultMock(CelHibernateStore.class);
    expect(getWikiMock().getStore()).andReturn(storeMock).anyTimes();
    sessionMock = createDefaultMock(Session.class);
    storePart = new CelHibernateStorePropertyPart(storeMock);
  }

  @Test
  public void test_loadXWikiProperty() throws Exception {
    BaseProperty property = new BaseProperty();

    expect(storeMock.getSession(same(getContext()))).andReturn(sessionMock);
    sessionMock.load(same(property), same(property));

    replayDefault();
    storePart.loadXWikiProperty(property, getContext(), false);
    verifyDefault();
  }

  @Test
  public void test_loadXWikiProperty_transaction() throws Exception {
    BaseProperty property = new BaseProperty();

    storeMock.checkHibernate(same(getContext()));
    expect(storeMock.beginTransaction(eq(false), same(getContext()))).andReturn(true);
    expect(storeMock.getSession(same(getContext()))).andReturn(sessionMock);
    sessionMock.load(same(property), same(property));
    storeMock.endTransaction(same(getContext()), eq(false), eq(false));

    replayDefault();
    storePart.loadXWikiProperty(property, getContext(), true);
    verifyDefault();
  }

  @Test
  public void test_loadXWikiProperty_StringProperty() throws Exception {
    StringProperty property = new StringProperty();

    expect(storeMock.getSession(same(getContext()))).andReturn(sessionMock);
    sessionMock.load(same(property), same(property));

    replayDefault();
    storePart.loadXWikiProperty(property, getContext(), false);
    verifyDefault();

    assertEquals("null should be converted to empty string", "", property.getValue());
  }

  @Test
  public void test_loadXWikiProperty_DateProperty() throws Exception {
    Date date = new Timestamp(4321);
    DateProperty property = new DateProperty();
    property.setValue(date);

    expect(storeMock.getSession(same(getContext()))).andReturn(sessionMock);
    sessionMock.load(same(property), same(property));

    replayDefault();
    storePart.loadXWikiProperty(property, getContext(), false);
    verifyDefault();

    assertSame("value should be of class Date", Date.class, property.getValue().getClass());
    assertTrue(property.getValue().equals(date));
  }

  @Test
  public void test_loadXWikiProperty_ListProperty() throws Exception {
    ListProperty property = createDefaultMock(ListProperty.class);

    expect(storeMock.getSession(same(getContext()))).andReturn(sessionMock);
    sessionMock.load(same(property), same(property));
    expect(property.getList()).andReturn(Collections.<String>emptyList());

    replayDefault();
    storePart.loadXWikiProperty(property, getContext(), false);
    verifyDefault();
  }

}
