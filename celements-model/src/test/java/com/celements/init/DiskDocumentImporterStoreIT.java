package com.celements.init;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.hibernate.Version;
import org.hibernate.connection.DriverManagerConnectionProvider;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.Contextualiser;
import com.celements.model.util.ModelUtils;
import com.celements.store.CelHibernateStore;
import com.celements.store.DocumentCacheStore;
import com.celements.store.ModelAccessStore;
import com.celements.store.StoreFactory;
import com.celements.wiki.WikiService;
import com.mysql.jdbc.Driver;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;

/** Opt-in destructive schema creation, restricted to the owned loopback acceptance database. */
public class DiskDocumentImporterStoreIT extends AbstractComponentTest {

  private final WikiReference wiki = new WikiReference("test");
  private final Map<String, String> resources = new HashMap<>();
  private CelHibernateStore store;

  @After
  public void closeStore() {
    if (store != null && store.getSessionFactory() != null) {
      store.getSessionFactory().close();
      store.setSessionFactory(null);
    }
  }

  @Test
  public void actualPackageAndStoreReplacementWithControlsAndSecondInvocation() throws Exception {
    String port = System.getProperty("celdev1355.testDbPort");
    assumeNotNull(port);
    assertEquals("32769", port);
    assertEquals("FIX-STORE-01", System.getProperty("celdev1355.storeAcceptance"));
    verifyOwnedContainer();
    prepareStore(port);
    new Contextualiser().withWiki(wiki).execute(() -> {
      try {
        exerciseReplacement();
      } catch (Exception exception) {
        throw new AssertionError("actual Package/store acceptance failed", exception);
      }
    });
  }

