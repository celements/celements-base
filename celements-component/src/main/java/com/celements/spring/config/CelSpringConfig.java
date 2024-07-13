package com.celements.spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.celements",
    "org.opencelements" })
public class CelSpringConfig {}
