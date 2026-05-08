package org.xwiki.observation;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.function.Consumer;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.observation.event.ActionExecutionEvent;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.internal.OutgoingObservationManager;

import com.celements.common.test.AbstractBaseComponentTest;

public class ObservationManagerTest extends AbstractBaseComponentTest {

  private DefaultObservationManager manager;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(OutgoingObservationManager.class);
    manager = getBeanFactory().getBean(DefaultObservationManager.class);
  }

  @Test
  public void test_notify_whenMatching() {
    EventListener listener = createDefaultMock(EventListener.class);
    Event event = createMock(Event.class);
    expectRemoteObservation();

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(event)).anyTimes();
    expect(event.matches(event)).andReturn(true);
    listener.onEvent(event, "some source", "some data");

    replayDefault(event);
    manager.addListener(listener);
    assertSame(listener, manager.getListener("mylistener"));
    manager.notify(event, "some source", "some data");
    verifyDefault(event);
  }

  @Test
  public void test_removeListener() {
    EventListener listener = createDefaultMock(EventListener.class);
    Event event = createMock(Event.class);
    expectRemoteObservation();

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(event)).anyTimes();

    replayDefault(event);
    manager.addListener(listener);
    manager.removeListener("mylistener");
    manager.notify(event, null);
    verifyDefault(event);
  }

  @Test
  public void test_addEvent() {
    EventListener listener = createDefaultMock(EventListener.class);
    Event initialEvent = createMock(Event.class);
    Event afterEvent = createMock(Event.class);
    Event notifyEvent = createMock(Event.class);
    expectRemoteObservation();

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(initialEvent)).anyTimes();
    expect(initialEvent.matches(same(notifyEvent))).andReturn(false);
    expect(afterEvent.matches(same(notifyEvent))).andReturn(true);
    listener.onEvent(same(notifyEvent), isNull(), isNull());

    replayDefault(initialEvent, afterEvent, notifyEvent);
    manager.addListener(listener);
    manager.addEvent("mylistener", afterEvent);
    manager.notify(notifyEvent, null);
    verifyDefault(initialEvent, afterEvent, notifyEvent);
  }

  @Test
  public void test_removeEvent() {
    EventListener listener = createDefaultMock(EventListener.class);
    Event initialEvent = createMock(Event.class);
    Event afterEvent = createMock(Event.class);
    Event notifyEvent = createMock(Event.class);
    expectRemoteObservation();

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(initialEvent)).anyTimes();
    expect(initialEvent.matches(same(notifyEvent))).andReturn(false);

    replayDefault(initialEvent, afterEvent, notifyEvent);
    manager.addListener(listener);
    manager.addEvent("mylistener", afterEvent);
    manager.removeEvent("mylistener", afterEvent);
    manager.notify(notifyEvent, null);
    verifyDefault(initialEvent, afterEvent, notifyEvent);
  }

  @Test
  public void test_registerSeveralListenersForSameEvent() {
    EventListener listener1 = createMock(EventListener.class);
    EventListener listener2 = createMock(EventListener.class);
    Event event = createMock(Event.class);
    Event notifyEvent = createMock(Event.class);
    expectRemoteObservation();

    expect(listener1.getName()).andReturn("listener 1").anyTimes();
    expect(listener2.getName()).andReturn("listener 2").anyTimes();
    expect(listener1.getEvents()).andReturn(Arrays.asList(event)).anyTimes();
    expect(listener2.getEvents()).andReturn(Arrays.asList(event)).anyTimes();
    expect(event.matches(same(notifyEvent))).andReturn(true).times(2);
    listener1.onEvent(same(notifyEvent), isNull(), isNull());
    listener2.onEvent(same(notifyEvent), isNull(), isNull());

    replayDefault(listener1, listener2, event, notifyEvent);
    manager.addListener(listener1);
    manager.addListener(listener2);
    manager.notify(notifyEvent, null);
    verifyDefault(listener1, listener2, event, notifyEvent);
  }

  @Test
  public void test_registerListenerForAllEvents() {
    EventListener listener = createDefaultMock(EventListener.class);
    Event event = createMock(Event.class);
    expectRemoteObservation();

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(AllEvent.ALLEVENT)).anyTimes();
    listener.onEvent(event, "some source", "some data");

    replayDefault(event);
    manager.addListener(listener);
    assertSame(listener, manager.getListener("mylistener"));
    manager.notify(event, "some source", "some data");
    verifyDefault(event);
  }

  @Test
  public void test_registerSameListenerSeveralTimes() {
    EventListener listener = createDefaultMock(EventListener.class);

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(AllEvent.ALLEVENT)).anyTimes();

    replayDefault();
    manager.addListener(listener);
    manager.addListener(listener);
    manager.removeListener("mylistener");
    manager.addListener(listener);
    verifyDefault();
  }

  @Test
  public void test_registerListenerForTwoEventsOfSameType() {
    EventListener listener = createDefaultMock(EventListener.class);
    Event eventMatcher1 = new ActionExecutionEvent("action1");
    Event eventMatcher2 = new ActionExecutionEvent("action2");
    expectRemoteObservation();
    expectRemoteObservation();

    expect(listener.getName()).andReturn("mylistener").anyTimes();
    expect(listener.getEvents()).andReturn(Arrays.asList(eventMatcher1, eventMatcher2)).anyTimes();
    listener.onEvent(same(eventMatcher1), eq("some source"), eq("some data"));
    listener.onEvent(same(eventMatcher2), eq("some source"), eq("some data"));

    replayDefault();
    manager.addListener(listener);
    manager.notify(eventMatcher1, "some source", "some data");
    manager.notify(eventMatcher2, "some source", "some data");
    verifyDefault();
  }

  private void expectRemoteObservation() {
    expect(getMock(OutgoingObservationManager.class).isEnabled()).andReturn(true);
    getMock(OutgoingObservationManager.class).notifyLocalThenRemote(anyObject(LocalEventData.class),
        anyObject(Consumer.class));
    expectLastCall().andAnswer((IAnswer<Void>) () -> {
      LocalEventData localEvent = getCurrentArgument(0);
      Consumer<LocalEventData> notify = getCurrentArgument(1);
      notify.accept(localEvent);
      return null;
    });
  }
}
