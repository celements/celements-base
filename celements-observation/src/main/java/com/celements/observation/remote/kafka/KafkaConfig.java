package com.celements.observation.remote.kafka;

import static com.google.common.base.Preconditions.*;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

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
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.google.common.base.Suppliers;

@Component
public class KafkaConfig {

  private static final String CFG_PREFIX = RemoteObservationManagerConfiguration.CFG_KEY
      + ".kafka.";

  private final ConfigurationSource configSource;

  @Inject
  public KafkaConfig(
      @Named("allproperties") ConfigurationSource configSource) {
    this.configSource = configSource;
  }

  private ConfigurationSource getConfigSource() {
    return configSource;
  }

  private Supplier<String> servers = Suppliers.memoize(() -> getConfigSource()
      .getProperty(CFG_PREFIX + "servers", "").trim());

  /**
   * Kafka bootstrap servers as a comma-separated list.
   */
  public String getServers() {
    return servers.get();
  }

  private Supplier<String> topic = Suppliers.memoize(() -> getConfigSource()
      .getProperty(CFG_PREFIX + "topic", "").trim());

  /**
   * Kafka topic to use for remote observation events.
   */
  public String getTopic() {
    return topic.get();
  }

  private Supplier<String> clientId = Suppliers.memoize(this::getJvmRoute);

  public String getClientId() {
    return clientId.get();
  }

  /**
   * Reads the Tomcat jvmRoute via JMX. It provides a stable, node-specific identifier in clustered
   * environments and avoids having to configure an explicit Kafka client id.
   */
  private String getJvmRoute() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("Catalina:type=Engine");
      return Objects.toString(mBeanServer.getAttribute(name, "jvmRoute"), "");
    } catch (JMException e) {
      // without a clientId, message loop prevention and grouping semantics would break.
      throw new IllegalStateException("Failed to read jvmRoute", e);
    }
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
    var cryptoPass = configSource.getProperty(CFG_PREFIX + "crypto.pass", "").trim();
    // openssl rand -hex 16
    var cryptoSalt = configSource.getProperty(CFG_PREFIX + "crypto.salt", "").trim();
    if (!cryptoPass.isEmpty() && !cryptoSalt.isEmpty()) {
      return Optional.of(Encryptors.stronger(cryptoPass, cryptoSalt));
    }
    return Optional.empty();
  }
}
