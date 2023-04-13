package com.celements.model.reference;

import static com.google.common.base.Strings.*;

import java.util.Optional;

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

  public RefBuilder create() {
    return RefBuilder.create().nullable().with(context.getWikiRef());
  }

  public RefBuilder create(EntityReference... refs) {
    RefBuilder builder = create();
    if (refs != null) {
      StreamEx.of(refs).forEach(builder::with);
    }
    return builder;
  }

  public ClassReference createClassRef(String space, String name) {
    try {
      return new ClassReference(space, name);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  public ClassReference createClassRef(EntityReference ref) {
    try {
      return new ClassReference(ref);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  public ImmutableObjectReference createObjRef(DocumentReference docRef, ClassReference classRef,
      int objNb) {
    try {
      return new ImmutableObjectReference(docRef, classRef, objNb);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  public ImmutableObjectReference createObjRef(DocumentReference docRef, String space, String name,
      int objNb) {
    return createObjRef(docRef, createClassRef(space, name), objNb);
  }

  @Deprecated
  public EntityReference resolve(String name) {
    return resolve(name, (EntityReference) null);
  }

  @Deprecated
  public EntityReference resolve(String name, EntityReference baseRef) {
    try {
      return utils.resolveRef(nullToEmpty(name), baseRef);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  public WikiReference resolveWikiRef(String name) {
    return resolve(name, null, WikiReference.class);
  }

  public WikiReference resolveWikiRef(String name, EntityReference baseRef) {
    return resolve(name, baseRef, WikiReference.class);
  }

  public SpaceReference resolveSpaceRef(String name) {
    return resolve(name, null, SpaceReference.class);
  }

  public SpaceReference resolveSpaceRef(String name, EntityReference baseRef) {
    return resolve(name, baseRef, SpaceReference.class);
  }

  public DocumentReference resolveDocRef(String name) {
    return resolve(name, null, DocumentReference.class);
  }

  public DocumentReference resolveDocRef(String name, EntityReference baseRef) {
    return resolve(name, baseRef, DocumentReference.class);
  }

  public AttachmentReference resolveAttRef(String name) {
    return resolve(name, null, AttachmentReference.class);
  }

  public AttachmentReference resolveAttRef(String name, EntityReference baseRef) {
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

  public String serialize(EntityReference ref) {
    return serialize(ref, null);
  }

  public String serialize(EntityReference ref, String mode) {
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

  public EntityType getEntityType(String name) {
    name = Strings.nullToEmpty(name).toUpperCase();
    return Enums.getIfPresent(EntityType.class, name).toJavaUtil().orElse(null);
  }

}
