/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.celements.execution.XWikiExecutionProp;
import com.celements.init.XWikiProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiForm;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiURLFactory;

public class XWikiContext extends Hashtable<Object, Object> {

  public static final int MODE_SERVLET = 0;

  @Deprecated
  public static final int MODE_PORTLET = 1;

  @Deprecated
  public static final int MODE_XMLRPC = 2;

  @Deprecated
  public static final int MODE_ATOM = 3;

  @Deprecated
  public static final int MODE_PDF = 4;

  @Deprecated
  public static final int MODE_GWT = 5;

  @Deprecated
  public static final int MODE_GWT_DEBUG = 6;

  /**
   * @deprecated since 6.0 instead use {@link XWikiExecutionProp#XWIKI_CONTEXT}
   */
  @Deprecated
  public static final String EXECUTIONCONTEXT_KEY = "xwikicontext";

  private static final String WIKI_KEY = "wiki";

  private static final String ORIGINAL_WIKI_KEY = "originalWiki";

  private boolean finished = false;

  private XWiki wiki;

  private XWikiEngineContext engine_context;

  private XWikiRequest request;

  private XWikiResponse response;

  private XWikiForm form;

  private String action;

  private String orig_database;

  private String database;

  private XWikiUser user;

  private static final String USER_KEY = "user";

  private String language;

  private static final String LANGUAGE_KEY = "language";

  private String interfaceLanguage;

  private URI uri;

  private XWikiURLFactory urlFactory;

  private int cacheDuration = 0;

  private int classCacheSize = 20;

  private int archiveCacheSize = 20;

  // Used to avoid recursive loading of documents if there are recursives usage of classes
  @SuppressWarnings("unchecked")
  private Map<DocumentReference, BaseClass> classCache = Collections
      .synchronizedMap(new LRUMap(this.classCacheSize));

  // Used to avoid reloading archives in the same request
  @SuppressWarnings("unchecked")
  private Map<String, XWikiDocumentArchive> archiveCache = Collections
      .synchronizedMap(new LRUMap(this.archiveCacheSize));

  private List<String> displayedFields = Collections.synchronizedList(new ArrayList<String>());

  /**
   * Used to resolve a string into a proper Document Reference using the current document's
   * reference to fill the
   * blanks, except for the page name for which the default page name is used instead and for the
   * wiki name for which
   * the current wiki is used instead of the current document reference's wiki.
   */
  @SuppressWarnings("unchecked")
  private static final Supplier<DocumentReferenceResolver<String>> currentMixedDocumentReferenceResolver = () -> Utils
      .getComponent(DocumentReferenceResolver.class, "currentmixed");

  private static final Supplier<XWikiProvider> xwikiProvider = () -> Utils
      .getComponent(XWikiProvider.class);

  public XWikiContext() {}

  /**
   * @deprecated instead use {@link XWikiProvider}
   */
  @Deprecated
  public XWiki getWiki() {
    if (wiki == null) {
      wiki = xwikiProvider.get().get().orElse(null);
    }
    return wiki;
  }

  public Util getUtil() {
    Util util = (Util) this.get("util");
    if (util == null) {
      util = new Util();
      this.put("util", util);
    }
    return util;
  }

  public void setWiki(XWiki wiki) {
    this.wiki = wiki;
  }

  public XWikiEngineContext getEngineContext() {
    return this.engine_context;
  }

  public void setEngineContext(XWikiEngineContext engine_context) {
    this.engine_context = engine_context;
  }

  public XWikiRequest getRequest() {
    return this.request;
  }

  public boolean hasRequest() {
    return (request != null) && !(request instanceof XWikiServletRequestStub)
        && (request.getHttpServletRequest() != null)
        && !(request.getHttpServletRequest() instanceof XWikiServletRequestStub);
  }

  public void setRequest(XWikiRequest request) {
    this.request = request;
  }