  private void verifyOwnedContainer() throws Exception {
    String format = "{{.Id}}|{{.Name}}|{{index .Config.Labels \"task\"}}|"
        + "{{index .Config.Labels \"owner\"}}|{{.State.Running}}|{{.Config.Image}}|"
        + "{{.Config.Hostname}}|"
        + "{{len .NetworkSettings.Ports}}|{{range $port, $bindings := .NetworkSettings.Ports}}"
        + "{{$port}}={{len $bindings}}{{range $bindings}}:{{.HostIp}}:{{.HostPort}}{{end}}{{end}}|"
        + "{{range .Mounts}}{{.Type}}:{{.Destination}}:{{.RW}};{{end}}";
    var process = new ProcessBuilder("/usr/bin/docker", "--host",
        "unix:///run/user/1001/docker.sock", "inspect", "--type=container", "--format", format,
        "celdev1355-imp01-mariadb").redirectErrorStream(true).start();
    if (!process.waitFor(15, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      fail("owned container inspection timed out before database contact");
    }
    String identity = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
        .strip();
    assertEquals("owned container inspection failed: " + identity, 0, process.exitValue());
    assertEquals("6ce9fc6f03e0703dc8a3808fa1c42cd9e801120c3f3f9943f1bf1a166d78c9d8"
        + "|/celdev1355-imp01-mariadb|CELDEV-1355|IMP-01|true|mariadb:10.11|6ce9fc6f03e0|1|"
        + "3306/tcp=1:127.0.0.1:32769|volume:/var/lib/mysql:true;", identity);
    System.out.println("CELDEV1355 verified owned container before JDBC: " + identity);
  }

  private void prepareStore(String port) throws Exception {
    WikiService wikis = registerComponentMock(WikiService.class);
    expect(wikis.hasWiki(wiki)).andReturn(true).anyTimes();
    replay(wikis);
    getConfigurationSource().setProperty("celements.store.main", ModelAccessStore.NAME);
    getConfigurationSource().setProperty("celements.store.modelAccess.backingStore",
        DocumentCacheStore.COMPONENT_NAME);
    getConfigurationSource().setProperty("celements.store.docCache.backingStore",
        CelHibernateStore.NAME);
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_DOC_CACHE_CAPACITY, 100);
    getConfigurationSource().setProperty("celements.store.docCache.capacityExists", 100);
    var cfg = Utils.getComponent(XWikiConfigSource.class).getXWikiConfig();
    cfg.setProperty("xwiki.db.prefix", "celdev1355_");
    cfg.setProperty("xwiki.store.hibernate.useclasstables.read", "0");
    cfg.setProperty("xwiki.store.hibernate.useclasstables.write", "0");
    cfg.setProperty("xwiki.store.versioning", "1");
    store = (CelHibernateStore) Utils.getComponent(XWikiStoreInterface.class,
        CelHibernateStore.NAME);
    assertEquals(CelHibernateStore.class, store.getClass());
    assertEquals("3.5.6-Final", Version.getVersionString());
    var versioning = Utils.getComponent(XWikiVersioningStoreInterface.class);
    assertEquals(XWikiHibernateVersioningStore.class, versioning.getClass());
    assertEquals("celdev1355_test", store.getSchemaFromWikiName(wiki));
    var hibernate = store.getConfiguration();
    hibernate.setProperties(new Properties());
    hibernate.setProperty("hibernate.hbm2ddl.auto", "none");
    hibernate.setProperty("hibernate.connection.provider_class",
        DriverManagerConnectionProvider.class.getName());
    hibernate.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
    hibernate.setProperty("hibernate.connection.url", "jdbc:mysql://127.0.0.1:" + port
        + "/celdev1355_test?useSSL=false");
    hibernate.setProperty("hibernate.connection.username", "root");
    hibernate.setProperty("hibernate.connection.password", "");
    hibernate.setProperty("hibernate.dialect", "com.celements.query.CelMySQLDialect");
    hibernate.setProperty("hibernate.bytecode.use_reflection_optimizer", "false");
    hibernate.addResource("xwiki.celements.hbm.xml");
    hibernate.buildMappings();
    var tables = hibernate.getTableMappings();
    while (tables.hasNext()) {
      var table = (Table) tables.next();
      assertNull("mapping must not route to another catalog", table.getCatalog());
      assertNull("mapping must not route to another schema", table.getSchema());
    }
    String importFile = "celdev1355-disabled-import.sql";
    assertNull("unowned schema import script must not be on the classpath",
        getClass().getClassLoader().getResource(importFile));
    var jdbc = new Properties();
    jdbc.setProperty("user", "root");
    jdbc.setProperty("password", "");
    try (var connection = new Driver().connect(hibernate.getProperty("hibernate.connection.url"),
        jdbc)) {
      assertNotNull("approved JDBC driver must accept the owned URL", connection);
      try (var statement = connection.createStatement();
          var identity = statement.executeQuery("SELECT DATABASE(), @@hostname, VERSION()")) {
        assertTrue(identity.next());
        assertEquals("celdev1355_test", identity.getString(1));
        assertEquals("6ce9fc6f03e0", identity.getString(2));
        assertTrue(identity.getString(3).startsWith("10.11.19-MariaDB"));
        assertFalse(identity.next());
      }
      var drop = new SchemaExport(hibernate, connection).setImportFile(importFile);
      drop.drop(true, true);
      assertTrue("schema reset drop errors must be preserved and stop before create: "
          + formatExceptions(drop.getExceptions()), drop.getExceptions().isEmpty());
      var schema = new SchemaExport(hibernate, connection).setImportFile(importFile);
      schema.execute(true, true, false, true);
      assertTrue("isolated schema creation errors: " + formatExceptions(schema.getExceptions()),
          schema.getExceptions().isEmpty());
    }
    store.setSessionFactory(hibernate.buildSessionFactory());
    XWikiRightService rights = createNiceMock(XWikiRightService.class);
    expect(rights.hasAdminRights(anyObject())).andReturn(true).anyTimes();
    XWikiMessageTool messages = createNiceMock(XWikiMessageTool.class);
    expect(messages.get(anyString())).andReturn("disk acceptance import").anyTimes();
    XWikiNotificationManager notifications = createNiceMock(XWikiNotificationManager.class);
    ObservationManager observations = registerComponentMock(ObservationManager.class);
    observations.notify(anyObject(), anyObject(), anyObject());
    expectLastCall().anyTimes();
    replay(rights, messages, notifications, observations);
    var xwiki = new AcceptanceWiki(rights);
    var mainStore = StoreFactory.getMainStore();
    if (mainStore instanceof ModelAccessStore modelStore
        && modelStore.getBackingStore() instanceof DocumentCacheStore cache) {
      assertSame(store, cache.getBackingStore());
    } else {
      throw new AssertionError("expected real modelAccess -> docCache -> celHibernate chain");
    }
    if (versioning instanceof XWikiHibernateVersioningStore hibernateVersioning) {
      assertSame(store.getSessionFactory(), hibernateVersioning.getSessionFactory());
    } else {
      throw new AssertionError("expected real Hibernate versioning store");
    }
    xwiki.setStore(mainStore);
    xwiki.setVersioningStore(versioning);
    xwiki.setNotificationManager(notifications);
    getContext().setWiki(xwiki);
    getContext().setUser("XWiki.ImportOperator");
    getContext().put("msg", messages);
    assertTrue(xwiki.hasVersioning(getContext()));
    assertNoSession();
  }

