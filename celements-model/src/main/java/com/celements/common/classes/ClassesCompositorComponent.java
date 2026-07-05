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
package com.celements.common.classes;

import static com.celements.execution.XWikiExecutionProp.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.init.update.WikiUpdater;
import com.celements.model.classes.ClassPackage;
import com.celements.model.context.Contextualiser;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

@Component
public class ClassesCompositorComponent implements IClassesCompositorComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassesCompositorComponent.class);

  @Requirement
  private XClassCreator classCreator;

  @Requirement
  private List<IClassCollectionRole> classCollections;

  @Requirement
  private List<ICelementsClassCollection> classCollectionsOld;

  @Requirement
  private WikiUpdater wikiUpdater;

  @Requirement
  private Execution execution;

  protected XWikiContext getContext() {
    return execution.getContext().get(XWIKI_CONTEXT).orElseThrow();
  }

  @Override
  public void checkClasses() {
    execution.getContext().get(WIKI)
        .ifPresent(this::checkClasses);
  }

  @Override
  public void checkClasses(WikiReference wikiRef) {
    if (wikiUpdater.isShutdown()) {
      new Contextualiser().withWiki(wikiRef)
          .execute(this::checkClassesForContext);
    } else {
      wikiUpdater.runUpdateAsync(wikiRef, this::checkClassesForContext);
    }
  }

  private void checkClassesForContext() {
    LOGGER.info("start checkClasses for wiki '{}'", getContext().getDatabase());
    classCreator.createXClasses();
    checkClassCollections();
    checkOldClassCollections();
    LOGGER.debug("finish checkClasses for wiki '{}'", getContext().getDatabase());
  }

  @Deprecated
  void checkOldClassCollections() {
    for (ICelementsClassCollection classCollection : classCollectionsOld) {
      try {
        classCollection.runUpdate(getContext());
      } catch (XWikiException xwe) {
        LOGGER.error("Exception checking class collection '{}'", classCollection.getConfigName(),
            xwe);
      }
    }
  }

  @Deprecated
  void checkClassCollections() {
    for (IClassCollectionRole classCollection : classCollections) {
      try {
        classCollection.runUpdate();
      } catch (XWikiException xwe) {
        LOGGER.error("Exception checking class collection '{}'", classCollection.getConfigName(),
            xwe);
      }
    }
  }

  @Override
  public boolean isActivated(String name) {
    try {
      return Utils.getComponentManager().lookup(ClassPackage.class, name).isActivated();
    } catch (ComponentLookupException exc) {
      LOGGER.debug("ClassDefinitionPackage '{}' doesn't exist", name);
    }
    for (IClassCollectionRole classCollection : classCollections) {
      if (classCollection.getConfigName().equals(name)) {
        return classCollection.isActivated();
      }
    }
    return false;
  }

}
