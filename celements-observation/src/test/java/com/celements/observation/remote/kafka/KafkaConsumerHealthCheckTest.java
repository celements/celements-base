package com.celements.observation.remote.kafka;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;

public class KafkaConsumerHealthCheckTest extends AbstractBaseComponentTest {

  private KafkaConsumerHealthCheck check;
  private KafkaNetworkAdapter adapter;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(RemoteObservationManagerConfiguration.class, KafkaConfig.class);
    adapter = getBeanFactory().getBean(KafkaNetworkAdapter.class);
    check = getBeanFactory().getBean(KafkaConsumerHealthCheck.class);
  }

  @Test
  public void test_check_disabledDoesNotInitializeClients() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.empty());

    replayDefault();
    assertEquals("observation.kafka.consumer", check.id());
    assertEquals(Scope.READINESS, check.scope());
    assertEquals(Status.DISABLED, result().status());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_check_otherAdapterIsDisabled() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of("other"));

    replayDefault();
    assertEquals(Status.DISABLED, result().status());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_check_beforeStartIsDownWithoutInitializingClients() {
    expectKafka();

    replayDefault();
    assertEquals(Status.DOWN, result().status());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_check_stoppedContainerIsDown() {
    expectKafka();
    var container = registerConsumer();
    expect(container.isRunning()).andReturn(false);

    replayDefault();
    assertEquals(Status.DOWN, result().status());
    verifyDefault();
  }

  @Test
  public void test_check_runningParentWithStoppedWorkerIsDown() {
    expectKafka();
    var container = registerConsumer();
    expect(container.isRunning()).andReturn(true);
    expect(container.isChildRunning()).andReturn(false);

    replayDefault();
    assertEquals(Status.DOWN, result().status());
    verifyDefault();
  }

  @Test
  public void test_check_tracksWorkerStateWithoutBrokerOrTrafficChecks() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of("kafka")).times(3);
    var container = registerConsumer();
    expect(container.isRunning()).andReturn(true).times(3);
    expect(container.isChildRunning()).andReturn(true).andReturn(false).andReturn(true);

    replayDefault();
    assertEquals(Status.UP, result().status());
    assertEquals(Status.DOWN, result().status());
    assertEquals(Status.UP, result().status());
    verifyDefault();
  }

  private void expectKafka() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of("kafka"));
  }

  @SuppressWarnings("unchecked")
  private ConcurrentMessageListenerContainer<String, byte[]> registerConsumer() {
    var container = createDefaultMock(ConcurrentMessageListenerContainer.class);
    ReflectionTestUtils.setField(adapter, "consumer", container);
    return container;
  }

  private Result result() {
    var future = check.check().toCompletableFuture();
    assertTrue(future.isDone());
    return future.join();
  }

}
