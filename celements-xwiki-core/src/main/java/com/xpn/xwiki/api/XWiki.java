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
 */

package com.xpn.xwiki.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Programming;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiMessageTool;

public class XWiki extends Api {

  /** Logging helper object. */
  protected static final Logger LOG = LoggerFactory.getLogger(XWiki.class);

  /** The internal object wrapped by this API. */
  private com.xpn.xwiki.XWiki xwiki;

  private CriteriaService criteriaService;

  @SuppressWarnings("unchecked")
  private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils
      .getComponent(DocumentReferenceResolver.class, "currentmixed");

  @SuppressWarnings("unchecked")
  private DocumentReferenceResolver<String> defaultDocumentReferenceResolver = Utils
      .getComponent(DocumentReferenceResolver.class);

  private Util util;

  /**
   * XWiki API Constructor
   *
   * @param xwiki
   *          XWiki Main Object to wrap
   * @param context
   *          XWikiContext to wrap
   */
  public XWiki(com.xpn.xwiki.XWiki xwiki, XWikiContext context) {
    super(context);
    this.xwiki = xwiki;
    this.criteriaService = new CriteriaService(context);
    this.util = new Util(xwiki, context);
  }

  /**
   * Privileged API allowing to access the underlying main XWiki Object
   *
   * @return Privileged Main XWiki Object
   */
  @Programming
  public com.xpn.xwiki.XWiki getXWiki() {
    if (hasProgrammingRights()) {
      return this.xwiki;
    }

    return null;
  }

  /**
   * @return XWiki's version in the format <code>(version).(SVN build number)</code>, or "Unknown
   *         version" if it
   *         failed to be retrieved
   */
  public String getVersion() {
    return this.xwiki.getVersion();
  }

  /**
   * API Allowing to access the current request URL being requested
   *
   * @return URL
   * @throws XWikiException
   */
  public String getRequestURL() throws XWikiException {
    return getXWikiContext().getURLFactory().getRequestURL(getXWikiContext()).toString();
  }

  /**
   * Loads an Document from the database. Rights are checked before sending back the document.
   *
   * @param fullName
   *          the full name of the XWiki document to be loaded
   * @return a Document object or null if it is not accessible
   * @throws XWikiException
   */
  public Document getDocument(String fullName) throws XWikiException {
    DocumentReference reference;

    // We ignore the passed full name if it's null to be backward compatible with previous
    // behaviors.
    if (fullName != null) {
      // Note: We use the CurrentMixed Resolver since we want to use the default page name if the
      // page isn't
      // specified in the passed string, rather than use the current document's page name.
      reference = this.currentMixedDocumentReferenceResolver.resolve(fullName);
    } else {
      reference = this.defaultDocumentReferenceResolver.resolve("");
    }

    return getDocument(reference);
  }

