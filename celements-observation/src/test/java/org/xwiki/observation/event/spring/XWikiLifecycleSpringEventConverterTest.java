package org.xwiki.observation.event.spring;

import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.test.AbstractComponentTestCase;

import com.celements.init.CelementsLifecycleEvent;
import com.celements.init.CelementsStartedEvent;
import com.celements.init.CelementsStoppedEvent;

public class XWikiLifecycleSpringEventConverterTest extends AbstractComponentTestCase {

  XWikiLifecycleSpringEventConverter converter;
  ObservationManager observationManagerMock;

  @Override
  @Before
  public void setUp() throws Exception {
    observationManagerMock = createMock(ObservationManager.class);
    converter = new XWikiLifecycleSpringEventConverter(observationManagerMock);
  }

  @Test
  public void test_CelementsStartedEvent() {
    observationManagerMock.notify(anyObject(ApplicationStartedEvent.class), same(converter));
    replay(observationManagerMock);
    converter.onApplicationEvent(new CelementsStartedEvent(this));
    verify(observationManagerMock);
  }

  @Test
  public void test_CelementsStoppedEvent() {
    observationManagerMock.notify(anyObject(ApplicationStoppedEvent.class), same(converter));
    replay(observationManagerMock);
    converter.onApplicationEvent(new CelementsStoppedEvent(this));
    verify(observationManagerMock);
  }

  @Test
  public void test_other() {
    replay(observationManagerMock);
    converter.onApplicationEvent(new CelementsLifecycleEvent(this, "other"));
    verify(observationManagerMock);
  }

}
