package com.celements.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "org.xwiki",
    "com.xpn.xwiki" })
public class XWikiSpringConfig {}