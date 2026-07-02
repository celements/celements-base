package com.celements.rights.access;

import static java.util.Arrays.*;
import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.common.ValueGetter;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public enum EAccessLevel implements ValueGetter<String> {

  VIEW("view"), COMMENT("comment"), EDIT("edit"), DELETE("delete"), UNDELETE("undelete"), REGISTER(
      "register"), PROGRAMMING("programming"), ADMIN("admin");

  private static final BiMap<String, EAccessLevel> idMap = ImmutableBiMap.copyOf(
      stream(values()).collect(toMap(EAccessLevel::getIdentifier, identity())));

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
