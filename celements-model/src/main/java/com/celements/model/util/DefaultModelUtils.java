package com.celements.model.util;

import static com.celements.model.access.IModelAccessFacade.*;
import static com.celements.model.util.EntityTypeUtil.*;
import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

@Component
public class DefaultModelUtils implements ModelUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelUtils.class);

  @Requirement
  private Execution exec;

  @Requirement
  private ModelContext context;

  @Requirement("explicit")
  private EntityReferenceResolver<String> resolver;

  private final XWiki getXWiki() {
    return context.getXWikiContext().getWiki();
  }

  @Override
  @Deprecated
  public boolean isAbsoluteRef(EntityReference ref) {
    return References.isAbsoluteRef(ref);
  }

  @Override
  @Deprecated
  public EntityReference cloneRef(EntityReference ref) {
    return References.cloneRef(ref);
  }

  @Override
  @Deprecated
  public <T extends EntityReference> T cloneRef(EntityReference ref, Class<T> token) {
    return References.cloneRef(ref, token);
  }

  @Override
  @Deprecated
  public <T extends EntityReference> com.google.common.base.Optional<T> extractRef(
      EntityReference fromRef, Class<T> token) {
    return References.extractRef(fromRef, token);
  }

  @Override
  @Deprecated
  public <T extends EntityReference> T adjustRef(T ref, Class<T> token, EntityReference toRef) {
    return References.adjustRef(ref, token, firstNonNull(toRef, context.getWikiRef()));
  }

  @Override
  public EntityReference resolveRef(String name) {
    return resolveRef(name, (EntityReference) null);
  }

  @Override
  public EntityReference resolveRef(String name, EntityReference baseRef) {
    return identifyEntityTypeFromName(name).toJavaUtil()
        .map(type -> resolveRef(name, getClassForEntityType(type), baseRef))
        .orElseThrow(() -> new IllegalArgumentException(
            "No valid reference class found for '" + name + "'"));
  }

  @Override
  public <T extends EntityReference> T resolveRef(String name, Class<T> token) {
    return resolveRef(name, token, null);
  }

  @Override
  public <T extends EntityReference> T resolveRef(String name, Class<T> token,
      EntityReference baseRef) {
    RefBuilder builder = RefBuilder.create()
        .with(context.getWikiRef())
        .with(baseRef);
    EntityType type = getEntityTypeForClassOrThrow(token);
    if (checkNotNull(name).isEmpty()) {
      throw new IllegalArgumentException("name may not be empty");
    } else if (type == getRootEntityType()) {
      // resolver cannot handle root reference
      builder = builder.wiki(name);
    } else {
      builder = builder.with(resolver.resolve(name, type, builder.buildRelative()));
    }
    return builder.build(token);
  }

  @Override
  public WikiReference getMainWikiRef() {
    return RefBuilder.create().wiki("xwiki").build(WikiReference.class);
  }

  @Override
  public Stream<WikiReference> getAllWikis() {
    Stream<WikiReference> stream;
    try {
      String prefix = "XWikiServer";
      String xwql = "select distinct doc.name from XWikiDocument as doc, BaseObject as obj "
          + "where doc.space = 'XWiki' and doc.name <> 'XWikiServerClassTemplate' "
          + "and obj.name=doc.fullName and obj.className='XWiki.XWikiServerClass'";
      stream = getQueryManager().createQuery(xwql, Query.XWQL)
          .setWiki(getMainWikiRef().getName())
          .<String>execute().stream()
          .filter(name -> name.startsWith(prefix) && (name.length() > prefix.length()))
          .map(name -> name.substring(prefix.length()).toLowerCase())
          .map(name -> RefBuilder.create().wiki(name).build(WikiReference.class));
    } catch (QueryException exc) {
      LOGGER.error("getAllWikis - failed", exc);
      stream = Stream.of(context.getWikiRef());
    }
    return Stream.concat(Stream.of(getMainWikiRef()), stream).distinct();
  }

  @Override
  public Stream<SpaceReference> getAllSpaces(WikiReference wikiRef) {
    RefBuilder builder = RefBuilder.from(wikiRef);
    try {
      return getQueryManager().getNamedQuery("getSpaces")
          .setWiki(wikiRef.getName())
          .<String>execute().stream()
          .map(name -> builder.space(name).build(SpaceReference.class))
          .distinct();
    } catch (QueryException exc) {
      LOGGER.error("getAllSpaces - failed for [{}]", wikiRef, exc);
      return Stream.of();
    }
  }

  @Override
  public Stream<DocumentReference> getAllDocsForSpace(SpaceReference spaceRef) {
    RefBuilder builder = RefBuilder.from(spaceRef);
    try {
      return getQueryManager().getNamedQuery("getSpaceDocsName")
          .setWiki(spaceRef.extractReference(EntityType.WIKI).getName())
          .bindValue("space", spaceRef.getName())
          .<String>execute().stream()
          .map(name -> builder.doc(name).build(DocumentReference.class));
    } catch (QueryException exc) {
      LOGGER.error("getAllDocsForSpace - failed for [{}]", spaceRef, exc);
      return Stream.of();
    }
  }

  @Override
  public String getDatabaseName(WikiReference wikiRef) {
    checkNotNull(wikiRef);
    String database = "";
    if (getMainWikiRef().equals(wikiRef)) {
      database = getXWiki().Param("xwiki.db", "").trim();
    }
    if (database.isEmpty()) {
      database = wikiRef.getName().replace('-', '_');
    }
    return getXWiki().Param("xwiki.db.prefix", "") + database.replace('-', '_');
  }

  @Override
  public String serializeRef(EntityReference ref, ReferenceSerializationMode mode) {
    checkNotNull(ref);
    // strip child from immutable references by creating relative reference
    // for reason see DefaultStringEntityReferenceSerializer#L29
    ref = new RefBuilder().with(ref).buildRelative();
    return getSerializerForMode(mode).serialize(ref);
  }

  @SuppressWarnings("unchecked")
  private EntityReferenceSerializer<String> getSerializerForMode(ReferenceSerializationMode mode) {
    String hint;
    switch (mode) {
      case GLOBAL:
        hint = "default";
        break;
      case LOCAL:
        hint = "local";
        break;
      case COMPACT:
        hint = "compact";
        break;
      case COMPACT_WIKI:
        hint = "compactwiki";
        break;
      default:
        throw new IllegalArgumentException(String.valueOf(mode));
    }
    return Utils.getComponent(EntityReferenceSerializer.class, hint);
  }

  @Override
  public String serializeRef(EntityReference ref) {
    return serializeRef(ref, ReferenceSerializationMode.GLOBAL);
  }

  @Override
  public String serializeRefLocal(EntityReference ref) {
    return serializeRef(ref, ReferenceSerializationMode.LOCAL);
  }

  @Override
  public String normalizeLang(final String lang) {
    String ret = nullToEmpty(lang).trim();
    if (ret.equals("default")) {
      ret = DEFAULT_LANG;
    } else {
      ret = Util.normalizeLanguage(ret);
      if ("default".equals(ret)) {
        throw new IllegalArgumentException("Invalid language: " + lang);
      }
    }
    return ret;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T computeExecPropIfAbsent(String key, Supplier<T> defaultGetter) {
    T ret = (T) exec.getContext().getProperty(key);
    if (ret == null) {
      exec.getContext().setProperty(key, ret = defaultGetter.get());
    }
    return ret;
  }

  /**
   * load lazy since it may cause an NPE if a unit tests requires ModelUtils
   */
  private QueryManager getQueryManager() {
    return Utils.getComponent(QueryManager.class);
  }

}
