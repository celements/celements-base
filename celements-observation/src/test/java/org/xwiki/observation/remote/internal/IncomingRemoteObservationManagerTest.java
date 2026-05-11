package org.xwiki.observation.remote.internal;

import static org.easymock.EasyMock.*;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
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
  public void test_initialize_starts_configuredAdapter() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of(ADAPTER_NAME));
    adapter.start(anyObject(Consumer.class));

    replayDefault();
    getBeanFactory().getBean(IncomingRemoteObservationManager.class);
    verifyDefault();
  }

  @Test
  public void test_initialize_disabled() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.empty());

    replayDefault();
    getBeanFactory().getBean(IncomingRemoteObservationManager.class);
    verifyDefault();
  }

  @Test
  public void test_notify_replays_remoteEventLocally() throws Exception {
    IncomingRemoteObservationManager manager = newIncomingRemoteObservationManager();
    RemoteEventData remoteEvent = new RemoteEventData();
    Event event = new TestEvent();
    LocalEventData localEvent = new LocalEventData(event, "source", "data");

    getMock(Execution.class).setContext(anyObject(ExecutionContext.class));
    getMock(ExecutionContextManager.class).initialize(anyObject(ExecutionContext.class));
    expect(getMock(EventConverterManager.class).createLocalEventData(same(remoteEvent)))
        .andReturn(localEvent);
    getMock(RemoteObservationManagerContext.class).pushRemoteState();
    getMock(ObservationManager.class).notify(same(event), eq("source"), eq("data"));
    getMock(RemoteObservationManagerContext.class).popRemoteState();
    getMock(Execution.class).removeContext();

    replayDefault();
    manager.notify(remoteEvent);
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

  private static class TestEvent implements Event {

    @Override
    public boolean matches(Object otherEvent) {
      return this == otherEvent;
    }
  }
}
