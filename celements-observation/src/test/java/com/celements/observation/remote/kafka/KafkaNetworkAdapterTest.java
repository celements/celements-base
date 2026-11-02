package com.celements.observation.remote.kafka;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.xwiki.observation.remote.RemoteEventData;

import com.celements.common.test.AbstractBaseComponentTest;

public class KafkaNetworkAdapterTest extends AbstractBaseComponentTest {

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(KafkaConfig.class);
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
