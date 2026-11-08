package com.celements.convert.classes;

import static java.util.stream.Collectors.*;

import org.xwiki.component.annotation.Requirement;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.web.classes.oldcore.XWikiObjectClass;
import com.google.common.collect.ImmutableList;

public abstract class AbstractObjectConverter<O, T> extends AbstractClassDefConverter<O, T> {

  @Requirement(XWikiObjectClass.CLASS_DEF_HINT)
  private ClassDefinition xObjClassDef;

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
