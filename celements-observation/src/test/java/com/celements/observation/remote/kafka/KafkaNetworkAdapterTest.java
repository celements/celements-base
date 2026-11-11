package com.celements.observation.remote.kafka;

import static java.util.concurrent.TimeUnit.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.springframework.util.SerializationUtils.*;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.xwiki.observation.remote.RemoteEventData;

import com.celements.common.test.AbstractBaseComponentTest;

public class KafkaNetworkAdapterTest extends AbstractBaseComponentTest {

  private KafkaNetworkAdapter adapter;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(KafkaConfig.class);
    adapter = getBeanFactory().getBean(KafkaNetworkAdapter.class);
  }

  @Test
  public void test_beanCreationDoesNotInitializeClients() {
    replayDefault();
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test(timeout = 10000)
  public void test_start_initializesFactoriesOnlyOnce() {
    var consumerFactory = new DefaultKafkaConsumerFactory<String, byte[]>(Map.of(
        ConsumerConfig.GROUP_ID_CONFIG, "test-group",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)) {

      @Override
      public Consumer<String, byte[]> createConsumer(String groupId, String clientIdPrefix,
          String clientIdSuffix, Properties properties) {
        return new MockConsumer<>(OffsetResetStrategy.LATEST);
      }
    };
    expect(getMock(KafkaConfig.class).buildProducerFactory())
        .andReturn(new DefaultKafkaProducerFactory<>(Map.of()));
    expect(getMock(KafkaConfig.class).buildConsumerFactory()).andReturn(consumerFactory);
    expect(getMock(KafkaConfig.class).getServers()).andStubReturn("unused:9092");
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn("test-topic");
    expect(getMock(KafkaConfig.class).getClientId()).andStubReturn("test-node");

    replayDefault();
    try {
      adapter.start(event -> {});
      assertTrue(adapter.isProducerRunning());
      assertTrue(adapter.isConsumerRunning());
      adapter.stop();
      assertFalse(adapter.isProducerRunning());
      assertFalse(adapter.isConsumerRunning());
      adapter.start(event -> {});
      assertTrue(adapter.isProducerRunning());
      assertTrue(adapter.isConsumerRunning());
      adapter.start(event -> {});
      assertTrue(adapter.isProducerRunning());
      assertTrue(adapter.isConsumerRunning());
    } finally {
      adapter.stop();
    }
    verifyDefault();
  }

  @Test
  public void test_start_failedInitializationCanBeRetried() {
    var failure = new IllegalStateException("Invalid consumer configuration");
    expect(getMock(KafkaConfig.class).buildProducerFactory())
        .andReturn(new DefaultKafkaProducerFactory<>(Map.of()));
    expect(getMock(KafkaConfig.class).buildConsumerFactory())
        .andThrow(failure).times(2);
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn("test-topic");

    replayDefault();
    assertSame(failure, assertThrows(IllegalStateException.class, () -> adapter.start(event -> {})));
    assertSame(failure, assertThrows(IllegalStateException.class, () -> adapter.start(event -> {})));
    assertTrue(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_receive_acknowledgesCheckWithoutDecryptingOrPublishingEvent() {
    var record = new ConsumerRecord<String, byte[]>("test-topic", 0, 0L,
        "observation", new byte[0]);
    record.headers().add("health-probe", new byte[] { 1 });
    var acknowledgement = createDefaultMock(Acknowledgment.class);
    var listener = createDefaultMock(java.util.function.Consumer.class);
    var encryptor = createDefaultMock(BytesEncryptor.class);
    ReflectionTestUtils.setField(adapter, "encryptor",
        (Supplier<Optional<BytesEncryptor>>) () -> Optional.of(encryptor));
    acknowledgement.acknowledge();

    replayDefault();
    ReflectionTestUtils.invokeMethod(adapter, "receive", record, acknowledgement,
        (java.util.function.Consumer<RemoteEventData>) listener);
    verifyDefault();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_receive_unmarkedEventIsStillPublished() {
    var event = new RemoteEventData();
    event.setEvent("event");
    var record = new ConsumerRecord<String, byte[]>("test-topic", 0, 0L,
        "observation", serialize(event));
    var acknowledgement = createDefaultMock(Acknowledgment.class);
    java.util.function.Consumer<RemoteEventData> listener = createDefaultMock(
        java.util.function.Consumer.class);
    Capture<RemoteEventData> delivered = newCapture();
    expect(getMock(KafkaConfig.class).getClientId()).andReturn("test-node");
    expect(getMock(KafkaConfig.class).getTopic()).andReturn("test-topic");
    listener.accept(capture(delivered));
    acknowledgement.acknowledge();

    replayDefault();
    ReflectionTestUtils.invokeMethod(adapter, "receive", record, acknowledgement, listener);
    assertEquals("event", delivered.getValue().getEvent());
    verifyDefault();
  }

  @Test
  public void test_send_initializesProducerBeforeConsumptionStarts() {
    var producer = expectProducer();
    var remoteEvent = new RemoteEventData();
    remoteEvent.setEvent("startup event");

    replayDefault();
    adapter.send(remoteEvent);
    assertEquals(1, producer.history().size());
    assertEquals("observation.remote.test", producer.history().get(0).topic());
    assertTrue(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_send_concurrentFirstSendsInitializeProducerOnlyOnce() throws Exception {
    var producer = expectProducer();
    var remoteEvent = new RemoteEventData();
    remoteEvent.setEvent("concurrent startup event");
    var start = new CompletableFuture<Void>();

    replayDefault();
    var sends = IntStream.range(0, 8)
        .mapToObj(ignored -> start.thenRunAsync(() -> adapter.send(remoteEvent)))
        .toArray(CompletableFuture<?>[]::new);
    start.complete(null);
    CompletableFuture.allOf(sends).get(2, SECONDS);
    assertEquals(8, producer.history().size());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_send_afterStopDoesNotRestartProducer() {
    var producer = expectProducer();
    var remoteEvent = new RemoteEventData();
    remoteEvent.setEvent("startup event");

    replayDefault();
    adapter.send(remoteEvent);
    adapter.stop();
    var failure = assertThrows(IllegalStateException.class, () -> adapter.send(remoteEvent));
    assertEquals("Kafka adapter is stopped", failure.getMessage());
    assertEquals(1, producer.history().size());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  @Test
  public void test_send_afterStopFailsBeforeReadingEventOrInitializingClients() {
    expect(getMock(KafkaConfig.class).getTopic()).andReturn("test-topic");

    replayDefault();
    adapter.stop();
    var failure = assertThrows(IllegalStateException.class,
        () -> adapter.send(new RemoteEventData()));
    assertEquals("Kafka adapter is stopped", failure.getMessage());
    assertFalse(adapter.isProducerRunning());
    assertFalse(adapter.isConsumerRunning());
    verifyDefault();
  }

  private MockProducer<String, byte[]> expectProducer() {
    var producer = new MockProducer<>(true, new StringSerializer(), new ByteArraySerializer());
    var producerFactory = new DefaultKafkaProducerFactory<String, byte[]>(Map.of()) {
      @Override
      protected Producer<String, byte[]> createRawProducer(Map<String, Object> configProps) {
        return producer;
      }
    };
    expect(getMock(KafkaConfig.class).buildProducerFactory()).andReturn(producerFactory);
    expect(getMock(KafkaConfig.class).buildEncryptor()).andReturn(Optional.empty());
    expect(getMock(KafkaConfig.class).getServers()).andStubReturn("localhost:9092");
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn("observation.remote.test");
    expect(getMock(KafkaConfig.class).getClientId()).andStubReturn("testNode");
    return producer;
  }
}
