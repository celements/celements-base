package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.BinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
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

  @Override
  public URL getServerURL(BinaryOperator<String> cfg) throws MalformedURLException {
    String protocol = cfg.apply("xwiki.url.protocol", "http");
    String host = cfg.apply("xwiki.url.host", "localhost");
    Integer port = Ints.tryParse(cfg.apply("xwiki.url.port", ""));
    String maindb = cfg.apply("xwiki.db", "");
    if (!host.isEmpty() && !maindb.isEmpty() && !host.startsWith(maindb + ".")) {
      host = maindb + "." + host;
    }
    return new URL(protocol, host, (port != null) ? port : -1, "/");
  }

  @Override
  public URL getServerURL(String wikiName, XWikiContext context) throws MalformedURLException {
    try {
      BaseObject serverObj = getServerObject(wikiName, context);
      if (serverObj != null) {
        String protocol = getProtocol(serverObj, context);
        String host = serverObj.getStringValue("server");
        int port = getUrlPort(context);
        return new URL(protocol, host, port, "/");
      }
    } catch (XWikiException exc) {
      LOGGER.error("getServerURL - failed for: " + wikiName, exc);
    }
    return getServerURL(context.getWiki()::Param);
  }

  private String getProtocol(BaseObject serverObj, XWikiContext context) {
    String protocol = context.getWiki().Param("xwiki.url.protocol", "");
    if (protocol.isEmpty()) {
      int iSecure = serverObj.getIntValue("secure", -1);
      boolean secure = (iSecure > 0) || ((iSecure < 0) && (context.getRequest() != null)
          && context.getRequest().isSecure());
      protocol = secure ? "https" : "http";
    }
    return protocol;
  }

  private int getUrlPort(XWikiContext context) {
    if (context.getURL() != null) {
      int port = context.getURL().getPort();
      if ((port != 80) && (port != 443)) {
        return port;
      }
    }
    return -1; // use default port
  }

  BaseObject getServerObject(String wikiName, XWikiContext context) throws XWikiException {
    DocumentReference serverDocRef = docRefResolver.resolve(XWiki.getServerWikiPage(wikiName),
        new WikiReference(context.getDatabase()));
    XWikiDocument serverDoc = context.getWiki().getDocument(serverDocRef, context);
    BaseObject serverObj = null;
    if ((context.getURL() != null) && (context.getURL().getHost() != null)) {
      serverObj = serverDoc.getXObject(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "server",
          context.getURL().getHost().replaceFirst(context.getDatabase(), wikiName));
    }
    if (serverObj == null) {
      serverObj = serverDoc.getXObject(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
    }
    return serverObj;
  }

}
