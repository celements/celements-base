package com.celements.model.reference;

import static com.google.common.base.Strings.*;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ImmutableObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Enums;
import com.google.common.base.Strings;

import one.util.streamex.StreamEx;

@Component(ReferenceScriptService.NAME)
public class ReferenceScriptService implements ScriptService {

  public static final String NAME = "reference";

  @Requirement
  private ModelUtils utils;

  @Requirement
  private ModelContext context;

  @NotNull
  public RefBuilder create() {
    return RefBuilder.create().nullable().with(context.getWikiRef());
  }

  @NotNull
  public RefBuilder create(@Nullable EntityReference... refs) {
    RefBuilder builder = create();
    if (refs != null) {
      StreamEx.of(refs).forEach(builder::with);
    }
    return builder;
  }

  @Nullable
  public ClassReference createClassRef(@Nullable String space, @Nullable String name) {
    try {
      return new ClassReference(space, name);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  @Nullable
  public ClassReference createClassRef(@Nullable EntityReference ref) {
    try {
      return new ClassReference(ref);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  @Nullable
  public ImmutableObjectReference createObjRef(@Nullable DocumentReference docRef,
      @Nullable ClassReference classRef, int objNb) {
    try {
      return new ImmutableObjectReference(docRef, classRef, objNb);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  @Nullable
  public ImmutableObjectReference createObjRef(@Nullable DocumentReference docRef,
      @Nullable String space, @Nullable String name, int objNb) {
    return createObjRef(docRef, createClassRef(space, name), objNb);
  }

  @Deprecated
  @Nullable
  public EntityReference resolve(@Nullable String name) {
    return resolve(name, (EntityReference) null);
  }

  @Deprecated
  @Nullable
  public EntityReference resolve(@Nullable String name, @Nullable EntityReference baseRef) {
    try {
      return utils.resolveRef(nullToEmpty(name), baseRef);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  @Nullable
  public WikiReference resolveWikiRef(@Nullable String name) {
    return resolve(name, null, WikiReference.class);
  }

  @Nullable
  public WikiReference resolveWikiRef(@Nullable String name, @Nullable EntityReference baseRef) {
    return resolve(name, baseRef, WikiReference.class);
  }

  @Nullable
  public SpaceReference resolveSpaceRef(@Nullable String name) {
    return resolve(name, null, SpaceReference.class);
  }

  @Nullable
  public SpaceReference resolveSpaceRef(@Nullable String name, @Nullable EntityReference baseRef) {
    return resolve(name, baseRef, SpaceReference.class);
  }

  @Nullable
  public DocumentReference resolveDocRef(@Nullable String name) {
    return resolve(name, null, DocumentReference.class);
  }

  @Nullable
  public DocumentReference resolveDocRef(@Nullable String name, @Nullable EntityReference baseRef) {
    return resolve(name, baseRef, DocumentReference.class);
  }

  @Nullable
  public AttachmentReference resolveAttRef(@Nullable String name) {
    return resolve(name, null, AttachmentReference.class);
  }

  @Nullable
  public AttachmentReference resolveAttRef(@Nullable String name,
      @Nullable EntityReference baseRef) {
    return resolve(name, baseRef, AttachmentReference.class);
  }

  private <T extends EntityReference> T resolve(String name, EntityReference baseRef,
      Class<T> token) {
    try {
      return utils.resolveRef(nullToEmpty(name), token, Optional.ofNullable(baseRef)
          .orElseGet(this::getCurrentReference));
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  private EntityReference getCurrentReference() {
    return context.getDocRef()
        .map(EntityReference.class::cast)
        .orElseGet(context::getWikiRef);
  }

  @Nullable
  public String serialize(@Nullable EntityReference ref) {
    return serialize(ref, null);
  }

  @Nullable
  public String serialize(@Nullable EntityReference ref, @Nullable String mode) {
    try {
      if (ref == null) {
        return null;
      }
      return utils.serializeRef(ref, Enums.getIfPresent(ReferenceSerializationMode.class,
          Strings.nullToEmpty(mode).toUpperCase()).or(ReferenceSerializationMode.COMPACT_WIKI));
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  @Nullable
  public EntityType getEntityType(@Nullable String name) {
    name = Strings.nullToEmpty(name).toUpperCase();
    return Enums.getIfPresent(EntityType.class, name).toJavaUtil().orElse(null);
  }

}
