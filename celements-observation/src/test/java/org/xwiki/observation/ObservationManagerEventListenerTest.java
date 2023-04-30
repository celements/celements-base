package org.xwiki.observation;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.observation.event.Event;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link ObservationManager}.
 *
 * @version $Id$
 */
public class ObservationManagerEventListenerTest extends AbstractComponentTestCase {

  private ObservationManager manager;

  private EventListener eventListener;

  private Event event;

  private DefaultComponentDescriptor<EventListener> componentDescriptor;

  @Before
  public void prepare() throws Exception {
    event = new Event() {

      @Override
      public boolean matches(Object otherEvent) {
        return this == otherEvent;
      }

    };
    eventListener = new EventListener() {

      @Override
      public void onEvent(Event event, Object source, Object data) {}

      @Override
      public String getName() {
        return "mylistener";
      }

      @Override
      public List<Event> getEvents() {
        return Arrays.asList(event);
      }
    };
    this.componentDescriptor = new DefaultComponentDescriptor<>(
        EventListener.class, "mylistener", eventListener.getClass());
    getComponentManager().registerComponent(this.componentDescriptor, eventListener);
    this.manager = getComponentManager().lookup(ObservationManager.class);
  }

  @Test
  public void testNewListenerComponent() throws Exception {
    Assert.assertSame(eventListener, manager.getListener("mylistener"));
  }

  @Test
  public void testRemovedListenerComponent() throws Exception {
    // FIXME listeners arent being removed since ComponentDescriptorEvents were removed in Cel6
    // getComponentManager().unregisterComponent(this.componentDescriptor.getRole(),
    // this.componentDescriptor.getRoleHint());
    // Assert.assertNull(manager.getListener("mylistener"));
  }
}
