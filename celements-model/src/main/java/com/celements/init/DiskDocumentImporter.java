package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.XWikiConstant.*;
import static com.xpn.xwiki.plugin.packaging.DocumentInfo.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.Contextualiser;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.Package;

import one.util.streamex.StreamEx;

@Service
public class DiskDocumentImporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiskDocumentImporter.class);

  public static final String DOCS_DIR = "/docs/";

  private final ServletContext servletContext;
  private final Execution execution;
  private final WikiService wikiService;
  private final ModelUtils modelUtils;
  private final IModelAccessFacade modelAccess;

  @Inject
  public DiskDocumentImporter(
      ServletContext servletContext,
      Execution execution,
      WikiService wikiService,
      ModelUtils modelUtils,
      IModelAccessFacade modelAccess) {
    this.servletContext = servletContext;
    this.execution = execution;
    this.wikiService = wikiService;
    this.modelUtils = modelUtils;
    this.modelAccess = modelAccess;
  }

  public void importAll(boolean force) {
    var paths = collectPathsByWiki();
    LOGGER.debug("importAll: {}", paths);
    StreamEx.of(MAIN_WIKI, CENTRAL_WIKI)
        .append(paths.keySet())
        .distinct()
        .forEach(wiki -> importDocs(wiki, paths.getOrDefault(wiki, List.of()), force));
  }

  public void importForWiki(WikiReference wiki, boolean force) {
    var paths = collectPathsByWiki().getOrDefault(wiki, List.of());
    LOGGER.debug("importAll: [{}: {}]", wiki, paths);
    importDocs(wiki, paths, force);
  }

  private Map<WikiReference, List<Path>> collectPathsByWiki() {
    return streamResourcePaths(DOCS_DIR)
        .filter(p -> p.endsWith("/")) // only subdirectories
        .map(Paths::get)
        .sorted()
        .mapToEntry(this::extractWiki, p -> p)
        .flatMapToKey((wikis, path) -> wikis)
        .grouping();
  }

  /**
   *
   * @param path
   *          /docs/$wiki$/...
   */
  private Stream<WikiReference> extractWiki(Path path) {
    String subdir = path.getName(1).toString();
    return ("all".equals(subdir))
        ? wikiService.streamAllWikis()
        : Stream.of(new WikiReference(subdir)).filter(wikiService::hasWiki);
  }

  private void importDocs(WikiReference wiki, List<Path> paths, boolean force) {
    new Contextualiser().withWiki(wiki).execute(() -> paths.forEach(path -> {
      LOGGER.info("import [{}] into [{}]", path, wiki);
      try {
        importDocs(path, force);
      } catch (XWikiException e) {
        LOGGER.error("Failed to import [{}]", path, e);
      }
    }));
  }

  private void importDocs(Path path, boolean force) throws XWikiException {
    var importer = new Package();
    importer.setCheckPackageDefinition(false); // we don't need a package.xml
    importer.setCheckEditRights(false); // we have the rights
    importer.setPreserveVersion(true);
    importer.setWithVersions(false);
    importer.setBackupPack(false);
    importer.Import(getEntries(path).iterator(), getXContext());
    var skipAll = true;
    for (DocumentInfo docInfo : importer.getFiles()) {
      var docRef = modelUtils.resolveRef(docInfo.getFullName(), DocumentReference.class);
      var skip = !force && modelAccess.exists(docRef);
      docInfo.setAction(skip ? ACTION_SKIP : ACTION_OVERWRITE);
      skipAll &= skip;
    }
    if (!skipAll) {
      importer.install(getXContext());
      LOGGER.debug("imported [{}]", path);
    } else {
      LOGGER.debug("skipped import [{}], nothing to do", path);
    }
  }

  private Stream<Package.Entry> getEntries(Path path) {
    return streamResourcePaths(path.toString())
        .flatMap(p -> p.endsWith("/") ? streamResourcePaths(p) : Stream.of(p))
        .filter(p -> !p.endsWith("/"))
        .map(Paths::get)
        .map(p -> new Package.Entry() {

          @Override
          public boolean isDirectory() {
            return false;
          }

          @Override
          public String getName() {
            return p.getFileName().toString();
          }

          @Override
          public InputStream getInputStream() {
            return servletContext.getResourceAsStream(p.toString());
          }
        });
  }

  private StreamEx<String> streamResourcePaths(String path) {
    var paths = servletContext.getResourcePaths(path);
    return (paths != null) ? StreamEx.of(paths) : StreamEx.empty();
  }

  private XWikiContext getXContext() {
    return execution.getContext().get(XWIKI_CONTEXT).orElseThrow();
  }

}
