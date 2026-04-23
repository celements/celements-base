package com.celements.servlet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xwiki.configuration.SystemEnvUtils;

@Configuration
public class NodeConfig {

  @Bean
  public NodeIdentity nodeIdentity() {
    return new NodeIdentity(
        getEnv("app.name"),
        getEnv("cluster.name"),
        getEnv("node.name"));
  }

  private String getEnv(String key) {
    return SystemEnvUtils.getEnv(key)
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .orElseThrow(() -> new IllegalStateException("Cannot determine " + key));
  }

  public static class NodeIdentity {

    private final String appName;
    private final String clusterName;
    private final String nodeName;

    private NodeIdentity(String appName, String clusterName, String nodeName) {
      this.appName = appName;
      this.clusterName = clusterName;
      this.nodeName = nodeName;
    }

    public String appName() {
      return appName;
    }

    public String clusterName() {
      return clusterName;
    }

    public String nodeName() {
      return nodeName;
    }

    public String nodeId() {
      return appName + ":" + clusterName + ":" + nodeName;
    }
  }
}
