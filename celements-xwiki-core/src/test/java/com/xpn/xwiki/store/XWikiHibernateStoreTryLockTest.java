package com.xpn.xwiki.store;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiHibernateStoreTryLockTest extends AbstractComponentTest {

  private static final WikiReference TARGET_WIKI = new WikiReference("other-wiki");
  private static final Date EXPIRED_BEFORE = new Date(1000L);

  private XWikiHibernateStore store;
  private org.hibernate.classic.Session session;
  private Transaction transaction;
  private XWikiLock lock;
  private HibernateException closeFailure;
  private boolean upsertExecuted;
  private boolean ownerRead;

  @Before
  public void prepareTest() throws Exception {
    store = createMockBuilder(XWikiHibernateStore.class)
        .withConstructor()
        .addMockedMethod("getSessionFactory")
        .addMockedMethod("setDatabase", Session.class, WikiReference.class)
        .createMock();
    store.execution = getBeanFactory().getBean(Execution.class);
    SessionFactory factory = createDefaultMock(SessionFactory.class);
    session = createDefaultMock(org.hibernate.classic.Session.class);
    transaction = createDefaultMock(Transaction.class);
    lock = new XWikiLock(42L, "alice", TARGET_WIKI);
    expect(store.getSessionFactory()).andReturn(factory);
    expect(factory.openSession()).andReturn(session);
    store.setDatabase(same(session), eq(TARGET_WIKI));
    expectLastCall().andAnswer(() -> {
      store.setCurrentWiki(TARGET_WIKI);
      return null;
    });
    expect(session.beginTransaction()).andReturn(transaction);
    expect(session.close()).andAnswer(() -> {
      if (closeFailure != null) {
        throw closeFailure;
      }
      return null;
    });
  }

  @Test
  public void test_tryLock_commitsInTargetWikiAndPreservesCaller() throws Exception {
    Transaction outerTransaction = createDefaultMock(Transaction.class);
    Session outerSession = createDefaultMock(Session.class);
    outerSession.setFlushMode(FlushMode.COMMIT);
    store.setTransaction(outerTransaction);
    store.setSession(outerSession);
    WikiReference outerWiki = getContext().getWikiRef();
    assertNotEquals(outerWiki, TARGET_WIKI);
    store.setCurrentWiki(outerWiki);
    expectUpsert(1);
    expectOwner("alice");
    expectCommit();

    replayDefault(store);
    assertTrue(store.tryLock(lock, EXPIRED_BEFORE));
    assertSame(outerTransaction, store.getTransaction());
    assertSame(outerSession, store.getSession());
    assertSame(outerWiki, store.getCurrentWiki());
    assertEquals(outerWiki, getContext().getWikiRef());
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_activeLockForSameUser() throws Exception {
    expectUpsert(0);
    expectOwner("alice");
    expectCommit();
    replayDefault(store);
    assertTrue(store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_activeLockForOtherUserWithFoundRowCount() throws Exception {
    expectUpsert(1);
    expectOwner("bob");
    expectCommit();
    replayDefault(store);
    assertFalse(store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_reclaimedLockWithChangedRowCount() throws Exception {
    expectUpsert(2);
    expectOwner("alice");
    expectCommit();
    replayDefault(store);
    assertTrue(store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_upsertFailure() throws Exception {
    HibernateException databaseFailure = new HibernateException("upsert failed");
    expect(upsertQuery().executeUpdate()).andThrow(databaseFailure);
    transaction.rollback();
    replayDefault(store);
    XWikiException failure = assertThrows(XWikiException.class,
        () -> store.tryLock(lock, EXPIRED_BEFORE));
    assertSame(databaseFailure, failure.getCause());
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_ownerReadFailure() throws Exception {
    HibernateException readFailure = new HibernateException("owner read failed");
    expectUpsert(1);
    expect(expectLoadedLock().getUserName()).andThrow(readFailure);
    transaction.rollback();
    replayDefault(store);
    XWikiException failure = assertThrows(XWikiException.class,
        () -> store.tryLock(lock, EXPIRED_BEFORE));
    assertSame(readFailure, failure.getCause());
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_commitFailure() throws Exception {
    expectUpsert(1);
    expectOwner("alice");
    transaction.commit();
    expectLastCall().andThrow(new HibernateException("commit failed"));
    transaction.rollback();
    replayDefault(store);
    assertThrows(XWikiException.class, () -> store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_closeFailureAfterCommit() throws Exception {
    closeFailure = new HibernateException("close failed");
    expectUpsert(1);
    expectOwner("alice");
    expectCommit();
    replayDefault(store);
    assertTrue(store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_closeFailureAfterRejection() throws Exception {
    closeFailure = new HibernateException("close failed");
    expectUpsert(0);
    expectOwner("bob");
    expectCommit();
    replayDefault(store);
    assertFalse(store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_closeFailureDoesNotMaskDatabaseFailure() throws Exception {
    closeFailure = new HibernateException("close failed");
    HibernateException databaseFailure = new HibernateException("database unavailable");
    expect(upsertQuery().executeUpdate()).andThrow(databaseFailure);
    transaction.rollback();
    replayDefault(store);
    XWikiException failure = assertThrows(XWikiException.class,
        () -> store.tryLock(lock, EXPIRED_BEFORE));
    assertSame(databaseFailure, failure.getCause());
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_rollbackFailureIsSuppressed() throws Exception {
    HibernateException databaseFailure = new HibernateException("upsert failed");
    HibernateException rollbackFailure = new HibernateException("rollback failed");
    expect(upsertQuery().executeUpdate()).andThrow(databaseFailure);
    transaction.rollback();
    expectLastCall().andThrow(rollbackFailure);
    replayDefault(store);
    XWikiException failure = assertThrows(XWikiException.class,
        () -> store.tryLock(lock, EXPIRED_BEFORE));
    assertSame(databaseFailure, failure.getCause());
    assertSame(rollbackFailure, databaseFailure.getSuppressed()[0]);
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_legacyConstructorUsesExecutionWiki() throws Exception {
    lock = new XWikiLock(42L, "alice");
    assertTrue(lock.getWikiRef().isEmpty());
    getContext().setDatabase(TARGET_WIKI.getName());
    expectUpsert(1);
    expectOwner("alice");
    expectCommit();
    replayDefault(store);
    assertTrue(store.tryLock(lock, EXPIRED_BEFORE));
    verifyDefault(store);
  }

  private Query upsertQuery() {
    Query query = createDefaultMock(Query.class);
    expect(session.getNamedQuery("tryLock")).andReturn(query);
    expect(query.setLong("docId", lock.getDocId())).andReturn(query);
    expect(query.setString("userName", lock.getUserName())).andReturn(query);
    expect(query.setTimestamp("date", lock.getDate())).andReturn(query);
    expect(query.setTimestamp("expiredBefore", EXPIRED_BEFORE)).andReturn(query);
    return query;
  }

  private void expectUpsert(int updateCount) {
    expect(upsertQuery().executeUpdate()).andAnswer(() -> {
      upsertExecuted = true;
      return updateCount;
    });
  }

  private XWikiLock expectLoadedLock() {
    XWikiLock stored = createDefaultMock(XWikiLock.class);
    expect(session.load(XWikiLock.class, lock.getDocId())).andAnswer(() -> {
      assertTrue("load the stored lock after the upsert", upsertExecuted);
      return stored;
    });
    return stored;
  }

  private void expectOwner(String owner) {
    expect(expectLoadedLock().getUserName()).andAnswer(() -> {
      ownerRead = true;
      return owner;
    });
  }

  private void expectCommit() {
    transaction.commit();
    expectLastCall().andAnswer(() -> {
      assertTrue("commit after reading the owner", ownerRead);
      return null;
    });
  }

}
