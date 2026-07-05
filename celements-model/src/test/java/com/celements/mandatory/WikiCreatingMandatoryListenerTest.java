package com.celements.mandatory;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.mandatory.IMandatoryDocumentCompositorRole;
import com.celements.mandatory.WikiCreatingMandatoryListener;
import com.celements.wiki.event.WikiCreatingEvent;

public class WikiCreatingMandatoryListenerTest extends AbstractComponentTest {

  private WikiCreatingMandatoryListener listener;

  private IMandatoryDocumentCompositorRole mandatoryDocCmpMock;

  @Before
  public void prepare() throws Exception {
    mandatoryDocCmpMock = registerComponentMock(IMandatoryDocumentCompositorRole.class);
    listener = getBeanFactory().getBean(WikiCreatingMandatoryListener.class);
  }

  @Test
  public void test_getOrder() {
    assertEquals(-100, listener.getOrder());
  }

  @Test
  public void test_onApplicationEvent() {
    String database = "db";
    WikiCreatingEvent event = new WikiCreatingEvent(new WikiReference(database));

    mandatoryDocCmpMock.checkAllMandatoryDocuments(event.getWiki());
    expectLastCall().once();

    replayDefault();
    listener.onApplicationEvent(event);
    verifyDefault();
  }

}
