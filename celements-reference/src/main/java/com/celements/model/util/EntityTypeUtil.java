package com.celements.model.util;

import static com.google.common.base.Preconditions.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

import one.util.streamex.StreamEx;

public class EntityTypeUtil {

  private static final BiMap<Class<? extends EntityReference>, EntityType> ENTITY_TYPE_MAP = ImmutableBiMap
      .<Class<? extends EntityReference>, EntityType>builder()
      .put(WikiReference.class, EntityType.WIKI)
      .put(SpaceReference.class, EntityType.SPACE)
      .put(DocumentReference.class, EntityType.DOCUMENT)
      .put(AttachmentReference.class, EntityType.ATTACHMENT)
      .put(ObjectReference.class, EntityType.OBJECT)
      .put(ObjectPropertyReference.class, EntityType.OBJECT_PROPERTY)
      .build();

  public static final String REGEX_WORD = "[a-zA-Z0-9_-]+";
  public static final String REGEX_WIKINAME = "[a-zA-Z0-9]+";
  public static final String REGEX_SPACE = "(" + REGEX_WIKINAME + "\\:)?" + REGEX_WORD;
  public static final String REGEX_DOC = REGEX_SPACE + "\\." + REGEX_WORD;
  public static final String REGEX_ATT = REGEX_DOC + "\\@" + ".+";
  public static final Pattern PATTERN_WORD = Pattern.compile(REGEX_WORD);
  public static final Pattern PATTERN_WIKI = Pattern.compile(REGEX_WIKINAME);
  public static final Pattern PATTERN_SPACE = Pattern.compile(REGEX_SPACE);
  public static final Pattern PATTERN_DOC = Pattern.compile(REGEX_DOC);
  public static final Pattern PATTERN_ATT = Pattern.compile(REGEX_ATT);
  private static final Map<EntityType, Pattern> REGEX_MAP = ImmutableMap
      .<EntityType, Pattern>builder()
      .put(EntityType.WIKI, PATTERN_WIKI)
      .put(EntityType.SPACE, PATTERN_SPACE)
      .put(EntityType.DOCUMENT, PATTERN_DOC)
      .put(EntityType.ATTACHMENT, PATTERN_ATT)
      .build();

  private EntityTypeUtil() {}

  @NotNull
  public static Optional<EntityType> determineEntityTypeForClass(
      @NotNull Class<? extends EntityReference> token) {
    checkNotNull(token);
    return Optional.ofNullable(ENTITY_TYPE_MAP.get(token));
  }

  /**
   * @deprecated since 6.0 instead use {@link #determineEntityTypeForClass(Class)}
   */
  @Deprecated
  @NotNull
  public static com.google.common.base.Optional<EntityType> getEntityTypeForClass(
      @NotNull Class<? extends EntityReference> token) {
    return com.google.common.base.Optional.fromJavaUtil(determineEntityTypeForClass(token));
  }

  @NotNull
  public static EntityType getEntityTypeForClassOrThrow(
      @NotNull Class<? extends EntityReference> token) {
    return determineEntityTypeForClass(token)
        .orElseThrow(() -> new IllegalArgumentException("No entity type for class: " + token));
  }

  @NotNull
  public static Class<? extends EntityReference> getClassForEntityType(@NotNull EntityType type) {
    Class<? extends EntityReference> token = ENTITY_TYPE_MAP.inverse().get(checkNotNull(type));
    if (token != null) {
      return token;
    } else {
      throw new IllegalStateException("No class for entity type: " + type);
    }
  }

  @NotNull
  public static EntityType getRootEntityType() {
    return EntityType.values()[0];
  }

  @NotNull
  public static EntityType getLastEntityType() {
    return EntityType.values()[EntityType.values().length - 1];
  }

  /**
   * @return the class for the root entity type
   */
  @NotNull
  public static Class<? extends EntityReference> getRootClass() {
    return checkNotNull(ENTITY_TYPE_MAP.inverse().get(getRootEntityType()));
  }

  @NotNull
  public static Optional<EntityType> determineEntityTypeFromName(@NotNull String name) {
    return StreamEx.ofKeys(REGEX_MAP)
        .findFirst(type -> isMatchingEntityType(name, type));
  }

  /**
   * @deprecated since 6.0 instead use {@link #determineEntityTypeFromName(String)}
   */
  @Deprecated
  @NotNull
  public static com.google.common.base.Optional<EntityType> identifyEntityTypeFromName(
      @NotNull String name) {
    return com.google.common.base.Optional.fromJavaUtil(determineEntityTypeFromName(name));
  }

  public static boolean isMatchingEntityType(@NotNull String name, @NotNull EntityType type) {
    Pattern pattern = REGEX_MAP.get(type);
    return (pattern != null) && pattern.matcher(name).matches();
  }

  @NotNull
  public static Iterator<EntityType> createIterator() {
    return createIteratorAt(null);
  }

  @NotNull
  public static Stream<EntityType> stream() {
    return streamAt(null);
  }

  @NotNull
  public static Iterator<EntityType> createIteratorFrom(@Nullable final EntityType startType) {
    return streamFrom(startType).iterator();
  }

  @NotNull
  public static Stream<EntityType> streamFrom(@Nullable final EntityType startType) {
    return streamAt(startType).skip((startType != null) ? 1 : 0);
  }

  @NotNull
  public static Iterator<EntityType> createIteratorAt(@Nullable final EntityType startType) {
    return streamAt(startType).iterator();
  }

  @NotNull
  public static Stream<EntityType> streamAt(@Nullable final EntityType startType) {
    int startOrdinal = MoreObjects.firstNonNull(startType, getLastEntityType()).ordinal();
    return StreamEx.iterate(startOrdinal,
        ordinal -> ordinal >= 0, // hasNext
        // skip type attachment for type OBJECT or OBJECT_PROPERTY
        ordinal -> ordinal - ((ordinal == EntityType.OBJECT.ordinal()) ? 2 : 1)) // next
        .map(ordinal -> EntityType.values()[ordinal]);
  }

}
