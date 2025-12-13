package com.celements.observation.remote.kafka;

import static org.springframework.util.SerializationUtils.*;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

@Component("kafka")
public class KafkaNetworkAdapter implements NetworkAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaNetworkAdapter.class);

  private static final String CFG_PREFIX = RemoteObservationManagerConfiguration.CFG_KEY
      + ".kafka.";
  private static final String ORIGIN = "origin";

  private final String servers;
  private final String topic;
  private final String clientId;

  private KafkaTemplate<String, byte[]> producer;
  private DefaultKafkaConsumerFactory<String, byte[]> consumerFactory;
  private ConcurrentMessageListenerContainer<String, byte[]> consumer;

  @Inject
  public KafkaNetworkAdapter(ConfigurationSource cfgSrc) {
    this.servers = cfgSrc.getProperty(CFG_PREFIX + "servers", "").trim();
    this.topic = cfgSrc.getProperty(CFG_PREFIX + "topic", "").trim();
    this.clientId = getJvmRoute();
  }

  @PostConstruct
  public void initialize() {
    if (servers.isEmpty() || topic.isEmpty() || clientId.isEmpty()) {
      throw new IllegalStateException("configuration missing");
    }
    producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
        ProducerConfig.CLIENT_ID_CONFIG, clientId,
        ProducerConfig.ACKS_CONFIG, "all",
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true",
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "600000", // 10 minutes
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class)));
    consumerFactory = new DefaultKafkaConsumerFactory<>(Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
        ConsumerConfig.GROUP_ID_CONFIG, clientId,
        ConsumerConfig.CLIENT_ID_CONFIG, clientId,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "50",
        ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class));
    LOGGER.info("initialized (servers={}, topic={}, clientId={})", servers, topic, clientId);
  }

  @Override
  public void send(RemoteEventData remoteEvent) {
    if (producer == null) {
      return; // not initialized
    }
    final byte[] payload;
    try {
      payload = serialize(remoteEvent);
    } catch (IllegalArgumentException e) {
      LOGGER.error("Failed to serialize RemoteEventData; dropping message", e);
      return;
    }
    var kafkaRecord = new ProducerRecord<>(topic, "observation", payload);
    // self-sign
    var originHeader = new RecordHeader(ORIGIN, clientId.getBytes(StandardCharsets.UTF_8));
    kafkaRecord.headers().add(originHeader);
    producer.send(kafkaRecord).addCallback(
        result -> LOGGER.trace("sent to [{}]: {}", topic, remoteEvent),
        exc -> LOGGER.error("send to [{}] failed: {}", topic, remoteEvent, exc));
  }

  @Override
  public synchronized void start(Consumer<RemoteEventData> onRemoteEvent)
      throws RemoteEventException {
    if (consumerFactory == null) {
      throw new IllegalStateException("KafkaNetworkAdapter not initialized");
    } else if ((consumer != null) && consumer.isRunning()) {
      return;
    }
    AcknowledgingMessageListener<String, byte[]> listener = (kafkaRecord, ack) -> {
      try {
        extractEvent(kafkaRecord).ifPresent(onRemoteEvent::accept);
        ack.acknowledge();
      } catch (Exception e) {
        LOGGER.warn("Remote event handler failed; will retry message later", e);
      }
    };
    ContainerProperties props = new ContainerProperties(topic);
    props.setAckMode(AckMode.MANUAL_IMMEDIATE);
    props.setMessageListener(listener);
    consumer = new ConcurrentMessageListenerContainer<>(consumerFactory, props);
    consumer.setConcurrency(1);
    consumer.start();
    LOGGER.info("Kafka remote observation consumer started (topic={})", topic);
  }

  private Optional<RemoteEventData> extractEvent(ConsumerRecord<String, byte[]> kafkaRecord) {
    var origin = Optional.ofNullable(kafkaRecord.headers().lastHeader(ORIGIN))
        .map(header -> new String(header.value(), StandardCharsets.UTF_8))
        .orElse("");
    if (clientId.equals(origin)) {
      LOGGER.trace("receive - skipping self-signed message: {}", kafkaRecord.timestamp());
      return Optional.empty();
    }
    try {
      var remoteEvent = deserialize(kafkaRecord.value());
      if (remoteEvent instanceof RemoteEventData) {
        LOGGER.trace("receive - from [{}]: {}", topic, remoteEvent);
        return Optional.of((RemoteEventData) remoteEvent);
      } else {
        LOGGER.warn("receive - invalid remote event data: {}", kafkaRecord);
      }
    } catch (IllegalArgumentException | IllegalStateException e) {
      LOGGER.warn("receive - failed to deserialize data: {}", kafkaRecord, e);
    }
    return Optional.empty();
  }

  @Override
  public synchronized void stop() throws RemoteEventException {
    if (consumer == null) {
      return;
    }
    try {
      consumer.stop();
      LOGGER.info("Kafka remote observation consumer stopped (topic={})", topic);
    } catch (Exception e) {
      throw new RemoteEventException("Failed to stop Kafka consumer", e);
    } finally {
      consumer = null;
    }
  }

  private String getJvmRoute() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("Catalina:type=Engine");
      return Objects.toString(mBeanServer.getAttribute(name, "jvmRoute"), "");
    } catch (JMException e) {
      LOGGER.error("Failed to get JVM route", e);
      return "";
    }
  }
}
