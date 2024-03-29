package com.celements.mandatory;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
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
    IMandatoryDocumentRole mockA_mandDoc = createDefaultMock(IMandatoryDocumentRole.class);
    expect(mockA_mandDoc.dependsOnMandatoryDocuments()).andReturn(
        Collections.<String>emptyList()).atLeastOnce();
    IMandatoryDocumentRole mockB_mandDocDepA = createDefaultMock(
        IMandatoryDocumentRole.class);
    expect(mockB_mandDocDepA.dependsOnMandatoryDocuments()).andReturn(Arrays.asList(
        "A_mandDoc")).atLeastOnce();
    mdCompositor.mandatoryDocumentsMap.put("B_mandDocDepA", mockB_mandDocDepA);
    mdCompositor.mandatoryDocumentsMap.put("A_mandDoc", mockA_mandDoc);
    replayDefault();
    List<String> expectedExedList = Arrays.asList("A_mandDoc", "B_mandDocDepA");
    assertFalse("check precondition", expectedExedList.equals(new ArrayList<>(
        mdCompositor.mandatoryDocumentsMap.keySet())));
    assertEquals(expectedExedList, mdCompositor.getMandatoryDocumentsList());
    verifyDefault();
  }

}
