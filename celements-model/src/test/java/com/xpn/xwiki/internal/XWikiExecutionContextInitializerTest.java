package com.xpn.xwiki.internal;

import static com.celements.execution.XWikiExecutionProp.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiUser;

public class XWikiExecutionContextInitializerTest extends AbstractComponentTest {

  @Test
  public void test_initializeFromSource() throws Exception {
    assertTrue(getBeanFactory().getBeansOfType(ExecutionContextInitializer.class).values().stream()
        .anyMatch(XWikiExecutionContextInitializer.class::isInstance));
    ExecutionContext source = getBeanFactory().getBean(Execution.class).getContext();
    XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki", "Space", "Doc"));
    XWikiUser user = new XWikiUser("XWiki.User");
    source.set(WIKI, new WikiReference("wiki"));
    source.set(DOC, doc);
    source.set(XWIKI_USER, user);

    ExecutionContext target = new ExecutionContext();
    getBeanFactory().getBean(XWikiExecutionContextInitializer.class).initialize(target, source);

    assertEquals(new WikiReference("wiki"), target.get(WIKI).orElseThrow());
    assertSame(doc, target.get(DOC).orElseThrow());
    assertSame(user, target.get(XWIKI_USER).orElseThrow());
    assertNotSame(getXContext(), target.get(XWIKI_CONTEXT).orElseThrow());
  }

}
