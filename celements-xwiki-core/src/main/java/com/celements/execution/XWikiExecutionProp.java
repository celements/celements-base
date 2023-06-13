package com.celements.execution;

import java.net.URI;

import org.xwiki.context.ExecutionContext.Property;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public final class XWikiExecutionProp {

  public static final Property<WikiReference> WIKI = new Property<>(
      "wiki", WikiReference.class);
  public static final Property<XWikiDocument> DOC = new Property<>(
      "doc", XWikiDocument.class);

  public static final Property<XWiki> XWIKI = new Property<>(
      "xwiki.instance", XWiki.class);
  public static final Property<XWikiContext> XWIKI_CONTEXT = new Property<>(
      "xwikicontext", XWikiContext.class);

  public static final Property<XWikiRequest> XWIKI_REQUEST = new Property<>(
      "xwiki.request", XWikiRequest.class);
  public static final Property<URI> XWIKI_REQUEST_URI = new Property<>(
      "xwiki.request.uri", URI.class);
  public static final Property<String> XWIKI_REQUEST_ACTION = new Property<>(
      "xwiki.request.action", String.class);
  public static final Property<XWikiResponse> XWIKI_RESPONSE = new Property<>(
      "xwiki.response", XWikiResponse.class);

  private XWikiExecutionProp() {}

}
