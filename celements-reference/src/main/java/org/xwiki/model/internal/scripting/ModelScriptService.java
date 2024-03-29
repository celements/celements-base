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
package org.xwiki.model.internal.scripting;

import static com.celements.model.util.EntityTypeUtil.*;
import static com.google.common.base.Strings.*;

import java.util.Arrays;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.script.service.ScriptService;

import com.celements.model.reference.RefBuilder;
import com.google.common.base.Strings;

/**
 * Provides Model-specific Scripting APIs.
 *
 * @version $Id: 4230cc79681d08643453d7c31bb68373d3571bdc $
 * @since 2.3M1
 */
@Component("model")
@Singleton
@Deprecated
public class ModelScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelScriptService.class);

  /**
   * The default hint used when resolving string references.
   */
  private static final String DEFAULT_STRING_RESOLVER_HINT = "currentmixed";

  /**
   * Used to dynamically look up component implementations based on a given hint.
   */
  @Requirement
  private ComponentManager componentManager;

  /**
   * Create a Document Reference from a passed wiki, space and page names, which can be empty
   * strings or null in which
   * case they are resolved using a "currentmixed/reference" resolver.
   *
   * @param wiki
   *          the wiki reference name to use (can be empty or null)
   * @param space
   *          the space reference name to use (can be empty or null)
   * @param page
   *          the page reference name to use (can be empty or null)
   * @return the typed Document Reference object or null if no Resolver with the passed hint could
   *         be found
   * @since 2.3M2
   */
  public DocumentReference createDocumentReference(String wiki, String space, String page) {
    return createDocumentReference(wiki, space, page, "currentmixed/reference");
  }

  /**
   * Create a Document Reference from a passed wiki, space and page names, which can be empty
   * strings or null in which
   * case they are resolved against the Resolver having the hint passed as parameter. Valid hints
   * are for example
   * "default/reference", "current/reference", "currentmixed/reference".
   *
   * @param wiki
   *          the wiki reference name to use (can be empty or null)
   * @param space
   *          the space reference name to use (can be empty or null)
   * @param page
   *          the page reference name to use (can be empty or null)
   * @param hint
   *          the hint of the Resolver to use in case any parameter is empty or null
   * @return the typed Document Reference object or null if no Resolver with the passed hint could
   *         be found
   */
  @SuppressWarnings("unchecked")
  public DocumentReference createDocumentReference(String wiki, String space, String page,
      String hint) {
    EntityReference reference = null;
    if (!Strings.isNullOrEmpty(wiki)) {
      reference = new EntityReference(wiki, EntityType.WIKI);
    }
    if (!Strings.isNullOrEmpty(space)) {
      reference = new EntityReference(space, EntityType.SPACE, reference);
    }
    if (!Strings.isNullOrEmpty(page)) {
      reference = new EntityReference(page, EntityType.DOCUMENT, reference);
    }
    DocumentReference documentReference;
    try {
      documentReference = this.componentManager.lookup(DocumentReferenceResolver.class, hint)
          .resolve(reference);
    } catch (ComponentLookupException e) {
      documentReference = null;
    }
    return documentReference;
  }

  /**
   * Creates an {@link AttachmentReference} from a file name and a reference to the document holding
   * that file.
   *
   * @param documentReference
   *          a reference to the document the file is attached to
   * @param fileName
   *          the name of a file attached to a document
   * @return a reference to the specified attachment
   * @since 2.5M2
   */
  public AttachmentReference createAttachmentReference(DocumentReference documentReference,
      String fileName) {
    return new AttachmentReference(fileName, documentReference);
  }

  /**
   * @param stringRepresentation
   *          the document reference specified as a String (using the "wiki:space.page" format and
   *          with special characters escaped where required)
   * @return the typed Document Reference object (resolved using the
   *         {@value #DEFAULT_STRING_RESOLVER_HINT} resolver)
   * @since 2.3M2
   */
  public DocumentReference resolveDocument(String stringRepresentation) {
    return resolveDocument(stringRepresentation, DEFAULT_STRING_RESOLVER_HINT);
  }

  /**
   * @param stringRepresentation
   *          the document reference specified as a String (using the "wiki:space.page" format and
   *          with special characters escaped where required)
   * @param hint
   *          the hint of the Resolver to use in case any part of the reference is missing (no wiki
   *          specified, no
   *          space or no page)
   * @param parameters
   *          extra parameters to pass to the resolver; you can use these parameters to resolve a
   *          document
   *          reference relative to another entity reference
   * @return the typed Document Reference object or null if no Resolver with the passed hint could
   *         be found
   */
  public DocumentReference resolveDocument(String stringRepresentation, String hint,
      Object... parameters) {
    return resolve(DocumentReference.class, stringRepresentation, hint, parameters);
  }

  /**
   * @param stringRepresentation
   *          an attachment reference specified as {@link String} (using the "wiki:space.page@file"
   *          format and with special characters escaped where required)
   * @return the corresponding typed {@link AttachmentReference} object (resolved using the
   *         {@value #DEFAULT_STRING_RESOLVER_HINT} resolver)
   * @since 2.5M2
   */
  public AttachmentReference resolveAttachment(String stringRepresentation) {
    return resolveAttachment(stringRepresentation, DEFAULT_STRING_RESOLVER_HINT);
  }

  /**
   * @param stringRepresentation
   *          an attachment reference specified as {@link String} (using the "wiki:space.page@file"
   *          format and with special characters escaped where required)
   * @param hint
   *          the hint of the resolver to use in case any part of the reference is missing (no wiki
   *          specified, no
   *          space or no page)
   * @param parameters
   *          extra parameters to pass to the resolver; you can use these parameters to resolve an
   *          attachment
   *          reference relative to another entity reference
   * @return the corresponding typed {@link AttachmentReference} object
   * @since 2.5M2
   */
  public AttachmentReference resolveAttachment(String stringRepresentation, String hint,
      Object... parameters) {
    return resolve(AttachmentReference.class, stringRepresentation, hint, parameters);
  }

  /**
   * @param stringRepresentation
   *          an object reference specified as {@link String} (using the "wiki:space.page^object"
   *          format and with special characters escaped where required)
   * @return the corresponding typed {@link ObjectReference} object (resolved using the
   *         {@value #DEFAULT_STRING_RESOLVER_HINT} resolver)
   * @since 3.2M3
   */
  public ObjectReference resolveObject(String stringRepresentation) {
    return resolveObject(stringRepresentation, DEFAULT_STRING_RESOLVER_HINT);
  }

  /**
   * @param stringRepresentation
   *          an object reference specified as {@link String} (using the "wiki:space.page^object"
   *          format and with special characters escaped where required)
   * @param hint
   *          the hint of the resolver to use in case any part of the reference is missing (no wiki
   *          specified, no
   *          space or no page)
   * @param parameters
   *          extra parameters to pass to the resolver; you can use these parameters to resolve an
   *          object
   *          reference relative to another entity reference
   * @return the corresponding typed {@link ObjectReference} object
   * @since 3.2M3
   */
  public ObjectReference resolveObject(String stringRepresentation, String hint,
      Object... parameters) {
    return resolve(ObjectReference.class, stringRepresentation, hint, parameters);
  }

  /**
   * @param stringRepresentation
   *          an object property reference specified as {@link String} (using the
   *          "wiki:space.page^object.property" format and with special characters escaped where
   *          required)
   * @return the corresponding typed {@link ObjectReference} object (resolved using the
   *         {@value #DEFAULT_STRING_RESOLVER_HINT} resolver)
   * @since 3.2M3
   */
  public ObjectPropertyReference resolveObjectProperty(String stringRepresentation) {
    return resolveObjectProperty(stringRepresentation, DEFAULT_STRING_RESOLVER_HINT);
  }

  /**
   * @param stringRepresentation
   *          an object property reference specified as {@link String} (using the
   *          "wiki:space.page^object.property" format and with special characters escaped where
   *          required)
   * @param hint
   *          the hint of the resolver to use in case any part of the reference is missing (no wiki
   *          specified, no
   *          space or no page)
   * @param parameters
   *          extra parameters to pass to the resolver; you can use these parameters to resolve an
   *          object
   *          property reference relative to another entity reference
   * @return the corresponding typed {@link ObjectReference} object
   * @since 3.2M3
   */
  public ObjectPropertyReference resolveObjectProperty(String stringRepresentation, String hint,
      Object... parameters) {
    return resolve(ObjectPropertyReference.class, stringRepresentation, hint, parameters);
  }

  @SuppressWarnings("unchecked")
  private <T extends EntityReference> T resolve(Class<T> type, String stringRepresentation,
      String hint, Object... parameters) {
    try {
      if (determineEntityTypeFromName(nullToEmpty(stringRepresentation)).orElse(null) != null) {
        return RefBuilder.from(componentManager.lookup(EntityReferenceResolver.class, hint)
            .resolve(stringRepresentation, getEntityTypeForClassOrThrow(type), parameters))
            .build(type);
      }
    } catch (ComponentLookupException | IllegalArgumentException | ClassCastException exc) {
      LOGGER.debug("resolve - failed for [{}], [{}], [{}], [{}]",
          type, stringRepresentation, hint, Arrays.toString(parameters), exc);
    }
    return null;
  }

  /**
   * @param reference
   *          the entity reference to transform into a String representation
   * @return the string representation of the passed entity reference (using the "compact"
   *         serializer)
   * @since 2.3M2
   */
  public String serialize(EntityReference reference) {
    return serialize(reference, "compact");
  }

  /**
   * @param reference
   *          the entity reference to transform into a String representation
   * @param hint
   *          the hint of the Serializer to use (valid hints are for example "default", "compact",
   *          "local")
   * @return the string representation of the passed entity reference
   */
  public String serialize(EntityReference reference, String hint) {
    String result;
    try {
      result = (String) this.componentManager.lookup(EntityReferenceSerializer.class, hint)
          .serialize(reference);
    } catch (ComponentLookupException e) {
      result = null;
    }
    return result;
  }
}
