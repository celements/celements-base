package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiDocumentClass.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.classes.TestClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.xpn.xwiki.doc.XWikiDocument;

public class XDocumentFieldAccessorTest extends AbstractComponentTest {

  private XDocumentFieldAccessor accessor;

  private DocumentReference documentReference = new DocumentReference("wiki", "space", "doc");
  private DocumentReference parentReference = new DocumentReference("wiki", "space", "parent");
  private String language = "de";
  private String defaultLanguage = "en";
  private boolean translation = true;
  private String creator = "creator";
  private String author = "author";
  private String contentAuthor = "contentAuthor";
  private Date creationDate = new Date(0);
  private Date updateDate = new Date(1);
  private Date contentUpdateDate = new Date(2);
  private String title = "title";
  private String content = "content";

  @Before
  public void prepareTest() throws Exception {
    accessor = getBeanFactory().getBean(XDocumentFieldAccessor.class);
  }

  @Test
  public void test_getName() {
    assertEquals(XDocumentFieldAccessor.NAME, accessor.getName());
  }

  @Test
  public void test_getSetValue() {
    XWikiDocument doc = new XWikiDocument(documentReference);
    assertGetSet(doc, FIELD_PARENT_REF, parentReference, false);
    assertGetSet(doc, FIELD_LANGUAGE, language, false);
    assertGetSet(doc, FIELD_DEFAULT_LANGUAGE, defaultLanguage, false);
    assertGetSet(doc, FIELD_TRANSLATION, translation, true);
    assertGetSet(doc, FIELD_CREATOR, creator, false);
    assertGetSet(doc, FIELD_AUTHOR, author, false);
    assertGetSet(doc, FIELD_CONTENT_AUTHOR, contentAuthor, false);
    assertGetSet(doc, FIELD_CREATION_DATE, creationDate, true);
    assertGetSet(doc, FIELD_UPDATE_DATE, updateDate, true);
    assertGetSet(doc, FIELD_CONTENT_UPDATE_DATE, contentUpdateDate, true);
    assertGetSet(doc, FIELD_TITLE, title, false);
    assertGetSet(doc, FIELD_CONTENT, content, false);
  }

  private <V> void assertGetSet(XWikiDocument doc, ClassField<V> field, V value,
      boolean hasDefaultValue) {
    assertEquals(hasDefaultValue, accessor.getValue(doc, field).isPresent());
    assertTrue(accessor.setValue(doc, field, value));
    assertEquals(value, accessor.getValue(doc, field).orNull());
    assertFalse(accessor.setValue(doc, field, value));
  }

  @Test
  public void test_getSetValue_documentReference() {
    final XWikiDocument doc = new XWikiDocument(documentReference);
    assertEquals(documentReference, accessor.getValue(doc, FIELD_DOC_REF).orNull());
    assertFalse(accessor.setValue(doc, FIELD_DOC_REF, documentReference));
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.setValue(doc, FIELD_DOC_REF, new DocumentReference("wiki", "space", "other"));
      }
    }.evaluate();
  }

  @Test
  public void test_FieldAccessException_invalidField() {
    final ClassField<String> field = TestClassDefinition.FIELD_MY_STRING;
    final XWikiDocument doc = new XWikiDocument(documentReference);
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.getValue(doc, field);
      }
    }.evaluate();
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.setValue(doc, field, null);
      }
    }.evaluate();
  }

}
