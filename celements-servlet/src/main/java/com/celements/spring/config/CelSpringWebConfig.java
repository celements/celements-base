package com.celements.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class CelSpringWebConfig {

  @Bean
  public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
  }
}
