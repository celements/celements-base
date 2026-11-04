package com.celements.observation.remote.kafka;

import static java.util.concurrent.TimeUnit.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;
import com.celements.health.HealthService;

public class KafkaProducerHealthCheckTest extends AbstractBaseComponentTest {

  private KafkaProducerHealthCheck check;
  private KafkaNetworkAdapter adapter;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(RemoteObservationManagerConfiguration.class, KafkaConfig.class);
    adapter = getBeanFactory().getBean(KafkaNetworkAdapter.class);
    check = getBeanFactory().getBean(KafkaProducerHealthCheck.class);
  }

  @After
  public void close() {
    check.close();
  }

  @Test
  public void test_check_disabledDoesNotInitializeClients() throws Exception {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.empty()).andReturn(Optional.of("other"));

    replayDefault();
    assertEquals("observation.kafka.producer", check.id());
    assertEquals(Scope.MONITORING, check.scope());
    assertEquals(Status.DISABLED, result().status());
    assertEquals(Status.DISABLED, result().status());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_check_beforeStartIsDownWithoutInitializingClients() throws Exception {
    expectKafka();

    replayDefault();
    assertEquals(Status.DOWN, result().status());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_check_waitsForAcknowledgementAndSharesPendingCheck() throws Exception {
    expectKafka();
    var producer = registerProducer();
    Capture<ProducerRecord<String, byte[]>> record = newCapture();
    var sent = new CountDownLatch(1);
    var acknowledgement = new SettableListenableFuture<SendResult<String, byte[]>>();
    expect(producer.send(capture(record))).andAnswer(() -> {
      sent.countDown();
      return acknowledgement;
    });

    replayDefault();
    var pending = check.check().toCompletableFuture();
    assertTrue(sent.await(2, SECONDS));
    assertFalse(pending.isDone());
    assertEquals("test-topic", record.getValue().topic());
    assertEquals("observation", record.getValue().key());
    assertArrayEquals(new byte[0], record.getValue().value());
    assertNotNull(record.getValue().headers().lastHeader("health-probe"));

    var timedOutRequest = pending.copy().orTimeout(1, MILLISECONDS);
    var timeout = assertThrows(ExecutionException.class, () -> timedOutRequest.get(2, SECONDS));
    assertTrue(timeout.getCause() instanceof TimeoutException);
    assertFalse(pending.isDone());
    assertSame(pending, check.check().toCompletableFuture());

    acknowledgement.set(new SendResult<>(record.getValue(), null));
    assertEquals(new Result(Status.UP, "Kafka producer check succeeded"), pending.get(2, SECONDS));
    verifyDefault();
  }

  @Test
  public void test_check_failedAcknowledgementAllowsNewCheck() throws Exception {
    expectKafka();
    var producer = registerProducer();
    var failed = new SettableListenableFuture<SendResult<String, byte[]>>();
    var retry = new SettableListenableFuture<SendResult<String, byte[]>>();
    var failure = new KafkaException("send failed");
    failed.setException(failure);
    expect(producer.send(anyObject(ProducerRecord.class))).andReturn(failed).andReturn(retry);

    replayDefault();
    var first = check.check().toCompletableFuture();
    var exception = assertThrows(ExecutionException.class, () -> first.get(2, SECONDS));
    assertSame(failure, exception.getCause());
    var second = check.check().toCompletableFuture();
    assertNotSame(first, second);
    retry.set(new SendResult<>(new ProducerRecord<>("test-topic", new byte[0]), null));
    assertEquals(Status.UP, second.get(2, SECONDS).status());
    verifyDefault();
  }

  @Test
  public void test_check_synchronousSendFailureCompletesExceptionally() throws Exception {
    expectKafka();
    var producer = registerProducer();
    var failure = new KafkaException("send failed");
    expect(producer.send(anyObject(ProducerRecord.class))).andThrow(failure);

    replayDefault();
    var pending = check.check().toCompletableFuture();
    var exception = assertThrows(ExecutionException.class, () -> pending.get(2, SECONDS));
    assertSame(failure, exception.getCause());
    verifyDefault();
  }

  @Test
  public void test_check_blockingSendKeepsHealthDeadlineAndSingleCheck() throws Exception {
    expectKafka();
    var producer = registerProducer();
    var enteredSend = new CountDownLatch(1);
    var releaseSend = new CountDownLatch(1);
    var acknowledgement = new SettableListenableFuture<SendResult<String, byte[]>>();
    var requestThread = Thread.currentThread();
    expect(producer.send(anyObject(ProducerRecord.class))).andAnswer(() -> {
      assertNotSame(requestThread, Thread.currentThread());
      enteredSend.countDown();
      assertTrue(releaseSend.await(5, SECONDS));
      return acknowledgement;
    });

    replayDefault();
    try {
      var report = new HealthService(List.of(check)).check(Scope.MONITORING).toCompletableFuture();
      assertTrue(enteredSend.await(2, SECONDS));
      var pending = check.check().toCompletableFuture();
      assertFalse(pending.isDone());
      assertSame(pending, check.check().toCompletableFuture());
      assertEquals(new Result(Status.DOWN, "Check timed out"),
          report.get(3, SECONDS).checks().get(check.id()).result());
      assertFalse(pending.isDone());
      releaseSend.countDown();
      acknowledgement.set(new SendResult<>(new ProducerRecord<>("test-topic", new byte[0]), null));
      assertEquals(Status.UP, pending.get(2, SECONDS).status());
    } finally {
      releaseSend.countDown();
    }
    verifyDefault();
  }

  @Test
  public void test_check_stoppedProducerIsDown() throws Exception {
    expectKafka();
    registerProducer();

    replayDefault();
    adapter.stop();
    assertEquals(Status.DOWN, result().status());
    verifyDefault();
  }

  private void expectKafka() {
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andStubReturn(Optional.of("kafka"));
  }

  @SuppressWarnings("unchecked")
  private KafkaTemplate<String, byte[]> registerProducer() {
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn("test-topic");
    var producer = createDefaultMock(KafkaTemplate.class);
    ReflectionTestUtils.setField(adapter, "producer", producer);
    return producer;
  }

  private Result result() throws Exception {
    return check.check().toCompletableFuture().get(2, SECONDS);
  }

}
