package com.celements.model.classes.fields.list;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.marshalling.ReferenceMarshaller;
import com.xpn.xwiki.objects.classes.GroupsClass;

@Immutable
public final class GroupListField extends ListField<DocumentReference> {

  private final Boolean usesList;

  public static class Builder extends ListField.Builder<Builder, DocumentReference> {

    private Boolean usesList;

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name, new ReferenceMarshaller<>(DocumentReference.class));
      separator(",");
    }

    @Override
    public Builder getThis() {
      return this;
    }

    public Builder usesList(@NotNull Boolean usesList) {
      this.usesList = usesList;
      return getThis();
    }

    @Override
    public GroupListField build() {
      return new GroupListField(getThis());
    }

  }

  protected GroupListField(@NotNull Builder builder) {
    super(builder);
    this.usesList = builder.usesList;
  }

  public Boolean getUsesList() {
    return usesList;
  }

  @Override
  protected GroupsClass getListClass() {
    GroupsClass element = new GroupsClass();
    if (usesList != null) {
      element.setUsesList(usesList);
    }
    return element;
  }

}
