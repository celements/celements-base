package org.xwiki.observation.remote.internal;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;

import com.celements.common.observation.converter.Local;
import com.celements.common.test.AbstractBaseComponentTest;

public class OutgoingObservationManagerTest extends AbstractBaseComponentTest {

  private static final String ADAPTER_NAME = "testAdapter";

  private OutgoingObservationManager manager;
  private NetworkAdapter adapter;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(
        RemoteObservationManagerConfiguration.class,
        EventConverterManager.class,
        RemoteObservationManagerContext.class);
    adapter = createDefaultMock(NetworkAdapter.class);
    getBeanFactory().registerSingleton(ADAPTER_NAME, adapter);
    manager = getBeanFactory().getBean(OutgoingObservationManager.class);
  }

  @Test
  public void test_notify_sendsConvertedEvent() {
    LocalEventData localEvent = new LocalEventData(new TestEvent(), "source", "data");
    RemoteEventData remoteEvent = new RemoteEventData();

    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of(ADAPTER_NAME));
    expect(getMock(EventConverterManager.class).createRemoteEventData(same(localEvent)))
        .andReturn(remoteEvent);
    adapter.send(same(remoteEvent));

    replayDefault();
    manager.notify(localEvent);
    verifyDefault();
  }

  @Test
  public void test_isEnabled_false_whenDisabled() {
    expect(getMock(RemoteObservationManagerConfiguration.class).isEnabled()).andReturn(false);

    replayDefault();
    assertFalse(manager.isEnabled());
    verifyDefault();
  }

  @Test
  public void test_isEnabled_false_whenRemoteState() {
    expect(getMock(RemoteObservationManagerConfiguration.class).isEnabled()).andReturn(true);
    expect(getMock(RemoteObservationManagerContext.class).isRemoteState()).andReturn(true);

    replayDefault();
    assertFalse(manager.isEnabled());
    verifyDefault();
  }

  @Test
  public void test_notifyLocalThenRemote_skip_localAnnotatedEvent() {
    LocalEventData localEvent = new LocalEventData(new LocalTestEvent(), "source", "data");
    AtomicBoolean notifiedLocal = new AtomicBoolean();

    expect(getMock(RemoteObservationManagerConfiguration.class).isEnabled()).andReturn(true);
    expect(getMock(RemoteObservationManagerContext.class).isRemoteState()).andReturn(false);

    replayDefault();
    manager.notifyLocalThenRemote(localEvent, eventData -> notifiedLocal.set(true));
    assertTrue(notifiedLocal.get());
    verifyDefault();
  }

  @Test
  public void test_notifyLocalThenRemote_keeps_parentBeforeNestedEventOrder() {
    RecordingOutgoingObservationManager manager = new RecordingOutgoingObservationManager();
    LocalEventData parent = new LocalEventData(new TestEvent(), "parent source", "parent data");
    LocalEventData nested = new LocalEventData(new TestEvent(), "nested source", "nested data");

    manager.notifyLocalThenRemote(parent, localEvent -> manager.notifyLocalThenRemote(nested,
        nestedEventData -> {
          // nested local notification
        }));

    assertEquals(List.of(parent, nested), manager.notifiedEvents);
  }

  private static class RecordingOutgoingObservationManager extends OutgoingObservationManager {

    private final List<LocalEventData> notifiedEvents = new ArrayList<>();

    RecordingOutgoingObservationManager() {
      super(null, null, null, null);
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    void notify(LocalEventData localEvent) {
      notifiedEvents.add(localEvent);
    }
  }

  private static class TestEvent implements Event {

    @Override
    public boolean matches(Object otherEvent) {
      return this == otherEvent;
    }
  }

  @Local
  private static class LocalTestEvent implements Event {

    @Override
    public boolean matches(Object otherEvent) {
      return this == otherEvent;
    }
  }
}
