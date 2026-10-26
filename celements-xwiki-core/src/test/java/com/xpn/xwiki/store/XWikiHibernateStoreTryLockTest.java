package com.xpn.xwiki.store;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.sql.BatchUpdateException;
import java.sql.SQLException;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiHibernateStoreTryLockTest extends AbstractComponentTest {

  private static final WikiReference TARGET_WIKI = new WikiReference("other-wiki");

  private XWikiHibernateStore store;
  private org.hibernate.classic.Session session;
  private Transaction transaction;
  private XWikiLock lock;

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
    lock = new XWikiLock(42L, "alice");
    expect(store.getSessionFactory()).andReturn(factory);
    expect(factory.openSession()).andReturn(session);
    store.setDatabase(same(session), eq(TARGET_WIKI));
    expectLastCall().andAnswer(() -> {
      store.setCurrentWiki(TARGET_WIKI);
      return null;
    });
    expect(session.beginTransaction()).andReturn(transaction);
    expect(session.save(same(lock))).andReturn(lock.getDocId());
    expect(session.close()).andReturn(null);
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
    transaction.commit();

    replayDefault(store);
    assertTrue(store.tryLock(lock, TARGET_WIKI));
    assertSame(outerTransaction, store.getTransaction());
    assertSame(outerSession, store.getSession());
    assertSame(outerWiki, store.getCurrentWiki());
    assertEquals(outerWiki, getContext().getWikiRef());
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_duplicateAtCommit() throws Exception {
    expectFailure(new SQLException("duplicate key", "23000", 1062));
    replayDefault(store);
    assertFalse(store.tryLock(lock, TARGET_WIKI));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_duplicateInBatchException() throws Exception {
    SQLException batch = new BatchUpdateException("batch update failed", new int[0]);
    batch.setNextException(new SQLException("duplicate key", "23000", 1062));
    expectFailure(batch);
    replayDefault(store);
    assertFalse(store.tryLock(lock, TARGET_WIKI));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_otherConstraintFailure() throws Exception {
    expectFailure(new SQLException("invalid column value", "23000", 1048));
    replayDefault(store);
    assertThrows(XWikiException.class, () -> store.tryLock(lock, TARGET_WIKI));
    verifyDefault(store);
  }

  @Test
  public void test_tryLock_databaseFailure() throws Exception {
    transaction.commit();
    expectLastCall().andThrow(new HibernateException("database unavailable"));
    transaction.rollback();
    replayDefault(store);
    assertThrows(XWikiException.class, () -> store.tryLock(lock, TARGET_WIKI));
    verifyDefault(store);
  }

  private void expectFailure(SQLException sqlException) {
    transaction.commit();
    expectLastCall().andThrow(new ConstraintViolationException("insert failed", sqlException,
        "xwikilock_pkey"));
    transaction.rollback();
  }

}
