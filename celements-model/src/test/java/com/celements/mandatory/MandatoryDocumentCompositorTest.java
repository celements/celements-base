package com.celements.mandatory;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.web.Utils;

public class MandatoryDocumentCompositorTest extends AbstractComponentTest {

  private MandatoryDocumentCompositor mdCompositor;

  @Before
  public void setUp_MandatoryDocumentCompositorTest() throws Exception {
    mdCompositor = new MandatoryDocumentCompositor();
    mdCompositor.execution = Utils.getComponent(Execution.class);
    mdCompositor.mandatoryDocumentsMap = new HashMap<>();
  }

  @Test
  public void testGetMandatoryDocumentsList_empty() {
    replayDefault();
    assertEquals(Collections.emptyList(), mdCompositor.getMandatoryDocumentsList());
    verifyDefault();
  }

  @Test
  public void testGetMandatoryDocumentsList() {
    // use LinkedHashMap to preserve inserting order.
    mdCompositor.mandatoryDocumentsMap = new LinkedHashMap<>();

    IMandatoryDocumentRole mandDocMockA = createDefaultMock(IMandatoryDocumentRole.class);
    expect(mandDocMockA.dependsOnMandatoryDocuments()).andReturn(List.of()).atLeastOnce();
    expect(mandDocMockA.order()).andReturn(2).anyTimes();
    mdCompositor.mandatoryDocumentsMap.put("mandDocMockA", mandDocMockA);

    IMandatoryDocumentRole mandDocMockB = createDefaultMock(IMandatoryDocumentRole.class);
    expect(mandDocMockB.dependsOnMandatoryDocuments())
        .andReturn(List.of("mandDocMockA")).atLeastOnce();
    expect(mandDocMockB.order()).andReturn(1).anyTimes();
    mdCompositor.mandatoryDocumentsMap.put("mandDocMockB", mandDocMockB);

    IMandatoryDocumentRole mandDocMockC = createDefaultMock(IMandatoryDocumentRole.class);
    expect(mandDocMockC.dependsOnMandatoryDocuments()).andReturn(List.of()).atLeastOnce();
    expect(mandDocMockC.order()).andReturn(0).anyTimes();
    mdCompositor.mandatoryDocumentsMap.put("mandDocMockC", mandDocMockC);

    replayDefault();
    List<String> expectedExedList = List.of("mandDocMockC", "mandDocMockA", "mandDocMockB");
    assertNotEquals("check precondition", expectedExedList,
        new ArrayList<>(mdCompositor.mandatoryDocumentsMap.keySet()));
    assertEquals(expectedExedList, mdCompositor.getMandatoryDocumentsList());
    verifyDefault();
  }

}
