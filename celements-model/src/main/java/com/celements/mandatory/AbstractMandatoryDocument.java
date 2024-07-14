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
package com.celements.mandatory;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

public abstract class AbstractMandatoryDocument implements IMandatoryDocumentRole {

  @Requirement
  protected IModelAccessFacade modelAccess;

  @Requirement
  protected ModelContext modelContext;

  @Requirement
  protected ModelUtils modelUtils;

  @Requirement("xwikiproperties")
  protected ConfigurationSource xwikiPropConfigSource;

  @Deprecated
  protected XWikiContext getContext() {
    return modelContext.getXWikiContext();
  }

  public abstract String getName();

  @Override
  public void checkDocuments() throws XWikiException {
    getLogger().debug("starting mandatory '{}' for db '{}'", getName(), getWiki());
    if (!skip()) {
      XWikiDocument doc = getDoc();
      boolean dirty;
      if (!modelUtils.isMainWiki(modelContext.getWikiRef())) {
        dirty = checkDocuments(doc);
      } else {
        dirty = checkDocumentsMain(doc);
      }
      saveDoc(doc, dirty);
    } else {
      getLogger().debug("skipping mandatory '{}' for db '{}'", getName(), getWiki());
    }
    getLogger().debug("end mandatory '{}' for db '{}'", getName(), getWiki());
  }

  private XWikiDocument getDoc() throws XWikiException {
    XWikiUser originalUser = modelContext.getUser();
    try {
      modelContext.setUser(getUser());
      return modelAccess.getOrCreateDocument(getDocRef());
    } catch (DocumentLoadException dle) {
      throw new XWikiException(0, 0, "failed to load doc", dle);
    } finally {
      modelContext.setUser(originalUser);
    }
  }

  protected void saveDoc(XWikiDocument doc, boolean dirty) throws XWikiException {
    if (dirty) {
      try {
        modelAccess.saveDocument(doc, "autocreate mandatory " + getName());
        getLogger().info("updated doc '{}' for '{}'", doc.getDocRef(), getName());
      } catch (DocumentSaveException exc) {
        throw new XWikiException(0, 0, "failed to save doc", exc);
      }
    } else {
      getLogger().debug("is uptodate '{}' for '{}'", doc.getDocRef(), getName());
    }
  }

  protected abstract DocumentReference getDocRef();

  protected abstract boolean skip();

  protected abstract boolean checkDocuments(XWikiDocument doc) throws XWikiException;

  protected abstract boolean checkDocumentsMain(XWikiDocument doc) throws XWikiException;

  public abstract Logger getLogger();

  protected String getWiki() {
    return modelContext.getWikiRef().getName();
  }

  protected XWikiUser getUser() {
    XWikiUser user = modelContext.getUser();
    String defaultUserName = xwikiPropConfigSource.getProperty(
        "celements.mandatory.defaultGlobalUserName", "").trim();
    if (!defaultUserName.isEmpty()) {
      user = new XWikiUser(defaultUserName, true);
    }
    return user;
  }

}
