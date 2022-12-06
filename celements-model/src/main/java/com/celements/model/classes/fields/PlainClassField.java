package com.celements.model.classes.fields;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.ClassDefinition;
import com.xpn.xwiki.objects.PropertyInterface;

@Immutable
public class PlainClassField<T> implements ClassField<T> {

  private final ClassReference classRef;
  private final String name;
  private final Class<T> type;

  public PlainClassField(ClassReference classRef, String name, Class<T> type) {
    this.classRef = classRef;
    this.name = name;
    this.type = type;
  }

  @Override
  public ClassReference getClassReference() {
    return classRef;
  }

  @Override
  public ClassDefinition getClassDef() {
    return getClassReference().getClassDefinition()
        .orElseThrow(() -> new IllegalArgumentException("no class definition for " + classRef));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<T> getType() {
    return type;
  }

  @Override
  public PropertyInterface getXField() {
    throw new UnsupportedOperationException("PlainClassField not generateable");
  }

  @Override
  public int hashCode() {
    return Objects.hash(classRef, name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassField) {
      ClassField<?> other = (ClassField<?>) obj;
      return Objects.equals(this.getClassReference(), other.getClassReference())
          && Objects.equals(this.getName(), other.getName());
    }
    return false;
  }

  @Override
  public String serialize() {
    return getClassReference().serialize() + "." + name;
  }

  @Override
  public String toString() {
    return serialize();
  }
}
