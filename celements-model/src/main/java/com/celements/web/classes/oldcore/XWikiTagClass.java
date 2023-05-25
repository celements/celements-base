package com.celements.web.classes.oldcore;

import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.list.DisplayType;
import com.celements.model.classes.fields.list.StaticListField;

@Singleton
@Component(XWikiTagClass.CLASS_DEF_HINT)
public class XWikiTagClass extends AbstractClassDefinition implements IOldCoreClassDef {

  public static final String CLASS_NAME = "TagClass";
  public static final String CLASS_FN = IOldCoreClassDef.CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;
  public static final ClassReference CLASS_REF = new ClassReference(CLASS_SPACE, CLASS_NAME);

  public static final ClassField<List<String>> FIELD_TAGS = new StaticListField.Builder(CLASS_REF,
      "tags").size(30).multiSelect(true).displayType(DisplayType.input).separator("|,")
          .values(Collections.<String>emptyList())/* .relationalStorage(true) */.build();

  public XWikiTagClass() {
    super(CLASS_REF);
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

}
