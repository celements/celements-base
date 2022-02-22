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

package com.xpn.xwiki.objects;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for representing an element having a name (either a reference of a free form name) and
 * a pretty name.
 *
 * @version $Id$
 */
public abstract class BaseElement implements ElementInterface, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(BaseElement.class);

  /**
   * Used to convert a Document Reference to string (compact form without the wiki part if it
   * matches the current wiki).
   */
  protected final EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer = Utils
      .getComponent(EntityReferenceSerializer.class, "compactwiki");

  /**
   * Used to convert a proper Document Reference to a string but without the wiki name.
   */
  protected final EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils
      .getComponent(EntityReferenceSerializer.class, "local");

  /**
   * Used to convert a proper Document Reference to a string but with the wiki name.
   */
  protected final EntityReferenceSerializer<String> entityReferenceSerializer = Utils
      .getComponent(EntityReferenceSerializer.class);

  /**
   * Reference to the document in which this element is defined (for elements where this make sense,
   * for example for an XClass or a XObject).
   */
  private DocumentReference reference;

  /**
   * Free form name (for elements which don't point to a reference, for example for instances of
   * {@link BaseProperty}).
   */
  private String name;

  private String prettyName;

  private long id;

  private IdVersion idVersion;

  public long getId() {
    return id;
  }

  /**
   * IMPORTANT: must never be called outside of store logic
   */
  public void setId(long id, IdVersion idVersion) {
    if (hasValidId()) {
      throw new IllegalStateException("id override not permitted");
    } else if (idVersion != null) {
      setIdInternal(id, idVersion);
    } else {
      verifyIdVersion();
    }
  }

  protected void setIdInternal(long id, IdVersion idVersion) {
    this.id = id;
    this.idVersion = idVersion;
  }

  public boolean hasValidId() {
    return idVersion != null;
  }

  public IdVersion getIdVersion() {
    verifyIdVersion();
    return idVersion;
  }

  private void verifyIdVersion() {
    if (!hasValidId()) {
      throw new IllegalStateException("no id version set");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.ElementInterface#getDocumentReference()
   * @since 2.2M2
   */
  @Override
  public DocumentReference getDocumentReference() {
    // Object using name without setting a reference are not allowed to retrieve the reference
    if ((this.reference == null) && (this.name != null)) {
      throw new IllegalStateException(
          "BaseElement#getDocumentReference could not be called when a non-reference Name has been set.");
    }
    return reference;
  }

  /**
   * Note that this method is used by Hibernate for saving an element. {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.ElementInterface#getName()
   */
  @Override
  public String getName() {
    // If the name is null then serialize the reference as a string.
    if ((this.name == null) && (this.reference != null)) {
      this.name = this.localEntityReferenceSerializer.serialize(this.reference);
    }
    return this.name;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.ElementInterface#setDocumentReference(DocumentReference)
   * @since 2.2M2
   */
  @Override
  public void setDocumentReference(DocumentReference reference) {
    // If the name is already set then reset it since we're now using a reference
    this.reference = reference;
    this.name = null;
  }

  /**
   * Note that this method is used by Hibernate for loading an element. {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.ElementInterface#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    // If a reference is already set, then you cannot set a name
    if (this.reference != null) {
      throw new IllegalStateException(
          "BaseElement#setName could not be called when a reference has been set.");
    }

    this.name = name;
  }

  public String getPrettyName() {
    return this.prettyName;
  }

  public void setPrettyName(String name) {
    this.prettyName = name;
  }

  /**
   * @return the name of the wiki where this element is stored. If null, the context's wiki is used.
   * @deprecated since 2.2M2 use {@link #getDocumentReference()} and
   *             {@link org.xwiki.model.reference.DocumentReference#getWikiReference()}
   */
  @Deprecated
  public String getWiki() {
    return getDocumentReference().getWikiReference().getName();
  }

  /**
   * @param wiki
   *          the name of the wiki where this element is stored. If null, the context's wiki is
   *          used.
   * @deprecated since 2.2M2 use {@link #setDocumentReference(DocumentReference)}
   */
  @Deprecated
  public void setWiki(String wiki) {
    // Object using name without setting a reference are not allowed to set a wiki reference
    if ((this.reference == null) && (this.name != null)) {
      throw new IllegalStateException(
          "BaseElement#setWiki could not be called when a non-reference Name has been set.");
    }

    if (!StringUtils.isEmpty(wiki)) {
      DocumentReference reference = getDocumentReference();

      if (reference != null) {
        setDocumentReference(new DocumentReference(wiki,
            reference.getLastSpaceReference().getName(), reference.getName()));
      } else {
        this.reference = new DocumentReference(wiki, "Main", "WebHome");

        if (this.name != null) {
          setName(this.name);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object el) {
    // Same Java object, they sure are equal
    if (this == el) {
      return true;
    }

    if ((el == null) || !(el.getClass().equals(this.getClass()))) {
      return false;
    }

    BaseElement element = (BaseElement) el;

    if (element.reference != null) {
      if (!element.reference.equals(this.reference)) {
        return false;
      }
    } else {
      if (this.reference != null) {
        return false;
      }
      if (element.name == null) {
        if (this.name != null) {
          return false;
        }
      } else if (!element.name.equals(this.name)) {
        return false;
      }
    }

    if (element.getPrettyName() == null) {
      if (getPrettyName() != null) {
        return false;
      }
    } else if (!element.getPrettyName().equals(getPrettyName())) {
      return false;
    }

    return true;
  }

  @Override
  public Object clone() {
    return clone(true);
  }

  protected BaseElement clone(boolean keepsIdentity) {
    BaseElement clone;
    try {
      clone = getClass().newInstance();
      if (keepsIdentity && hasValidId()) {
        clone.setId(getId(), getIdVersion());
      }
      // Make sure we clone either the reference or the name depending on which one is used.
      if (this.reference != null) {
        clone.setDocumentReference(getDocumentReference());
      } else if (this.name != null) {
        clone.setName(getName());
      }
      clone.setPrettyName(getPrettyName());
    } catch (Exception exc) {
      LOG.error("should not happen", exc);
      clone = null;
    }
    return clone;
  }

  /**
   * Return the document where this element is stored.
   *
   * @param context
   *          the XWiki context
   * @return the document
   * @deprecated since 2.2M2 use {@link #getDocumentReference()} instead
   */
  @Deprecated
  public XWikiDocument getDocument(XWikiContext context) throws XWikiException {
    return context.getWiki().getDocument(getDocumentReference(), context);
  }

  /**
   * @return the syntax id of the document containing this element. If an error occurs while
   *         retrieving the document a syntax id of "xwiki/1.0" is assumed.
   * @deprecated since 2.2M2 use
   *             <code>context.getWiki().getDocument(object.getDocumentReference())</code> and then
   *             get the syntax on the document object
   */
  @Deprecated
  public String getDocumentSyntaxId(XWikiContext context) {
    String syntaxId;
    try {
      syntaxId = getDocument(context).getSyntaxId();
    } catch (Exception e) {
      LOG.warn("Error while getting the syntax corresponding to object ["
          + this.compactWikiEntityReferenceSerializer.serialize(getDocumentReference())
          + "]. Defaulting to using XWiki 1.0 syntax. Internal error [" + e.getMessage() + "]");
      syntaxId = "xwiki/1.0";
    }

    return syntaxId;
  }

  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean withDefinition) {
    StringBuilder ret = new StringBuilder();
    if (withDefinition) {
      ret.append(getClass().getSimpleName()).append(" ");
      if (hasValidId()) {
        ret.append(getId()).append(" ");
      }
    }
    if (reference != null) {
      ret.append(entityReferenceSerializer.serialize(reference));
    } else if ((name != null) && !name.isEmpty()) {
      ret.append(name);
    } else {
      ret.append("?");
    }
    return ret.toString();
  }

}
