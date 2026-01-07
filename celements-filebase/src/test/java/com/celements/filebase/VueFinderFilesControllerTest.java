package com.celements.filebase;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.celements.common.test.AbstractComponentTest;

public class VueFinderFilesControllerTest extends AbstractComponentTest {

  private VueFinderFilesController vueFinderCtrl;

  @Before
  public void prepare() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("test", "n/a", "ROLE_USER"));
    vueFinderCtrl = getBeanFactory().getBean(VueFinderFilesController.class);
  }

  @Test
  public void testNormalizeFileName_file() {
    String fileName = vueFinderCtrl.normalizeFileName("local://IMG-20250606-WA0000.jpg");
    assertEquals("IMG-20250606-WA0000.jpg", fileName);
  }

  @Test
  public void testNormalizeFileName_subPath() {
    String fileName = vueFinderCtrl.normalizeFileName("local://test/IMG-20250606-WA0000.jpg");
    assertEquals("IMG-20250606-WA0000.jpg", fileName);
  }

}
