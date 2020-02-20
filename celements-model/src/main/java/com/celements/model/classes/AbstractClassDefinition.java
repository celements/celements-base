package com.celements.model.classes;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class AbstractClassDefinition implements ClassDefinition {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassDefinition.class);

  @Requirement
  protected ModelContext context;

  @Requirement
  protected ModelUtils modelUtils;

  @Requirement
  protected ConfigurationSource configSrc;

  private ClassReference classRef;
  private volatile Map<String, ClassField<?>> fields;

  @Override
  public ClassReference getClassReference() {
    if (classRef == null) {
      classRef = new ClassReference(getClassSpaceName(), getClassDocName());
    }
    return classRef;
  }

  @Deprecated
  @Override
  public DocumentReference getClassRef() {
    return getDocRef();
  }

  @Override
  public DocumentReference getDocRef() {
    return getClassReference().getDocRef();
  }

  @Deprecated
  @Override
  public DocumentReference getClassRef(WikiReference wikiRef) {
    return getDocRef(wikiRef);
  }

  @Override
  public DocumentReference getDocRef(WikiReference wikiRef) {
    return getClassReference().getDocRef(wikiRef);
  }

  /**
   * @return space name for this class definition.
   */
  protected @NotNull String getClassSpaceName() {
    return getClassReference().getParent().getName();
  }

  /**
   * @return doc name for this class definition.
   */
  protected @NotNull String getClassDocName() {
    return getClassReference().getName();
  }

  @Override
  public boolean isBlacklisted() {
    boolean ret = false;
    Object prop = configSrc.getProperty(CFG_SRC_KEY);
    if (prop instanceof List) {
      ret = ((List<?>) prop).contains(getName());
    }
    LOGGER.debug("isBlacklisted: '{}' for '{}'", ret, getName());
    return ret;
  }

  @Override
  public boolean isValidObjectClass() {
    return true;
  }

  private synchronized void loadFields() {
    if (fields == null) {
      Map<String, ClassField<?>> map = new LinkedHashMap<>();
      for (Field declField : this.getClass().getDeclaredFields()) {
        try {
          if (ClassField.class.isAssignableFrom(declField.getType())) {
            ClassField<?> field = (ClassField<?>) declField.get(this);
            map.put(field.getName(), field);
          }
        } catch (IllegalAccessException | IllegalArgumentException exc) {
          LOGGER.error("failed to get field '{}", declField, exc);
        }
      }
      fields = ImmutableMap.copyOf(map);
    }
  }

  private Map<String, ClassField<?>> getFieldMap() {
    if (fields == null) {
      loadFields();
    }
    return fields;
  }

  @Override
  public List<ClassField<?>> getFields() {
    return ImmutableList.copyOf(getFieldMap().values());
  }

  @Override
  public Optional<ClassField<?>> getField(@NotNull String name) {
    return Optional.<ClassField<?>>fromNullable(getFieldMap().get(name));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<ClassField<T>> getField(String name, Class<T> token) {
    Optional<ClassField<?>> field = getField(name);
    if (field.isPresent() && token.isAssignableFrom(field.get().getType())) {
      return Optional.of((ClassField<T>) field.get());
    }
    return Optional.absent();
  }

  @Override
  public int hashCode() {
    return getClassReference().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassDefinition) {
      obj = ((ClassDefinition) obj).getClassReference();
    }
    return Objects.equals(this.getClassReference(), obj);
  }

  @Override
  public String serialize() {
    return getClassReference().serialize();
  }

  @Override
  public String toString() {
    return serialize();
  }

}
