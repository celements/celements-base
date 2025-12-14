package com.celements.observation.remote.kafka;

import static com.google.common.base.Preconditions.*;
import static java.nio.charset.StandardCharsets.*;
import static org.springframework.util.SerializationUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Component;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;

/**
 * {@link NetworkAdapter} implementation that broadcasts {@link RemoteEventData} via Apache Kafka.
 * <p>
 * Bootstrap servers, topic and client id are provided by {@link KafkaConfig}.
 * <p>
 * Call {@link #start(Consumer)} to initialize and start a Kafka producer and a single-threaded
 * Kafka consumer for the topic configured in {@link KafkaConfig}. Call {@link #stop()} to stop
 * the consumer/producer.
 * <p>
 * For loop prevention, outgoing messages include an {@code origin} header containing the
 * configured {@code clientId}. Incoming messages with the same {@code origin} are ignored.
 * <p>
 * The consumer implements at-most-once processing semantics by immediately acknowledging records,
 * even when deserialization fails.
 * <p>
 * If {@link KafkaConfig} provides a {@link BytesEncryptor}, message payloads are encrypted before
 * sending and decrypted on reception. The authenticated encryptor detects payload tampering and
 * provides confidentiality for payloads in transit and at rest outside the application process.
 */
@Component("kafka")
public class KafkaNetworkAdapter implements NetworkAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaNetworkAdapter.class);

  private static final String KEY_ORIGIN = "origin";
  private static final String KEY_ENCRYPTED = "encrypted";
  private static final String ENCRYPTION_VERSION = "v1";

  private final KafkaConfig config;

  private DefaultKafkaProducerFactory<String, byte[]> producerFactory;
  private KafkaTemplate<String, byte[]> producer;

  private DefaultKafkaConsumerFactory<String, byte[]> consumerFactory;
  private ConcurrentMessageListenerContainer<String, byte[]> consumer;

  private BytesEncryptor encryptor;

  @Inject
  public KafkaNetworkAdapter(KafkaConfig kafkaConfig) {
    this.config = kafkaConfig;
  }

  @PostConstruct
  public void init() {
    producerFactory = config.buildProducerFactory();
    consumerFactory = config.buildConsumerFactory();
    encryptor = config.buildEncryptor().orElse(null);
    LOGGER.info("initialized (servers={}, topic={}, clientId={}, encryption={})",
        config.getServers(), config.getTopic(), config.getClientId(),
        (encryptor != null ? "enabled" : "disabled"));
  }

  @Override
  public synchronized void start(Consumer<RemoteEventData> onRemoteEvent) {
    startProducer();
    startConsumer(onRemoteEvent);
  }

  private void startProducer() {
    if (producer != null) {
      return;
    }
    checkState(producerFactory != null, "not initialized");
    producer = new KafkaTemplate<>(producerFactory);
    LOGGER.info("startProducer - topic [{}]", config.getTopic());
  }

  private void startConsumer(Consumer<RemoteEventData> onRemoteEvent) {
    if ((consumer != null) && consumer.isRunning()) {
      return;
    }
    checkState(consumerFactory != null, "not initialized");
    ContainerProperties props = new ContainerProperties(config.getTopic());
    props.setAckMode(AckMode.MANUAL_IMMEDIATE);
    AcknowledgingMessageListener<String, byte[]> listener;
    listener = (kafkaRecord, ack) -> receive(kafkaRecord, ack, onRemoteEvent);
    props.setMessageListener(listener);
    consumer = new ConcurrentMessageListenerContainer<>(consumerFactory, props);
    consumer.setConcurrency(1);
    consumer.start();
    LOGGER.info("startConsumer - topic [{}]", config.getTopic());
  }

  @Override
  public void send(RemoteEventData remoteEvent) {
    if (producer == null) {
      LOGGER.warn("send - producer not started; dropping message: {}", remoteEvent);
      return;
    }
    List<RecordHeader> headers = new ArrayList<>();
    // loop prevention
    headers.add(new RecordHeader(KEY_ORIGIN, config.getClientId().getBytes(UTF_8)));
    byte[] payload;
    try {
      payload = serialize(remoteEvent);
      if (encryptor != null) {
        payload = encryptor.encrypt(payload);
        headers.add(new RecordHeader(KEY_ENCRYPTED, ENCRYPTION_VERSION.getBytes(UTF_8)));
      }
    } catch (Exception e) {
      LOGGER.error("Failed to serialize RemoteEventData; dropping message", e);
      return;
    }
    var kafkaRecord = new ProducerRecord<>(config.getTopic(), "observation", payload);
    headers.forEach(kafkaRecord.headers()::add);
    producer.send(kafkaRecord).addCallback(
        result -> LOGGER.debug("sent to [{}]: {}", config.getTopic(), remoteEvent),
        exc -> LOGGER.error("send to [{}] failed: {}", config.getTopic(), remoteEvent, exc));
  }

  private void receive(ConsumerRecord<String, byte[]> kafkaRecord, Acknowledgment ack,
      Consumer<RemoteEventData> onRemoteEvent) {
    try {
      if (checkOrigin(kafkaRecord) && checkEncryption(kafkaRecord)) {
        extractEvent(kafkaRecord).ifPresent(onRemoteEvent::accept);
      }
    } catch (Exception e) {
      LOGGER.warn("receive - failed to process message: {}", kafkaRecord.timestamp(), e);
    } finally {
      ack.acknowledge(); // always acknowledge to avoid indefinite reprocessing
    }
  }

  private boolean checkOrigin(ConsumerRecord<String, byte[]> kafkaRecord) {
    var origin = Optional.ofNullable(kafkaRecord.headers().lastHeader(KEY_ORIGIN))
        .map(header -> new String(header.value(), UTF_8))
        .orElse("");
    if (config.getClientId().equals(origin)) {
      LOGGER.debug("receive - skipping loop message: {}", kafkaRecord.timestamp());
      return false;
    }
    return true;
  }

  private boolean checkEncryption(ConsumerRecord<String, byte[]> kafkaRecord) {
    var version = getEncryption(kafkaRecord);
    if (version.isPresent() && (encryptor == null)) {
      LOGGER.warn("receive - no encryptor, unable to decrypt message: {}", kafkaRecord.timestamp());
      return false;
    } else if (version.isPresent() && !version.get().equals(ENCRYPTION_VERSION)) {
      LOGGER.warn("receive - unsupported encryption version {}, dropping message: {}",
          version.get(), kafkaRecord.timestamp());
      return false;
    }
    return true;
  }

  private Optional<String> getEncryption(ConsumerRecord<String, byte[]> kafkaRecord) {
    return Optional.ofNullable(kafkaRecord.headers().lastHeader(KEY_ENCRYPTED))
        .map(header -> new String(header.value(), UTF_8));
  }

  private Optional<RemoteEventData> extractEvent(ConsumerRecord<String, byte[]> kafkaRecord) {
    var isEncrypted = getEncryption(kafkaRecord).isPresent();
    var payload = kafkaRecord.value();
    if (isEncrypted) {
      try {
        payload = encryptor.decrypt(payload);
      } catch (Exception e) {
        LOGGER.warn("receive - failed to decrypt message: {}", kafkaRecord.timestamp(), e);
        return Optional.empty();
      }
    }
    try {
      var remoteEvent = deserialize(payload);
      if (remoteEvent instanceof RemoteEventData) {
        LOGGER.debug("receive - from [{}]: {}", config.getTopic(), remoteEvent);
        return Optional.of((RemoteEventData) remoteEvent);
      } else {
        LOGGER.warn("receive - invalid remote message: {}", kafkaRecord.timestamp());
      }
    } catch (IllegalArgumentException | IllegalStateException e) {
      LOGGER.warn("receive - failed to deserialize data: {}", kafkaRecord.timestamp(), e);
    }
    return Optional.empty();
  }

  @Override
  public synchronized void stop() {
    LOGGER.info("stop - topic [{}]", config.getTopic());
    try {
      if (consumer == null) {
        return;
      }
      consumer.stop();
      LOGGER.info("stop - topic [{}]", config.getTopic());
    } finally {
      producer = null;
      consumer = null;
    }
  }
}
