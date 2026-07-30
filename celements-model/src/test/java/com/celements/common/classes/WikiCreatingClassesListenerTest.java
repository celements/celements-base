package com.celements.common.classes;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.classes.IClassesCompositorComponent;
import com.celements.common.classes.WikiCreatingClassesListener;
import com.celements.common.test.AbstractComponentTest;
import com.celements.wiki.event.WikiCreatingEvent;

public class WikiCreatingClassesListenerTest extends AbstractComponentTest {

  private WikiCreatingClassesListener listener;

  private IClassesCompositorComponent classesCmpMock;

  @Before
  public void prepare() throws Exception {
    classesCmpMock = registerComponentMock(IClassesCompositorComponent.class);
    listener = getBeanFactory().getBean(WikiCreatingClassesListener.class);
  }

  @Test
  public void test_getOrder() {
    assertEquals(-200, listener.getOrder());
  }

  @Test
  public void test_onApplicationEvent() {
    String database = "db";
    WikiCreatingEvent event = new WikiCreatingEvent(new WikiReference(database));

    classesCmpMock.checkClasses(event.getWiki());
    expectLastCall().once();

    replayDefault();
    listener.onApplicationEvent(event);
    verifyDefault();
  }

}
