package com.celements.observation.remote.kafka;

import static com.google.common.base.Preconditions.*;
import static org.xwiki.observation.remote.RemoteObservationManagerConfiguration.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.servlet.NodeConfig.NodeIdentity;
import com.google.common.base.Suppliers;

@Component
public class KafkaConfig {

  private final NodeIdentity nodeIdentity;
  private final ConfigurationSource configSource;

  @Inject
  public KafkaConfig(
      NodeIdentity nodeIdentity,
      @Named("allproperties") ConfigurationSource configSource) {
    this.nodeIdentity = nodeIdentity;
    this.configSource = configSource;
  }

  private ConfigurationSource getConfigSource() {
    return configSource;
  }

  private Supplier<String> servers = Suppliers.memoize(() -> getConfigSource()
      .getProperty("kafka.servers", "").trim());

  /**
   * Kafka bootstrap servers as a comma-separated list.
   */
  public String getServers() {
    return servers.get();
  }

  /**
   * Kafka topic to use for remote observation events.
   */
  public String getTopic() {
    return "observation.remote." + nodeIdentity.clusterName();
  }

  public String getClientId() {
    return nodeIdentity.nodeName();
  }

  /**
   * Returns true if all mandatory Kafka settings are present.
   */
  public boolean isConfigured() {
    return !getServers().isEmpty() && !getTopic().isEmpty() && !getClientId().isEmpty();
  }

  /**
   * Builds a Kafka producer factory for sending remote observation events.
   * <p>
   * Key characteristics:
   * - acks=all + idempotence enabled for strongest delivery guarantees
   * - long delivery timeout to tolerate temporary broker unavailability
   * - String keys and byte[] payloads (payload is serialized and optionally encrypted)
   */
  public DefaultKafkaProducerFactory<String, byte[]> buildProducerFactory() {
    checkState(isConfigured(), "KafkaConfig not configured");
    return new DefaultKafkaProducerFactory<>(Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getServers(),
        ProducerConfig.CLIENT_ID_CONFIG, getClientId(),
        ProducerConfig.ACKS_CONFIG, "all",
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true",
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "600000", // 10 minutes
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class));
  }

  /**
   * Builds a Kafka consumer factory for receiving remote observation events.
   * <p>
   * Notable settings:
   * - group.id=client.id: every node is its own group, achieving broadcast semantics.
   * - auto-commit disabled: offsets are acknowledged manually by the listener.
   * - read_committed: only consume messages from committed transactions.
   */
  public DefaultKafkaConsumerFactory<String, byte[]> buildConsumerFactory() {
    checkState(isConfigured(), "KafkaConfig not configured");
    return new DefaultKafkaConsumerFactory<>(Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getServers(),
        ConsumerConfig.CLIENT_ID_CONFIG, getClientId(),
        ConsumerConfig.GROUP_ID_CONFIG, getClientId(), // broadcast every message to all consumers
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "50",
        ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class));
  }

  /**
   * Builds an optional encryptor for Kafka payloads.
   * <p>
   * If both password and salt are configured, a strong encryptor is created.
   * {@link Encryptors#stronger(String, String)} provides authenticated encryption
   * (AEAD), meaning that payload tampering will be detected during decryption.
   */
  public Optional<BytesEncryptor> buildEncryptor() {
    // openssl rand -base64 32
    var cryptoPass = configSource.getProperty(CFG_KEY + ".kafka.crypto.pass", "").trim();
    // openssl rand -hex 16
    var cryptoSalt = configSource.getProperty(CFG_KEY + ".kafka.crypto.salt", "").trim();
    if (!cryptoPass.isEmpty() && !cryptoSalt.isEmpty()) {
      return Optional.of(Encryptors.stronger(cryptoPass, cryptoSalt));
    }
    return Optional.empty();
  }
}
