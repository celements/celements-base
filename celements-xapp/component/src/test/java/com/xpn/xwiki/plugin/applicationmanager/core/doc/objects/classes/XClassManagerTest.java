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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for
 * {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractXClassManager}.
 *
 * @version $Id$
 */
public class XClassManagerTest extends AbstractComponentTest {

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
    modelAccessMock = registerComponentMock(IModelAccessFacade.class);
    mContext = Utils.getComponent(ModelContext.class);
    modelUtils = Utils.getComponent(ModelUtils.class);
    mockGetDocAndSaveDoc();
    xwiki = getMock(XWiki.class);
    mockClearName();
    mockGetXClass();
    /*
     * is getClass deprecated?!?
     * mockXWiki.stubs().method("getClass").will(new CustomStub("Implements XWiki.getClass") {
     *
     * @Override
     * public Object invoke(Invocation invocation) throws Throwable {
     * String classFullName = (String) invocation.parameterValues.get(0);
     * XWikiContext context = (XWikiContext) invocation.parameterValues.get(1);
     *
     * XWikiDocument doc = context.getWiki().getDocument(classFullName, context);
     *
     * return doc.getxWikiClass();
     * }
     * });
     */
  }

  // ///////////////////////////////////////////////////////////////////////////////////////:
  // Tests

  private static final String CLASS_SPACE_PREFIX = "Space";

  private static final String CLASS_PREFIX = "Prefix";

  private static final String CLASS_NAME = CLASS_PREFIX
      + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager.XWIKI_CLASS_SUFFIX;

  private static final String CLASSSHEET_NAME = CLASS_PREFIX
      + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager.XWIKI_CLASSSHEET_SUFFIX;

  private static final String CLASSTEMPLATE_NAME = CLASS_PREFIX
      + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager.XWIKI_CLASSTEMPLATE_SUFFIX;

  private static final String DISPATCH_CLASS_SPACE = CLASS_SPACE_PREFIX
      + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager.XWIKI_CLASS_SPACE_SUFFIX;

  private static final String DISPATCH_CLASS_FULLNAME = DISPATCH_CLASS_SPACE + "." + CLASS_NAME;

  private static final String DISPATCH_CLASSSHEET_SPACE = CLASS_SPACE_PREFIX
      + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager.XWIKI_CLASSSHEET_SPACE_SUFFIX;

  private static final String DISPATCH_CLASSSHEET_FULLNAME = DISPATCH_CLASSSHEET_SPACE + "."
      + CLASSSHEET_NAME;

  private static final String DISPATCH_CLASSTEMPLATE_SPACE = CLASS_SPACE_PREFIX
      + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager.XWIKI_CLASSTEMPLATE_SPACE_SUFFIX;

  private static final String DISPATCH_CLASSTEMPLATE_FULLNAME = DISPATCH_CLASSTEMPLATE_SPACE + "."
      + CLASSTEMPLATE_NAME;

  private static final String NODISPATCH_CLASS_SPACE = CLASS_SPACE_PREFIX;

  private static final String NODISPATCH_CLASS_FULLNAME = NODISPATCH_CLASS_SPACE + "." + CLASS_NAME;

  private static final String NODISPATCH_CLASSSHEET_SPACE = CLASS_SPACE_PREFIX;

  private static final String NODISPATCH_CLASSSHEET_FULLNAME = NODISPATCH_CLASSSHEET_SPACE + "."
      + CLASSSHEET_NAME;

  private static final String NODISPATCH_CLASSTEMPLATE_SPACE = CLASS_SPACE_PREFIX;

  private static final String NODISPATCH_CLASSTEMPLATE_FULLNAME = NODISPATCH_CLASSTEMPLATE_SPACE
      + "." + CLASSTEMPLATE_NAME;

  private static final String DEFAULT_ITEM_NAME = "item";

  private static final String DEFAULT_ITEMDOCUMENT_NAME = CLASS_PREFIX + "Item";

  private static final String DISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME = CLASS_SPACE_PREFIX + "."
      + DEFAULT_ITEMDOCUMENT_NAME;

  private static final String NODISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME = CLASS_SPACE_PREFIX + "."
      + DEFAULT_ITEMDOCUMENT_NAME;

  /**
   * Name of field <code>string</code>.
   */
  public static final String FIELD_string = "string";

  /**
   * Pretty name of field <code>string</code>.
   */
  public static final String FIELDPN_string = "String";

  /**
   * Name of field <code>string2</code>.
   */
  public static final String FIELD_string2 = "string2";

  /**
   * Pretty name of field <code>string2</code>.
   */
  public static final String FIELDPN_string2 = "String2";

  /**
   * Name of field <code>int</code>.
   */
  public static final String FIELD_int = "int";

  /**
   * Pretty name of field <code>int</code>.
   */
  public static final String FIELDPN_int = "Int";

  /**
   * Name of field <code>stringlist</code>.
   */
  public static final String FIELD_stringlist = "stringlist";

  /**
   * Pretty name of field <code>stringlist</code>.
   */
  public static final String FIELDPN_stringlist = "String List";

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with no parameters.
   */
  public static final String WHERECLAUSE_null = ", BaseObject as obj where doc.fullName=obj.name and obj.className=? and doc.fullName<>?";

  public static final String[] WHERECLAUSE_doc0 = { "df0", null, "dv0" };

  public static final String[] WHERECLAUSE_doc1 = { "df1", null, "dv1" };

  public static final String[] WHERECLAUSE_obj0 = { "of0", "String", "ov0" };

  public static final String[] WHERECLAUSE_obj1 = { "of1", "Int", "ov1" };

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with doc filter.
   */
  public static final String WHERECLAUSE_doc = ", BaseObject as obj where doc.fullName=obj.name and obj.className=? and doc.fullName<>? and lower(doc.df0)=?";

  public static final String[][] WHERECLAUSE_PARAM_doc = { WHERECLAUSE_doc0 };

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with more than one
   * docs filters.
   */
  public static final String WHERECLAUSE_doc_multi = ", BaseObject as obj where doc.fullName=obj.name and obj.className=? and doc.fullName<>? and lower(doc.df0)=? and lower(doc.df1)=?";

  public static final String[][] WHERECLAUSE_PARAM_doc_multi = { WHERECLAUSE_doc0,
      WHERECLAUSE_doc1 };

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with object filter.
   */
  public static final String WHERECLAUSE_obj = ", BaseObject as obj, String as field0 where doc.fullName=obj.name and obj.className=? and doc.fullName<>? and obj.id=field0.id.id and field0.name=? and lower(field0.value)=?";

  public static final String[][] WHERECLAUSE_PARAM_obj = { WHERECLAUSE_obj0 };

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with more than one
   * objects filters.
   */
  public static final String WHERECLAUSE_obj_multi = ", BaseObject as obj, String as field0, Int as field1 where doc.fullName=obj.name and obj.className=? and doc.fullName<>? and obj.id=field0.id.id and field0.name=? and lower(field0.value)=? and obj.id=field1.id.id and field1.name=? and lower(field1.value)=?";

  public static final String[][] WHERECLAUSE_PARAM_obj_multi = { WHERECLAUSE_obj0,
      WHERECLAUSE_obj1 };

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with object and doc
   * filter.
   */
  public static final String WHERECLAUSE_objdoc = ", BaseObject as obj, String as field1 where doc.fullName=obj.name and obj.className=? and doc.fullName<>? and lower(doc.df0)=? and obj.id=field1.id.id and field1.name=? and lower(field1.value)=?";

  public static final String[][] WHERECLAUSE_PARAM_objdoc = { WHERECLAUSE_doc0, WHERECLAUSE_obj0 };

  /**
   * Result of {@link AbstractXClassManager#createWhereClause(Object[][], List)} with more than one
   * objects and docs
   * filters.
   */
  public static final String WHERECLAUSE_objdoc_multi = ", BaseObject as obj, String as field2, Int as field3 where doc.fullName=obj.name and obj.className=? and doc.fullName<>? and lower(doc.df0)=? and lower(doc.df1)=? and obj.id=field2.id.id and field2.name=? and lower(field2.value)=? and obj.id=field3.id.id and field3.name=? and lower(field3.value)=?";

  public static final String[][] WHERECLAUSE_PARAM_objdoc_multi = { WHERECLAUSE_doc0,
      WHERECLAUSE_doc1, WHERECLAUSE_obj0, WHERECLAUSE_obj1 };

  static abstract public class TestXClassManager extends AbstractXClassManager<XObjectDocument> {

    /**
     * Default constructor for XWikiApplicationClass.
     */
    protected TestXClassManager(String spaceprefix, String prefix, boolean dispatch,
        IModelAccessFacade modelAccess, ModelContext mContext, ModelUtils modelUtils) {
      super(spaceprefix, prefix, dispatch, modelAccess, mContext, modelUtils);
    }

    @Override
    protected boolean updateBaseClass(BaseClass baseClass) {
      boolean needsUpdate = super.updateBaseClass(baseClass);

      needsUpdate |= baseClass.addTextField(FIELD_string, FIELDPN_string, 30);
      needsUpdate |= baseClass.addTextField(FIELD_string2, FIELDPN_string2, 30);
      needsUpdate |= baseClass.addNumberField(FIELD_int, FIELDPN_int, 10, "integer");
      needsUpdate |= baseClass.addTextField(FIELD_stringlist, FIELDPN_stringlist, 80);

      return needsUpdate;
    }
  }

  static public class DispatchXClassManager extends TestXClassManager {

    /**
     * Unique instance of XWikiApplicationClass;
     */
    private static DispatchXClassManager instance = null;

    /**
     * Return unique instance of XWikiApplicationClass and update documents for this context.
     *
     * @return XWikiApplicationClass Instance of XWikiApplicationClass.
     * @throws XWikiException
     */
    public static DispatchXClassManager getInstance(IModelAccessFacade modelAccess,
        ModelContext mContext, ModelUtils modelUtils) throws XWikiException {
      // if (instance == null)
      instance = new DispatchXClassManager(modelAccess, mContext, modelUtils);
      instance.check(mContext.getXWikiContext());
      return instance;
    }

    /**
     * Default constructor for XWikiApplicationClass.
     */
    private DispatchXClassManager(IModelAccessFacade modelAccess, ModelContext mContext,
        ModelUtils modelUtils) {
      super(CLASS_SPACE_PREFIX, CLASS_PREFIX, true, modelAccess, mContext, modelUtils);
    }
  }

  static public class NoDispatchXClassManager extends TestXClassManager {

    /**
     * Unique instance of XWikiApplicationClass;
     */
    private static NoDispatchXClassManager instance = null;

    /**
     * Return unique instance of XWikiApplicationClass and update documents for this context.
     *
     * @return XWikiApplicationClass Instance of XWikiApplicationClass.
     * @throws XWikiException
     */
    public static NoDispatchXClassManager getInstance(IModelAccessFacade modelAccess,
        ModelContext mContext, ModelUtils modelUtils) throws XWikiException {
      // if (instance == null)
      instance = new NoDispatchXClassManager(modelAccess, mContext, modelUtils);
      instance.check(mContext.getXWikiContext());
      return instance;
    }

    /**
     * Default constructor for XWikiApplicationClass.
     */
    private NoDispatchXClassManager(IModelAccessFacade modelAccess, ModelContext mContext,
        ModelUtils modelUtils) {
      super(CLASS_SPACE_PREFIX, CLASS_PREFIX, false, modelAccess, mContext, modelUtils);
    }
  }

  @Test
  public void testInitXClassManagerDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    XClassManager<XObjectDocument> xClassManager = DispatchXClassManager.getInstance(
        modelAccessMock, mContext, modelUtils);
    verifyDefault();
    assertEquals(CLASS_SPACE_PREFIX, xClassManager.getClassSpacePrefix());
    assertEquals(CLASS_PREFIX, xClassManager.getClassPrefix());

    assertEquals(CLASS_NAME, xClassManager.getClassName());
    assertEquals(CLASSSHEET_NAME, xClassManager.getClassSheetName());
    assertEquals(CLASSTEMPLATE_NAME, xClassManager.getClassTemplateName());

    assertEquals(DISPATCH_CLASS_SPACE, xClassManager.getClassSpace());
    assertEquals(DISPATCH_CLASS_FULLNAME, xClassManager.getClassFullName());
    assertEquals(DISPATCH_CLASSSHEET_SPACE, xClassManager.getClassSheetSpace());
    assertEquals(DISPATCH_CLASSSHEET_FULLNAME, xClassManager.getClassSheetFullName());
    assertEquals(DISPATCH_CLASSTEMPLATE_SPACE, xClassManager.getClassTemplateSpace());
    assertEquals(DISPATCH_CLASSTEMPLATE_FULLNAME, xClassManager.getClassTemplateFullName());
  }

  @Test
  public void testInitXClassManagerNoDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    XClassManager<XObjectDocument> xClassManager = NoDispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils);
    verifyDefault();
    assertEquals(CLASS_SPACE_PREFIX, xClassManager.getClassSpacePrefix());
    assertEquals(CLASS_PREFIX, xClassManager.getClassPrefix());

    assertEquals(CLASS_NAME, xClassManager.getClassName());
    assertEquals(CLASSSHEET_NAME, xClassManager.getClassSheetName());
    assertEquals(CLASSTEMPLATE_NAME, xClassManager.getClassTemplateName());

    assertEquals(NODISPATCH_CLASS_SPACE, xClassManager.getClassSpace());
    assertEquals(NODISPATCH_CLASS_FULLNAME, xClassManager.getClassFullName());
    assertEquals(NODISPATCH_CLASSSHEET_SPACE, xClassManager.getClassSheetSpace());
    assertEquals(NODISPATCH_CLASSSHEET_FULLNAME, xClassManager.getClassSheetFullName());
    assertEquals(NODISPATCH_CLASSTEMPLATE_SPACE, xClassManager.getClassTemplateSpace());
    assertEquals(NODISPATCH_CLASSTEMPLATE_FULLNAME, xClassManager.getClassTemplateFullName());
  }

  private void ptestCkeck(XClassManager<XObjectDocument> xclass) throws XWikiException {
    XWikiDocument doc = modelAccessMock.getOrCreateDocument(xclass.getClassDocRef());

    assertFalse(doc.isNew());

    BaseClass baseclass = doc.getxWikiClass();
    assertEquals(xclass.getClassFullName(), baseclass.getName());

    PropertyInterface prop = baseclass.getField(FIELD_string);
    assertNotNull(prop);

    prop = baseclass.getField(FIELD_stringlist);
    assertNotNull(prop);

    // ///

    XWikiDocument docSheet = modelAccessMock.getOrCreateDocument(xclass.getClassSheetDocRef());
    assertFalse(docSheet.isNew());

    // ///

    XWikiDocument docTemplate = modelAccessMock
        .getOrCreateDocument(xclass.getClassTemplateDocRef());
    assertFalse(docTemplate.isNew());

    BaseObject baseobject = docTemplate.getObject(xclass.getClassFullName());
    assertNotNull(baseobject);
  }

  @Test
  public void testCkeckDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    ptestCkeck(
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  @Test
  public void testCkeckNoDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    ptestCkeck(
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  private void ptestGetClassDocument(XClassManager<XObjectDocument> xClassManager)
      throws XWikiException {
    XWikiDocument doc = modelAccessMock.getOrCreateDocument(xClassManager.getClassDocRef());
    Document docFromClass = xClassManager.getClassDocument(getXContext());

    assertFalse(docFromClass.isNew());
    assertEquals(doc.getFullName(), docFromClass.getFullName());
  }

  @Test
  public void testGetClassDocumentDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    ptestGetClassDocument(
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  @Test
  public void testGetClassDocumentNoDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    ptestGetClassDocument(
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  private void ptestGetClassSheetDocument(XClassManager<XObjectDocument> xClassManager)
      throws XWikiException {
    XWikiDocument doc = modelAccessMock.getOrCreateDocument(xClassManager.getClassSheetDocRef());
    Document docFromClass = xClassManager.getClassSheetDocument(getXContext());
    verifyDefault();
    assertFalse(docFromClass.isNew());
    assertEquals(doc.getFullName(), docFromClass.getFullName());
  }

  @Test
  public void testGetClassSheetDocumentDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    ptestGetClassSheetDocument(
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  @Test
  public void testGetClassSheetDocumentNoDispatch() throws XWikiException {
    documents.clear();
    replayDefault();
    ptestGetClassSheetDocument(
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  private void ptestGetClassTemplateDocument(XClassManager<XObjectDocument> xClassManager)
      throws XWikiException {
    XWikiDocument doc = modelAccessMock.getOrCreateDocument(xClassManager.getClassTemplateDocRef());
    Document docFromClass = xClassManager.getClassTemplateDocument(getXContext());
    verifyDefault();
    assertFalse(docFromClass.isNew());
    assertEquals(doc.getFullName(), docFromClass.getFullName());
  }

  @Test
  public void testGetClassTemplateDocumentDispatch() throws Exception {
    documents.clear();
    replayDefault();
    ptestGetClassTemplateDocument(
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  @Test
  public void testGetClassTemplateDocumentNoDispatch() throws Exception {
    documents.clear();
    replayDefault();
    ptestGetClassTemplateDocument(
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils));
    verifyDefault();
  }

  @Test
  public void testGetItemDefaultNameDisptach() throws XWikiException {
    replayDefault();
    assertEquals(DEFAULT_ITEM_NAME,
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .getItemDefaultName(
                DISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME));
    verifyDefault();
  }

  @Test
  public void testGetItemDefaultNameNoDispatch() throws XWikiException {
    replayDefault();
    assertEquals(DEFAULT_ITEM_NAME,
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .getItemDefaultName(
                NODISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME));
    verifyDefault();
  }

  @Test
  public void testGetItemDocumentDefaultNameDispatch() throws XWikiException {
    replayDefault();
    assertEquals(DEFAULT_ITEMDOCUMENT_NAME,
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .getItemDocumentDefaultName(DEFAULT_ITEM_NAME, getXContext()));
    verifyDefault();
  }

  @Test
  public void testGetItemDocumentDefaultNameNoDispatch() throws XWikiException {
    replayDefault();
    assertEquals(DEFAULT_ITEMDOCUMENT_NAME,
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .getItemDocumentDefaultName(
                DEFAULT_ITEM_NAME,
                getXContext()));
    verifyDefault();
  }

  @Test
  public void testGetItemDocumentDefaultFullNameDispatch() throws XWikiException {
    replayDefault();
    assertEquals(DISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME,
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .getItemDocumentDefaultFullName(
                DEFAULT_ITEM_NAME,
                getXContext()));
    verifyDefault();
  }

  @Test
  public void testGetItemDocumentDefaultFullNameNoDispatch() throws XWikiException {
    replayDefault();
    assertEquals(NODISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME,
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .getItemDocumentDefaultFullName(
                DEFAULT_ITEM_NAME,
                getXContext()));
    verifyDefault();
  }

  @Test
  public void testIsInstanceNoDispatch() throws XWikiException {
    replayDefault();
    assertTrue(NoDispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils).isInstance(
            NoDispatchXClassManager
                .getInstance(modelAccessMock, mContext, modelUtils)
                .newXObjectDocument(getXContext())
                .getDocumentApi()));
    assertFalse(
        NoDispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .isInstance(new XWikiDocument()));
    verifyDefault();
  }

  @Test
  public void testIsInstanceDispatch() throws XWikiException {
    replayDefault();
    assertTrue(DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils).isInstance(
            DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
                .newXObjectDocument(getXContext())
                .getDocumentApi()));
    assertFalse(
        DispatchXClassManager.getInstance(modelAccessMock, mContext, modelUtils)
            .isInstance(new XWikiDocument()));
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_null() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(null, list);
    assertEquals(WHERECLAUSE_null, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_nothing() throws XWikiException {
    List<Object> list = new ArrayList<>();
    String[][] fieldDescriptors = new String[][] {};
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(fieldDescriptors, list);
    assertEquals(WHERECLAUSE_null, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_doc() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(WHERECLAUSE_PARAM_doc, list);
    assertEquals(WHERECLAUSE_doc, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_doc_multi() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(WHERECLAUSE_PARAM_doc_multi, list);
    assertEquals(WHERECLAUSE_doc_multi, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_obj() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(WHERECLAUSE_PARAM_obj, list);
    assertEquals(WHERECLAUSE_obj, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_obj_multi() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(WHERECLAUSE_PARAM_obj_multi, list);
    assertEquals(WHERECLAUSE_obj_multi, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_objdoc() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(WHERECLAUSE_PARAM_objdoc, list);
    assertEquals(WHERECLAUSE_objdoc, where);
    verifyDefault();
  }

  @Test
  public void testCreateWhereClause_objdoc_multi() throws XWikiException {
    List<Object> list = new ArrayList<>();
    replayDefault();
    String where = DispatchXClassManager
        .getInstance(modelAccessMock, mContext, modelUtils)
        .createWhereClause(WHERECLAUSE_PARAM_objdoc_multi, list);
    assertEquals(WHERECLAUSE_objdoc_multi, where);
    verifyDefault();
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

  private void mockClearName() throws Exception {
    expect(xwiki.clearName(isA(String.class), same(getXContext())))
        .andAnswer(() -> {
          return getCurrentArgument(0);
        }).anyTimes();
    expect(xwiki.clearName(isA(String.class), anyBoolean(), anyBoolean(), same(getXContext())))
        .andAnswer(() -> {
          return getCurrentArgument(0);
        }).anyTimes();
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
