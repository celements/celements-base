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
package org.xwiki.configuration.internal;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;

import com.celements.model.reference.RefBuilder;

/**
 * Configuration source taking its data in the Wiki Preferences wiki document
 * (using data from the XWiki.XWikiPreferences object attached to that document).
 *
 * @version $Id$
 * @since 2.0M2
 */
@Component("wiki")
public class WikiPreferencesConfigurationSource extends AbstractDocumentConfigurationSource {

  private static final String SPACE_NAME = "XWiki";
  private static final String PAGE_NAME = "XWikiPreferences";

  @Inject
  public WikiPreferencesConfigurationSource(@Nullable ConverterManager converterManager) {
    super(converterManager);
  }

  @Override
  protected DocumentReference getClassReference() {
    // The Class reference is the same as the document reference for XWiki.XWikiPreferences since
    // the class is stored in the document of the same name.
    return getDocumentReference();
  }

  @Override
  protected DocumentReference getDocumentReference() {
    return RefBuilder.from(getCurrentWikiReference())
        .space(SPACE_NAME)
        .doc(PAGE_NAME)
        .build(DocumentReference.class);
  }
}