  public String getAction() {
    return this.action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public XWikiResponse getResponse() {
    return this.response;
  }

  public void setResponse(XWikiResponse response) {
    this.response = response;
  }

  public String getDatabase() {
    return this.database;
  }

  public void setDatabase(String database) {
    this.database = database;
    if (database == null) {
      super.remove(WIKI_KEY);
    } else {
      super.put(WIKI_KEY, database);
    }
    if (this.orig_database == null) {
      this.orig_database = database;
      if (database == null) {
        super.remove(ORIGINAL_WIKI_KEY);
      } else {
        super.put(ORIGINAL_WIKI_KEY, database);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Make sure to keep {@link #database} field and map synchronized.
   *
   * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public synchronized Object put(Object key, Object value) {
    Object previous;

    if (WIKI_KEY.equals(key)) {
      previous = get(WIKI_KEY);
      setDatabase((String) value);
    } else {
      previous = super.put(key, value);
    }

    return previous;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Make sure to keep {@link #database} field and map synchronized.
   *
   * @see java.util.Hashtable#remove(java.lang.Object)
   */
  @Override
  public synchronized Object remove(Object key) {
    Object previous;

    if (WIKI_KEY.equals(key)) {
      previous = get(WIKI_KEY);
      setDatabase(null);
    } else {
      previous = super.remove(key);
    }

    return previous;
  }

  public String getOriginalDatabase() {
    return this.orig_database;
  }

  public void setOriginalDatabase(String database) {
    this.orig_database = database;
    if (database == null) {
      remove(ORIGINAL_WIKI_KEY);
    } else {
      put(ORIGINAL_WIKI_KEY, database);
    }
  }

  /**
   * @return true it's main wiki's context, false otherwise.
   */
  public boolean isMainWiki() {
    return isMainWiki(getDatabase());
  }

  /**
   * @param wikiName
   *          the name of the wiki.
   * @return true it's main wiki's context, false otherwise.
   */
  public boolean isMainWiki(String wikiName) {
    String mainWiki = Optional.ofNullable(getMainXWiki())
        .orElse(XWikiConstant.MAIN_WIKI.getName());
    return !getXWikiCfg().isVirtualMode()
        || mainWiki.equalsIgnoreCase(wikiName)
        || getXWikiCfg().getProperty("xwiki.db", mainWiki).equalsIgnoreCase(wikiName);
  }

  public XWikiDocument getDoc() {
    return (XWikiDocument) get("doc");
  }

  public void setDoc(XWikiDocument doc) {
    if (doc == null) {
      remove("doc");
    } else {
      put("doc", doc);
    }
  }

  public void setUser(String user, boolean main) {
    if (user == null) {
      this.user = null;
      remove(USER_KEY);
    } else {
      this.user = new XWikiUser(user, main);
      put(USER_KEY, user);
    }
  }

  public void setUser(String user) {
    setUser(user, false);
  }

  public String getUser() {
    if (this.user != null) {
      return this.user.getUser();
    } else {
      return XWikiRightService.GUEST_USER_FULLNAME;
    }
  }

  public String getLocalUser() {
    String username = getUser();
    return username.substring(username.indexOf(":") + 1);
  }

  public XWikiUser getXWikiUser() {
    return this.user;
  }

  public String getLanguage() {
    return this.language;
  }

  public void setLanguage(String language) {
    this.language = Util.normalizeLanguage(language);
    if (language == null) {
      remove(LANGUAGE_KEY);
    } else {
      put(LANGUAGE_KEY, language);
    }
  }

  public String getInterfaceLanguage() {
    return this.interfaceLanguage;
  }

  public void setInterfaceLanguage(String interfaceLanguage) {
    this.interfaceLanguage = interfaceLanguage;
  }

  public int getMode() {
    return MODE_SERVLET;
  }

  /**
   * @deprecated since 6.0 instead use {@link #getUri()}
   */
  @Deprecated
  public URL getURL() {
    try {
      return getUri().toURL();
    } catch (MalformedURLException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  public URI getUri() {
    return this.uri;
  }

  /**
   * @deprecated since 6.0 instead use {@link #getUri()}
   */
  @Deprecated
  public void setURL(URL url) {
    try {
      setUri(url.toURI());
    } catch (URISyntaxException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public XWikiURLFactory getURLFactory() {
    return this.urlFactory;
  }

  public void setURLFactory(XWikiURLFactory urlFactory) {
    this.urlFactory = urlFactory;
  }

  public XWikiForm getForm() {
    return this.form;
  }

  public void setForm(XWikiForm form) {
    this.form = form;
  }

  public boolean isFinished() {
    return this.finished;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public int getCacheDuration() {
    return this.cacheDuration;
  }

  public void setCacheDuration(int cacheDuration) {
    this.cacheDuration = cacheDuration;
  }

  public String getMainXWiki() {
    return (String) get("mainxwiki");
  }

  public void setMainXWiki(String str) {
    put("mainxwiki", str);
  }

  // Used to avoid recursive loading of documents if there are recursives usage of classes
  public void addBaseClass(BaseClass bclass) {
    this.classCache.put(bclass.getDocumentReference(), bclass);
  }

  /**
   * @since 2.2M2
   */
  // Used to avoid recursive loading of documents if there are recursives usage of classes
  public BaseClass getBaseClass(DocumentReference documentReference) {
    return this.classCache.get(documentReference);
  }

  /**
   * @deprecated since 2.2M2 use {@link #getBaseClass(DocumentReference)}
   */
  // Used to avoid recursive loading of documents if there are recursives usage of classes
  @Deprecated
  public BaseClass getBaseClass(String name) {
    BaseClass baseClass = null;
    if (!StringUtils.isEmpty(name)) {
      baseClass = this.classCache.get(currentMixedDocumentReferenceResolver.get().resolve(name));
    }
    return baseClass;
  }

  /**
   * Empty the class cache.
   */
  public void flushClassCache() {
    this.classCache.clear();
  }

  // Used to avoid recursive loading of documents if there are recursives usage of classes
  public void addDocumentArchive(String key, XWikiDocumentArchive obj) {
    this.archiveCache.put(key, obj);
  }

  // Used to avoid recursive loading of documents if there are recursives usage of classes
  public XWikiDocumentArchive getDocumentArchive(String key) {
    return this.archiveCache.get(key);
  }

  /**
   * Empty the archive cache.
   */
  public void flushArchiveCache() {
    this.archiveCache.clear();
  }

  public void setLinksAction(String action) {
    put("links_action", action);
  }

  public void unsetLinksAction() {
    remove("links_action");
  }

  public String getLinksAction() {
    return (String) get("links_action");
  }

  public void setLinksQueryString(String value) {
    put("links_qs", value);
  }

  public void unsetLinksQueryString() {
    remove("links_qs");
  }

  public String getLinksQueryString() {
    return (String) get("links_qs");
  }

  public XWikiMessageTool getMessageTool() {
    XWikiMessageTool msg = ((XWikiMessageTool) get("msg"));
    if (msg == null) {
      getWiki().prepareResources(this);
      msg = ((XWikiMessageTool) get("msg"));
    }
    return msg;
  }

  public XWikiValidationStatus getValidationStatus() {
    return (XWikiValidationStatus) get("validation_status");
  }

  public void setValidationStatus(XWikiValidationStatus status) {
    put("validation_status", status);
  }

  public void addDisplayedField(String fieldname) {
    this.displayedFields.add(fieldname);
  }

  public List<String> getDisplayedFields() {
    return this.displayedFields;
  }

  public String getEditorWysiwyg() {
    return (String) get("editor_wysiwyg");
  }

  // START XWikiContextCompatibilityAspect
  /**
   * @return true it's main wiki's context, false otherwise.
   * @deprecated replaced by {@link XWikiContext#isMainWiki()} since 1.4M1.
   */
  @Deprecated
  public boolean isVirtual() {
    return !this.isMainWiki();
  }

  /**
   * @param virtual
   *          true it's main wiki's context, false otherwise.
   * @deprecated this methods is now useless because the virtuality of a wiki is resolved with a
   *             comparison between {@link XWikiContext#getDatabase()} and
   *             {@link XWikiContext#getMainXWiki()} since 1.4M1.
   */
  @Deprecated
  public void setVirtual(boolean virtual) {
    // this.virtual = virtual;
  }
  // END XWikiContextCompatibilityAspect

  private XWikiConfigSource getXWikiCfg() {
    return Utils.getComponent(XWikiConfigSource.class);
  }

}
