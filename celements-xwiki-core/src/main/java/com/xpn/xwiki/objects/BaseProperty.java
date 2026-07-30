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

import static java.util.Objects.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.doc.CelProperty;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 */
// TODO: shouldn't this be abstract? toFormString and toText
// will never work unless getValue is overriden
public class BaseProperty extends BaseElement
    implements PropertyInterface, Serializable, Cloneable {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseProperty.class);

  private BaseCollection object;

  public static BaseProperty from(CelProperty celProperty) {
    requireNonNull(celProperty);
    BaseProperty property = switch (celProperty.getType()) {
      case BASE -> new BaseProperty();
      case BASE_STRING -> new BaseStringProperty();
      case STRING -> new StringProperty();
      case LARGE_STRING -> new LargeStringProperty();
      case INTEGER -> new IntegerProperty();
      case LONG -> new LongProperty();
      case FLOAT -> new FloatProperty();
      case DOUBLE -> new DoubleProperty();
      case DATE -> new DateProperty();
      case LIST -> new ListProperty();
      case STRING_LIST -> new StringListProperty();
      case DB_STRING_LIST -> new DBStringListProperty();
    };
    property.setName(celProperty.getName());
    property.setValue(celProperty.getValue());
    return property;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.PropertyInterface#getObject()
   */
  @Override
  public BaseCollection getObject() {
    return this.object;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.PropertyInterface#setObject(com.xpn.xwiki.objects.BaseCollection)
   */
  @Override
  public void setObject(BaseCollection object) {
    this.object = object;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.BaseElement#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object el) {
    if (this == el) {
      return true;
    }
    if (el == null) {
      return false;
    }
    // I hate this.. needed for hibernate to find the object
    // when loading the collections..
    if ((this.object == null) || (((BaseProperty) el).getObject() == null)) {
      return (hashCode() == el.hashCode());
    }

    if (!super.equals(el)) {
      return false;
    }

    return (getId() == ((BaseProperty) el).getId());
  }

  @Override
  public long getId() {
    return getObject() != null ? getObject().getId() : super.getId();
  }

  // needed for properties because access=field not possible (composite id)
  public void setId(long id) {
    setId(id, IdVersion.CELEMENTS_3);
  }

  @Override
  public boolean hasValidId() {
    return getObject() != null ? getObject().hasValidId() : super.hasValidId();
  }

  @Override
  public IdVersion getIdVersion() {
    return getObject() != null ? getObject().getIdVersion() : super.getIdVersion();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // I hate this.. needed for hibernate to find the object
    // when loading the collections..
    return ("" + getId() + getName()).hashCode();
  }

  public String getClassType() {
    return getClass().getName();
  }

  public void setClassType(String type) {}

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.BaseElement#clone()
   */
  @Override
  public Object clone() {
    BaseProperty property = (BaseProperty) super.clone();
    property.setObject(getObject());
    return property;
  }

  public Object getValue() {
    return null;
  }

  public void setValue(Object value) {}

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.PropertyInterface#toXML()
   */
  @Override
  public Element toXML() {
    Element el = new DOMElement(getName());
    Object value = getValue();
    el.setText((value == null) ? "" : value.toString());

    return el;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.objects.PropertyInterface#toFormString()
   */
  @Override
  public String toFormString() {
    return Utils.formEncode(toText());
  }

  public String toText() {
    Object value = getValue();

    return (value == null) ? "" : value.toString();
  }

  public String toXMLString() {
    Document doc = new DOMDocument();
    doc.setRootElement(toXML());
    OutputFormat outputFormat = new OutputFormat("", true);
    StringWriter out = new StringWriter();
    XMLWriter writer = new XMLWriter(out, outputFormat);
    try {
      writer.write(doc);

      return out.toString();
    } catch (IOException e) {
      e.printStackTrace();

      return "";
    }
  }

  @Override
  public String toString(boolean withDefinition) {
    String ret = super.toString(withDefinition);
    if (getObject() != null) {
      ret += " " + getObject().toString(false);
    }
    return ret;
  }

  public Object getCustomMappingValue() {
    return getValue();
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
}
