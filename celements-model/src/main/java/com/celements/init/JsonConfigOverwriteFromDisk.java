package com.celements.init;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.springframework.stereotype.Service;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.util.ModelUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/** Loads operator-owned JSON resources; never retains mutable wiki policy in the singleton. */
@Service
public class JsonConfigOverwriteFromDisk {

  private static final JsonFactory JSON = new JsonFactory();
  private final ServletContext servletContext;
  private final ModelUtils modelUtils;

  @Inject
  public JsonConfigOverwriteFromDisk(ServletContext servletContext, ModelUtils modelUtils) {
    this.servletContext = servletContext;
    this.modelUtils = modelUtils;
  }

  public Predicate<DocumentReference> forWiki(WikiReference wiki) {
    var path = DiskDocumentImporter.DOCS_DIR + wiki.getName() + ".json";
    try {
      if (servletContext.getResource(path) == null) {
        return document -> false;
      }
      try (var stream = openResource(path); var parser = JSON.createParser(stream)) {
        var snapshot = readSnapshot(parser, wiki, path);
        return snapshot::contains;
      }
    } catch (IOException | SecurityException exception) {
      // Do not attach parser exceptions: their messages can contain configuration contents.
      throw invalid(path, "resource cannot be read as valid JSON");
    }
  }

  private InputStream openResource(String path) {
    var stream = servletContext.getResourceAsStream(path);
    if (stream == null) {
      throw invalid(path, "resource cannot be read");
    }
    return stream;
  }

  private Set<DocumentReference> readSnapshot(JsonParser parser, WikiReference wiki, String path)
      throws IOException {
    if (parser.nextToken() != JsonToken.START_ARRAY) {
      throw invalid(path, "expected an array of document FullNames");
    }
    Set<DocumentReference> selected = new HashSet<>();
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      if (parser.currentToken() != JsonToken.VALUE_STRING) {
        throw invalid(path, "expected a nonblank document FullName string");
      }
      selected.add(resolve(parser.getText(), wiki, path));
    }
    if (parser.nextToken() != null) {
      throw invalid(path, "unexpected content after array");
    }
    return Set.copyOf(selected);
  }

  private DocumentReference resolve(String name, WikiReference wiki, String path) {
    if (name.isBlank() || !name.equals(name.strip()) || name.startsWith("/")
        || name.chars().anyMatch(c -> "*?[](){}|^$".indexOf(c) >= 0)) {
      throw invalid(path, "expected a literal wiki-relative document FullName");
    }
    try {
      var reference = modelUtils.resolveRef(name, DocumentReference.class, wiki);
      if (!wiki.equals(reference.getWikiReference())
          || !name.equals(modelUtils.serializeRefLocal(reference))) {
        throw invalid(path, "expected a canonical wiki-relative document FullName");
      }
      return reference;
    } catch (IllegalArgumentException exception) {
      throw invalid(path, "invalid wiki-relative document FullName");
    }
  }

  private IllegalArgumentException invalid(String path, String reason) {
    return new IllegalArgumentException("Invalid disk overwrite configuration [" + path + "]: "
        + reason);
  }
}
