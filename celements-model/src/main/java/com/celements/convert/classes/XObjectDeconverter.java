package com.celements.convert.classes;

import java.util.function.Supplier;

import org.xwiki.component.annotation.Requirement;

import com.celements.model.field.FieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.model.object.xwiki.XWikiObjectSupplier;
import com.xpn.xwiki.objects.BaseObject;

public abstract class XObjectDeconverter<T> extends AbstractClassDefConverter<T, BaseObject> {

  @Requirement(XObjectFieldAccessor.NAME)
  private FieldAccessor<BaseObject> xObjAccessor;

  @Override
  public FieldAccessor<BaseObject> getToFieldAccessor() {
    return xObjAccessor;
  }

  @Override
  protected Supplier<BaseObject> getInstanceSupplier() {
    return new XWikiObjectSupplier(getClassDef().getClassReference());
  }

}
