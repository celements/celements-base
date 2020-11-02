package com.celements.model.classes.fields;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.google.common.base.Strings.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.ClassDefinition;
import com.google.common.base.Strings;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.Utils;

/**
 * Subclasses are expected to be immutable
 */
public abstract class AbstractClassField<T> implements ClassField<T> {

  private final ClassReference classRef;
  private final String name;
  private final String prettyName;
  private final String validationRegExp;
  private final String validationMessage;

  public abstract static class Builder<B extends Builder<B, T>, T> {

    protected final ClassReference classRef;
    protected final String name;
    protected String prettyName;
    protected String validationRegExp;
    protected String validationMessage;

    /**
     * @deprecated instead use {@link Builder#Builder(ClassReference, String)}
     * @since 4.3
     */
    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      this(new ClassReference(classDefName), name);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      this.classRef = checkNotNull(classRef);
      this.name = checkNotNull(Strings.emptyToNull(name));
    }

    public abstract B getThis();

    public B prettyName(@Nullable String val) {
      prettyName = val;
      return getThis();
    }

    public B validationRegExp(@Nullable String val) {
      validationRegExp = val;
      return getThis();
    }

    public B validationMessage(@Nullable String val) {
      validationMessage = val;
      return getThis();
    }

    public abstract AbstractClassField<T> build();

  }

  protected AbstractClassField(@NotNull Builder<?, T> builder) {
    this.classRef = builder.classRef;
    this.name = builder.name;
    this.prettyName = Optional.ofNullable(builder.prettyName)
        .orElseGet(() -> generatePrettyName(builder));
    this.validationRegExp = builder.validationRegExp;
    this.validationMessage = firstNonNull(builder.validationMessage,
        builder.classRef.serialize() + "_" + builder.name);
  }

  protected String generatePrettyName(Builder<?, T> builder) {
    String generatedPrettyName = Stream.of(
        StringUtils.splitByCharacterTypeCamelCase(builder.name))
        .map(s -> s.replaceAll("[^A-Za-z0-9]", ""))
        .map(StringUtils::capitalize)
        .filter(not(String::isEmpty))
        .collect(Collectors.joining(" "));
    return Optional.ofNullable(emptyToNull(generatedPrettyName)).orElse(builder.name);
  }

  @Override
  public ClassDefinition getClassDef() {
    return Utils.getComponent(ClassDefinition.class, getClassReference().serialize());
  }

  @Override
  public ClassReference getClassReference() {
    return classRef;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getPrettyName() {
    return prettyName;
  }

  public String getValidationRegExp() {
    return validationRegExp;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  @Override
  public PropertyInterface getXField() {
    PropertyClass element = getPropertyClass();
    element.setName(name);
    if (prettyName != null) {
      element.setPrettyName(prettyName);
    }
    if (validationRegExp != null) {
      element.setValidationRegExp(validationRegExp);
      element.setValidationMessage(validationMessage);
    }
    return element;
  }

  protected abstract PropertyClass getPropertyClass();

  @Override
  public int hashCode() {
    return Objects.hash(classRef, name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AbstractClassField) {
      AbstractClassField<?> other = (AbstractClassField<?>) obj;
      return Objects.equals(this.classRef, other.classRef) && Objects.equals(this.name,
          other.name);
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
