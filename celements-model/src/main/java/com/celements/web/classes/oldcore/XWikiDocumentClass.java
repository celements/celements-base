package com.celements.web.classes.oldcore;

import java.util.Date;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.ref.DocumentReferenceField;

@Singleton
@Component(XWikiDocumentClass.CLASS_DEF_HINT)
public class XWikiDocumentClass extends PseudoClassDefinition {

  public static final String CLASS_NAME = "XWikiDocumentClass";
  public static final String CLASS_FN = PseudoClassDefinition.CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;
  public static final ClassReference CLASS_REF = new ClassReference(CLASS_SPACE, CLASS_NAME);

  public static final ClassField<DocumentReference> FIELD_DOC_REF = new DocumentReferenceField.Builder(
      CLASS_REF, "documentReference").build();

  public static final ClassField<DocumentReference> FIELD_PARENT_REF = new DocumentReferenceField.Builder(
      CLASS_REF, "parentReference").build();

  public static final ClassField<String> FIELD_LANGUAGE = new StringField.Builder(CLASS_REF,
      "language").build();

  public static final ClassField<String> FIELD_DEFAULT_LANGUAGE = new StringField.Builder(CLASS_REF,
      "defaultLanguage").build();

  public static final ClassField<Boolean> FIELD_TRANSLATION = new BooleanField.Builder(CLASS_REF,
      "translation").build();

  public static final ClassField<String> FIELD_CREATOR = new StringField.Builder(CLASS_REF,
      "creator").build();

  public static final ClassField<String> FIELD_AUTHOR = new StringField.Builder(CLASS_REF,
      "author").build();

  public static final ClassField<String> FIELD_CONTENT_AUTHOR = new StringField.Builder(CLASS_REF,
      "contentAuthor").build();

  public static final ClassField<Date> FIELD_CREATION_DATE = new DateField.Builder(CLASS_REF,
      "creationDate").build();

  public static final ClassField<Date> FIELD_UPDATE_DATE = new DateField.Builder(CLASS_REF,
      "updateDate").build();

  public static final ClassField<Date> FIELD_CONTENT_UPDATE_DATE = new DateField.Builder(CLASS_REF,
      "contentUpdateDate").build();

  public static final ClassField<String> FIELD_TITLE = new StringField.Builder(CLASS_REF,
      "title").build();

  public static final ClassField<String> FIELD_CONTENT = new StringField.Builder(CLASS_REF,
      "content").build();

  public XWikiDocumentClass() {
    super(CLASS_REF);
  }

}
