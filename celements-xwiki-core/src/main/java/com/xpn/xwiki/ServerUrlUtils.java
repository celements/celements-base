package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class ServerUrlUtils implements ServerUrlUtilsRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerUrlUtils.class);

  @Requirement
  private DocumentReferenceResolver<String> docRefResolver;

  @Override
  public URL getServerURL(String wikiName, XWikiContext context) throws MalformedURLException {
    URL serverurl = null;
    // In virtual wiki path mode the server is the standard one
    if ("0".equals(context.getWiki().Param("xwiki.virtual.usepath", "0"))) {
      try {
        BaseObject serverobject = getServerObject(wikiName, context);
        if (serverobject != null) {
          String protocol = context.getWiki().Param("xwiki.url.protocol", null);
          if (protocol == null) {
            int iSecure = serverobject.getIntValue("secure", -1);
            // Check the request object if the "secure" property is undefined.
            boolean secure = (iSecure > 0) || ((iSecure < 0) && (context.getRequest() != null)
                && context.getRequest().isSecure());
            protocol = secure ? "https" : "http";
          }
          String host = serverobject.getStringValue("server");
          int port = getUrlPort(context);
          serverurl = new URL(protocol, host, port, "/");
        }
      } catch (XWikiException | MalformedURLException exc) {
        LOGGER.error("getServerURL - failed for: " + wikiName, exc);
      }
    }
    return serverurl;
  }

  private int getUrlPort(XWikiContext context) {
    int port = -1;
    if ((context != null) && (context.getURL() != null)) {
      int thePort = context.getURL().getPort();
      if ((thePort != 80) && (thePort != 443)) {
        port = thePort;
      }
    }
    return port;
  }

  BaseObject getServerObject(String wikiName, XWikiContext context) throws XWikiException {
    XWiki xwiki = context.getWiki();
    XWikiDocument doc = xwiki.getDocument(docRefResolver.resolve(XWiki.getServerWikiPage(wikiName),
        new WikiReference(xwiki.getDatabase())), context);
    BaseObject serverobject = getMatchingServerObject(doc, wikiName, context);
    if (serverobject == null) {
      serverobject = doc.getXObject(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
    }
    return serverobject;
  }

  private BaseObject getMatchingServerObject(XWikiDocument doc, String wikiName,
      XWikiContext context) {
    BaseObject serverobject = null;
    if ((context != null) && (context.getURL() != null) && (context.getURL().getHost() != null)) {
      String server = context.getURL().getHost().replaceFirst(context.getDatabase(), wikiName);
      serverobject = doc.getXObject(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "server",
          server);
    }
    return serverobject;
  }

}
