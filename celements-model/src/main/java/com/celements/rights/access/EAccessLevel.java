package com.celements.rights.access;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.common.ValueGetter;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;

public enum EAccessLevel implements ValueGetter<String> {

  VIEW("view"), COMMENT("comment"), EDIT("edit"), DELETE("delete"), UNDELETE("undelete"), REGISTER(
      "register"), PROGRAMMING("programming"), ADMIN("admin");

  private static BiMap<String, EAccessLevel> idMap = EnumHashBiMap
      .<EAccessLevel, String>create(EAccessLevel.class)
      .inverse();

  private final String identifier;

  EAccessLevel(String identifier) {
    this.identifier = identifier;
  }

  @NotNull
  public String getIdentifier() {
    return identifier;
  }

  @NotNull
  public static Optional<EAccessLevel> getAccessLevel(@Nullable String identifier) {
    return Optional.fromNullable(idMap.get(identifier));
  }

  @Override
  public String getValue() {
    return getIdentifier();
  }
}
