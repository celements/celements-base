package com.celements.init;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.web.Utils;

public class JsonConfigOverwriteFromDiskTest extends AbstractComponentTest {

  private final WikiReference wiki = new WikiReference("test");
  private final Map<String, String> resources = new HashMap<>();
  private JsonConfigOverwriteFromDisk strategy;
  private ModelUtils modelUtils;
  private int reads;
  private int closes;

  @Before
  public void preparePolicy() throws Exception {
    modelUtils = Utils.getComponent(ModelUtils.class);
    ServletContext servlet = createNiceMock(ServletContext.class);
    expect(servlet.getResource(anyString())).andAnswer(() -> resources.containsKey(
        getCurrentArgument(0)) ? new URL("file:/present") : null).anyTimes();
    expect(servlet.getResourceAsStream(anyString())).andAnswer(() -> {
      reads++;
      var content = resources.get(getCurrentArgument(0));
      return content == null ? null : new ByteArrayInputStream(
          content.getBytes(StandardCharsets.UTF_8)) {
        @Override
        public void close() {
          closes++;
        }
      };
    }).anyTimes();
    replay(servlet);
    strategy = new JsonConfigOverwriteFromDisk(servlet, modelUtils);
  }

  @Test
  public void absentAndEmptyGrantNothing() {
    assertFalse(strategy.forWiki(wiki).test(reference("XWiki.XWikiPreferences")));
    assertEquals(0, reads);
    resources.put("/docs/test.json", "[]");
    assertFalse(strategy.forWiki(wiki).test(reference("XWiki.XWikiPreferences")));
    assertEquals(1, reads);
    assertTrue(closes > 0);
  }

  @Test
  public void exactWikiScopeImmutableSnapshotAndReload() {
    resources.put("/docs/test.json", "[\"XWiki.XWikiPreferences\",\"XWiki.XWikiPreferences\"]");
    var first = strategy.forWiki(wiki);
    resources.put("/docs/test.json", "[\"Main.Other\"]");
    assertTrue(first.test(reference("XWiki.XWikiPreferences")));
    assertFalse(first.test(reference("Main.Other")));
    assertFalse(first.test(new DocumentReference("other", "XWiki", "XWikiPreferences")));
    assertFalse(first.test(reference("xwiki.XWikiPreferences")));
    assertEquals(1, reads);

    var second = strategy.forWiki(wiki);
    assertFalse(second.test(reference("XWiki.XWikiPreferences")));
    assertTrue(second.test(reference("Main.Other")));
    assertTrue(first.test(reference("XWiki.XWikiPreferences")));
    assertEquals(2, reads);
  }

  @Test
  public void escapedDotsAreLiteralNamesInTheExistingSingleSpaceGrammar() {
    var name = "Space.Page\\.WithDot";
    resources.put("/docs/test.json", "[\"Space.Page\\\\.WithDot\",\"Outer\\\\.Inner.Page\"]");
    var snapshot = strategy.forWiki(wiki);
    assertTrue(snapshot.test(reference(name)));
    assertTrue(snapshot.test(reference("Outer\\.Inner.Page")));
    assertEquals("Page.WithDot", reference(name).getName());
    assertEquals("Outer.Inner", reference("Outer\\.Inner.Page").getParent().getName());
    assertEquals(wiki, reference("Outer\\.Inner.Page").getParent().getParent());
  }

  @Test
  public void noncanonicalUnescapedMultiDotNamesAreRejected() {
    resources.put("/docs/test.json", "[\"Outer.Inner.Page\"]");
    assertThrows(IllegalArgumentException.class, () -> strategy.forWiki(wiki));
  }

  @Test
  public void rejectsInvalidResourcesWithoutDisclosingContents() {
    for (var content : new String[] { "", "{}", "null", "[null]", "[1]", "[true]",
        "[\"\"]", "[\" \"]", "[\" XWiki.XWikiPreferences\"]", "[\"Main.Secret*\"]",
        "[\"other:Main.Secret\"]", "[\"test:Main.Secret\"]", "[\"/Main.Secret\"]",
        "[\"Main.Secret\",", "[\"Main.Secret\"] true", "[\"Main.\"]",
        "[\"Secret\"]", "[\"Main.Secr(et)\"]" }) {
      resources.put("/docs/test.json", content);
      var exception = assertThrows(IllegalArgumentException.class, () -> strategy.forWiki(wiki));
      assertTrue(exception.getMessage().contains("/docs/test.json"));
      assertFalse(exception.getMessage().contains("Secret"));
      assertNull(exception.getCause());
    }
  }

  @Test
  public void presentButUnreadableFailsClosed() throws Exception {
    ServletContext servlet = createNiceMock(ServletContext.class);
    expect(servlet.getResource("/docs/test.json")).andReturn(new URL("file:/present"));
    expect(servlet.getResourceAsStream("/docs/test.json")).andReturn(null);
    replay(servlet);
    var unreadable = new JsonConfigOverwriteFromDisk(servlet, modelUtils);
    assertThrows(IllegalArgumentException.class, () -> unreadable.forWiki(wiki));
    verify(servlet);
  }

  private DocumentReference reference(String name) {
    return modelUtils.resolveRef(name, DocumentReference.class, wiki);
  }
}
