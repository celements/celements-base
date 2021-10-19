package com.celements.model.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ImmutableObjectReference;
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

  public EntityReference resolve(String name) {
    return resolve(name, null);
  }

  public EntityReference resolve(String name, EntityReference baseRef) {
    try {
      return utils.resolveRef(name, baseRef);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  public String serialize(EntityReference ref) {
    return serialize(ref, null);
  }

  public String serialize(EntityReference ref, String mode) {
    try {
      return utils.serializeRef(ref, Enums.getIfPresent(ReferenceSerializationMode.class,
          Strings.nullToEmpty(mode).toUpperCase()).or(ReferenceSerializationMode.COMPACT_WIKI));
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

}
