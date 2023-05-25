package com.celements.web.classes.oldcore;

import static com.celements.model.util.ReferenceSerializationMode.*;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.ref.DocumentReferenceField;

@Singleton
@Component(XWikiGroupsClass.CLASS_DEF_HINT)
public class XWikiGroupsClass extends AbstractClassDefinition implements IOldCoreClassDef {

  public static final String CLASS_NAME = "XWikiGroups";
  public static final String CLASS_FN = IOldCoreClassDef.CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;
  public static final ClassReference CLASS_REF = new ClassReference(CLASS_SPACE, CLASS_NAME);

  public static final ClassField<DocumentReference> FIELD_MEMBER = new DocumentReferenceField.Builder(
      CLASS_REF, "member").prettyName("Member").refSerializationMode(COMPACT_WIKI).size(30).build();

  public XWikiGroupsClass() {
    super(CLASS_REF);
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

}
