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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for
 * {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultXObjectDocument}.
 *
 * @version $Id$
 */
public class DefaultXObjectDocumentTest extends AbstractComponentTest {

  private XWiki xwiki;

  private Map<DocumentReference, XWikiDocument> documents = new HashMap<>();

  private IModelAccessFacade modelAccessMock;
  private ModelContext mContext;
  private ModelUtils modelUtils;

  /**
   * {@inheritDoc}
   *
   * @see junit.framework.TestCase#setUp()
   */
  @Before
  public void prepare() throws Exception {
    mContext = Utils.getComponent(ModelContext.class);
    modelUtils = Utils.getComponent(ModelUtils.class);
    DEFAULT_DOC_REF = RefBuilder.from(mContext.getWikiRef()).space(DEFAULT_SPACE)
        .doc(DEFAULT_DOCFULLNAME).build(DocumentReference.class);
    modelAccessMock = registerComponentMock(IModelAccessFacade.class);
    mockGetDocAndSaveDoc();
    xwiki = getMock(XWiki.class);
    mockGetXClass();
  }

  // ///////////////////////////////////////////////////////////////////////////////////////:
  // Tests

  private final String DEFAULT_SPACE = "Space";

  private final String DEFAULT_DOCNAME = "Document";

  private final String DEFAULT_DOCFULLNAME = DEFAULT_SPACE + "." + DEFAULT_DOCNAME;
  private DocumentReference DEFAULT_DOC_REF;

  @Test
  public void testInitXObjectDocumentEmpty() throws XWikiException {
    documents.clear();
    replayDefault();
    XClassManager<XObjectDocument> sclass = XClassManagerTest.DispatchXClassManager.getInstance(
        modelAccessMock,
        mContext, modelUtils);
    DefaultXObjectDocument sdoc = (DefaultXObjectDocument) sclass.newXObjectDocument(getXContext());
    verifyDefault();
    assertNotNull(sdoc);
    assertTrue(sdoc.isNew());

    com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

    assertNotNull(obj);
    assertEquals(sdoc.getXClassManager(), sclass);
  }

  @Test
  public void testInitXObjectDocumentDocName() throws XWikiException {
    documents.clear();
    replayDefault();
    XClassManager<XObjectDocument> sclass = XClassManagerTest.DispatchXClassManager.getInstance(
        modelAccessMock,
        mContext, modelUtils);
    DefaultXObjectDocument sdoc = (DefaultXObjectDocument) sclass
        .newXObjectDocument(DEFAULT_DOC_REF, 0, getXContext());
    verifyDefault();
    assertNotNull(sdoc);
    assertTrue(sdoc.isNew());

    com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

    assertNotNull(obj);
    assertEquals(sdoc.getXClassManager(), sclass);
  }

  @Test
  public void testInitXObjectDocumentDocNameExists() throws Exception {
    documents.clear();
    replayDefault();
    XWikiDocument doc = modelAccessMock.getOrCreateDocument(DEFAULT_DOC_REF);
    modelAccessMock.saveDocument(doc);

    XClassManager<XObjectDocument> sclass = XClassManagerTest.DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils);
    DefaultXObjectDocument sdoc = (DefaultXObjectDocument) sclass
        .newXObjectDocument(DEFAULT_DOC_REF, 0, getXContext());
    verifyDefault();
    assertNotNull(sdoc);
    assertTrue(sdoc.isNew());

    com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

