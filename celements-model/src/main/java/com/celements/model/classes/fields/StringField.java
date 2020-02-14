package com.celements.model.classes.fields;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;

@Immutable
public class StringField extends AbstractClassField<String> {

  private final Integer size;

  public static class Builder extends AbstractClassField.Builder<Builder, String> {

    private Integer size;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name);
    }

    @Override
    public Builder getThis() {
      return this;
    }

    public Builder size(@Nullable Integer val) {
      size = val;
      return getThis();
    }

    @Override
    public StringField build() {
      return new StringField(getThis());
    }

  }

  protected StringField(@NotNull Builder builder) {
    super(builder);
    this.size = builder.size;
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }

  public Integer getSize() {
    return size;
  }

  @Override
  protected PropertyClass getPropertyClass() {
    StringClass element = this.getStringPropertyClass();
    if (size != null) {
      element.setSize(size);
    }
    return element;
  }

  protected StringClass getStringPropertyClass() {
    return new StringClass();
  }

}
