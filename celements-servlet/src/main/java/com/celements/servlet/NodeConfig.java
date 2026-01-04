package com.celements.servlet;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Supplier;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import one.util.streamex.StreamEx;

@Configuration
public class NodeConfig {

  @Bean(name = "nodeName")
  public String nodeName() {
    return StreamEx.of(
        getJvmRoute(),
        getSystemProperty(),
        getSystemEnv(),
        getRuntimeMXBeanName())
        .map(Supplier::get)
        .flatMap(Optional::stream)
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Cannot determine node name"));
  }

  /**
   * Reads the Tomcat jvmRoute via JMX. It provides a stable, node-specific identifier in clustered
   * environments.
   */
  private Supplier<Optional<String>> getJvmRoute() {
    return () -> {
      try {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("Catalina:type=Engine");
        return Optional.ofNullable(mBeanServer.getAttribute(name, "jvmRoute"))
            .map(Object::toString);
      } catch (JMException e) {
        return Optional.empty();
      }
    };
  }

  private Supplier<Optional<String>> getSystemProperty() {
    return () -> Optional.ofNullable(System.getProperty("NODE_NAME"));
  }

  private Supplier<Optional<String>> getSystemEnv() {
    return () -> Optional.ofNullable(System.getenv("NODE_NAME"));
  }

  private Supplier<Optional<String>> getRuntimeMXBeanName() {
    return () -> Optional.ofNullable(ManagementFactory.getRuntimeMXBean().getName())
        .map(name -> name.substring(name.indexOf('@') + 1));
  }
}
