package com.celements.observation.remote.kafka;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.springframework.util.SerializationUtils.*;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

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
    var consumer = new MockConsumer<String, byte[]>(OffsetResetStrategy.LATEST);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, byte[]>(Map.of(
        ConsumerConfig.GROUP_ID_CONFIG, "test-group",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)) {

      @Override
      public Consumer<String, byte[]> createConsumer(String groupId, String clientIdPrefix,
          String clientIdSuffix, Properties properties) {
        return consumer;
      }
    };
    expect(getMock(KafkaConfig.class).buildProducerFactory())
        .andReturn(new DefaultKafkaProducerFactory<>(Map.of()));
    expect(getMock(KafkaConfig.class).buildConsumerFactory()).andReturn(consumerFactory);
    expect(getMock(KafkaConfig.class).buildEncryptor()).andReturn(Optional.empty());
    expect(getMock(KafkaConfig.class).getServers()).andStubReturn("unused:9092");
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn("test-topic");
    expect(getMock(KafkaConfig.class).getClientId()).andStubReturn("test-node");

    replayDefault();
    try {
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
        .andReturn(new DefaultKafkaProducerFactory<>(Map.of())).times(2);
    expect(getMock(KafkaConfig.class).buildConsumerFactory())
        .andThrow(failure).times(2);
    expect(getMock(KafkaConfig.class).buildEncryptor()).andReturn(Optional.empty()).times(2);

    replayDefault();
    assertSame(failure, assertThrows(IllegalStateException.class, () -> adapter.start(event -> {})));
    assertSame(failure, assertThrows(IllegalStateException.class, () -> adapter.start(event -> {})));
    assertFalse(adapter.isProducerRunning());
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
    ReflectionTestUtils.setField(adapter, "encryptor", createDefaultMock(BytesEncryptor.class));
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
  public void test_init_allowsSendingBeforeConsumptionStarts() {
    var producer = new MockProducer<>(true, new StringSerializer(), new ByteArraySerializer());
    var producerFactory = new DefaultKafkaProducerFactory<String, byte[]>(Map.of()) {
      @Override
      public Producer<String, byte[]> createProducer(String transactionIdPrefix) {
        return producer;
      }
    };
    var consumerFactory = createDefaultMock(DefaultKafkaConsumerFactory.class);
    expect(getMock(KafkaConfig.class).buildProducerFactory()).andReturn(producerFactory);
    expect(getMock(KafkaConfig.class).buildConsumerFactory()).andReturn(consumerFactory);
    expect(getMock(KafkaConfig.class).buildEncryptor()).andReturn(Optional.empty());
    expect(getMock(KafkaConfig.class).getServers()).andStubReturn("localhost:9092");
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn("observation.remote.test");
    expect(getMock(KafkaConfig.class).getClientId()).andStubReturn("testNode");
    var remoteEvent = new RemoteEventData();
    remoteEvent.setEvent("startup event");

    replayDefault();
    var adapter = getBeanFactory().getBean(KafkaNetworkAdapter.class);
    adapter.send(remoteEvent);
    assertEquals(1, producer.history().size());
    assertEquals("observation.remote.test", producer.history().get(0).topic());
    verifyDefault();
  }
}
