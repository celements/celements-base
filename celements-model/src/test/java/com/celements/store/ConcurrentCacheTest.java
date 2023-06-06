package com.celements.store;

import static com.celements.store.TestHibernateQuery.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.easymock.IAnswer;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsAllPropertiesConfigurationSource;
import com.celements.store.DocumentCacheStore.InvalidateState;
import com.celements.store.id.IdVersion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.AbstractXWikiRunnable;
import com.xpn.xwiki.web.Utils;

public class ConcurrentCacheTest extends AbstractComponentTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentCacheTest.class);

  private volatile DocumentCacheStore cacheStore;
  private volatile Map<DocumentReference, List<BaseObject>> baseObjMap;
  private volatile Map<Long, List<String[]>> propertiesMap;
  private volatile DocumentReference testDocRef;
  private static volatile XWikiContext defaultContext;
  private final static AtomicBoolean fastFail = new AtomicBoolean();

  private final String wikiName = "testWiki";
  private final String testFullName = "TestSpace.TestDoc";
  private XWikiConfig config;
  private SessionFactory sessionFactoryMock;

  /**
   * CAUTION: the doc load counting with AtomicIntegers leads to better memory visibility
   * and thus reduces likeliness of a race condition. Hence it may camouflage the race condition!
   * Nevertheless is it important to check that the cache is working at all.
   * Hence test it with and without counting.
   */
  private final boolean verifyDocLoads = false;
  private final AtomicInteger countDocLoads = new AtomicInteger();
  private final AtomicInteger expectedCountDocLoads = new AtomicInteger();
  private final AtomicInteger failedToRemoveFromCacheCount = new AtomicInteger(0);
  private final AtomicInteger invalidatedLoadingCount = new AtomicInteger(0);
  private final AtomicInteger failedToInvalidatedLoadingCount = new AtomicInteger(0);
  private final AtomicInteger invalidatedMultipleCount = new AtomicInteger(0);

  @Before
  public void setUp_ConcurrentCacheTest() throws Exception {
    getXContext().setDatabase(wikiName);
    sessionFactoryMock = createDefaultMock(SessionFactory.class);
    Utils.getComponent(HibernateSessionFactory.class).setSessionFactory(sessionFactoryMock);
    testDocRef = new DocumentReference(wikiName, "TestSpace", "TestDoc");
    config = Utils.getComponent(XWikiConfigSource.class).getXWikiConfig();
    config.setProperty("xwiki.store.hibernate.path", "testhibernate.cfg.xml");
    registerComponentMock(ConfigurationSource.class, CelementsAllPropertiesConfigurationSource.NAME,
        getConfigurationSource());
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_EXIST_CACHE_CAPACITY, 10000);
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_DOC_CACHE_CAPACITY, 100);
    expect(getMock(XWiki.class).hasDynamicCustomMappings()).andReturn(false).anyTimes();
    expect(getMock(XWiki.class).getXClass(isA(DocumentReference.class), isA(
        XWikiContext.class))).andStubDelegateTo(new TestXWiki());
    createBaseObjects();
    createPropertiesMap();
  }

  @Test
  public void test_singleThreaded_sync() throws Exception {
    setupTestMocks();
    replayDefault();
    initStorePrepareMultiThreadMocks();
    preloadCache(cacheStore);
    if (verifyDocLoads) {
      assertEquals(expectedCountDocLoads.get(), countDocLoads.get());
    }
    assertNotNull("Expecting document in cache.", cacheStore.getDocFromCache(
        cacheStore.getKeyWithLang(testDocRef, "")));
    verifyDefault();
  }

  @Test
  public void test_singleThreaded_async() throws Exception {
    setupTestMocks();
    replayDefault();
    initStorePrepareMultiThreadMocks();
    preloadCache(cacheStore);
    ScheduledExecutorService theExecutor = Executors.newScheduledThreadPool(1);
    Future<LoadDocCheckResult> testFuture = theExecutor.submit(
        (Callable<LoadDocCheckResult>) new LoadXWikiDocCommand(cacheStore));
    theExecutor.shutdown();
    while (!theExecutor.isTerminated()) {
      theExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }
    LoadDocCheckResult result = testFuture.get();
    assertTrue(Arrays.deepToString(result.getMessages().toArray()), result.isSuccessfull());
    if (verifyDocLoads) {
      assertEquals(expectedCountDocLoads.get(), countDocLoads.get());
    }
    verifyDefault();
  }

  @Test
  public void test_multiRuns_singleThreaded_scenario1() throws Exception {
    int cores = 1;
    int executeRuns = 5000;
    setupTestMocks();
    replayDefault();
    initStorePrepareMultiThreadMocks();
    assertSuccessFullRuns(testScenario1(cacheStore, cores, executeRuns));
    if (verifyDocLoads) {
      assertEquals(expectedCountDocLoads.get(), countDocLoads.get());
    }
    verifyDefault();
  }

  @Test
  public void test_multiThreaded_scenario1_DocumentCacheStore() throws Exception {
    int cores = Runtime.getRuntime().availableProcessors();
    assertTrue("This tests needs real multi core processors, but found " + cores, cores > 1);
    // tested on an intel quadcore 4770
    // without triggering any race condition! Tested with up to 10'000'000 executeRuns!
    // int executeRuns = 10000000;
    int executeRuns = 30000;
    setupTestMocks();
    replayDefault();
    initStorePrepareMultiThreadMocks();
    assertSuccessFullRuns(testScenario1(cacheStore, cores, executeRuns));
    if (verifyDocLoads) {
      int countLoads = countDocLoads.get();
      int expectedLoads = expectedCountDocLoads.get();
      final String failingDetails = " expected loads '" + expectedLoads
          + "' must be lower equal to count loads '" + countLoads + "'\n diff '" + (countLoads
              - expectedLoads)
          + "'\n invalidatedLoadingCount '" + invalidatedLoadingCount.get()
          + "'\n invalidatedMultipleCount '" + invalidatedMultipleCount.get()
          + "'\n failedToInvalidatedLoadingCount '" + failedToInvalidatedLoadingCount.get()
          + "'\n failedToRemoveFromCacheCount '" + failedToRemoveFromCacheCount.get() + "'";
      assertTrue("invalidating during load leads to multiple loads for one invalidation, thus\n"
          + failingDetails, expectedLoads <= countLoads);
    }
    verifyDefault();
  }

  private void preloadCache(XWikiCacheStoreInterface store) throws Exception {
    if (verifyDocLoads) {
      countDocLoads.set(0);
      expectedCountDocLoads.set(1);
    }
    LoadXWikiDocCommand testLoadCommand = new LoadXWikiDocCommand(store);
    LoadDocCheckResult result = testLoadCommand.call();
    assertTrue(Arrays.deepToString(result.getMessages().toArray()), result.isSuccessfull());
  }

  private void setupTestMocks() {
    Session sessionMock = createDefaultMock(Session.class);
    expect(sessionFactoryMock.openSession()).andReturn(sessionMock).anyTimes();
    sessionMock.setFlushMode(eq(FlushMode.COMMIT));
    expectLastCall().atLeastOnce();
    sessionMock.setFlushMode(eq(FlushMode.MANUAL));
    expectLastCall().atLeastOnce();
    Transaction transactionMock = createDefaultMock(Transaction.class);
    expect(sessionMock.beginTransaction()).andReturn(transactionMock).anyTimes();
    transactionMock.rollback();
    expectLastCall().anyTimes();
    expect(sessionMock.close()).andReturn(null).anyTimes();
    XWikiDocument myDoc = new XWikiDocument(testDocRef);
    long docId = 0x718b1d979ccec000L;
    expectLoadExistingDocs(sessionMock, ImmutableList.of(
        new Object[] { docId, testFullName, myDoc.getLanguage() }));
    expectXWikiDocLoad(sessionMock, myDoc, docId);
    expectLoadAttachments(sessionMock, Collections.<XWikiAttachment>emptyList());
    expectLoadObjects(sessionMock, getNewObjList(testDocRef));
    expectLoadProperties(sessionMock, baseObjMap.get(testDocRef), propertiesMap);
  }

  /**
   * Scenario 1
   * prepare executeRuns as follows
   * 1.1 first and every 3*cores run add a reset cache entry task
   * 1.2 load document 3*cores in parallels for core threads
   * 2. invoke all tasks once to the executor
   * !!CAUTION!!!
   * be careful NOT to add accidentally any memory visibility synchronization
   * e.g. by using CountDownLatch or similar
   * for more details see:
   * http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/package-summary.html#
   * MemoryVisibility
   */
  private List<Future<LoadDocCheckResult>> testScenario1(XWikiCacheStoreInterface store, int cores,
      int maxLoadTasks) throws Exception {
    fastFail.set(false);
    preloadCache(store);
    final int numTimesFromCache = cores * 3;
    final int oneRunRepeats = 200;
    int count = (maxLoadTasks / (oneRunRepeats * numTimesFromCache)) + 1;
    ScheduledExecutorService theExecutor = Executors.newScheduledThreadPool(cores);
    List<Future<LoadDocCheckResult>> futureList = new ArrayList<>(count * (numTimesFromCache + 1)
        * oneRunRepeats);
    try {
      do {
        count--;
        List<Callable<LoadDocCheckResult>> loadTasks = new ArrayList<>(oneRunRepeats
            * numTimesFromCache);
        for (int i = 0; i < oneRunRepeats; i++) {
          if (i > 0) {
            loadTasks.add(new RefreshCacheEntryCommand(store));
            loadTasks.add(new LoadXWikiDocCommand(store));
          }
          for (int j = 1; j <= numTimesFromCache; j++) {
            loadTasks.add(new LoadXWikiDocCommand(store));
          }
        }
        futureList.addAll(theExecutor.invokeAll(loadTasks));
        final Future<LoadDocCheckResult> lastFuture = futureList.get(futureList.size()
            - oneRunRepeats);
        while (!lastFuture.isDone() && !fastFail.get()) {
          Thread.sleep(50);
        }
      } while (!fastFail.get() && (count > 0));
    } finally {
      theExecutor.shutdown();
      while (!theExecutor.isTerminated()) {
        if (fastFail.get()) {
          theExecutor.shutdownNow();
        }
        theExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
      }
    }
    return futureList;
  }

  private void assertSuccessFullRuns(List<Future<LoadDocCheckResult>> futureList)
      throws InterruptedException, ExecutionException {
    int successfulRuns = 0;
    int failedRuns = 0;
    List<String> failMessgs = new ArrayList<>();
    for (Future<LoadDocCheckResult> testFuture : futureList) {
      LoadDocCheckResult result = testFuture.get();
      if (result.isSuccessfull()) {
        successfulRuns += 1;
      } else {
        failedRuns += 1;
        List<String> messages = result.getMessages();
        failMessgs.add("Run num: " + (successfulRuns + failedRuns) + "\n");
        failMessgs.addAll(messages);
      }
    }
    assertEquals("Found " + failedRuns + " failing runs: " + Arrays.deepToString(
        failMessgs.toArray()), futureList.size(), successfulRuns);
  }

  private List<BaseObject> getNewObjList(DocumentReference fromDocRef) {
    List<BaseObject> objList = new ArrayList<>();
    for (BaseObject templBaseObject : baseObjMap.get(fromDocRef)) {
      BaseObject bObj = createBaseObject(templBaseObject.getNumber(),
          templBaseObject.getXClassReference());
      bObj.setDocumentReference(fromDocRef);
      objList.add(bObj);
    }
    return objList;
  }

  private void expectXWikiDocLoad(Session sessionMock, XWikiDocument myDoc, long docId) {
    sessionMock.load(isA(XWikiDocument.class), eq(docId));
    expectLastCall().andAnswer(new IAnswer<Object>() {

      @Override
      public Object answer() throws Throwable {
        XWikiDocument theDoc = (XWikiDocument) getCurrentArguments()[0];
        if (testDocRef.equals(theDoc.getDocumentReference())) {
          if (verifyDocLoads) {
            countDocLoads.incrementAndGet();
          }
          theDoc.setContent("test Content");
          theDoc.setTitle("the test Title");
          theDoc.setAuthor("XWiki.testAuthor");
          theDoc.setCreationDate(new java.sql.Date(new Date().getTime() - 5000L));
          theDoc.setContentUpdateDate(new java.sql.Date(new Date().getTime() - 2000L));
          theDoc.setLanguage("");
          theDoc.setDefaultLanguage("en");
        }
        return this;
      }
    }).anyTimes();
  }

  private void createBaseObjects() {
    DocumentReference testDocRefClone = new DocumentReference(testDocRef);
    BaseObject bObj1 = createBaseObject(0, new DocumentReference(wikiName, "Celements2",
        "MenuName"));
    bObj1.setDocumentReference(testDocRefClone);
    addStringField(bObj1, "lang", "de");
    addStringField(bObj1, "menu_name", "Hause");
    BaseObject bObj2 = createBaseObject(1, new DocumentReference(wikiName, "Celements2",
        "MenuName"));
    bObj2.setDocumentReference(testDocRefClone);
    addStringField(bObj2, "lang", "en");
    addStringField(bObj2, "menu_name", "Home");
    BaseObject bObj3 = createBaseObject(0, new DocumentReference(wikiName, "Celements2",
        "MenuItem"));
    bObj3.setDocumentReference(testDocRefClone);
    addIntField(bObj3, "menu_position", 1);
    BaseObject bObj4 = createBaseObject(0, new DocumentReference(wikiName, "Celements2",
        "PageType"));
    bObj4.setDocumentReference(testDocRefClone);
    addStringField(bObj4, "page_type", "Performance");
    List<BaseObject> attList = new Vector<>(Arrays.asList(bObj1, bObj2, bObj3, bObj4));
    baseObjMap = ImmutableMap.of(testDocRefClone, attList);
  }

  private void createPropertiesMap() {
    Map<Long, List<String[]>> propertiesMap = new HashMap<>();
    for (BaseObject templBaseObject : baseObjMap.get(testDocRef)) {
      List<String[]> propList = new ArrayList<>();
      for (Object theObj : templBaseObject.getFieldList()) {
        PropertyInterface theField = (PropertyInterface) theObj;
        String[] row = new String[2];
        row[0] = theField.getName();
        row[1] = theField.getClass().getCanonicalName();
        propList.add(row);
      }
      if (propertiesMap.containsKey(templBaseObject.getId())) {
        throw new IllegalStateException();
      }
      propertiesMap.put(templBaseObject.getId(), ImmutableList.copyOf(propList));
    }
    this.propertiesMap = ImmutableMap.copyOf(propertiesMap);
  }

  private void initStorePrepareMultiThreadMocks() throws XWikiException {
    defaultContext = (XWikiContext) getXContext().clone();
    cacheStore = (DocumentCacheStore) Utils.getComponent(XWikiStoreInterface.class,
        DocumentCacheStore.COMPONENT_NAME);
    cacheStore.initalize(); // ensure cache is initialized
    cacheStore.getStore(); // ensure store is initialized
  }

  private class LoadDocCheckResult {

    private final List<String> messages = new Vector<>();

    public void addMessage(String message) {
      messages.add(message);
      fastFail.set(true);
    }

    public boolean isSuccessfull() {
      return (messages.size() == 0);
    }

    public List<String> getMessages() {
      return messages;
    }

  }

  private abstract class AbstractXWikiTestFuture extends AbstractXWikiRunnable implements
      Callable<LoadDocCheckResult> {

    private boolean hasNewContext;
    private final LoadDocCheckResult result = new LoadDocCheckResult();
    private final XWikiCacheStoreInterface store;

    protected AbstractXWikiTestFuture(XWikiCacheStoreInterface store) {
      this.store = store;
    }

    protected XWikiCacheStoreInterface getStore() {
      return this.store;
    }

    private ExecutionContext getExecutionContext() {
      return Utils.getComponent(Execution.class).getContext();
    }

    protected LoadDocCheckResult getResult() {
      return result;
    }

    @Override
    public LoadDocCheckResult call() throws Exception {
      try {
        try {
          hasNewContext = (getExecutionContext() == null);
          if (hasNewContext) {
            initExecutionContext();
            getExecutionContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY,
                defaultContext.clone());
          }
          try {
            runInternal();
          } finally {
            if (hasNewContext) {
              // cleanup execution context
              cleanupExecutionContext();
            }
          }
        } catch (ExecutionContextException e) {
          LOGGER.error("Failed to initialize execution context", e);
        }
      } catch (Throwable exp) {
        // anything could happen in the test and we want to catch all failures
        getResult().addMessage("Exception: " + exp.getMessage() + "\n"
            + ExceptionUtils.getStackTrace(exp));
      }
      return getResult();
    }

  }

  private class RefreshCacheEntryCommand extends AbstractXWikiTestFuture {

    public RefreshCacheEntryCommand(XWikiCacheStoreInterface store) {
      super(store);
    }

    @Override
    public void runInternal() {
      if (!successfullRemoveFromCache()) {
        if (verifyDocLoads) {
          failedToRemoveFromCacheCount.incrementAndGet();
        }
      }
    }

    boolean successfullRemoveFromCache() {
      final InvalidateState invalidState = cacheStore.removeDocFromCache(new XWikiDocument(
          testDocRef), true);
      if (verifyDocLoads) {
        switch (invalidState) {
          case LOADING_CANCELED:
            invalidatedLoadingCount.incrementAndGet();
            break;
          case REMOVED:
            expectedCountDocLoads.incrementAndGet();
            break;
          case LOADING_MULTI_CANCELED:
            invalidatedMultipleCount.incrementAndGet();
            break;
          default:
            failedToInvalidatedLoadingCount.incrementAndGet();
            break;
        }
      }
      return (invalidState.equals(InvalidateState.LOADING_CANCELED) || invalidState.equals(
          InvalidateState.REMOVED));
    }
  }

  private class LoadXWikiDocCommand extends AbstractXWikiTestFuture {

    private XWikiDocument loadedXWikiDoc;

    public LoadXWikiDocCommand(XWikiCacheStoreInterface store) {
      super(store);
    }

    @Override
    protected void runInternal() {
      loadTestDocument();
      testLoadedDocument();
    }

    private void testLoadedDocument() {
      if (loadedXWikiDoc != null) {
        if (loadedXWikiDoc.isNew()) {
          getResult().addMessage("unexpected: isNew is true");
        }
        if (!loadedXWikiDoc.isMostRecent()) {
          getResult().addMessage("unexpected: isMostRecent is false");
        }
        for (BaseObject theTestObj : baseObjMap.get(testDocRef)) {
          Map<DocumentReference, List<BaseObject>> loadedObjs = loadedXWikiDoc.getXObjects();
          final List<BaseObject> xclassObjs = loadedObjs.get(theTestObj.getXClassReference());
          if (!xclassObjs.contains(theTestObj)) {
            getResult().addMessage("Object missing " + theTestObj);
          } else {
            BaseObject theLoadedObj = xclassObjs.get(xclassObjs.indexOf(theTestObj));
            if (theLoadedObj == theTestObj) {
              getResult().addMessage("Object is same " + theTestObj);
            } else {
              for (String theFieldName : theTestObj.getPropertyNames()) {
                BaseProperty theField = (BaseProperty) theLoadedObj.getField(theFieldName);
                BaseProperty theTestField = (BaseProperty) theTestObj.getField(theFieldName);
                if (theField == theTestField) {
                  getResult().addMessage("Field is same " + theField);
                } else if (!theTestField.getValue().equals(theField.getValue())) {
                  getResult().addMessage("Field value missmatch expected: " + theField
                      + "\n but found: " + theField.getValue());
                }
              }
            }
          }
        }
      } else {
        getResult().addMessage("Loaded document reference is null.");
      }
    }

    private void loadTestDocument() {
      try {
        XWikiDocument myDoc = new XWikiDocument(testDocRef);
        try {
          loadedXWikiDoc = getStore().loadXWikiDoc(myDoc, getXContext());
        } catch (XWikiException exp) {
          throw new IllegalStateException(exp);
        }
      } catch (Exception exp) {
        throw new RuntimeException(exp);
      }
    }

  }

  private final void addIntField(BaseObject bObj, String fieldName, int value) {
    bObj.setIntValue(fieldName, value);
  }

  private final void addStringField(BaseObject bObj, String fieldName, String value) {
    bObj.setStringValue(fieldName, value);
  }

  private final BaseObject createBaseObject(int num, DocumentReference classRef) {
    BaseObject bObj = new BaseObject();
    bObj.setXClassReference(new DocumentReference(classRef));
    bObj.setNumber(num);
    bObj.setId(bObj.hashCode(), IdVersion.XWIKI_2);
    return bObj;
  }

  private class TestXWiki extends XWiki {

    @Override
    public BaseClass getXClass(DocumentReference documentReference, XWikiContext context)
        throws XWikiException {
      // Used to avoid recursive loading of documents if there are recursive usage of classes
      BaseClass bclass = context.getBaseClass(documentReference);
      if (bclass == null) {
        bclass = new BaseClass();
        bclass.setDocumentReference(documentReference);
        context.addBaseClass(bclass);
      }

      return bclass;
    }
  }

}