  private String formatExceptions(List<?> exceptions) {
    var result = new StringWriter();
    var writer = new PrintWriter(result);
    for (Object failure : exceptions) {
      if (failure instanceof Throwable throwable) {
        throwable.printStackTrace(writer);
      } else {
        writer.println(failure);
      }
    }
    writer.flush();
    return result.toString();
  }

  private void exerciseReplacement() throws Exception {
    var access = Utils.getComponent(IModelAccessFacade.class);
    var prefsRef = new DocumentReference("test", "XWiki", "XWikiPreferences");
    var original = document(prefsRef, "database", "oldField", "old-value");
    var unrelatedRef = new DocumentReference("test", "XWiki", "Unrelated");
    var unrelatedClass = document(unrelatedRef, "unrelated class", "", "");
    unrelatedClass.getXClass().addTextField("custom", "custom", 30);
    getContext().getWiki().saveDocument(unrelatedClass, getContext());
    assertNoSession();
    var unrelated = new BaseObject();
    unrelated.setXClassReference(unrelatedRef);
    unrelated.setDocumentReference(prefsRef);
    unrelated.setStringValue("custom", "unrelated-value");
    original.addXObject(unrelated);
    getContext().getWiki().saveDocument(original, getContext());
    assertNoSession();
    long id = original.getId();
    String seedVersion = original.getVersion();
    var controlRef = new DocumentReference("test", "Main", "Control");
    getContext().getWiki().saveDocument(document(controlRef, "control-database", "", ""),
        getContext());
    assertNoSession();
    String controlVersion = persisted(controlRef).getVersion();
    var disk = document(prefsRef, "disk", "diskField", "disk-value");
    resources.put("/docs/test/preferences.xml", disk.toXML(getContext()));
    resources.put("/docs/test/control.xml", document(controlRef, "control-disk", "", "")
        .toXML(getContext()));
    var missingRef = new DocumentReference("test", "Main", "Missing");
    resources.put("/docs/test/missing.xml", document(missingRef, "missing-disk", "", "")
        .toXML(getContext()));
    resources.put("/docs/test.json", "[\"XWiki.XWikiPreferences\"]");
    var importer = importer(access);

    assertTrue(access.existsLang(prefsRef, ""));
    assertFalse(access.existsLang(missingRef, ""));
    assertNoSession();
    importer.importForWiki(wiki, false);
    assertNoSession();
    var imported = persisted(prefsRef);
    assertFalse(imported.isNew());
    assertEquals(id, imported.getId());
    assertEquals(prefsRef, imported.getDocumentReference());
    assertEquals("disk", imported.getContent());
    assertEquals("disk title", imported.getTitle());
    assertNull(imported.getXClass().safeget("oldField"));
    assertNotNull(imported.getXClass().safeget("diskField"));
    assertEquals("disk-value", imported.getXObject(prefsRef).getStringValue("diskField"));
    // Source-derived expectations; actual G3 acceptance is still required for this store contract.
    assertEquals("old-value", imported.getXObject(prefsRef).getStringValue("oldField"));
    assertNotNull(imported.getXObject(unrelatedRef));
    assertEquals("unrelated-value", imported.getXObject(unrelatedRef).getStringValue("custom"));
    assertEquals("control-database", persisted(controlRef).getContent());
    assertEquals(controlVersion, persisted(controlRef).getVersion());
    var missing = persisted(missingRef);
    assertEquals("missing-disk", missing.getContent());
    String missingVersion = missing.getVersion();
    assertEquals("XWiki.ImportOperator", imported.getAuthor());
    assertEquals("XWiki.ImportOperator", imported.getContentAuthor());
    assertEquals("XWiki.ImportOperator", imported.getCreator());
    assertEquals(new Date(946684800000L), imported.getCreationDate());
    assertTrue(imported.getDate().after(new Date(946684800000L)));
    var versioning = getContext().getWiki().getVersioningStore();
    var firstVersions = versioning.getXWikiDocVersions(imported, getContext());
    assertTrue(Arrays.stream(firstVersions).anyMatch(version -> seedVersion.equals(version.toString())));
    assertEquals("database", versioning.loadXWikiDoc(imported, seedVersion, getContext()).getContent());
    assertTrue("import must advance real history", firstVersions.length >= 2);
    assertNoSession();

    var edited = access.getDocument(prefsRef);
    edited.setContent("operator edit");
    getContext().getWiki().saveDocument(edited, getContext());
    assertNoSession();
    String operatorVersion = persisted(prefsRef).getVersion();
    assertEquals("operator edit", access.getDocument(prefsRef).getContent());
    importer.importForWiki(wiki, false);
    assertNoSession();
    var repeated = persisted(prefsRef);
    assertEquals(id, repeated.getId());
    assertEquals("disk", repeated.getContent());
    assertEquals("disk-value", repeated.getXObject(prefsRef).getStringValue("diskField"));
    assertEquals("old-value", repeated.getXObject(prefsRef).getStringValue("oldField"));
    assertEquals("unrelated-value", repeated.getXObject(unrelatedRef).getStringValue("custom"));
    assertNull(repeated.getXClass().safeget("oldField"));
    assertNotNull(repeated.getXClass().safeget("diskField"));
    assertEquals("XWiki.ImportOperator", repeated.getAuthor());
    assertEquals("XWiki.ImportOperator", repeated.getContentAuthor());
    assertEquals(imported.getCreator(), repeated.getCreator());
    assertEquals(imported.getCreationDate(), repeated.getCreationDate());
    assertFalse(repeated.getDate().before(imported.getDate()));
    assertEquals("disk title", repeated.getTitle());
    assertFalse(operatorVersion.equals(repeated.getVersion()));
    var repeatedVersions = versioning.getXWikiDocVersions(repeated, getContext());
    assertTrue(repeatedVersions.length > firstVersions.length);
    assertTrue(Arrays.stream(repeatedVersions)
        .anyMatch(version -> operatorVersion.equals(version.toString())));
    assertEquals("operator edit",
        versioning.loadXWikiDoc(repeated, operatorVersion, getContext()).getContent());
    assertEquals("control-database", persisted(controlRef).getContent());
    assertEquals(controlVersion, persisted(controlRef).getVersion());
    assertEquals("missing-disk", persisted(missingRef).getContent());
    assertEquals(missingVersion, persisted(missingRef).getVersion());
    assertNoSession();
    System.out.println("CELDEV1355 all persisted replacement assertions completed");
  }

