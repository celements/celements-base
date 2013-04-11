/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.celements.web.plugin.cmd;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.celements.navigation.TreeNode;
import com.celements.navigation.service.ITreeNodeService;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class EmptyCheckCommandTest extends AbstractBridgedComponentTestCase {

  private XWikiContext context;
  private XWiki xwiki;
  private EmptyCheckCommand emptyChildCheckCmd;
  private ITreeNodeService treeNodeService;
  private ComponentDescriptor<ITreeNodeService> treeNodeServiceDesc;
  private ITreeNodeService savedTreeNodeServiceDesc;

  @Before
  public void setUp_EmptyCheckCommandTest() throws Exception {
    context = getContext();
    xwiki = getWikiMock();
    emptyChildCheckCmd = new EmptyCheckCommand();
    treeNodeService = createMockAndAddToDefault(ITreeNodeService.class);
    treeNodeServiceDesc = getComponentManager().getComponentDescriptor(
        ITreeNodeService.class, "default");
    savedTreeNodeServiceDesc = getComponentManager().lookup(ITreeNodeService.class);
    getComponentManager().unregisterComponent(ITreeNodeService.class, "default");
    getComponentManager().registerComponent(treeNodeServiceDesc, treeNodeService);
  }

  @After
  public void shutdown_EmptyCheckCommandTest() throws Exception {
    getComponentManager().unregisterComponent(ITreeNodeService.class, "default");
    getComponentManager().registerComponent(treeNodeServiceDesc,
        savedTreeNodeServiceDesc);
  }

  @Test
  public void testIsEmptyRTEDocument_empty() {
    assertTrue(emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc("")));
  }

  @Test
  public void testIsEmptyRTEDocument_cel2_standard_oldRTE_2Space() {
    assertTrue("Lonly non breaking spaces (2) with break should be"
        + " treated as empty", emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
            "<p>&nbsp;&nbsp;</p>")));
  }

  @Test
  public void testIsEmptyRTEDocument_cel2_standard_oldRTE_1Break() {
    assertTrue("Lonly non breaking spaces (2) with break should be"
        + " treated as empty", emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
            "<p><br /></p>")));
  }

  @Test
  public void testIsEmptyRTEDocument_manualizer_example() {
    assertTrue("Paragraph with span surrounding break should be"
        + " treated as empty", emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
        "<p><span style=\"line-height: normal; font-size: 10px;\"><br /></span></p>")));
  }
  
  @Test
  public void testIsEmptyRTEDocument_cel2_standard_oldRTE_2Space1Break() {
    assertTrue("Lonly non breaking spaces (2) with break should be"
        + " treated as empty", emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
            "<p>&nbsp;&nbsp; <br /></p>")));
  }

  @Test
  public void testIsEmptyRTEDocument_cel2_standard_oldRTE_3Space1Break() {
    assertTrue("Lonly non breaking spaces (3) with break should be"
        + " treated as empty", emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
            "<p>&nbsp;&nbsp;&nbsp;<br /></p>")));
  }


  @Test
  public void testIsEmptyRTEDocument_cel2_standard_oldRTE_1Space2Break() {
    assertTrue("Lonly non breaking spaces with break (2) should be"
        + " treated as empty", emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
            "<p>&nbsp;<br /><br /></p>")));
  }

  @Test
  public void testIsEmptyRTEDocument_cel2_standard_oldRTE_REGULAR_TEXT() {
    assertFalse("Regular Text (2) should not be treated as empty.",
        emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc(
            "<p>adsf  &nbsp; <br />sadf</p>")));
  }

  @Test
  public void testIsEmptyRTEDocument_nbsp() {
    assertTrue("Lonly non breaking spaces should be treated as empty",
        emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc("&nbsp;")));
    assertTrue("Non breaking spaces in a paragraph should be treated as empty",
        emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc("<p>&nbsp;</p>")));
    assertTrue("Non breaking spaces in a paragraph with white spaces"
        + " should be treated as empty",
        emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc("<p>  &nbsp; </p>")));
    assertFalse("Regular Text should not be treated as empty.",
        emptyChildCheckCmd.isEmptyRTEDocument(getTestDoc("<p>adsf  &nbsp; </p>")));
  }

  @Test
  public void testGetNextNonEmptyChildren_notEmpty() throws Exception {
    DocumentReference documentRef = new DocumentReference(context.getDatabase(),
        "mySpace", "MyDoc");
    XWikiDocument myXdoc = new XWikiDocument(documentRef);
    myXdoc.setContent("test content not empty");
    expect(xwiki.getDocument(eq(documentRef), same(context))).andReturn(myXdoc).once();
    Object[] mocks = {};
    replayDefault(mocks);
    assertEquals(documentRef, emptyChildCheckCmd.getNextNonEmptyChildren(documentRef));
    Object[] mocks1 = {};
    verifyDefault(mocks1);
  }

  @Test
  public void testGetNextNonEmptyChildren_empty_but_noChildren() throws Exception {
    DocumentReference emptyDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "MyEmptyDoc");
    XWikiDocument myXdoc = createEmptyDoc(emptyDocRef);
    List<TreeNode> noChildrenList = Collections.emptyList();
    expect(treeNodeService.getSubNodesForParent(eq(emptyDocRef), eq(""))
        ).andReturn(noChildrenList).once();
    Object[] mocks = { myXdoc };
    replayDefault(mocks);
    assertEquals(emptyDocRef, emptyChildCheckCmd.getNextNonEmptyChildren(emptyDocRef));
    Object[] mocks1 = { myXdoc };
    verifyDefault(mocks1);
  }

  @Test
  public void testGetNextNonEmptyChildren_empty_with_nonEmptyChildren() throws Exception {
    DocumentReference emptyDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "MyEmptyDoc");
    XWikiDocument myXdoc = createEmptyDoc(emptyDocRef);
    List<TreeNode> childrenList = Arrays.asList(new TreeNode(new DocumentReference(
        context.getDatabase(), "mySpace", "myChild"), "mySpace.MyEmptyDoc", 0),
        new TreeNode(new DocumentReference(context.getDatabase(), "mySpace", "myChild2"),
            "mySpace.MyEmptyDoc", 1));
    expect(treeNodeService.getSubNodesForParent(eq(emptyDocRef), eq(""))
        ).andReturn(childrenList).once();
    DocumentReference expectedChildDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "myChild");
    XWikiDocument childXdoc = new XWikiDocument(expectedChildDocRef);
    childXdoc.setContent("non empty child content");
    expect(xwiki.getDocument(eq(expectedChildDocRef), same(context))).andReturn(childXdoc 
        ).once();
    Object[] mocks = { myXdoc };
    replayDefault(mocks);
    assertEquals(expectedChildDocRef, emptyChildCheckCmd.getNextNonEmptyChildren(
        emptyDocRef));
    Object[] mocks1 = { myXdoc };
    verifyDefault(mocks1);
  }

  @Test
  public void testGetNextNonEmptyChildren_empty_recurse_on_EmptyChildren(
      ) throws Exception {
    DocumentReference emptyDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "MyEmptyDoc");
    XWikiDocument myXdoc = createEmptyDoc(emptyDocRef);
    List<TreeNode> childrenList = Arrays.asList(new TreeNode(new DocumentReference(
        context.getDatabase(), "mySpace", "myChild"), "mySpace.MyEmptyDoc", 0),
        new TreeNode(new DocumentReference(context.getDatabase(), "mySpace", "myChild2"),
            "mySpace.MyEmptyDoc", 1));
    expect(treeNodeService.getSubNodesForParent(eq(emptyDocRef), eq(""))
        ).andReturn(childrenList).once();
    DocumentReference childDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "myChild");
    XWikiDocument childXdoc = createEmptyDoc(childDocRef);
    List<TreeNode> childrenList2 = Arrays.asList(new TreeNode(new DocumentReference(
        context.getDatabase(), "mySpace", "myChildChild"), "mySpace.MyEmptyDoc", 0),
        new TreeNode(new DocumentReference(context.getDatabase(), "mySpace",
            "myChildChild2"), "mySpace.MyEmptyDoc", 1));
    expect(treeNodeService.getSubNodesForParent(eq(childDocRef), eq(""))).andReturn(
        childrenList2).once();
    DocumentReference expectedChildDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "myChildChild");
    XWikiDocument childChildXdoc = new XWikiDocument(expectedChildDocRef);
    childChildXdoc.setContent("non empty child content");
    expect(xwiki.getDocument(eq(expectedChildDocRef), same(context))).andReturn(
        childChildXdoc).once();
    Object[] mocks = { myXdoc, childXdoc };
    replayDefault(mocks);
    assertEquals(expectedChildDocRef, emptyChildCheckCmd.getNextNonEmptyChildren(
        emptyDocRef));
    Object[] mocks1 = { myXdoc, childXdoc };
    verifyDefault(mocks1);
  }

  @Test
  public void testGetNextNonEmptyChildren_empty_recurse_on_EmptyChildren_reconize_loop(
      ) throws Exception {
    DocumentReference emptyDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "MyEmptyDoc");
    XWikiDocument myXdoc = createEmptyDoc(emptyDocRef);
    List<TreeNode> childrenList = Arrays.asList(new TreeNode(new DocumentReference(
        context.getDatabase(), "mySpace", "myChild"), "mySpace.MyEmptyDoc", 0),
        new TreeNode(new DocumentReference(context.getDatabase(), "mySpace", "myChild2"),
            "mySpace.MyEmptyDoc", 1));
    //if called more than once the recursion detection is very likely broken!
    expect(treeNodeService.getSubNodesForParent(eq(emptyDocRef), eq(""))).andReturn(
        childrenList).once();
    DocumentReference childDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "myChild");
    XWikiDocument childXdoc = createEmptyDoc(childDocRef);
    List<TreeNode> childrenList2 = Arrays.asList(new TreeNode(new DocumentReference(
        context.getDatabase(), "mySpace", "myChildChild"), "mySpace.MyEmptyDoc", 0),
        new TreeNode(new DocumentReference(
            context.getDatabase(), "mySpace", "myChildChild2"), "mySpace.MyEmptyDoc", 1));
    expect(treeNodeService.getSubNodesForParent(eq(childDocRef), eq(""))).andReturn(
        childrenList2).once();
    DocumentReference expectedChildDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "myChildChild");
    XWikiDocument childChildXdoc = createEmptyDoc(expectedChildDocRef);
    List<TreeNode> childrenList3 = Arrays.asList(new TreeNode(new DocumentReference(
        context.getDatabase(), "mySpace", "MyEmptyDoc"), "mySpace.MyEmptyDoc", 0));
    expect(treeNodeService.getSubNodesForParent(eq(expectedChildDocRef), eq(""))
        ).andReturn(childrenList3).once();
    Object[] mocks = { myXdoc, childXdoc, childChildXdoc };
    replayDefault(mocks);
    assertEquals(expectedChildDocRef, emptyChildCheckCmd.getNextNonEmptyChildren(
        emptyDocRef));
    Object[] mocks1 = { myXdoc, childXdoc, childChildXdoc };
    verifyDefault(mocks1);
  }

  //*****************************************************************
  //*                  H E L P E R  - M E T H O D S                 *
  //*****************************************************************/

  private XWikiDocument createEmptyDoc(DocumentReference emptyDocRef)
      throws XWikiException {
    XWikiDocument myXdoc = createMock(XWikiDocument.class);
    XWikiDocument myXTdoc = new XWikiDocument(emptyDocRef);
    expect(myXdoc.getContent()).andReturn("").atLeastOnce();
    expect(myXdoc.getTranslatedDocument(same(context))).andReturn(myXTdoc).atLeastOnce();
    expect(xwiki.getDocument(eq(emptyDocRef), same(context))).andReturn(myXdoc
        ).atLeastOnce();
    return myXdoc;
  }

  private XWikiDocument getTestDoc(String inStr) {
    DocumentReference testDocRef = new DocumentReference("xwiki", "testSpace", "testDoc");
    XWikiDocument doc = new XWikiDocument(testDocRef );
    doc.setContent(inStr);
    return doc;
  }

}
