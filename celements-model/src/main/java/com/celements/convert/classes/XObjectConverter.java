package com.celements.convert.classes;

import static java.util.stream.Collectors.*;

import org.xwiki.component.annotation.Requirement;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.web.classes.oldcore.XWikiObjectClass;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.objects.BaseObject;

public abstract class XObjectConverter<T> extends AbstractClassDefConverter<BaseObject, T> {

  @Requirement(XWikiObjectClass.CLASS_DEF_HINT)
  private ClassDefinition xObjClassDef;

  @Requirement(XObjectFieldAccessor.NAME)
  private FieldAccessor<BaseObject> xObjAccessor;

  @Override
  public FieldAccessor<BaseObject> getFromFieldAccessor() {
    return xObjAccessor;
  }

  @Override
  protected ImmutableList.Builder<ClassField<?>> aggregateClassFields(
      ImmutableList.Builder<ClassField<?>> iter) {
    var fields = super.aggregateClassFields(iter);
    var names = fields.build().stream().map(ClassField::getName).collect(toSet());
    xObjClassDef.getFields().stream()
        .filter(field -> !names.contains(field.getName()))
        .forEach(fields::add);
    return fields;
  }

}