    assertNotNull(obj);
    assertEquals(sdoc.getXClassManager(), sclass);
  }

  @Test
  public void testMergeObject() throws XWikiException {
    replayDefault();
    XClassManager<XObjectDocument> sclass = XClassManagerTest.DispatchXClassManager.getInstance(
        modelAccessMock, mContext, modelUtils);
    DefaultXObjectDocument sdoc1 = (DefaultXObjectDocument) sclass
        .newXObjectDocument(getXContext());

    DefaultXObjectDocument sdoc2 = (DefaultXObjectDocument) sclass
        .newXObjectDocument(getXContext());

    sdoc1.setStringValue(XClassManagerTest.FIELD_string, "valuesdoc1");
    sdoc1.setStringValue(XClassManagerTest.FIELD_string2, "value2sdoc1");

    sdoc2.setStringValue(XClassManagerTest.FIELD_string, "valuesdoc2");
    sdoc2.setIntValue(XClassManagerTest.FIELD_int, 2);

    sdoc1.mergeObject(sdoc2);
    verifyDefault();
    assertEquals("The field is not overwritten",
        sdoc1.getStringValue(XClassManagerTest.FIELD_string),
        sdoc2.getStringValue(XClassManagerTest.FIELD_string));
    assertEquals("The field is removed", "value2sdoc1", sdoc1
        .getStringValue(XClassManagerTest.FIELD_string2));
    assertEquals("The field is not added", sdoc1.getIntValue(XClassManagerTest.FIELD_int), sdoc1
        .getIntValue(XClassManagerTest.FIELD_int));
  }

  /*******************
   * HELPER METHODS
   *******************/

  private @NotNull XWikiDocument stubGetDocument(DocumentReference theDocRef)
      throws DocumentNotExistsException {
    if (documents.containsKey(theDocRef)) {
      return documents.get(theDocRef);
    } else {
      throw new DocumentNotExistsException(theDocRef);
    }
  }

  private @NotNull XWikiDocument stubGetOrCreateDocument(DocumentReference theDocRef)
      throws DocumentNotExistsException {
    if (documents.containsKey(theDocRef)) {
      return documents.get(theDocRef);
    } else {
      XWikiDocument doc = new XWikiDocument(theDocRef);
      doc.setNew(true);
      doc.setLanguage(DEFAULT_LANG);
      doc.setDefaultLanguage(DEFAULT_LANG);
      doc.setTranslation(0);
      Date creationDate = new Date();
      doc.setCreationDate(creationDate);
      doc.setContentUpdateDate(creationDate);
      doc.setDate(creationDate);
      doc.setCreator(XWikiRightService.SUPERADMIN_USER);
      doc.setAuthor(XWikiRightService.SUPERADMIN_USER);
      doc.setContentAuthor(XWikiRightService.SUPERADMIN_USER);
      doc.setContent("");
      doc.setContentDirty(true);
      doc.setMetaDataDirty(true);
      doc.setOriginalDocument(new XWikiDocument(doc.getDocumentReference()));
      doc.setSyntax(Syntax.XWIKI_1_0);
      return doc;
    }
  }

  private void mockGetDocAndSaveDoc() throws Exception {
    expect(modelAccessMock.getDocument(isA(DocumentReference.class)))
        .andAnswer(() -> stubGetDocument(getCurrentArgument(0))).anyTimes();
    expect(modelAccessMock.getOrCreateDocument(isA(DocumentReference.class)))
        .andAnswer(() -> stubGetOrCreateDocument(getCurrentArgument(0))).anyTimes();
    IAnswer<? extends Object> saveDocStub = () -> {
      XWikiDocument theDoc = getCurrentArgument(0);
      theDoc.setNew(false);
      documents.put(theDoc.getDocumentReference(), theDoc);
      return null;
    };
    modelAccessMock.saveDocument(isA(XWikiDocument.class));
    expectLastCall().andAnswer(saveDocStub).anyTimes();
    modelAccessMock.saveDocument(isA(XWikiDocument.class), isA(String.class));
    expectLastCall().andAnswer(saveDocStub).anyTimes();
    modelAccessMock.saveDocument(isA(XWikiDocument.class), isA(String.class), anyBoolean());
    expectLastCall().andAnswer(saveDocStub).anyTimes();
  }

  private void mockGetXClass() throws Exception {
    expect(xwiki.getXClass(isA(DocumentReference.class), same(getXContext())))
        .andAnswer(() -> {
          DocumentReference classReference = getCurrentArgument(0);
          XWikiDocument doc = stubGetDocument(classReference);
          return doc.getxWikiClass();
        }).anyTimes();
  }
}
