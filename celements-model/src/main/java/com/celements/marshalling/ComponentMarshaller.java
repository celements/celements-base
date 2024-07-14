package com.celements.marshalling;

import static com.google.common.base.Preconditions.*;

import java.util.Optional;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.web.Utils;

@Immutable
public final class ComponentMarshaller<T> extends AbstractMarshaller<T> {

  public ComponentMarshaller(@NotNull Class<T> role) {
    super(role);
    checkArgument(role.isInterface());
  }

  @Override
  public String serialize(T val) {
    var type = val.getClass();
    return getRoleDefault(getSpringComponentValue(type)
        .or(() -> getXWikiComponentValue(type))
        .orElseThrow());
  }

  private Optional<String> getSpringComponentValue(Class<?> type) {
    return Optional
        .ofNullable(type.getAnnotation(org.springframework.stereotype.Component.class))
        .map(org.springframework.stereotype.Component::value);
  }

  private Optional<String> getXWikiComponentValue(Class<?> type) {
    return Optional
        .ofNullable(type.getAnnotation(org.xwiki.component.annotation.Component.class))
        .map(org.xwiki.component.annotation.Component::value);
  }

  @Override
  public com.google.common.base.Optional<T> resolve(String val) {
    T component = null;
    try {
      component = Utils.getComponentManager().lookup(getToken(), getRoleDefault(val));
    } catch (ComponentLookupException exc) {
      LOGGER.info("failed to resolve '{}' for '{}'", val, getToken(), exc);
    }
    return com.google.common.base.Optional.fromNullable(component);
  }

  private String getRoleDefault(String str) {
    return checkNotNull(str).trim().isEmpty() ? "default" : str;
  }

}