  private XWikiDocument persisted(DocumentReference reference) throws Exception {
    assertNoSession();
    var document = store.loadXWikiDoc(new XWikiDocument(reference), getContext());
    assertNoSession();
    return document;
  }

  private void assertNoSession() {
    assertNull("standalone operation must close its session", store.getSession());
    assertNull("standalone operation must close its transaction", store.getTransaction());
  }

  private XWikiDocument document(DocumentReference reference, String content, String field,
      String value) {
    var document = new XWikiDocument(reference);
    document.setContent(content);
    document.setTitle(content + " title");
    document.setCreator("XWiki.DiskCreator");
    document.setAuthor("XWiki.DiskAuthor");
    document.setContentAuthor("XWiki.DiskAuthor");
    document.setCreationDate(new Date(946684800000L));
    if (!field.isEmpty()) {
      document.getXClass().addTextField(field, field, 30);
      var object = new BaseObject();
      object.setDocumentReference(reference);
      object.setXClassReference(reference);
      object.setStringValue(field, value);
      document.addXObject(object);
    }
    return document;
  }

  private DiskDocumentImporter importer(IModelAccessFacade access) throws Exception {
    ServletContext servlet = createNiceMock(ServletContext.class);
    expect(servlet.getResourcePaths("/docs/")).andReturn(Set.of("/docs/test/")).anyTimes();
    expect(servlet.getResourcePaths("/docs/test")).andReturn(Set.of(
        "/docs/test/preferences.xml", "/docs/test/control.xml", "/docs/test/missing.xml"))
        .anyTimes();
    expect(servlet.getResource(anyString())).andAnswer(() -> resources.containsKey(
        getCurrentArgument(0)) ? new URL("file:/present") : null).anyTimes();
    expect(servlet.getResourceAsStream(anyString())).andAnswer(() -> new ByteArrayInputStream(
        resources.get(getCurrentArgument(0)).getBytes(StandardCharsets.UTF_8))).anyTimes();
    replay(servlet);
    var modelUtils = Utils.getComponent(ModelUtils.class);
    return new DiskDocumentImporter(servlet, Utils.getComponent(Execution.class),
        Utils.getComponent(WikiService.class), modelUtils, access,
        new JsonConfigOverwriteFromDisk(servlet, modelUtils));
  }

  /** Real document read/save methods; only optional runtime services are disabled. */
  private static class AcceptanceWiki extends XWiki {

    private final XWikiRightService rights;

    AcceptanceWiki(XWikiRightService rights) {
      this.rights = rights;
    }

    @Override
    public XWikiRightService getRightService() {
      return rights;
    }

    @Override
    public boolean hasBacklinks(XWikiContext context) {
      return false;
    }

    @Override
    public boolean hasDynamicCustomMappings() {
      return false;
    }
  }
}
