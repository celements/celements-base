package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import com.google.common.primitives.Ints;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class ServerUrlUtils implements ServerUrlUtilsRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerUrlUtils.class);

  @Requirement
  private DocumentReferenceResolver<String> docRefResolver;

  @Requirement(XWikiConfigSource.NAME)
  private ConfigurationSource xwikiCfg;

  @Requirement
  private Execution execution;

  @Override
  public URL getServerURL() throws MalformedURLException {
    String protocol = xwikiCfg.getProperty("xwiki.url.protocol", "http");
    String host = xwikiCfg.getProperty("xwiki.url.host", "localhost");
    Integer port = Ints.tryParse(xwikiCfg.getProperty("xwiki.url.port", ""));
    String maindb = xwikiCfg.getProperty("xwiki.db", "");
    if (!host.isEmpty() && !maindb.isEmpty() && !host.startsWith(maindb + ".")) {
      host = maindb + "." + host;
    }
    URL url = new URL(protocol, host, (port != null) ? port : -1, "/");
    LOGGER.debug("getServerURL - {}", url);
    return url;
  }

  @Override
  public Optional<URL> getServerURL(WikiReference wikiRef) throws MalformedURLException {
    try {
      BaseObject serverObj = getServerObject(wikiRef.getName());
      if (serverObj != null) {
        String protocol = getProtocol(serverObj);
        String host = serverObj.getStringValue("server");
        int port = getUrlPort();
        URL url = new URL(protocol, host, port, "/");
        LOGGER.debug("getServerURL - {}", url);
        return Optional.of(url);
      }
    } catch (XWikiException exc) {
      LOGGER.error("getServerURL - failed for: {}", wikiRef, exc);
    }
    return Optional.empty();
  }

  private String getProtocol(BaseObject serverObj) {
    String protocol = xwikiCfg.getProperty("xwiki.url.protocol", "");
    if (protocol.isEmpty()) {
      int iSecure = serverObj.getIntValue("secure", -1);
      boolean secure = (iSecure > 0) || ((iSecure < 0) && (getContext().getRequest() != null)
          && getContext().getRequest().isSecure());
      protocol = secure ? "https" : "http";
    }
    return protocol;
  }

  private int getUrlPort() {
    if (getContext().getURL() != null) {
      int port = getContext().getURL().getPort();
      if ((port != 80) && (port != 443)) {
        return port;
      }
    }
    return -1; // use default port
  }

  BaseObject getServerObject(String wikiName) throws XWikiException {
    DocumentReference serverDocRef = docRefResolver.resolve(XWiki.getServerWikiPage(wikiName),
        new WikiReference(getContext().getDatabase()));
    XWikiDocument serverDoc = getContext().getWiki().getDocument(serverDocRef, getContext());
    BaseObject serverObj = null;
    if ((getContext().getURL() != null) && (getContext().getURL().getHost() != null)) {
      serverObj = serverDoc.getXObject(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "server",
          getContext().getURL().getHost().replaceFirst(getContext().getDatabase(), wikiName));
    }
    if (serverObj == null) {
      serverObj = serverDoc.getXObject(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
    }
    return serverObj;
  }

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
  }

}