  /**
   * Loads an Document from the database. Rights are checked before sending back the document.
   *
   * @param reference
   *          the reference of the XWiki document to be loaded
   * @return a Document object or null if it is not accessible
   * @throws XWikiException
   * @since 2.3M1
   */
  public Document getDocument(DocumentReference reference) throws XWikiException {
    try {
      XWikiDocument doc = this.xwiki.getDocument(reference, getXWikiContext());
      if (!this.xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(),
          doc.getFullName(),
          getXWikiContext())) {
        return null;
      }

      return doc.newDocument(getXWikiContext());
    } catch (Exception ex) {
      LOG.warn("Failed to access document " + reference + ": " + ex.getMessage());
      return new Document(new XWikiDocument(reference), getXWikiContext());
    }
  }

  /**
   * Loads an Document from the database. Rights are checked on the author (contentAuthor) of the
   * document containing
   * the currently executing script before sending back the loaded document.
   *
   * @param fullName
   *          the full name of the XWiki document to be loaded
   * @return a Document object or null if it is not accessible
   * @throws XWikiException
   * @since 2.3M2
   */
  public Document getDocumentAsAuthor(String fullName) throws XWikiException {
    DocumentReference reference;

    // We ignore the passed full name if it's null to match behavior of getDocument
    if (fullName != null) {
      // Note: We use the CurrentMixed Resolver since we want to use the default page name if the
      // page isn't
      // specified in the passed string, rather than use the current document's page name.
      reference = this.currentMixedDocumentReferenceResolver.resolve(fullName);
    } else {
      reference = this.defaultDocumentReferenceResolver.resolve("");
    }

    return getDocumentAsAuthor(reference);
  }

  /**
   * Loads an Document from the database. Rights are checked on the author (contentAuthor) of the
   * document containing
   * the currently executing script before sending back the loaded document.
   *
   * @param reference
   *          the reference of the XWiki document to be loaded
   * @return a Document object or null if it is not accessible
   * @throws XWikiException
   * @since 2.3M2
   */
  public Document getDocumentAsAuthor(DocumentReference reference) throws XWikiException {
    String author = this.getEffectiveScriptAuthorName();
    XWikiDocument doc = this.xwiki.getDocument(reference, getXWikiContext());
    if (!this.xwiki.getRightService().hasAccessLevel("view", author, doc.getFullName(),
        getXWikiContext())) {
      return null;
    }

    return doc.newDocument(getXWikiContext());
  }

  /**
   * @param fullname
   *          the {@link XWikiDocument#getFullName() name} of the document to search for.
   * @param lang
   *          an optional {@link XWikiDocument#getLanguage() language} to filter results.
   * @return A list with all the deleted versions of a document in the recycle bin.
   * @throws XWikiException
   *           if any error
   */
  public List<DeletedDocument> getDeletedDocuments(String fullname, String lang)
      throws XWikiException {
    XWikiDeletedDocument[] dds = this.xwiki.getDeletedDocuments(fullname, lang, this.context);
    if ((dds == null) || (dds.length == 0)) {
      return Collections.emptyList();
    }
    List<DeletedDocument> result = new ArrayList<>(dds.length);
    for (XWikiDeletedDocument dd : dds) {
      result.add(new DeletedDocument(dd, this.context));
    }
    return result;
  }

  /**
   * @return specified documents in recycle bin
   * @param fullname
   *          - {@link XWikiDocument#getFullName()}
   * @param lang
   *          - {@link XWikiDocument#getLanguage()}
   * @throws XWikiException
   *           if any error
   */
  public DeletedDocument getDeletedDocument(String fullname, String lang, String index)
      throws XWikiException {
    if (!NumberUtils.isDigits(index)) {
      return null;
    }
    XWikiDeletedDocument dd = this.xwiki.getDeletedDocument(fullname, lang, Integer.parseInt(index),
        this.context);
    if (dd == null) {
      return null;
    }

    return new DeletedDocument(dd, this.context);
  }

  /**
   * Retrieve all the deleted attachments that belonged to a certain document. Note that this does
   * not distinguish
   * between different incarnations of a document name, and it does not require that the document
   * still exists, it
   * returns all the attachments that at the time of their deletion had a document with the
   * specified name as their
   * owner.
   *
   * @param docName
   *          the {@link XWikiDocument#getFullName() name} of the owner document
   * @return A list with all the deleted attachments which belonged to the specified document. If no
   *         such attachments
   *         are found in the trash, an empty list is returned.
   */
  public List<DeletedAttachment> getDeletedAttachments(String docName) {
    try {
      List<com.xpn.xwiki.doc.DeletedAttachment> attachments = this.xwiki
          .getDeletedAttachments(docName, this.context);
      if ((attachments == null) || attachments.isEmpty()) {
        attachments = Collections.emptyList();
      }
      List<DeletedAttachment> result = new ArrayList<>(attachments.size());
      for (com.xpn.xwiki.doc.DeletedAttachment attachment : attachments) {
        result.add(new DeletedAttachment(attachment, this.context));
      }
      return result;
    } catch (Exception ex) {
      LOG.warn("Failed to retrieve deleted attachments", ex);
    }
    return Collections.emptyList();
  }

  /**
   * Retrieve all the deleted attachments that belonged to a certain document and had the specified
   * name. Multiple
   * versions can be returned since the same file can be uploaded and deleted several times,
   * creating different
   * instances in the trash. Note that this does not distinguish between different incarnations of a
   * document name,
   * and it does not require that the document still exists, it returns all the attachments that at
   * the time of their
   * deletion had a document with the specified name as their owner.
   *
   * @param docName
   *          the {@link DeletedAttachment#getDocName() name of the document} the attachment
   *          belonged to
   * @param filename
   *          the {@link DeletedAttachment#getFilename() name} of the attachment to search for
   * @return A list with all the deleted attachments which belonged to the specified document and
   *         had the specified
   *         filename. If no such attachments are found in the trash, an empty list is returned.
   */
  public List<DeletedAttachment> getDeletedAttachments(String docName, String filename) {
    try {
      List<com.xpn.xwiki.doc.DeletedAttachment> attachments = this.xwiki
          .getDeletedAttachments(docName, filename, this.context);
      if (attachments == null) {
        attachments = Collections.emptyList();
      }
      List<DeletedAttachment> result = new ArrayList<>(attachments.size());
      for (com.xpn.xwiki.doc.DeletedAttachment attachment : attachments) {
        result.add(new DeletedAttachment(attachment, this.context));
      }
      return result;
    } catch (Exception ex) {
      LOG.warn("Failed to retrieve deleted attachments", ex);
    }
    return Collections.emptyList();
  }

  /**
   * Retrieve a specific attachment from the trash.
   *
   * @param id
   *          the unique identifier of the entry in the trash
   * @return specified attachment from the trash, {@code null} if not found
   */
  public DeletedAttachment getDeletedAttachment(String id) {
    try {
      com.xpn.xwiki.doc.DeletedAttachment attachment = this.xwiki.getDeletedAttachment(id,
          this.context);
      if (attachment != null) {
        return new DeletedAttachment(attachment, this.context);
      }
    } catch (Exception ex) {
      LOG.warn("Failed to retrieve deleted attachment", ex);
    }
    return null;
  }

  /**
   * Returns whether a document exists or not
   *
   * @param fullname
   *          Fullname of the XWiki document to be loaded
   * @return true if the document exists, false if not
   * @throws XWikiException
   */
  public boolean exists(String fullname) throws XWikiException {
    return this.xwiki.exists(fullname, getXWikiContext());
  }

  /**
   * Returns whether a document exists or not
   *
   * @param reference
   *          the reference of the document to check for its existence
   * @return true if the document exists, false if not
   * @since 2.3M2
   */
  public boolean exists(DocumentReference reference) throws XWikiException {
    return this.xwiki.exists(reference, getXWikiContext());
  }

  /**
   * Verify the rights the current user has on a document. If the document requires rights and the
   * user is not
   * authenticated he will be redirected to the login page.
   *
   * @param docname
   *          fullname of the document
   * @param right
   *          right to check ("view", "edit", "admin", "delete")
   * @return true if it exists
   */
  public boolean checkAccess(String docname, String right) {
    try {
      XWikiDocument doc = getXWikiContext().getWiki().getDocument(docname, this.context);
      return getXWikiContext().getWiki().checkAccess(right, doc, getXWikiContext());
    } catch (XWikiException e) {
      return false;
    }
  }

  /**
   * Loads an Document from the database. Rights are checked before sending back the document.
   *
   * @param space
   *          Space to use in case no space is defined in the provided <code>fullname</code>
   * @param fullname
   *          the full name or relative name of the document to load
   * @return a Document object or null if it is not accessible
   * @throws XWikiException
   */
  public Document getDocument(String space, String fullname) throws XWikiException {
    XWikiDocument doc = this.xwiki.getDocument(space, fullname, getXWikiContext());
    if (!this.xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(),
        doc.getFullName(),
        getXWikiContext())) {
      return null;
    }

    return doc.newDocument(getXWikiContext());
  }

  /**
   * Load a specific revision of a document
   *
   * @param doc
   *          Document for which to load a specific revision
   * @param rev
   *          Revision number
   * @return Specific revision of a document
   * @throws XWikiException
   */
  public Document getDocument(Document doc, String rev) throws XWikiException {
    if ((doc == null) || (doc.getDoc() == null)
        || !this.xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(),
            doc.getFullName(),
            getXWikiContext())) {
      // Finally we return null, otherwise showing search result is a real pain
      return null;
    }

    try {
      XWikiDocument revdoc = this.xwiki.getDocument(doc.getDoc(), rev, getXWikiContext());
      return revdoc.newDocument(getXWikiContext());
    } catch (Exception e) {
      // Can't read versioned document
      LOG.error("Failed to read versioned document", e);

      return null;
    }
  }

  /**
   * Transform a text in a form compatible text
   *
   * @param content
   *          text to transform
   * @return encoded result
   */
  public String getFormEncoded(String content) {
    return com.xpn.xwiki.XWiki.getFormEncoded(content);
  }

  /**
   * Transform a text in a XML compatible text This method uses Apache CharacterFilter which swaps
   * single quote
   * (&#39;) for left single quotation mark (&#8217;)
   *
   * @param content
   *          text to transform
   * @return encoded result
   */
  public String getXMLEncoded(String content) {
    return com.xpn.xwiki.XWiki.getXMLEncoded(content);
  }

  /**
   * Output content in the edit content textarea
   *
   * @param content
   *          content to output
   * @return the textarea text content
   */
  public String getTextArea(String content) {
    return com.xpn.xwiki.XWiki.getTextArea(content, getXWikiContext());
  }

  /**
   * Output content in the edit content htmlarea
   *
   * @param content
   *          content to output
   * @return the htmlarea text content
   */
  public String getHTMLArea(String content) {
    return this.xwiki.getHTMLArea(content, getXWikiContext());
  }

  /**
   * Get the list of available classes in the wiki
   *
   * @return list of classes names
   * @throws XWikiException
   */
  public List<String> getClassList() throws XWikiException {
    return this.xwiki.getClassList(getXWikiContext());
  }

  /**
   * Get the global MetaClass object
   *
   * @return MetaClass object
   */
  public MetaClass getMetaclass() {
    return this.xwiki.getMetaclass();
  }

  /**
   * Privileged API allowing to run a search on the database returning a list of data This search is
   * send to the store engine (Hibernate HQL or XWQL)
   *
   * @param wheresql
   *          Query to be run (HQL, XPath)
   * @return A list of rows (Object[])
   * @throws XWikiException
   */
  public <T> List<T> search(String wheresql) throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.search(wheresql, getXWikiContext());
    }

    return Collections.emptyList();
  }

  /**
   * Privileged API allowing to run a search on the database returning a list of data. The HQL where
   * clause uses
   * parameters (question marks) instead of values, and the actual values are passed in the
   * parameters list. This
   * allows generating a query which will automatically encode the passed values (like escaping
   * single quotes). This
   * API is recommended to be used over the other similar methods where the values are passed inside
   * the where clause
   * and for which manual encoding/escaping is needed to avoid SQL injections or bad queries.
   *
   * @param parameterizedWhereClause
   *          query to be run (HQL)
   * @param parameterValues
   *          the where clause values that replace the question marks
   * @return a list of rows, where each row has either the selected data type
   *         ({@link XWikiDocument}, {@code String},
   *         {@code Integer}, etc.), or {@code Object[]} if more than one column was selected
   * @throws XWikiException
   */
  public <T> List<T> search(String parameterizedWhereClause, List<?> parameterValues)
      throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.getStore().search(parameterizedWhereClause, 0, 0, parameterValues,
          getXWikiContext());
    }

    return Collections.emptyList();
  }

  /**
   * Privileged API allowing to run a search on the database returning a list of data. This search
   * is sent to the store engine (Hibernate HQL or XWQL)
   *
   * @param wheresql
   *          Query to be run (HQL, XPath)
   * @param nb
   *          return only 'nb' rows
   * @param start
   *          skip the 'start' first elements
   * @return A list of rows (Object[])
   * @throws XWikiException
   */
  public <T> List<T> search(String wheresql, int nb, int start) throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.search(wheresql, nb, start, getXWikiContext());
    }

    return Collections.emptyList();
  }

  /**
   * Privileged API allowing to run a search on the database returning a list of data. The HQL where
   * clause uses
   * parameters (question marks) instead of values, and the actual values are passed in the
   * paremeters list. This
   * allows generating a query which will automatically encode the passed values (like escaping
   * single quotes). This
   * API is recommended to be used over the other similar methods where the values are passed inside
   * the where clause
   * and for which manual encoding/escaping is needed to avoid sql injections or bad queries.
   *
   * @param parameterizedWhereClause
   *          query to be run (HQL)
   * @param maxResults
   *          maximum number of results to return; if 0 all results are returned
   * @param startOffset
   *          skip the first N results; if 0 no items are skipped
   * @param parameterValues
   *          the where clause values that replace the question marks
   * @return a list of rows, where each row has either the selected data type
   *         ({@link XWikiDocument}, {@code String},
   *         {@code Integer}, etc.), or {@code Object[]} if more than one column was selected
   * @throws XWikiException
   */
  public <T> List<T> search(String parameterizedWhereClause, int maxResults, int startOffset,
      List<?> parameterValues) throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.getStore().search(parameterizedWhereClause, maxResults, startOffset,
          parameterValues,
          getXWikiContext());
    }

    return Collections.emptyList();
  }

  /**
   * API allowing to search for document names matching a query. Examples:
   * <ul>
   * <li>Query: <code>where doc.space='Main' order by doc.creationDate desc</code>. Result: All the
   * documents in space
   * 'Main' ordered by the creation date from the most recent</li>
   * <li>Query: <code>where doc.name like '%sport%' order by doc.name asc</code>. Result: All the
   * documents containing
   * 'sport' in their name ordered by document name</li>
   * <li>Query: <code>where doc.content like '%sport%' order by doc.author</code> Result: All the
   * documents containing
   * 'sport' in their content ordered by the author</li>
   * <li>Query: <code>where doc.creator = 'XWiki.LudovicDubost' order by doc.creationDate
   *       desc</code>. Result: All the documents with creator LudovicDubost ordered by the creation
   * date from the
   * most recent</li>
   * <li>Query: <code>where doc.author = 'XWiki.LudovicDubost' order by doc.date desc</code>.
   * Result: All the
   * documents with last author LudovicDubost ordered by the last modification date from the most
   * recent.</li>
   * <li>Query: <code>,BaseObject as obj where doc.fullName=obj.name and
   *       obj.className='XWiki.XWikiComments' order by doc.date desc</code>. Result: All the
   * documents with at least
   * one comment ordered by the last modification date from the most recent</li>
   * <li>Query: <code>,BaseObject as obj, StringProperty as prop where
   *       doc.fullName=obj.name and obj.className='XWiki.XWikiComments' and obj.id=prop.id.id
   *       and prop.id.name='author' and prop.value='XWiki.LudovicDubost' order by doc.date
   *       desc</code>. Result: All the documents with at least one comment from LudovicDubost
   * ordered by the last
   * modification date from the most recent</li>
   * </ul>
   *
   * @param wheresql
   *          Query to be run (either starting with ", BaseObject as obj where.." or by "where ..."
   * @return List of document names matching (Main.Page1, Main.Page2)
   * @throws XWikiException
   */
  public List<String> searchDocuments(String wheresql) throws XWikiException {
    return this.xwiki.getStore().searchDocumentsNames(wheresql, getXWikiContext());
  }

  /**
   * API allowing to count the total number of documents that would be returned by a query.
   *
   * @param wheresql
   *          Query to use, similar to the ones accepted by {@link #searchDocuments(String)}. If
   *          possible, it
   *          should not contain <code>order by</code> or <code>group</code> clauses, since this
   *          kind of queries are
   *          not portable.
   * @return The number of documents that matched the query.
   * @throws XWikiException
   *           if there was a problem executing the query.
   */
  public int countDocuments(String wheresql) throws XWikiException {
    return this.xwiki.getStore().countDocuments(wheresql, getXWikiContext());
  }

  /**
   * API allowing to search for document names matching a query return only a limited number of
   * elements and skipping
   * the first rows. The query part is the same as searchDocuments
   *
   * @param wheresql
   *          query to use similar to searchDocuments(wheresql)
   * @param nb
   *          return only 'nb' rows
   * @param start
   *          skip the first 'start' rows
   * @return List of document names matching
   * @throws XWikiException
   * @see List searchDocuments(String where sql)
   */
  public List<String> searchDocuments(String wheresql, int nb, int start) throws XWikiException {
    return this.xwiki.getStore().searchDocumentsNames(wheresql, nb, start, getXWikiContext());
  }

  /**
   * Privileged API allowing to search for document names matching a query return only a limited
   * number of elements
   * and skipping the first rows. The return values contain the list of columns specified in
   * addition to the document
   * space and name The query part is the same as searchDocuments
   *
   * @param wheresql
   *          query to use similar to searchDocuments(wheresql)
   * @param nb
   *          return only 'nb' rows
   * @param start
   *          skip the first 'start' rows
   * @param selectColumns
   *          List of columns to add to the result
   * @return List of Object[] with the column values of the matching rows
   * @throws XWikiException
   */
  public List<String> searchDocuments(String wheresql, int nb, int start, String selectColumns)
      throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.getStore().searchDocumentsNames(wheresql, nb, start, selectColumns,
          getXWikiContext());
    }

    return Collections.emptyList();
  }

  /**
   * API allowing to search for documents allowing to have mutliple entries per language
   *
   * @param wheresql
   *          query to use similar to searchDocuments(wheresql)
   * @param distinctbylanguage
   *          true to return multiple rows per language
   * @return List of Document object matching
   * @throws XWikiException
   */
  public List<Document> searchDocuments(String wheresql, boolean distinctbylanguage)
      throws XWikiException {
    return convert(
        this.xwiki.getStore().searchDocuments(wheresql, distinctbylanguage, getXWikiContext()));
  }

  /**
   * API allowing to search for documents allowing to have multiple entries per language
   *
   * @param wheresql
   *          query to use similar to searchDocuments(wheresql)
   * @param distinctbylanguage
   *          true to return multiple rows per language
   * @return List of Document object matching
   * @param nb
   *          return only 'nb' rows
   * @param start
   *          skip the first 'start' rows
   * @throws XWikiException
   */
  public List<Document> searchDocuments(String wheresql, boolean distinctbylanguage, int nb,
      int start)
      throws XWikiException {
    return convert(this.xwiki.getStore()
        .searchDocuments(wheresql, distinctbylanguage, nb, start, getXWikiContext()));
  }

  /**
   * Search documents by passing HQL where clause values as parameters. This allows generating a
   * Named HQL query which
   * will automatically encode the passed values (like escaping single quotes). This API is
   * recommended to be used
   * over the other similar methods where the values are passed inside the where clause and for
   * which you'll need to
   * do the encoding/escaping yourself before calling them.
   * <p>
   * Example
   * </p>
   *
   * <pre>
   * &lt;code&gt;
   * #set($orphans = $xwiki.searchDocuments(&quot; where doc.fullName &lt;&gt; ? and (doc.parent = ? or &quot;
   *     + &quot;(doc.parent = ? and doc.space = ?))&quot;,
   *     [&quot;${doc.fullName}as&quot;, ${doc.fullName}, ${doc.name}, ${doc.space}]))
   * &lt;/code&gt;
   * </pre>
   *
   * @param parameterizedWhereClause
   *          the HQL where clause. For example <code>" where doc.fullName
   *        <> ? and (doc.parent = ? or (doc.parent = ? and doc.space = ?))"</code>
   * @param maxResults
   *          the number of rows to return. If 0 then all rows are returned
   * @param startOffset
   *          the number of rows to skip. If 0 don't skip any row
   * @param parameterValues
   *          the where clause values that replace the question marks (?)
   * @return a list of document names
   * @throws XWikiException
   *           in case of error while performing the query
   */
  public List<String> searchDocuments(String parameterizedWhereClause, int maxResults,
      int startOffset,
      List<?> parameterValues) throws XWikiException {
    return this.xwiki.getStore().searchDocumentsNames(parameterizedWhereClause, maxResults,
        startOffset,
        parameterValues, getXWikiContext());
  }

  /**
   * Same as {@link #searchDocuments(String, int, int, java.util.List)} but returns all rows.
   *
   * @see #searchDocuments(String, int, int, java.util.List)
   */
  public List<String> searchDocuments(String parameterizedWhereClause, List<?> parameterValues)
      throws XWikiException {
    return this.xwiki.getStore().searchDocumentsNames(parameterizedWhereClause, parameterValues,
        getXWikiContext());
  }

  /**
   * API allowing to count the total number of documents that would be returned by a parameterized
   * query.
   *
   * @param parameterizedWhereClause
   *          the parameterized query to use, similar to the ones accepted by
   *          {@link #searchDocuments(String, List)}. If possible, it should not contain
   *          <code>order by</code> or
   *          <code>group</code> clauses, since this kind of queries are not portable.
   * @param parameterValues
   *          The parameter values that replace the question marks.
   * @return The number of documents that matched the query.
   * @throws XWikiException
   *           if there was a problem executing the query.
   */
  public int countDocuments(String parameterizedWhereClause, List<?> parameterValues)
      throws XWikiException {
    return this.xwiki.getStore().countDocuments(parameterizedWhereClause, parameterValues,
        getXWikiContext());
  }

  /**
   * Search documents in the provided wiki by passing HQL where clause values as parameters. See
   * {@link #searchDocuments(String, int, int, java.util.List)} for more details.
   *
   * @param wikiName
   *          the name of the wiki where to search.
   * @param parameterizedWhereClause
   *          the HQL where clause. For example <code>" where doc.fullName
   *        <> ? and (doc.parent = ? or (doc.parent = ? and doc.space = ?))"</code>
   * @param maxResults
   *          the number of rows to return. If 0 then all rows are returned
   * @param startOffset
   *          the number of rows to skip. If 0 don't skip any row
   * @param parameterValues
   *          the where clause values that replace the question marks (?)
   * @return a list of document full names (Space.Name).
   * @see #searchDocuments(String, int, int, java.util.List)
   * @throws XWikiException
   *           in case of error while performing the query
   */
  public List<String> searchDocumentsNames(String wikiName, String parameterizedWhereClause,
      int maxResults,
      int startOffset, List<?> parameterValues) throws XWikiException {
    String database = this.context.getDatabase();

    try {
      this.context.setDatabase(wikiName);

      return searchDocuments(parameterizedWhereClause, maxResults, startOffset, parameterValues);
    } finally {
      this.context.setDatabase(database);
    }
  }

  /**
   * Search spaces by passing HQL where clause values as parameters. See
   * {@link #searchDocuments(String, int, int, List)} for more about parameterized hql clauses.
   *
   * @param parametrizedSqlClause
   *          the HQL where clause. For example <code>" where doc.fullName
   *        <> ? and (doc.parent = ? or (doc.parent = ? and doc.space = ?))"</code>
   * @param nb
   *          the number of rows to return. If 0 then all rows are returned
   * @param start
   *          the number of rows to skip. If 0 don't skip any row
   * @param parameterValues
   *          the where clause values that replace the question marks (?)
   * @return a list of spaces names.
   * @throws XWikiException
   *           in case of error while performing the query
   */
  public List<String> searchSpacesNames(String parametrizedSqlClause, int nb, int start,
      List<?> parameterValues)
      throws XWikiException {
    return this.xwiki.getStore().search(
        "select distinct doc.space from XWikiDocument doc " + parametrizedSqlClause, nb, start,
        parameterValues,
        this.context);
  }

  /**
   * Function to wrap a list of XWikiDocument into Document objects
   *
   * @param docs
   *          list of XWikiDocument
   * @return list of Document objects
   */
  public List<Document> wrapDocs(List<?> docs) {
    List<Document> result = new ArrayList<>();
    if (docs != null) {
      for (java.lang.Object obj : docs) {
        try {
          if (obj instanceof XWikiDocument) {
            XWikiDocument doc = (XWikiDocument) obj;
            Document wrappedDoc = doc.newDocument(getXWikiContext());
            result.add(wrappedDoc);
          } else if (obj instanceof Document) {
            result.add((Document) obj);
          } else if (obj instanceof String) {
            Document doc = getDocument(obj.toString());
            if (doc != null) {
              result.add(doc);
            }
          }
        } catch (XWikiException ex) {}
      }
    }

    return result;
  }

  /**
   * API allowing to parse a text content to evaluate velocity scripts
   *
   * @param content
   * @return evaluated content if the content contains velocity scripts
   */
  public String parseContent(String content) {
    return this.xwiki.parseContent(content, getXWikiContext());
  }

  /**
   * API to parse a velocity template provided by the current Skin The template is first looked in
   * the skin active for
   * the user, the space or the wiki. If the template does not exist in that skin, the template is
   * looked up in the
   * "parent skin" of the skin
   *
   * @param template
   *          Template name ("view", "edit", "comment")
   * @return Evaluated content from the template
   */
  public String parseTemplate(String template) {
    return this.xwiki.parseTemplate(template, getXWikiContext());
  }

  /**
   * API to render a velocity template provided by the current Skin The template is first looked in
   * the skin active
   * for the user, the space or the wiki. If the template does not exist in that skin, the template
   * is looked up in
   * the "parent skin" of the skin
   *
   * @param template
   *          Template name ("view", "edit", "comment")
   * @return Evaluated content from the template
   */
  public String renderTemplate(String template) {
    return this.xwiki.renderTemplate(template, getXWikiContext());
  }

  /**
   * Return the URL of the static file provided by the current skin The file is first looked in the
   * skin active for
   * the user, the space or the wiki. If the file does not exist in that skin, the file is looked up
   * in the "parent
   * skin" of the skin. The file can be a CSS file, an image file, a javascript file, etc.
   *
   * @param filename
   *          Filename to be looked up in the skin (logo.gif, style.css)
   * @return URL to access this file
   */
  public String getSkinFile(String filename) {
    return this.xwiki.getSkinFile(filename, getXWikiContext());
  }

  /**
   * Return the URL of the static file provided by the current skin The file is first looked in the
   * skin active for
   * the user, the space or the wiki. If the file does not exist in that skin, the file is looked up
   * in the "parent
   * skin" of the skin. The file can be a CSS file, an image file, a javascript file, etc.
   *
   * @param filename
   *          Filename to be looked up in the skin (logo.gif, style.css)
   * @param forceSkinAction
   *          true to make sure that static files are retrieved through the skin action, to allow
   *          parsing of velocity on CSS files
   * @return URL to access this file
   */
  public String getSkinFile(String filename, boolean forceSkinAction) {
    return this.xwiki.getSkinFile(filename, forceSkinAction, getXWikiContext());
  }

  /**
   * API to retrieve the current skin for this request and user The skin is first derived from the
   * request "skin"
   * parameter If this parameter does not exist, the user preference "skin" is looked up If this
   * parameter does not
   * exist or is empty, the space preference "skin" is looked up If this parameter does not exist or
   * is empty, the
   * XWiki preference "skin" is looked up If this parameter does not exist or is empty, the
   * xwiki.cfg parameter
   * xwiki.defaultskin is looked up If this parameter does not exist or is empty, the xwiki.cfg
   * parameter
   * xwiki.defaultbaseskin is looked up If this parameter does not exist or is empty, the skin is
   * "albatross"
   *
   * @return The current skin for this request and user
   */
  public String getSkin() {
    return this.xwiki.getSkin(getXWikiContext());
  }

  /**
   * API to retrieve the current skin for this request and user. Each skin has a skin it is based
   * on. If not the base
   * skin is the xwiki.cfg parameter "xwiki.defaultbaseskin". If this parameter does not exist or is
   * empty, the base
   * skin is "albatross".
   *
   * @return The current baseskin for this request and user
   */
  public String getBaseSkin() {
    return this.xwiki.getBaseSkin(getXWikiContext());
  }

  /**
   * API to access the copyright for this space. The copyright is read in the space preferences. If
   * it does not exist
   * or is empty it is read from the XWiki preferences.
   *
   * @return the text for the copyright
   */
  public String getSpaceCopyright() {
    return this.xwiki.getSpaceCopyright(getXWikiContext());
  }

  /**
   * API to access an XWiki Preference There can be one preference object per language This function
   * will find the
   * right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language
   */
  public String getXWikiPreference(String preference) {
    return this.xwiki.getXWikiPreference(preference, getXWikiContext());
  }

  /**
   * API to access an XWiki Preference There can be one preference object per language This function
   * will find the
   * right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the prefenrece does not exist or is empty
   * @return The preference for this wiki and the current language
   */
  public String getXWikiPreference(String preference, String defaultValue) {
    return this.xwiki.getXWikiPreference(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access an Space Preference There can be one preference object per language This function
   * will find the
   * right preference object associated to the current active language If no preference is found it
   * will look in the
   * XWiki Preferences
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language
   */
  public String getSpacePreference(String preference) {
    return this.xwiki.getSpacePreference(preference, getXWikiContext());
  }

  /**
   * API to access an Space Preference There can be one preference object per language This function
   * will find the
   * right preference object associated to the current active language If no preference is found it
   * will look in the
   * XWiki Preferences
   *
   * @param preference
   *          Preference name
   * @param space
   *          The space for which this preference is requested
   * @return The preference for this wiki and the current language
   */
  public String getSpacePreferenceFor(String preference, String space) {
    return this.xwiki.getSpacePreference(preference, space, "", getXWikiContext());
  }

  /**
   * API to access an Space Preference There can be one preference object per language This function
   * will find the
   * right preference object associated to the current active language If no preference is found it
   * will look in the
   * XWiki Preferences
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the preference does not exist or is empty
   * @return The preference for this wiki and the current language
   */
  public String getSpacePreference(String preference, String defaultValue) {
    return this.xwiki.getSpacePreference(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access a Skin Preference The skin object is the current user's skin
   *
   * @param preference
   *          Preference name
   * @return The preference for the current skin
   */
  public String getSkinPreference(String preference) {
    return this.xwiki.getSkinPreference(preference, getXWikiContext());
  }

  /**
   * API to access a Skin Preference The skin object is the current user's skin
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the preference does not exist or is empty
   * @return The preference for the current skin
   */
  public String getSkinPreference(String preference, String defaultValue) {
    return this.xwiki.getSkinPreference(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access an XWiki Preference as a long number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @param space
   *          The space for which this preference is requested
   * @param defaultValue
   *          default value to return if the prefenrece does not exist or is empty
   * @return The preference for this wiki and the current language in long format
   */
  public String getSpacePreferenceFor(String preference, String space, String defaultValue) {
    return this.xwiki.getSpacePreference(preference, space, defaultValue, getXWikiContext());
  }

  /**
   * API to access an XWiki Preference as a long number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the prefenrece does not exist or is empty
   * @return The preference for this wiki and the current language in long format
   */
  public long getXWikiPreferenceAsLong(String preference, long defaultValue) {
    return this.xwiki.getXWikiPreferenceAsLong(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access an XWiki Preference as a long number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language in long format
   */
  public long getXWikiPreferenceAsLong(String preference) {
    return this.xwiki.getXWikiPreferenceAsLong(preference, getXWikiContext());
  }

  /**
   * API to access a Space Preference as a long number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language If no
   * preference is found it will
   * look for the XWiki Preference
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the prefenrece does not exist or is empty
   * @return The preference for this wiki and the current language in long format
   */
  public long getSpacePreferenceAsLong(String preference, long defaultValue) {
    return this.xwiki.getSpacePreferenceAsLong(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access a Space Preference as a long number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language If no
   * preference is found it will
   * look for the XWiki Preference
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language in long format
   */
  public long getSpacePreferenceAsLong(String preference) {
    return this.xwiki.getSpacePreferenceAsLong(preference, getXWikiContext());
  }

  /**
   * API to access an XWiki Preference as an int number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the prefenrece does not exist or is empty
   * @return The preference for this wiki and the current language in int format
   */
  public int getXWikiPreferenceAsInt(String preference, int defaultValue) {
    return this.xwiki.getXWikiPreferenceAsInt(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access an XWiki Preference as a int number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language in int format
   */
  public int getXWikiPreferenceAsInt(String preference) {
    return this.xwiki.getXWikiPreferenceAsInt(preference, getXWikiContext());
  }

  /**
   * API to access a space Preference as a int number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language If no
   * preference is found it will
   * look for the XWiki Preference
   *
   * @param preference
   *          Preference name
   * @param defaultValue
   *          default value to return if the prefenrece does not exist or is empty
   * @return The preference for this wiki and the current language in int format
   */
  public int getSpacePreferenceAsInt(String preference, int defaultValue) {
    return this.xwiki.getSpacePreferenceAsInt(preference, defaultValue, getXWikiContext());
  }

  /**
   * API to access a Space Preference as a int number There can be one preference object per
   * language This function
   * will find the right preference object associated to the current active language If no
   * preference is found it will
   * look for the XWiki Preference
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language in int format
   */
  public int getSpacePreferenceAsInt(String preference) {
    return this.xwiki.getSpacePreferenceAsInt(preference, getXWikiContext());
  }

  /**
   * API to access a User Preference This function will look in the User profile for the preference
   * If no preference
   * is found it will look in the Space Preferences If no preference is found it will look in the
   * XWiki Preferences
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language
   */
  public String getUserPreference(String preference) {
    return this.xwiki.getUserPreference(preference, getXWikiContext());
  }

  /**
   * API to access a User Preference from cookie This function will look in the session cookie for
   * the preference
   *
   * @param preference
   *          Preference name
   * @return The preference for this wiki and the current language
   */
  public String getUserPreferenceFromCookie(String preference) {
    return this.xwiki.getUserPreferenceFromCookie(preference, getXWikiContext());
  }

  /**
   * First try to find the current language in use from the XWiki context. If none is used and if
   * the wiki is not
   * multilingual use the default language defined in the XWiki preferences. If the wiki is
   * multilingual try to get
   * the language passed in the request. If none was passed try to get it from a cookie. If no
   * language cookie exists
   * then use the user default language and barring that use the browser's "Accept-Language" header
   * sent in HTTP
   * request. If none is defined use the default language.
   *
   * @return the language to use
   */
  public String getLanguagePreference() {
    return this.xwiki.getLanguagePreference(getXWikiContext());
  }

  /**
   * API to access the interface language preference for the request Order of evaluation is:
   * Language of the wiki in
   * mono-lingual mode language request paramater language in context language user preference
   * language in cookie
   * language accepted by the navigator
   *
   * @return the document language preference for the request
   */
  public String getInterfaceLanguagePreference() {
    return this.xwiki.getInterfaceLanguagePreference(getXWikiContext());
  }

  /**
   * API to check if wiki is in multi-wiki mode (virtual)
   *
   * @return true for multi-wiki/false for mono-wiki
   */
  public boolean isVirtualMode() {
    return this.xwiki.isVirtualMode();
  }

  /**
   * API to check is wiki is multi-lingual
   *
   * @return true for multi-lingual/false for mono-lingual
   */
  public boolean isMultiLingual() {
    return this.xwiki.isMultiLingual(getXWikiContext());
  }

  /**
   * Priviledged API to flush the cache of the Wiki installation This flushed the cache of all
   * wikis, all plugins, all
   * renderers
   */
  public void flushCache() {
    if (hasProgrammingRights()) {
      this.xwiki.flushCache(getXWikiContext());
    }
  }

  /**
   * Priviledged API to reset the rendenring engine This would restore the rendering engine
   * evaluation loop and take
   * into account new configuration parameters
   */
  public void resetRenderingEngine() {
    if (hasProgrammingRights()) {
      try {
        this.xwiki.resetRenderingEngine(getXWikiContext());
      } catch (XWikiException e) {}
    }
  }

  /**
   * Priviledged API to create a new user from the request This API is used by RegisterNewUser wiki
   * page
   *
   * @return true for success/false for failure
   * @throws XWikiException
   */
  public int createUser() throws XWikiException {
    return createUser(false, "edit");
  }

  /**
   * Priviledged API to create a new user from the request This API is used by RegisterNewUser wiki
   * page This version
   * sends a validation email to the user Configuration of validation email is in the XWiki
   * Preferences
   *
   * @param withValidation
   *          true to send the validationemail
   * @return true for success/false for failure
   * @throws XWikiException
   */
  public int createUser(boolean withValidation) throws XWikiException {
    return createUser(withValidation, "edit");
  }

  /**
   * Priviledged API to create a new user from the request This API is used by RegisterNewUser wiki
   * page This version
   * sends a validation email to the user Configuration of validation email is in the XWiki
   * Preferences
   *
   * @param withValidation
   *          true to send the validation email
   * @param userRights
   *          Rights to set for the user for it's own page(defaults to "edit")
   * @return true for success/false for failure
   * @throws XWikiException
   */
  public int createUser(boolean withValidation, String userRights) throws XWikiException {
    boolean registerRight;
    try {
      // So, what's the register right for? This says that if the creator of the page
      // (Admin) has programming rights, anybody can register. Is this OK?
      if (hasProgrammingRights()) {
        registerRight = true;
      } else {
        registerRight = this.xwiki.getRightService().hasAccessLevel("register",
            getXWikiContext().getUser(),
            "XWiki.XWikiPreferences", getXWikiContext());
      }

      if (registerRight) {
        return this.xwiki.createUser(withValidation, userRights, getXWikiContext());
      }

      return -1;
    } catch (Exception e) {
      LOG.error("Failed to create user", e);

      return -2;
    }

  }

  /**
   * Priviledged API to validate the return code given by a user in response to an email validation
   * email The
   * validation information are taken from the request object
   *
   * @param withConfirmEmail
   *          true to send a account confirmation email/false to not send it
   * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for
   *         other errors
   * @throws XWikiException
   */
  public int validateUser(boolean withConfirmEmail) throws XWikiException {
    return this.xwiki.validateUser(withConfirmEmail, getXWikiContext());
  }

  /**
   * Priviledged API to send a confirmation email to a user
   *
   * @param xwikiname
   *          user to send the email to
   * @param password
   *          password to put in the mail
   * @param email
   *          email to send to
   * @param add_message
   *          Additional message to send to the user
   * @param contentfield
   *          Preference field to use as a mail template
   * @throws XWikiException
   *           if the mail was not send successfully
   */
  public void sendConfirmationMail(String xwikiname, String password, String email,
      String add_message,
      String contentfield) throws XWikiException {
    if (hasProgrammingRights()) {
      this.xwiki.sendConfirmationEmail(xwikiname, password, email, add_message, contentfield,
          getXWikiContext());
    }
  }

  /**
   * Priviledged API to send a confirmation email to a user
   *
   * @param xwikiname
   *          user to send the email to
   * @param password
   *          password to put in the mail
   * @param email
   *          email to send to
   * @param contentfield
   *          Preference field to use as a mail template
   * @throws XWikiException
   *           if the mail was not send successfully
   */
  public void sendConfirmationMail(String xwikiname, String password, String email,
      String contentfield)
      throws XWikiException {
    if (hasProgrammingRights()) {
      this.xwiki.sendConfirmationEmail(xwikiname, password, email, "", contentfield,
          getXWikiContext());
    }
  }

  /**
   * Privileged API to copy a document to another document in the same wiki
   *
   * @param docname
   *          source document
   * @param targetdocname
   *          target document
   * @return true if the copy was sucessfull
   * @throws XWikiException
   *           if the document was not copied properly
   */
  public boolean copyDocument(String docname, String targetdocname) throws XWikiException {
    return this.copyDocument(docname, targetdocname, null, null, null, false, false);
  }

  /**
   * Privileged API to copy a translation of a document to another document in the same wiki
   *
   * @param docname
   *          source document
   * @param targetdocname
   *          target document
   * @param wikilanguage
   *          language to copy
   * @return true if the copy was sucessfull
   * @throws XWikiException
   *           if the document was not copied properly
   */
  public boolean copyDocument(String docname, String targetdocname, String wikilanguage)
      throws XWikiException {
    return this.copyDocument(docname, targetdocname, null, null, wikilanguage, false, false);
  }

  /**
   * Privileged API to copy a translation of a document to another document of the same name in
   * another wiki
   *
   * @param docname
   *          source document
   * @param sourceWiki
   *          source wiki
   * @param targetWiki
   *          target wiki
   * @param wikilanguage
   *          language to copy
   * @return true if the copy was sucessfull
   * @throws XWikiException
   *           if the document was not copied properly
   */
  public boolean copyDocument(String docname, String sourceWiki, String targetWiki,
      String wikilanguage)
      throws XWikiException {
    return this.copyDocument(docname, docname, sourceWiki, targetWiki, wikilanguage, true, false);
  }

  /**
   * Privileged API to copy a translation of a document to another document of the same name in
   * another wiki
   * additionally resetting the version
   *
   * @param docname
   *          source document
   * @param sourceWiki
   *          source wiki
   * @param targetWiki
   *          target wiki
   * @param wikilanguage
   *          language to copy
   * @param reset
   *          true to reset versions
   * @return true if the copy was sucessfull
   * @throws XWikiException
   *           if the document was not copied properly
   */
  public boolean copyDocument(String docname, String targetdocname, String sourceWiki,
      String targetWiki,
      String wikilanguage, boolean reset) throws XWikiException {
    return this.copyDocument(docname, targetdocname, sourceWiki, targetWiki, wikilanguage, reset,
        false);
  }

  /**
   * Privileged API to copy a translation of a document to another document of the same name in
   * another wiki
   * additionally resetting the version and overwriting the previous document
   *
   * @param docname
   *          source document
   * @param sourceWiki
   *          source wiki
   * @param targetWiki
   *          target wiki
   * @param wikilanguage
   *          language to copy
   * @param reset
   *          true to reset versions
   * @param force
   *          true to overwrite the previous document
   * @return true if the copy was sucessfull
   * @throws XWikiException
   *           if the document was not copied properly
   */
  public boolean copyDocument(String docname, String targetdocname, String sourceWiki,
      String targetWiki,
      String wikilanguage, boolean reset, boolean force) throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.copyDocument(docname, targetdocname, sourceWiki, targetWiki, wikilanguage,
          reset, force,
          true, getXWikiContext());
    }

    return false;
  }

  /**
   * Privileged API to copy a space to another wiki, optionally deleting all document of the target
   * space
   *
   * @param space
   *          source Space
   * @param sourceWiki
   *          source Wiki
   * @param targetWiki
   *          target Wiki
   * @param language
   *          language to copy
   * @param clean
   *          true to delete all document of the target space
   * @return number of copied documents
   * @throws XWikiException
   *           if the space was not copied properly
   */
  public int copySpaceBetweenWikis(String space, String sourceWiki, String targetWiki,
      String language, boolean clean)
      throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, clean,
          getXWikiContext());
    }

    return -1;
  }

  /**
   * API to include a topic into another The topic is rendered fully in the context of itself
   *
   * @param topic
   *          page name of the topic to include
   * @return the content of the included page
   * @throws XWikiException
   *           if the include failed
   */
  public String includeTopic(String topic) throws XWikiException {
    return includeTopic(topic, true);
  }

  /**
   * API to execute a form in the context of an including topic The rendering is evaluated in the
   * context of the
   * including topic All velocity variables are the one of the including topic This api is usually
   * called using
   * #includeForm in a page, which modifies the behavior of "Edit this page" button to direct for
   * Form mode (inline)
   *
   * @param topic
   *          page name of the form to execute
   * @return the content of the included page
   * @throws XWikiException
   *           if the include failed
   */
  public String includeForm(String topic) throws XWikiException {
    return includeForm(topic, true);
  }

  /**
   * API to include a topic into another, optionally surrounding the content with {pre}{/pre} to
   * avoid future wiki
   * rendering. The topic is rendered fully in the context of itself.
   *
   * @param topic
   *          page name of the topic to include
   * @param pre
   *          true to add {pre} {/pre} (only if includer document is 1.0 syntax)
   * @return the content of the included page
   * @throws XWikiException
   *           if the include failed
   */
  public String includeTopic(String topic, boolean pre) throws XWikiException {
    String result = this.xwiki.include(topic, false, getXWikiContext());

    if (pre) {
      String includerSyntax = this.xwiki.getCurrentContentSyntaxId(null, this.context);

      if ((includerSyntax != null) && XWikiDocument.XWIKI10_SYNTAXID.equals(includerSyntax)) {
        result = "{pre}" + result + "{/pre}";
      }
    }

    return result;
  }

  /**
   * API to execute a form in the context of an including topic, optionnaly surrounding the content
   * with {pre}{/pre}
   * to avoid future wiki rendering The rendering is evaluated in the context of the including topic
   * All velocity
   * variables are the one of the including topic This api is usually called using #includeForm in a
   * page, which
   * modifies the behavior of "Edit this page" button to direct for Form mode (inline).
   *
   * @param topic
   *          page name of the form to execute
   * @param pre
   *          true to add {pre} {/pre} (only if includer document is 1.0 syntax)
   * @return the content of the included page
   * @throws XWikiException
   *           if the include failed
   */
  public String includeForm(String topic, boolean pre) throws XWikiException {
    String result = this.xwiki.include(topic, true, getXWikiContext());

    if (pre) {
      String includerSyntax = this.xwiki.getCurrentContentSyntaxId(null, this.context);

      if ((includerSyntax != null) && XWikiDocument.XWIKI10_SYNTAXID.equals(includerSyntax)) {
        result = "{pre}" + result + "{/pre}";
      }
    }

    return result;
  }

  /**
   * API to check rights on the current document for the current user
   *
   * @param level
   *          right to check (view, edit, comment, delete)
   * @return true if right is granted/false if not
   */
  public boolean hasAccessLevel(String level) {
    return hasAccessLevel(level, getXWikiContext().getUser(),
        getXWikiContext().getDoc().getFullName());
  }

  /**
   * API to check rights on a document for a given user
   *
   * @param level
   *          right to check (view, edit, comment, delete)
   * @param user
   *          user for which to check the right
   * @param docname
   *          document on which to check the rights
   * @return true if right is granted/false if not
   */
  public boolean hasAccessLevel(String level, String user, String docname) {
    try {
      return this.xwiki.getRightService().hasAccessLevel(level, user, docname, getXWikiContext());
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * API to render a text in the context of a document
   *
   * @param text
   *          text to render
   * @param doc
   *          the text is evaluated in the content of this document
   * @return evaluated content
   * @throws XWikiException
   *           if the evaluation went wrong
   */
  public String renderText(String text, Document doc) throws XWikiException {
    return this.xwiki.getRenderingEngine().renderText(text, doc.getDoc(), getXWikiContext());
  }

  /**
   * API to render a chunk (difference between two versions
   *
   * @param chunk
   *          difference between versions to render
   * @param doc
   *          document to use as a context for rendering
   * @return resuilt of the rendering
   */
  public String renderChunk(Chunk chunk, Document doc) {
    return renderChunk(chunk, false, doc);
  }

  /**
   * API to render a chunk (difference between two versions
   *
   * @param chunk
   *          difference between versions to render
   * @param doc
   *          document to use as a context for rendering
   * @param source
   *          true to render the difference as wiki source and not as wiki rendered text
   * @return resuilt of the rendering
   */
  public String renderChunk(Chunk chunk, boolean source, Document doc) {
    StringBuffer buf = new StringBuffer();
    chunk.toString(buf, "", "\n");
    if (source) {
      return buf.toString();
    }

    try {
      return this.xwiki.getRenderingEngine().renderText(buf.toString(), doc.getDoc(),
          getXWikiContext());
    } catch (Exception e) {
      return buf.toString();
    }
  }

  /**
   * API to list the current spaces in thiswiki
   *
   * @return a list for strings reprenseting the spaces
   * @throws XWikiException
   *           if something went wrong
   */
  public List<String> getSpaces() throws XWikiException {
    return this.xwiki.getSpaces(getXWikiContext());
  }

  /**
   * API to list all documents in a space
   *
   * @param SpaceName
   *          space tolest
   * @return A list of strings to lest the document
   * @throws XWikiException
   *           if the loading went wrong
   */
  public List<String> getSpaceDocsName(String SpaceName) throws XWikiException {
    return this.xwiki.getSpaceDocsName(SpaceName, getXWikiContext());
  }

  /**
   * API to retrieve the current encoding of the wiki engine The encoding is stored in xwiki.cfg
   * Default encoding is
   * ISO-8891-1
   *
   * @return encoding active in this wiki
   */
  public String getEncoding() {
    return this.xwiki.getEncoding();
  }

  /**
   * API to retrieve the URL of an attached file in a Wiki Document The URL is generated differently
   * depending on the
   * environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by implementing
   * a new
   * XWikiURLFactory object For compatibility with any target environement (and especially the
   * portlet environment) It
   * is important to always use the URL functions to generate URL and never hardcode URLs
   *
   * @param fullname
   *          page name which includes the attached file
   * @param filename
   *          attached filename to create a link for
   * @return a URL as a string pointing to the filename
   * @throws XWikiException
   *           if the URL could not be generated properly
   */
  public String getAttachmentURL(String fullname, String filename) throws XWikiException {
    return this.xwiki.getAttachmentURL(fullname, filename, getXWikiContext());
  }

  /**
   * API to retrieve the URL of an a Wiki Document in view mode The URL is generated differently
   * depending on the
   * environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by implementing
   * a new
   * XWikiURLFactory object For compatibility with any target environement (and especially the
   * portlet environment) It
   * is important to always use the URL functions to generate URL and never hardcode URLs
   *
   * @param fullname
   *          the name of the document for which to return the URL for
   * @return a URL as a string pointing to the wiki document in view mode
   * @throws XWikiException
   *           if the URL could not be generated properly
   */
  public String getURL(String fullname) throws XWikiException {
    return this.xwiki.getURL(fullname, "view", getXWikiContext());
  }

  /**
   * API to retrieve the URL of an a Wiki Document in view mode The URL is generated differently
   * depending on the
   * environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by implementing
   * a new
   * XWikiURLFactory object For compatibility with any target environement (and especially the
   * portlet environment) It
   * is important to always use the URL functions to generate URL and never hardcode URLs
   *
   * @param reference
   *          the reference to the document for which to return the URL for
   * @return a URL as a string pointing to the wiki document in view mode
   * @throws XWikiException
   *           if the URL could not be generated properly
   * @since 2.3M2
   */
  public String getURL(DocumentReference reference) throws XWikiException {
    return this.xwiki.getURL(reference, "view", getXWikiContext());
  }

  /**
   * API to retrieve the URL of an a Wiki Document in any mode. The URL is generated differently
   * depending on the
   * environment (Servlet, Portlet, PDF, etc..). The URL generation can be modified by implementing
   * a new
   * XWikiURLFactory object For compatibility with any target environement (and especially the
   * portlet environment).
   * It is important to always use the URL functions to generate URL and never hardcode URLs.
   *
   * @param fullname
   *          the page name which includes the attached file
   * @param action
   *          the mode in which to access the document (view/edit/save/..). Any valid XWiki action
   *          is possible.
   * @return a URL as a string pointing to the wiki document in view mode
   * @throws XWikiException
   *           if the URL could not be generated properly
   */
  public String getURL(String fullname, String action) throws XWikiException {
    return this.xwiki.getURL(fullname, action, getXWikiContext());
  }

  /**
   * API to retrieve the URL of an a Wiki Document in any mode, optionally adding a query string The
   * URL is generated
   * differently depending on the environment (Servlet, Portlet, PDF, etc..) The URL generation can
   * be modified by
   * implementing a new XWikiURLFactory object. The query string will be modified to be added in the
   * way the
   * environment needs it. It is important to not add the query string parameter manually after a
   * URL. Some
   * environments will not accept this (like the Portlet environement).
   *
   * @param fullname
   *          the page name which includes the attached file
   * @param action
   *          the mode in which to access the document (view/edit/save/..). Any valid XWiki action
   *          is possible
   * @param querystring
   *          the Query String to provide in the usual mode (name1=value1&name2=value=2) including
   *          encoding
   * @return a URL as a string pointing to the wiki document in view mode
   * @throws XWikiException
   *           if the URL could not be generated properly
   */
  public String getURL(String fullname, String action, String querystring) throws XWikiException {
    return this.xwiki.getURL(fullname, action, querystring, getXWikiContext());
  }

  /**
   * API to retrieve the URL of an a Wiki Document in any mode, optionally adding an anchor. The URL
   * is generated
   * differently depending on the environement (Servlet, Portlet, PDF, etc..) The URL generation can
   * be modified by
   * implementing a new XWikiURLFactory object. The anchor will be modified to be added in the way
   * the environment
   * needs it. It is important to not add the anchor parameter manually after a URL. Some
   * environments will not accept
   * this (like the Portlet environement).
   *
   * @param fullname
   *          the page name which includes the attached file
   * @param action
   *          the mode in which to access the document (view/edit/save/..). Any valid XWiki action
   *          is possible
   * @param querystring
   *          the Query String to provide in the usual mode (name1=value1&name2=value=2) including
   *          encoding
   * @param anchor
   *          the anchor that points at a location within the passed document name
   * @return a URL as a string pointing to the wiki document in view mode
   * @throws XWikiException
   *           if the URL could not be generated properly
   */
  public String getURL(String fullname, String action, String querystring, String anchor)
      throws XWikiException {
    return this.xwiki.getURL(fullname, action, querystring, anchor, getXWikiContext());
  }

  /**
   * Deprecated API which was retrieving the SQL to represent the fullName Document field depending
   * on the database
   * used This is not needed anymore and returns 'doc.fullName' for all databases
   *
   * @deprecated
   * @return "doc.fullName"
   */
  @Deprecated
  public String getFullNameSQL() {
    return this.xwiki.getFullNameSQL();
  }

  /**
   * API to retrieve a link to the User Name page displayed for the first name and last name of the
   * user The link will
   * link to the page on the wiki where the user is registered (in virtual wiki mode)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getUserName(String user) {
    return this.xwiki.getUserName(user, null, getXWikiContext());
  }

  /**
   * API to retrieve a link to the User Name page displayed with a custom view The link will link to
   * the page on the
   * wiki where the user is registered (in virtual wiki mode) The formating is done using the format
   * parameter which
   * can contain velocity scripting and access all properties of the User profile using variables
   * ($first_name
   * $last_name $email $city)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @param format
   *          formatting to be used ("$first_name $last_name", "$first_name")
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getUserName(String user, String format) {
    return this.xwiki.getUserName(user, format, getXWikiContext());
  }

  /**
   * API to retrieve a link to the User Name page displayed for the first name and last name of the
   * user The link will
   * link to the page on the local wiki even if the user is registered on a different wiki (in
   * virtual wiki mode)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getLocalUserName(String user) {
    try {
      return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), null, getXWikiContext());
    } catch (Exception e) {
      return this.xwiki.getUserName(user, null, getXWikiContext());
    }
  }

  /**
   * API to retrieve a link to the User Name page displayed with a custom view The link will link to
   * the page on the
   * local wiki even if the user is registered on a different wiki (in virtual wiki mode) The
   * formating is done using
   * the format parameter which can contain velocity scripting and access all properties of the User
   * profile using
   * variables ($first_name $last_name $email $city)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @param format
   *          formatting to be used ("$first_name $last_name", "$first_name")
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getLocalUserName(String user, String format) {
    try {
      return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), format,
          getXWikiContext());
    } catch (Exception e) {
      return this.xwiki.getUserName(user, format, getXWikiContext());
    }
  }

  /**
   * API to retrieve a text representing the user with the first name and last name of the user With
   * the link param
   * set to false it will not link to the user page With the link param set to true, the link will
   * link to the page on
   * the wiki where the user was registered (in virtual wiki mode)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @param link
   *          false to not add an HTML link to the user profile
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getUserName(String user, boolean link) {
    return this.xwiki.getUserName(user, null, link, getXWikiContext());
  }

  /**
   * API to retrieve a text representing the user with a custom view With the link param set to
   * false it will not link
   * to the user page With the link param set to true, the link will link to the page on the wiki
   * where the user was
   * registered (in virtual wiki mode) The formating is done using the format parameter which can
   * contain velocity
   * scripting and access all properties of the User profile using variables ($first_name $last_name
   * $email $city)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @param format
   *          formatting to be used ("$first_name $last_name", "$first_name")
   * @param link
   *          false to not add an HTML link to the user profile
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getUserName(String user, String format, boolean link) {
    return this.xwiki.getUserName(user, format, link, getXWikiContext());
  }

  /**
   * API to retrieve a text representing the user with the first name and last name of the user With
   * the link param
   * set to false it will not link to the user page With the link param set to true, the link will
   * link to the page on
   * the local wiki even if the user is registered on a different wiki (in virtual wiki mode)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @param link
   *          false to not add an HTML link to the user profile
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getLocalUserName(String user, boolean link) {
    try {
      return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), null, link,
          getXWikiContext());
    } catch (Exception e) {
      return this.xwiki.getUserName(user, null, link, getXWikiContext());
    }
  }

  /**
   * API to retrieve a text representing the user with a custom view The formating is done using the
   * format parameter
   * which can contain velocity scripting and access all properties of the User profile using
   * variables ($first_name
   * $last_name $email $city) With the link param set to false it will not link to the user page
   * With the link param
   * set to true, the link will link to the page on the local wiki even if the user is registered on
   * a different wiki
   * (in virtual wiki mode)
   *
   * @param user
   *          Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
   * @param format
   *          formatting to be used ("$first_name $last_name", "$first_name")
   * @param link
   *          false to not add an HTML link to the user profile
   * @return The first name and last name fields surrounded with a link to the user page
   */
  public String getLocalUserName(String user, String format, boolean link) {
    try {
      return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), format, link,
          getXWikiContext());
    } catch (Exception e) {
      return this.xwiki.getUserName(user, format, link, getXWikiContext());
    }
  }

  public User getUser() {
    return this.xwiki.getUser(getXWikiContext());
  }

  public User getUser(String username) {
    return this.xwiki.getUser(username, getXWikiContext());
  }

  /**
   * API allowing to format a date according to the default Wiki setting The date format is provided
   * in the
   * 'dateformat' parameter of the XWiki Preferences
   *
   * @param date
   *          date object to format
   * @return A string with the date formating from the default Wiki setting
   */
  public String formatDate(Date date) {
    return this.xwiki.formatDate(date, null, getXWikiContext());
  }

  /**
   * API allowing to format a date according to a custom format The date format is from
   * java.text.SimpleDateFormat
   * Example: "dd/MM/yyyy HH:mm:ss" or "d MMM yyyy" If the format is invalid the default format will
   * be used to show
   * the date
   *
   * @param date
   *          date to format
   * @param format
   *          format of the date to be used
   * @return the formatted date
   * @see java.text.SimpleDateFormat
   */
  public String formatDate(Date date, String format) {
    return this.xwiki.formatDate(date, format, getXWikiContext());
  }

  /*
   * Allow to read user setting providing the user timezone All dates will be expressed with this
   * timezone @return the
   * timezone
   */
  public String getUserTimeZone() {
    return this.xwiki.getUserTimeZone(this.context);
  }

  /**
   * Returns a plugin from the plugin API. Plugin Rights can be verified. Note that although this
   * API is a duplicate
   * of {@link #getPlugin(String)} it used to provide an easy access from Velocity to XWiki plugins.
   * Indeed Velocity
   * has a feature in that if a class has a get method, using the dot notation will automatically
   * call the get method
   * for the class. See
   * http://velocity.apache.org/engine/releases/velocity-1.5/user-guide.html#propertylookuprules.
   * This this allows the following constructs: <code>$xwiki.pluginName.somePluginMethod()</code>
   *
   * @param name
   *          Name of the plugin to retrieve (either short of full class name)
   * @return a plugin object
   */
  public Api get(String name) {
    return this.xwiki.getPluginApi(name, getXWikiContext());
  }

  /**
   * Returns a plugin from the plugin API. Plugin Rights can be verified.
   *
   * @param name
   *          Name of the plugin to retrieve (either short of full class name)
   * @return a plugin object
   */
  public Api getPlugin(String name) {
    return this.xwiki.getPluginApi(name, getXWikiContext());
  }

  /**
   * Returns the list of Macros documents in the specified content
   *
   * @param defaultSpace
   *          Default space to use for relative path names
   * @param content
   *          Content to parse
   * @return ArrayList of document names
   */
  public List<String> getIncludedMacros(String defaultSpace, String content) {
    return this.xwiki.getIncludedMacros(defaultSpace, content, getXWikiContext());
  }

  /**
   * returns true if xwiki.readonly is set in the configuration file
   *
   * @return the value of xwiki.isReadOnly()
   * @see com.xpn.xwiki.XWiki
   */
  public boolean isReadOnly() {
    return this.xwiki.isReadOnly();
  }

  /**
   * Privileged API to set/unset the readonly status of the Wiki After setting this to true no
   * writing to the database
   * will be performed All Edit buttons will be removed and save actions disabled This is used for
   * maintenance
   * purposes
   *
   * @param ro
   *          true to set read-only mode/false to unset
   */
  public void setReadOnly(boolean ro) {
    if (hasAdminRights()) {
      this.xwiki.setReadOnly(ro);
    }
  }

  /**
   * Priviledge API to regenerate the links/backlinks table Normally links and backlinks are stored
   * when a page is
   * modified This function will regenerate all the backlinks This function can be long to run
   *
   * @throws XWikiException
   *           exception if the generation fails
   */
  public void refreshLinks() throws XWikiException {
    if (hasAdminRights()) {
      this.xwiki.refreshLinks(getXWikiContext());
    }
  }

  /**
   * API to check if the backlinks feature is active Backlinks are activated in xwiki.cfg or in the
   * XWiki Preferences
   *
   * @return true if the backlinks feature is active
   * @throws XWikiException
   *           exception if the preference could not be retrieved
   */
  public boolean hasBacklinks() throws XWikiException {
    return this.xwiki.hasBacklinks(getXWikiContext());
  }

  /**
   * API to check if the tags feature is active. Tags are activated in xwiki.cfg or in the XWiki
   * Preferences
   *
   * @return true if the tags feature is active, false otherwise
   * @throws XWikiException
   *           exception if the preference could not be retrieved
   */
  public boolean hasTags() throws XWikiException {
    return this.xwiki.hasTags(getXWikiContext());
  }

  /**
   * API to check if the edit comment feature is active Edit comments are activated in xwiki.cfg or
   * in the XWiki
   * Preferences
   *
   * @return
   */
  public boolean hasEditComment() {
    return this.xwiki.hasEditComment(this.context);
  }

  /**
   * API to check if the edit comment field is shown in the edit form Edit comments are activated in
   * xwiki.cfg or in
   * the XWiki Preferences
   *
   * @return
   */
  public boolean isEditCommentFieldHidden() {
    return this.xwiki.isEditCommentFieldHidden(this.context);
  }

  /**
   * API to check if the edit comment is suggested (prompted once by Javascript if empty) Edit
   * comments are activated
   * in xwiki.cfg or in the XWiki Preferences
   *
   * @return
   */
  public boolean isEditCommentSuggested() {
    return this.xwiki.isEditCommentSuggested(this.context);
  }

  /**
   * API to check if the edit comment is mandatory (prompted by Javascript if empty) Edit comments
   * are activated in
   * xwiki.cfg or in the XWiki Preferences
   *
   * @return
   */
  public boolean isEditCommentMandatory() {
    return this.xwiki.isEditCommentMandatory(this.context);
  }

  /**
   * API to check if the minor edit feature is active minor edit is activated in xwiki.cfg or in the
   * XWiki Preferences
   */
  public boolean hasMinorEdit() {
    return this.xwiki.hasMinorEdit(this.context);
  }

  /**
   * API to check if the recycle bin feature is active recycle bin is activated in xwiki.cfg or in
   * the XWiki
   * Preferences
   */
  public boolean hasRecycleBin() {
    return this.xwiki.hasRecycleBin(this.context);
  }

  /**
   * API to rename a page (experimental) Rights are necessary to edit the source and target page All
   * objects and
   * attachments ID are modified in the process to link to the new page name
   *
   * @param doc
   *          page to rename
   * @param newFullName
   *          target page name to move the information to
   * @throws XWikiException
   *           exception if the rename fails
   */
  public boolean renamePage(Document doc, String newFullName) {
    try {
      if (this.xwiki.exists(newFullName, getXWikiContext())
          && !this.xwiki.getRightService().hasAccessLevel("delete", getXWikiContext().getUser(),
              newFullName,
              getXWikiContext())) {
        return false;
      }
      if (this.xwiki.getRightService().hasAccessLevel("edit", getXWikiContext().getUser(),
          doc.getFullName(),
          getXWikiContext())) {
        this.xwiki.renamePage(doc.getFullName(), newFullName, getXWikiContext());
      }
    } catch (XWikiException e) {
      return false;
    }

    return true;
  }

  /**
   * Retrieves the current editor preference for the request The preference is first looked up in
   * the user preference
   * and then in the space and wiki preference
   *
   * @return "wysiwyg" or "text"
   */
  public String getEditorPreference() {
    return this.xwiki.getEditorPreference(getXWikiContext());
  }

  /**
   * Privileged API to retrieve an object instantiated from groovy code in a String. Note that
   * Groovy scripts
   * compilation is cached.
   *
   * @param script
   *          the Groovy class definition string (public class MyClass { ... })
   * @return An object instantiating this class
   * @throws XWikiException
   */
  public java.lang.Object parseGroovyFromString(String script) throws XWikiException {
    if (hasProgrammingRights()) {
      return this.xwiki.parseGroovyFromString(script, getXWikiContext());
    }
    return "groovy_missingrights";
  }

  /**
   * Privileged API to retrieve an object instantiated from groovy code in a String, using a
   * classloader including all
   * JAR files located in the passed page as attachments. Note that Groovy scripts compilation is
   * cached
   *
   * @param script
   *          the Groovy class definition string (public class MyClass { ... })
   * @return An object instantiating this class
   * @throws XWikiException
   */
  public java.lang.Object parseGroovyFromPage(String script, String jarWikiPage)
      throws XWikiException {
    XWikiDocument doc = this.xwiki.getDocument(script, getXWikiContext());
    if (this.xwiki.getRightService().hasProgrammingRights(doc, getXWikiContext())) {
      return this.xwiki.parseGroovyFromString(doc.getContent(), jarWikiPage, getXWikiContext());
    }
    return "groovy_missingrights";
  }

  /**
   * Privileged API to retrieve an object instanciated from groovy code in a String Groovy scripts
   * compilation is
   * cached
   *
   * @param fullname
   *          // script containing a Groovy class definition (public class MyClass { ... })
   * @return An object instanciating this class
   * @throws XWikiException
   */
  public java.lang.Object parseGroovyFromPage(String fullname) throws XWikiException {
    XWikiDocument doc = this.xwiki.getDocument(fullname, getXWikiContext());
    if (this.xwiki.getRightService().hasProgrammingRights(doc, getXWikiContext())) {
      return this.xwiki.parseGroovyFromString(doc.getContent(), getXWikiContext());
    }
    return "groovy_missingrights";
  }

  /**
   * API to get the macro list from the XWiki Preferences The macro list are the macros available
   * from the Macro
   * Mapping System
   *
   * @return String with each macro on each line
   */
  public String getMacroList() {
    return this.xwiki.getMacroList(getXWikiContext());
  }

  /**
   * API to check if using which toolbars in Wysiwyg editor
   *
   * @return a string value
   */
  public String getWysiwygToolbars() {
    return this.xwiki.getWysiwygToolbars(getXWikiContext());
  }

  /**
   * API to create an object from the request The parameters are the ones that are created from
   * doc.display("field","edit") calls
   *
   * @param className
   *          XWiki Class Name to create the object from
   * @return a BaseObject wrapped in an Object
   * @throws XWikiException
   *           exception if the object could not be read
   */
  public com.xpn.xwiki.api.Object getObjectFromRequest(String className) throws XWikiException {
    return new com.xpn.xwiki.api.Object(
        this.xwiki.getObjectFromRequest(className, getXWikiContext()),
        getXWikiContext());
  }

  /**
   * API to create an empty document
   *
   * @return an XWikiDocument wrapped in a Document
   */
  public Document createDocument() {
    return new XWikiDocument().newDocument(getXWikiContext());
  }

  /**
   * API to convert the username depending on the configuration The username can be converted from
   * email to a valid
   * XWiki page name hidding the email address The username can be then used to login and link to
   * the right user page
   *
   * @param username
   *          username to use for login
   * @return converted wiki page name for this username
   */
  public String convertUsername(String username) {
    return this.xwiki.convertUsername(username, getXWikiContext());
  }

  /**
   * API to get the Property object from a class based on a property path A property path looks like
   * XWiki.ArticleClass_fieldname
   *
   * @param propPath
   *          Property path
   * @return a PropertyClass object from a BaseClass object
   */
  public com.xpn.xwiki.api.PropertyClass getPropertyClassFromName(String propPath) {
    return new PropertyClass(this.xwiki.getPropertyClassFromName(propPath, getXWikiContext()),
        getXWikiContext());
  }

  /**
   * Generates a unique page name based on initial page name and already existing pages
   *
   * @param name
   * @return a unique page name
   */
  public String getUniquePageName(String name) {
    return this.xwiki.getUniquePageName(name, getXWikiContext());
  }

  /**
   * Generates a unique page name based on initial page name and already existing pages
   *
   * @param space
   * @param name
   * @return a unique page name
   */
  public String getUniquePageName(String space, String name) {
    return this.xwiki.getUniquePageName(space, name, getXWikiContext());
  }

  /**
   * Inserts a tooltip using toolTip.js
   *
   * @param html
   *          HTML viewed
   * @param message
   *          HTML Tooltip message
   * @param params
   *          Parameters in Javascropt added to the tooltip config
   * @return HTML with working tooltip
   */
  public String addTooltip(String html, String message, String params) {
    return this.xwiki.addTooltip(html, message, params, getXWikiContext());
  }

  /**
   * Inserts a tooltip using toolTip.js
   *
   * @param html
   *          HTML viewed
   * @param message
   *          HTML Tooltip message
   * @return HTML with working tooltip
   */
  public String addTooltip(String html, String message) {
    return this.xwiki.addTooltip(html, message, getXWikiContext());
  }

  /**
   * Inserts the tooltip Javascript
   *
   * @return
   */
  public String addTooltipJS() {
    return this.xwiki.addTooltipJS(getXWikiContext());
  }

  /*
   * Inserts a Mandatory asterix
   */
  public String addMandatory() {
    return this.xwiki.addMandatory(getXWikiContext());
  }

  /**
   * Get the XWiki Class object defined in the passed Document name.
   * <p>
   * Note: This method doesn't require any rights for accessing the passed Document (as opposed to
   * the
   * {@link com.xpn.xwiki.api.Document#getClass()} method which does require to get a Document
   * object first. This is
   * thus useful in cases where the calling code doesn't have the access right to the specified
   * Document. It is safe
   * because there are no sensitive data stored in a Class definition.
   * </p>
   *
   * @param documentName
   *          the name of the document for which to get the Class object. For example
   *          "XWiki.XWikiPreferences"
   * @return the XWiki Class object defined in the passed Document name. If the passed Document name
   *         points to a
   *         Document with no Class defined then an empty Class object is returned (i.e. a Class
   *         object with no
   *         properties).
   * @throws XWikiException
   *           if the passed document name doesn't point to a valid Document
   */
  public Class getClass(String documentName) throws XWikiException {
    // TODO: The implementation should be done in com.xpn.xwiki.XWiki as this class should
    // delegate all implementations to that Class.
    return new Class(this.xwiki.getDocument(documentName, this.context).getXClass(), this.context);
  }

  /**
   * Provides an absolute counter
   *
   * @param name
   *          Counter name
   * @return String
   */
  public String getCounter(String name) {
    XWikiEngineContext econtext = this.context.getEngineContext();
    Integer counter = (Integer) econtext.getAttribute(name);
    if (counter == null) {
      counter = new Integer(0);
    }
    counter = new Integer(counter.intValue() + 1);
    econtext.setAttribute(name, counter);

    return counter.toString();
  }

  /**
   * Check authentication from request and set according persitent login information If it fails
   * user is unlogged
   *
   * @return null if failed, non null XWikiUser if sucess
   * @throws XWikiException
   */
  public XWikiUser checkAuth() throws XWikiException {
    return this.context.getWiki().getAuthService().checkAuth(this.context);
  }

  /**
   * Check authentication from username and password and set according persitent login information
   * If it fails user is
   * unlogged
   *
   * @param username
   *          username to check
   * @param password
   *          password to check
   * @param rememberme
   *          "1" if you want to remember the login accross navigator restart
   * @return null if failed, non null XWikiUser if sucess
   * @throws XWikiException
   */
  public XWikiUser checkAuth(String username, String password, String rememberme)
      throws XWikiException {
    return this.context.getWiki().getAuthService().checkAuth(username, password, rememberme,
        this.context);
  }

  /**
   * API to get the xwiki criteria service which allow to create various criteria : integer ranges,
   * date periods, date
   * intervals, etc.
   *
   * @return the xwiki criteria service
   */
  public CriteriaService getCriteriaService() {
    return this.criteriaService;
  }

  /**
   * @return the ids of configured syntaxes for this wiki (eg "xwiki/1.0", "xwiki/2.0",
   *         "mediawiki/1.0", etc)
   */
  public List<String> getConfiguredSyntaxes() {
    return this.xwiki.getConfiguredSyntaxes();
  }

  /**
   * @return secure {@link QueryManager} for execute queries to store.
   * @deprecated since XE 2.4M2 use the Query Manager Script Service
   */
  @Deprecated
  public QueryManager getQueryManager() {
    return Utils.getComponent(QueryManager.class, "secure");
  }

  /**
   * API to get the Servlet path for a given wiki. In mono wiki this is "bin/" or "xwiki/".
   *
   * @param wikiName
   *          wiki for which to get the path
   * @return The servlet path
   */
  public String getServletPath(String wikiName) {
    return this.xwiki.getServletPath(wikiName, this.context);
  }

  /**
   * API to get the Servlet path for the current wiki. In mono wiki this is "bin/" or "xwiki/".
   *
   * @return The servlet path
   */
  public String getServletPath() {
    return this.xwiki.getServletPath(this.context.getDatabase(), this.context);
  }

  /**
   * API to get the webapp path for the current wiki. This usually is "xwiki/". It can be configured
   * in xwiki.cfg with
   * the config <tt>xwiki.webapppath</tt>.
   *
   * @return The servlet path
   */
  @Deprecated
  public String getWebAppPath() {
    return this.xwiki.getWebAppPath(this.context);
  }

  /**
   * @return the syntax id of the syntax to use when creating new documents.
   */
  public String getDefaultDocumentSyntax() {
    return this.xwiki.getDefaultDocumentSyntax();
  }

  /**
   * Find the corresponding available renderer syntax.
   * <p>
   * If <code>syntaxVersion</code> is null the last version of the available provided syntax type is
   * returned.
   *
   * @param syntaxType
   *          the syntax type
   * @param syntaxVersion
   *          the syntax version
   * @return the available corresponding {@link Syntax}. Null if no available renderer can be found.
   */
  public Syntax getAvailableRendererSyntax(String syntaxType, String syntaxVersion) {
    Syntax syntax = null;

    List<PrintRendererFactory> factories = Utils.getComponentList(PrintRendererFactory.class);
    for (PrintRendererFactory factory : factories) {
      Syntax factorySyntax = factory.getSyntax();
      if (syntaxVersion != null) {
        if (factorySyntax.getType().getId().equalsIgnoreCase(syntaxType)
            && factorySyntax.getVersion().equals(syntaxVersion)) {
          syntax = factorySyntax;
          break;
        }
      } else {
        // TODO: improve version comparaison since it does not work when comparing 2.0 and 10.0 for
        // example. We
        // should have a Version which implements Comparable like we have SyntaxId in Syntax
        if (factorySyntax.getType().getId().equalsIgnoreCase(syntaxType)
            && ((syntax == null)
                || (factorySyntax.getVersion().compareTo(syntax.getVersion()) > 0))) {
          syntax = factorySyntax;
        }
      }
    }

    return syntax;
  }

  /**
   * @return the section depth for which section editing is available (can be configured through
   *         {@code xwiki.section.depth} configuration property. Defaults to 2 when not defined
   */
  public long getSectionEditingDepth() {
    return this.xwiki.getSectionEditingDepth();
  }

  /**
   * @return true if title handling should be using the compatibility mode or not. When the
   *         compatibility mode is
   *         active, if the document's content first header (level 1 or level 2) matches the
   *         document's title the
   *         first header is stripped.
   */
  public boolean isTitleInCompatibilityMode() {
    return this.xwiki.isTitleInCompatibilityMode();
  }

  /**
   * Get the syntax of the document currently being executed.
   * <p>
   * The document currently being executed is not the same than the context document since when
   * including a page with
   * velocity #includeForm(), method for example the context doc is the includer document even if
   * includeForm() fully
   * execute and render the included document before insert it in the includer document.
   * <p>
   * If the current document can't be found, the method assume that the executed document is the
   * context document
   * (it's generally the case when a document is directly rendered with
   * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
   *
   * @return the syntax identifier
   */
  public String getCurrentContentSyntaxId() {
    return this.xwiki.getCurrentContentSyntaxId(getXWikiContext());
  }

  // START ApiCompatibilityAspect
  /**
   * @return true if the current user has the Programming right or false otherwise
   * @deprecated use {@link Api#hasProgrammingRights()} instead
   */
  @Deprecated
  public boolean checkProgrammingRights() {
    return this.hasProgrammingRights();
  }
  // END ApiCompatibilityAspect

  // START XWikiCompatibilityAspect
  /**
   * Utility methods have been moved in version 1.3 Milestone 2 to the {@link Util} class.
   * However to preserve backward compatibility we have deprecated them in this class and
   * not removed them yet. All calls are funnelled through this class variable.
   */

  /**
   * API to protect Text from Wiki transformation
   *
   * @param text
   * @return escaped text
   * @deprecated replaced by Util#escapeText since 1.3M2
   */
  @Deprecated
  public String escapeText(String text) {
    return this.util.escapeText(text);
  }

  /**
   * API to protect URLs from Wiki transformation
   *
   * @param url
   * @return encoded URL
   * @deprecated replaced by Util#escapeURL since 1.3M2
   */
  @Deprecated
  public String escapeURL(String url) {
    return this.util.escapeURL(url);
  }

  /**
   * @deprecated use {@link #getLanguagePreference()} instead
   */
  @Deprecated
  public String getDocLanguagePreference() {
    return xwiki.getDocLanguagePreference(getXWikiContext());
  }

  /**
   * Privileged API to send a message to an email address
   *
   * @param sender
   *          email of the sender of the message
   * @param recipient
   *          email of the recipient of the message
   * @param message
   *          Message to send
   * @throws XWikiException
   *           if the mail was not send successfully
   * @deprecated replaced by the
   *             <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
   *             Plugin</a> since 1.3M2
   */
  @Deprecated
  public void sendMessage(String sender, String recipient, String message)
      throws XWikiException {
    if (hasProgrammingRights()) {
      xwiki.sendMessage(sender, recipient, message, getXWikiContext());
    }
  }

  /**
   * Privileged API to send a message to an email address
   *
   * @param sender
   *          email of the sender of the message
   * @param recipient
   *          emails of the recipients of the message
   * @param message
   *          Message to send
   * @throws XWikiException
   *           if the mail was not send successfully
   * @deprecated replaced by the
   *             <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
   *             Plugin</a> since 1.3M2
   */
  @Deprecated
  public void sendMessage(String sender, String[] recipient, String message)
      throws XWikiException {
    if (hasProgrammingRights()) {
      xwiki.sendMessage(sender, recipient, message, getXWikiContext());
    }
  }

  /**
   * @return the current date
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getDate()} since 1.3M2
   */
  @Deprecated
  public Date getCurrentDate() {
    return this.util.getDate();
  }

  /**
   * @return the current date
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getDate()} since 1.3M2
   */
  @Deprecated
  public Date getDate() {
    return this.util.getDate();
  }

  /**
   * @param time
   *          the time in milliseconds
   * @return the time delta in milliseconds between the current date and the time passed
   *         as parameter
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getTimeDelta(long)} since 1.3M2
   */
  @Deprecated
  public int getTimeDelta(long time) {
    return this.util.getTimeDelta(time);
  }

  /**
   * @param time
   *          time in milliseconds since 1970, 00:00:00 GMT
   * @return Date a date from a time in milliseconds since 01/01/1970 as a
   *         Java {@link Date} Object
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getDate(long)} since 1.3M2
   */
  @Deprecated
  public Date getDate(long time) {
    return this.util.getDate(time);
  }

  /**
   * Split a text to an array of texts, according to a separator.
   *
   * @param text
   *          the original text
   * @param sep
   *          the separator characters. The separator is one or more of the
   *          separator characters
   * @return An array containing the split text
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#split(String, String)} since 1.3M2
   */
  @Deprecated
  public String[] split(String text, String sep) {
    return this.util.split(text, sep);
  }

  /**
   * Get a stack trace as a String
   *
   * @param e
   *          the exception to convert to a String
   * @return the exception stack trace as a String
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#printStrackTrace(Throwable)}
   *             since 1.3M2
   */
  @Deprecated
  public String printStrackTrace(Throwable e) {
    return this.util.printStrackTrace(e);
  }

  /**
   * Get a Null object. This is useful in Velocity where there is no real null object
   * for comparaisons.
   *
   * @return a Null Object
   * @deprecated replaced by {@link Util#getNull()} since 1.3M2
   */
  @Deprecated
  public Object getNull() {
    return this.util.getNull();
  }

  /**
   * Get a New Line character. This is useful in Velocity where there is no real new
   * line character for inclusion in texts.
   *
   * @return a new line character
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getNewline()} since 1.3M2
   */
  @Deprecated
  public String getNl() {
    return this.util.getNewline();
  }

  /**
   * Creates an Array List. This is useful from Velocity since you cannot
   * create Object from Velocity with our secure uberspector.
   *
   * @return a {@link ArrayList} object
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getArrayList()} since 1.3M2
   */
  @Deprecated
  public List getArrayList() {
    return this.util.getArrayList();
  }

  /**
   * Creates a Hash Map. This is useful from Velocity since you cannot
   * create Object from Velocity with our secure uberspector.
   *
   * @return a {@link HashMap} object
   * @deprecated replaced by {@link Util#getHashMap()} since 1.3M2
   */
  @Deprecated
  public Map getHashMap() {
    return this.util.getHashMap();
  }

  /**
   * Creates a Tree Map. This is useful from Velocity since you cannot
   * create Object from Velocity with our secure uberspector.
   *
   * @return a {@link TreeMap} object
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getTreeMap()} since 1.3M2
   */
  @Deprecated
  public Map getTreeMap() {
    return this.util.getTreeMap();
  }

  /**
   * Sort a list using a standard comparator. Elements need to be mutally comparable and
   * implement the Comparable interface.
   *
   * @param list
   *          the list to sort
   * @return the sorted list (as the same oject reference)
   * @see {@link java.util.Collections#sort(java.util.List)}
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#sort(java.util.List)} since 1.3M2
   */
  @Deprecated
  public List sort(List list) {
    return this.util.sort(list);
  }

  /**
   * Convert an Object to a number and return null if the object is not a Number.
   *
   * @param object
   *          the object to convert
   * @return the object as a {@link Number}
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#toNumber(Object)} since 1.3M2
   */
  @Deprecated
  public Number toNumber(Object object) {
    return this.util.toNumber(object);
  }

  /**
   * Generate a random string.
   *
   * @param size
   *          the desired size of the string
   * @return the randomly generated string
   * @deprecated replaced by {@link com.xpn.xwiki.api.Util#generateRandomString(int)}
   *             since 1.3M2
   */
  @Deprecated
  public String generateRandomString(int size) {
    return this.util.generateRandomString(size);
  }

  /**
   * Output a BufferedImage object into the response outputstream.
   * Once this method has been called, not further action is possible.
   * Users should set $context.setFinished(true) to
   * avoid template output The image is outpout as image/jpeg.
   *
   * @param image
   *          the BufferedImage to output
   * @throws java.io.IOException
   *           if the output fails
   * @deprecated replaced by
   *             {@link com.xpn.xwiki.api.Util#outputImage(java.awt.image.BufferedImage)}
   *             since 1.3M2
   */
  @Deprecated
  public void outputImage(BufferedImage image) throws IOException {
    this.util.outputImage(image);
  }

  /**
   * @param str
   *          the String to convert to an integer
   * @return the parsed integer or zero in case of exception
   * @deprecated replaced by {@link Util#parseInt(String)} since 1.3M2
   */
  @Deprecated
  public int parseInt(String str) {
    return this.util.parseInt(str);
  }

  /**
   * @param str
   *          the String to convert to an Integer Object
   * @return the parsed integer or zero in case of exception
   * @deprecated replaced by {@link Util#parseInteger(String)} since 1.3M2
   */
  @Deprecated
  public Integer parseInteger(String str) {
    return this.util.parseInteger(str);
  }

  /**
   * @param str
   *          the String to convert to a long
   * @return the parsed long or zero in case of exception
   * @deprecated replaced by {@link Util#parseLong(String)} since 1.3M2
   */
  @Deprecated
  public long parseLong(String str) {
    return this.util.parseLong(str);
  }

  /**
   * @param str
   *          the String to convert to a float
   * @return the parsed float or zero in case of exception
   * @deprecated replaced by {@link Util#parseFloat(String)} since 1.3M2
   */
  @Deprecated
  public float parseFloat(String str) {
    return this.util.parseFloat(str);
  }

  /**
   * @param str
   *          the String to convert to a double
   * @return the parsed double or zero in case of exception
   * @deprecated replaced by {@link Util#parseDouble(String)} since 1.3M2
   */
  @Deprecated
  public double parseDouble(String str) {
    return this.util.parseDouble(str);
  }

  /**
   * Escape text so that it can be used in a like clause or in a test for equality clause.
   * For example it escapes single quote characters.
   *
   * @param text
   *          the text to escape
   * @return filtered text
   * @deprecated replaced by {@link Util#escapeSQL(String)} since 1.3M2
   */
  @Deprecated
  public String sqlfilter(String text) {
    return this.util.escapeSQL(text);
  }

  /**
   * Cleans up the passed text by removing all accents and special characters to make it
   * a valid page name.
   *
   * @param name
   *          the page name to normalize
   * @return the valid page name
   * @deprecated replaced by {@link Util#clearName(String)} since 1.3M2
   */
  @Deprecated
  public String clearName(String name) {
    return this.util.clearName(name);
  }

  /**
   * Replace all accents by their alpha equivalent.
   *
   * @param text
   *          the text to parse
   * @return a string with accents replaced with their alpha equivalent
   * @deprecated replaced by {@link Util#clearAccents(String)} since 1.3M2
   */
  @Deprecated
  public String clearAccents(String text) {
    return this.util.clearAccents(text);
  }

  /**
   * Add a and b because Velocity operations are not always working.
   *
   * @param a
   *          an integer to add
   * @param b
   *          an integer to add
   * @return the sum of a and b
   * @deprecated replaced by {@link Util#add(int, int)} since 1.3M2
   */
  @Deprecated
  public int add(int a, int b) {
    return this.util.add(a, b);
  }

  /**
   * Add a and b because Velocity operations are not working with longs.
   *
   * @param a
   *          a long to add
   * @param b
   *          a long to add
   * @return the sum of a and b
   * @deprecated replaced by {@link Util#add(long, long)} since 1.3M2
   */
  @Deprecated
  public long add(long a, long b) {
    return this.util.add(a, b);
  }

  /**
   * Add a and b where a and b are non decimal numbers specified as Strings.
   *
   * @param a
   *          a string representing a non decimal number
   * @param b
   *          a string representing a non decimal number
   * @return the sum of a and b as a String
   * @deprecated replaced by {@link Util#add(String, String)} since 1.3M2
   */
  @Deprecated
  public String add(String a, String b) {
    return this.util.add(a, b);
  }

  /**
   * Transform a text in a URL compatible text
   *
   * @param content
   *          text to transform
   * @return encoded result
   * @deprecated replaced by {@link Util#encodeURI(String)} since 1.3M2
   */
  @Deprecated
  public String getURLEncoded(String content) {
    return this.util.encodeURI(content);
  }

  /**
   * @return true for multi-wiki/false for mono-wiki
   * @deprecated replaced by {@link XWiki#isVirtualMode()} since 1.4M1.
   */
  @Deprecated
  public boolean isVirtual() {
    return this.isVirtualMode();
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpaceCopyright()} since 2.3M1
   */
  @Deprecated
  public String getWebCopyright() {
    return this.getSpaceCopyright();
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreference(String)} since 2.3M1
   */
  @Deprecated
  public String getWebPreference(String preference) {
    return this.getSpacePreference(preference);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreferenceFor(String, String)} since 2.3M1
   */
  @Deprecated
  public String getWebPreferenceFor(String preference, String space) {
    return this.getSpacePreferenceFor(preference, space);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreferenceFor(String, String, String)} since 2.3M1
   */
  @Deprecated
  public String getWebPreferenceFor(String preference, String space, String defaultValue) {
    return this.getSpacePreferenceFor(preference, space, defaultValue);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String, long)} since 2.3M1
   */
  @Deprecated
  public long getWebPreferenceAsLong(String preference, long defaultValue) {
    return this.getSpacePreferenceAsLong(preference, defaultValue);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String)} since 2.3M1
   */
  @Deprecated
  public long getWebPreferenceAsLong(String preference) {
    return this.getSpacePreferenceAsLong(preference);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String, int)} since 2.3M1
   */
  @Deprecated
  public int getWebPreferenceAsInt(String preference, int defaultValue) {
    return this.getSpacePreferenceAsInt(preference, defaultValue);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreference(String, String)} since 2.3M1
   */
  @Deprecated
  public String getWebPreference(String preference, String defaultValue) {
    return this.getSpacePreference(preference, defaultValue);
  }

  /**
   * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String)} since 2.3M1
   */
  @Deprecated
  public int getWebPreferenceAsInt(String preference) {
    return this.getSpacePreferenceAsInt(preference);
  }

  /**
   * @deprecated replaced by
   *             {@link XWiki#copySpaceBetweenWikis(String, String, String, String, boolean)} since
   *             2.3M1
   */
  @Deprecated
  public int copyWikiWeb(String space, String sourceWiki, String targetWiki, String language,
      boolean clean)
      throws XWikiException {
    return this.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, clean);
  }

  /**
   * API to parse the message being stored in the Context. A message can be an error message or an
   * information message
   * either as text or as a message ID pointing to ApplicationResources. The message is also parse
   * for velocity scripts
   *
   * @return Final message
   * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. From velocity you can
   *             access XWikiMessageTool
   *             with $msg binding.
   */
  @Deprecated
  public String parseMessage() {
    return this.xwiki.parseMessage(getXWikiContext());
  }

  /**
   * API to parse a message. A message can be an error message or an information message either as
   * text or as a message
   * ID pointing to ApplicationResources. The message is also parse for velocity scripts
   *
   * @return Final message
   * @param id
   * @return the result of the parsed message
   * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. From velocity you can
   *             access XWikiMessageTool
   *             with $msg binding.
   */
  @Deprecated
  public String parseMessage(String id) {
    return this.xwiki.parseMessage(id, getXWikiContext());
  }

  /**
   * API to get a message. A message can be an error message or an information message either as
   * text or as a message
   * ID pointing to ApplicationResources. The message is also parsed for velocity scripts
   *
   * @return Final message
   * @param id
   * @return the result of the parsed message
   * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. From velocity you can
   *             access XWikiMessageTool
   *             with $msg binding.
   */
  @Deprecated
  public String getMessage(String id) {
    return this.xwiki.getMessage(id, getXWikiContext());
  }

  // ENDXWikiCompatibilityAspect

}
