package com.celements.wiki.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;

public class WikiManagerServiceTest extends AbstractComponentTest {

  private WikiManagerService service;

  @Before
  public void setUp() throws Exception {
    service = getSpringContext().getBean(WikiManagerService.class);
  }

  @Test
  public void test_loadingService() throws Exception {
    replayDefault();
    assertNotNull(service);
    verifyDefault();
  }

}
