package org.xwiki.observation.remote.internal;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;
import java.util.function.Consumer;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.init.CelementsInitialisedEvent;

public class IncomingRemoteObservationManagerTest extends AbstractBaseComponentTest {

  private static final String ADAPTER_NAME = "testAdapter";

  private NetworkAdapter adapter;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(
        RemoteObservationManagerConfiguration.class,
        EventConverterManager.class,
        ObservationManager.class,
        RemoteObservationManagerContext.class,
        Execution.class,
        ExecutionContextManager.class);
    adapter = createDefaultMock(NetworkAdapter.class);
    getBeanFactory().registerSingleton(ADAPTER_NAME, adapter);
  }

  @Test
  public void test_beanCreation_doesNotStartConsumption() {
    replayDefault();
    getBeanFactory().getBean(IncomingRemoteObservationManager.class);
    verifyDefault();
  }

  @Test
  public void test_onApplicationEvent_startsAfterOtherInitialisationListeners() {
    var initialisationListener = new InitialisationListener();
    getSpringContext().addApplicationListener(initialisationListener);
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of(ADAPTER_NAME));
    adapter.start(anyObject(Consumer.class));
    expectLastCall().andAnswer(() -> {
      assertTrue("initialisation listener must finish before consumption starts",
          initialisationListener.completed);
      return null;
    });

    replayDefault();
    getBeanFactory().getBean(IncomingRemoteObservationManager.class);
    getSpringContext().publishEvent(new CelementsInitialisedEvent(this));
    verifyDefault();
  }

  @Test
  public void test_onApplicationEvent_disabled() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.empty());

    replayDefault();
    getBeanFactory().getBean(IncomingRemoteObservationManager.class);
    getSpringContext().publishEvent(new CelementsInitialisedEvent(this));
    verifyDefault();
  }

  @Test
  public void test_onRemoteEvent_replays_remoteEventLocally() throws Exception {
    Capture<Consumer<RemoteEventData>> callback = newCapture();
    RemoteEventData remoteEvent = new RemoteEventData();
    Event event = new TestEvent();
    LocalEventData localEvent = new LocalEventData(event, "source", "data");

    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of(ADAPTER_NAME));
    adapter.start(capture(callback));
    getMock(Execution.class).setContext(anyObject(ExecutionContext.class));
    getMock(ExecutionContextManager.class).initialize(anyObject(ExecutionContext.class));
    expect(getMock(EventConverterManager.class).createLocalEventData(same(remoteEvent)))
        .andReturn(localEvent);
    getMock(RemoteObservationManagerContext.class).pushRemoteState();
    getMock(ObservationManager.class).notify(same(event), eq("source"), eq("data"));
    getMock(RemoteObservationManagerContext.class).popRemoteState();
    getMock(Execution.class).removeContext();

    replayDefault();
    getBeanFactory().getBean(IncomingRemoteObservationManager.class);
    getSpringContext().publishEvent(new CelementsInitialisedEvent(this));
    callback.getValue().accept(remoteEvent);
    verifyDefault();
  }

  @Test
  public void test_notify_skips_whenConverterReturnsNull() throws Exception {
    IncomingRemoteObservationManager manager = newIncomingRemoteObservationManager();
    RemoteEventData remoteEvent = new RemoteEventData();

    getMock(Execution.class).setContext(anyObject(ExecutionContext.class));
    getMock(ExecutionContextManager.class).initialize(anyObject(ExecutionContext.class));
    expect(getMock(EventConverterManager.class).createLocalEventData(same(remoteEvent)))
        .andReturn(null);
    getMock(Execution.class).removeContext();

    replayDefault();
    manager.notify(remoteEvent);
    verifyDefault();
  }

  private IncomingRemoteObservationManager newIncomingRemoteObservationManager() {
    return new IncomingRemoteObservationManager(
        getMock(RemoteObservationManagerConfiguration.class),
        getMock(EventConverterManager.class),
        getMock(ObservationManager.class),
        getMock(RemoteObservationManagerContext.class),
        getMock(Execution.class),
        getMock(ExecutionContextManager.class),
        getBeanFactory());
  }

  private static class InitialisationListener
      implements ApplicationListener<CelementsInitialisedEvent>, Ordered {

    private boolean completed;

    @Override
    public void onApplicationEvent(CelementsInitialisedEvent event) {
      completed = true;
    }

    @Override
    public int getOrder() {
      return 100;
    }
  }

  private static class TestEvent implements Event {

    @Override
    public boolean matches(Object otherEvent) {
      return this == otherEvent;
    }
  }
}
