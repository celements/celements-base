package com.celements.observation.remote.kafka;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.util.SerializationUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;
import org.xwiki.observation.remote.internal.IncomingRemoteObservationManager;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.init.CelementsInitialisedEvent;

public class KafkaStartupTest extends AbstractBaseComponentTest {

  private final TopicPartition partition = new TopicPartition("observation.remote.test", 0);
  private final CountDownLatch processed = new CountDownLatch(1);
  private final CountDownLatch committed = new CountDownLatch(1);
  private final AtomicBoolean consumerCreated = new AtomicBoolean();

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(
        KafkaConfig.class,
        RemoteObservationManagerConfiguration.class,
        EventConverterManager.class,
        ObservationManager.class,
        RemoteObservationManagerContext.class,
        Execution.class,
        ExecutionContextManager.class);
  }

  @Test
  public void test_startup_processesAndAcknowledgesQueuedRecordOnlyAfterReadiness() throws Exception {
    try (var producer = new MockProducer<>(true, new StringSerializer(), new ByteArraySerializer());
        var consumer = newConsumer()) {
      queueStartupRecord(consumer);
      expectKafkaConfiguration(producer, consumer);
      expectLocalDelivery();

      replayDefault();
      var adapter = getBeanFactory().getBean(KafkaNetworkAdapter.class);
      try {
        getBeanFactory().getBean(IncomingRemoteObservationManager.class);
        assertRecordPending(consumer);

        getSpringContext().publishEvent(new CelementsInitialisedEvent(this));
        assertRecordProcessed(consumer);
      } finally {
        adapter.stop();
      }
      verifyDefault();
    }
  }

  private MockConsumer<String, byte[]> newConsumer() {
    return new MockConsumer<>(OffsetResetStrategy.LATEST) {
      @Override
      public synchronized void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets) {
        super.commitSync(offsets);
        var offset = offsets.get(partition);
        if ((offset != null) && (offset.offset() == 6)) {
          committed.countDown();
        }
      }
    };
  }

  private void queueStartupRecord(MockConsumer<String, byte[]> consumer) {
    var remoteEvent = new RemoteEventData();
    remoteEvent.setEvent("queued startup event");
    var record = new ConsumerRecord<>(partition.topic(), partition.partition(), 5L,
        "observation", SerializationUtils.serialize(remoteEvent));
    var previousOffset = Map.of(partition, new OffsetAndMetadata(5));
    consumer.subscribe(List.of(partition.topic()));
    consumer.rebalance(List.of(partition));
    consumer.commitSync(previousOffset);
    consumer.updateBeginningOffsets(Map.of(partition, 0L));
    consumer.updateEndOffsets(Map.of(partition, 6L));
    // MockConsumer requires assignment before adding records and clears offsets on subscribe.
    consumer.schedulePollTask(() -> {
      consumer.rebalance(List.of(partition));
      consumer.commitSync(previousOffset);
      consumer.addRecord(record);
    });
  }

  private void expectKafkaConfiguration(Producer<String, byte[]> producer,
      MockConsumer<String, byte[]> consumer) {
    var producerFactory = new DefaultKafkaProducerFactory<String, byte[]>(Map.of()) {
      @Override
      public Producer<String, byte[]> createProducer(String transactionIdPrefix) {
        return producer;
      }
    };
    var consumerFactory = new DefaultKafkaConsumerFactory<String, byte[]>(Map.of(
        ConsumerConfig.GROUP_ID_CONFIG, "testNode",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)) {
      @Override
      protected Consumer<String, byte[]> createKafkaConsumer(Map<String, Object> configProps) {
        consumerCreated.set(true);
        return consumer;
      }
    };
    expect(getMock(KafkaConfig.class).buildProducerFactory()).andReturn(producerFactory);
    expect(getMock(KafkaConfig.class).buildConsumerFactory()).andReturn(consumerFactory);
    expect(getMock(KafkaConfig.class).buildEncryptor()).andReturn(Optional.empty());
    expect(getMock(KafkaConfig.class).getServers()).andStubReturn("localhost:9092");
    expect(getMock(KafkaConfig.class).getTopic()).andStubReturn(partition.topic());
    expect(getMock(KafkaConfig.class).getClientId()).andStubReturn("testNode");
    expect(getMock(RemoteObservationManagerConfiguration.class).getImplementation())
        .andReturn(Optional.of("kafka"));
  }

  private void expectLocalDelivery() throws Exception {
    Event event = other -> false;
    getMock(Execution.class).setContext(anyObject(ExecutionContext.class));
    getMock(ExecutionContextManager.class).initialize(anyObject(ExecutionContext.class));
    expect(getMock(EventConverterManager.class).createLocalEventData(anyObject()))
        .andAnswer(() -> {
          RemoteEventData received = getCurrentArgument(0);
          assertEquals("queued startup event", received.getEvent());
          return new LocalEventData(event, "source", "data");
        });
    getMock(RemoteObservationManagerContext.class).pushRemoteState();
    getMock(ObservationManager.class).notify(same(event), eq("source"), eq("data"));
    expectLastCall().andAnswer(() -> {
      processed.countDown();
      return null;
    });
    getMock(RemoteObservationManagerContext.class).popRemoteState();
    getMock(Execution.class).removeContext();
  }

  private void assertRecordPending(MockConsumer<String, byte[]> consumer) {
    assertFalse("consumer must not start before readiness", consumerCreated.get());
    assertEquals(1L, processed.getCount());
    assertEquals(1L, committed.getCount());
    assertEquals(5L, consumer.committed(partition).offset());
  }

  private void assertRecordProcessed(MockConsumer<String, byte[]> consumer)
      throws InterruptedException {
    assertTrue("queued record must be acknowledged", committed.await(5, TimeUnit.SECONDS));
    assertTrue(consumerCreated.get());
    assertEquals("queued record must reach local observation", 0L, processed.getCount());
    assertEquals(6L, consumer.committed(partition).offset());
  }
}
