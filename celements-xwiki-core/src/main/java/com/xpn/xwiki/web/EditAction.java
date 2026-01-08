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
package com.xpn.xwiki.web;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class EditAction extends XWikiAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(EditAction.class);

  @Override
  public String render(XWikiContext context) throws XWikiException {
    XWikiRequest request = context.getRequest();
    String content = request.getParameter("content");
    String title = request.getParameter("title");
    XWikiDocument doc = context.getDoc();
    XWiki xwiki = getXWiki();
    XWikiForm form = context.getForm();
    VelocityContext vcontext = (VelocityContext) context.get("vcontext");

    boolean translationLoaded = false;
    if (doc != context.get("tdoc")) {
      translationLoaded = true;
    }

    XWikiDocument tdocDebug = (XWikiDocument) context.get("tdoc");
    if (tdocDebug != null) {
      LOGGER.debug("starting edit action with doc.defLang/lang={}/{}, tdoc.defLang/lang={}/{},"
          + " translationLoaded={}", doc.getDefaultLanguage(), doc.getLanguage(),
          tdocDebug.getDefaultLanguage(), tdocDebug.getLanguage(), translationLoaded);
    } else {
      LOGGER.debug("starting edit action with doc.defLang/lang={}/{}, tdoc=null,"
          + " translationLoaded={}", doc.getDefaultLanguage(), doc.getLanguage(),
          translationLoaded);
    }

    // we need to clone so that nothing happens in memory
    if (doc.isFromCache()) {
      doc = doc.clone();
      doc.setFromCache(false);
    }
    context.put("doc", doc);
    vcontext.put("doc", doc.newDocument(context));

    // Check for edit section
    String sectionContent = "";
    Integer sectionNumber = 0;
    if ((request.getParameter("section") != null) && xwiki.hasSectionEdit(context)) {
      sectionNumber = NumberUtils.toInt(request.getParameter("section"));
      sectionContent = doc.getContentOfSection(sectionNumber);
    }
    vcontext.put("sectionNumber", sectionNumber);

    synchronized (doc) {
      XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
      EditForm peform = (EditForm) form;
      String parent = peform.getParent();
      if (parent != null) {
        doc.setParent(parent);
      }
      String creator = peform.getCreator();
      if (creator != null) {
        doc.setCreator(creator);
      }
      String defaultTemplate = peform.getDefaultTemplate();
      if (defaultTemplate != null) {
        doc.setDefaultTemplate(defaultTemplate);
      }
      String defaultLanguage = peform.getDefaultLanguage();
      if ((defaultLanguage != null) && !defaultLanguage.equals("")) {
        doc.setDefaultLanguage(defaultLanguage);
      }
      if (doc.isNew() && doc.getDefaultLanguage().equals("")) {
        doc.setDefaultLanguage(getXWiki().getLanguagePreference(context));
      }

      String language = getXWiki().getLanguagePreference(context);
      String languagefromrequest = context.getRequest().getParameter("language");
      languagefromrequest = (languagefromrequest == null) ? "" : languagefromrequest;
      String languagetoedit = languagefromrequest.equals("") ? language : languagefromrequest;

      // if no specific language is set or if it is "default" then we edit the current doc
      if ((languagetoedit == null) || (languagetoedit.equals("default"))) {
        languagetoedit = "";
      }
      // if the document is new then we edit it as the default
      // if the language to edit is the one of the default document then the language is the
      // default
      if (doc.isNew() || (doc.getDefaultLanguage().equals(languagetoedit))) {
        languagetoedit = "";
      }
      // if the doc does not exist in the language to edit and the language was not
      // explicitely set in the URL
      // then we edit the default doc, otherwise this can cause to create translations without
      // wanting it.
      if ((!translationLoaded) && languagefromrequest.equals("")) {
        languagetoedit = "";
      }

      if (languagetoedit.equals("")) {
        // In this case edit the default document (sanitize if isNew)
        tdoc = doc;
        context.put("tdoc", doc);
        vcontext.put("tdoc", vcontext.get("doc"));
        if (doc.isNew()) {
          doc.setDefaultLanguage(language);
          doc.setLanguage("");
        }
        LOGGER.debug("edit action for default doc with doc.defLang/lang={}/{},"
            + " tdoc.defLang/lang={}/{}, tdoc.isTrans={}, doc.isNew={}",
            doc.getDefaultLanguage(), doc.getLanguage(),
            tdoc.getDefaultLanguage(), tdoc.getLanguage(), tdoc.isTrans(), doc.isNew());
      } else {
        // If the translated doc object is the same as the doc object
        // this means the translated doc did not exists so we need to create it
        if ((!translationLoaded) && getXWiki().isMultiLingual(context)) {
          tdoc = doc.getTranslatedDocument(languagetoedit, context);
          if (tdoc.isFromCache()) {
            tdoc = tdoc.clone();
            tdoc.setFromCache(false);
          }
          tdoc.setLanguage(languagetoedit);
          tdoc.setTitle(doc.getTitle());
          tdoc.setContent(doc.getContent());
          context.put("tdoc", tdoc);
          vcontext.put("tdoc", tdoc.newDocument(context));
          LOGGER.debug("edit action after creating translated doc with doc.defLang/lang={}/{},"
              + " tdoc.defLang/lang={}/{}, tdoc.isTrans={}",
              doc.getDefaultLanguage(), doc.getLanguage(),
              tdoc.getDefaultLanguage(), tdoc.getLanguage(), tdoc.isTrans());
        }
      }

      XWikiDocument tdoc2 = tdoc;
      if (tdoc2.isFromCache()) {
        tdoc2.clone();
        tdoc2.setFromCache(false);
      }
      if (content != null) {
        tdoc2.setContent(content);
        tdoc2.setTitle(title);
      }
      if ((sectionContent != null) && !sectionContent.equals("")) {
        if (content != null) {
          tdoc2.setContent(content);
        } else {
          tdoc2.setContent(sectionContent);
        }
        if (title != null) {
          tdoc2.setTitle(doc.getDocumentSection(sectionNumber).getSectionTitle());
        } else {
          tdoc2.setTitle(title);
        }
      }
      context.put("tdoc", tdoc2);
      vcontext.put("tdoc", tdoc2.newDocument(context));
      try {
        tdoc2.readFromTemplate(peform, context);
      } catch (XWikiException e) {
        if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
          context.put("exception", e);
          return "docalreadyexists";
        }
      }
      LOGGER.debug("ending edit action with doc.defLang/lang={}/{}, tdoc2.defLang/lang={}/{},"
          + " tdoc2.isTrans={}", doc.getDefaultLanguage(), doc.getLanguage(),
          tdoc2.getDefaultLanguage(), tdoc2.getLanguage(), tdoc2.isTrans());

      /* Setup a lock */
      try {
        XWikiLock lock = tdoc.getLock(context);
        if ((lock == null) || (lock.getUserName().equals(context.getUser()))
            || (peform.isLockForce())) {
          tdoc.setLock(context.getUser(), context);
        }
      } catch (Exception e) {
        // Lock should never make XWiki fail
        // But we should log any related information
        LOGGER.error("Exception while setting up lock", e);
      }
    }

    return "edit";
  }
}
