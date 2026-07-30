package com.celements.model.util;

import static com.celements.model.access.IModelAccessFacade.*;
import static com.celements.model.util.EntityTypeUtil.*;
import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.celements.model.reference.ReferenceProvider;
import com.google.common.base.Suppliers;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.util.Util;

@Component
public class DefaultModelUtils implements ModelUtils {

  private final Execution exec;
  private final ModelContext context;
  private final EntityReferenceResolver<String> resolver;
  private final EntityReferenceSerializer<String> defaultSerializer;
  private final EntityReferenceSerializer<String> localSerializer;
  private final EntityReferenceSerializer<String> compactSerializer;
  private final EntityReferenceSerializer<String> compactWikiSerializer;
  private final ReferenceProvider refProvider;
  private final XWikiConfigSource xwikiCfg;
  private final Supplier<WikiReference> mainWikiRef;

  @Inject
  public DefaultModelUtils(
      @Named("explicit") EntityReferenceResolver<String> resolver,
      @Named("default") EntityReferenceSerializer<String> defaultSerializer,
      @Named("local") EntityReferenceSerializer<String> localSerializer,
      @Named("compact") EntityReferenceSerializer<String> compactSerializer,
      @Named("compactwiki") EntityReferenceSerializer<String> compactWikiSerializer,
      ReferenceProvider refProvider,
      XWikiConfigSource xwikiCfg,
      ModelContext context,
      Execution exec) {
    this.resolver = resolver;
    this.defaultSerializer = defaultSerializer;
    this.localSerializer = localSerializer;
    this.compactSerializer = compactSerializer;
    this.compactWikiSerializer = compactWikiSerializer;
    this.refProvider = refProvider;
    this.xwikiCfg = xwikiCfg;
    this.context = context;
    this.exec = exec;
    this.mainWikiRef = Suppliers
        .memoize(() -> RefBuilder.create()
            .wiki(xwikiCfg.getMainWikiName())
            .buildOpt(WikiReference.class)
            .orElse(XWikiConstant.MAIN_WIKI));
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
  @NotNull
  public <T extends EntityReference> T resolveRef(@NotNull String name, @NotNull Class<T> token,
      @Nullable EntityReference baseRef) {
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
    return mainWikiRef.get();
  }

  @Override
  public boolean isMainWiki(@Nullable WikiReference wikiRef) {
    return XWikiConstant.MAIN_WIKI.equals(wikiRef)
        || getMainWikiRef().equals(wikiRef);
  }

  @Override
  @Nullable
  public WikiReference normalizeWikiRef(@Nullable WikiReference wikiRef) {
    if (isMainWiki(wikiRef)) {
      return getMainWikiRef();
    }
    return wikiRef;
  }

  @Override
  public Stream<WikiReference> getAllWikis() {
    return refProvider.getAllWikis().stream();
  }

  @Override
  public Stream<SpaceReference> getAllSpaces(WikiReference wikiRef) {
    return refProvider.getAllSpaces(wikiRef).stream();
  }

  @Override
  public Stream<DocumentReference> getAllDocsForSpace(SpaceReference spaceRef) {
    return refProvider.getAllDocsForSpace(spaceRef).stream();
  }

  @Override
  public String getDatabaseName(WikiReference wikiRef) {
    WikiReference normWikiRef = normalizeWikiRef(wikiRef);
    checkNotNull(normWikiRef);
    return xwikiCfg.getProperty("xwiki.db.prefix", "")
        + normWikiRef.getName().replace('-', '_');
  }

  @Override
  public String serializeRef(EntityReference ref, ReferenceSerializationMode mode) {
    checkNotNull(ref);
    return getSerializerForMode(mode).serialize(ref);
  }

  private EntityReferenceSerializer<String> getSerializerForMode(ReferenceSerializationMode mode) {
    return switch (mode) {
      case GLOBAL -> defaultSerializer;
      case LOCAL -> localSerializer;
      case COMPACT -> compactSerializer;
      case COMPACT_WIKI -> compactWikiSerializer;
    };
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
  @Deprecated
  public <T> T computeExecPropIfAbsent(String key, Supplier<T> defaultGetter) {
    return exec.getContext().computeIfAbsent(key, defaultGetter);
  }

}
