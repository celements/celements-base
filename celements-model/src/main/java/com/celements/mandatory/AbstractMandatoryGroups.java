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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public abstract class AbstractMandatoryGroups implements IMandatoryDocumentRole {

  @Requirement
  protected IModelAccessFacade modelAccess;

  @Requirement
  protected ModelContext modelContext;

  @Requirement
  protected ModelUtils modelUtils;

  public AbstractMandatoryGroups() {
    super();
  }

  @Deprecated
  protected XWikiContext getContext() {
    return modelContext.getXWikiContext();
  }

  @Override
  public abstract void checkDocuments() throws XWikiException;

  protected abstract String commitName();

  @Override
  public List<String> dependsOnMandatoryDocuments() {
    return List.of("celements.mandatory.MandatoryXClasses");
  }

  protected void checkGroup(DocumentReference groupDocRef) throws XWikiException {
    try {
      XWikiDocument groupDoc = modelAccess.createDocument(groupDocRef);
      XWikiObjectEditor.on(groupDoc).filter(getGroupClassRef()).createFirst();
      modelAccess.saveDocument(groupDoc, "autocreate " + commitName() + " group");
    } catch (DocumentAlreadyExistsException exc) {
      getLogger().debug("group doc already exists", exc);
    } catch (DocumentSaveException exc) {
      throw new XWikiException(0, 0, "failed to save", exc);
    }
  }

  protected ClassReference getGroupClassRef() {
    return new ClassReference("XWiki", "XWikiGroups");
  }

  protected Logger getLogger() {
    return LoggerFactory.getLogger(this.getClass());
  }

}
