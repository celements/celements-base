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
package com.celements.configuration.document;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.ConverterManager;

import com.celements.configuration.AbstractConvertingConfigurationSource;
import com.google.common.collect.ImmutableList;

/**
 * Common features for all Document sources (ie configuration data coming from wiki pages).
 *
 * @version $Id$
 * @since 2.0M2
 */
public abstract class AbstractDocumentConfigurationSource
    extends AbstractConvertingConfigurationSource {

  @Requirement
  private DocumentAccessBridge documentAccessBridge;

  @Requirement
  private ModelContext modelContext;

  @Requirement
  private ModelConfiguration modelConfig;

  protected AbstractDocumentConfigurationSource(@Nullable ConverterManager converterManager) {
    super(converterManager);
  }

  /**
   * @return the document reference of the document containing an XWiki Object with configuration
   *         data or null if there no such document in which case this configuration source will be
   *         skipped
   */
  protected abstract DocumentReference getDocumentReference();

  /**
   * @return the XWiki Class reference of the XWiki Object containing the configuration properties
   */
  protected abstract DocumentReference getClassReference();

  /**
   * @return the bridge used to access Object properties
   */
  protected DocumentAccessBridge getDocumentAccessBridge() {
    return documentAccessBridge;
  }

  /**
   * @return the reference pointing to the current wiki
   */
  protected WikiReference getCurrentWikiReference() {
    return Optional.ofNullable(modelContext.getCurrentEntityReference())
        .flatMap(ref -> ref.extractRef(WikiReference.class))
        .orElseGet(() -> new WikiReference(modelConfig.getDefaultReferenceValue(EntityType.WIKI)));
  }

  @Override
  public List<String> getKeys() {
    // TODO missing method to properly implement
    // return documentAccessBridge.getXClassPropertyNames(getDocumentReference());
    return ImmutableList.of();
  }

  @Override
  protected Object getValue(String key, Class<?> type) {
    DocumentReference docRef = getDocumentReference();
    DocumentReference classDocRef = getClassReference();
    if ((docRef != null) && (classDocRef != null)) {
      return getDocumentAccessBridge().getProperty(docRef, classDocRef, key);
    }
    return null;
  }

}
