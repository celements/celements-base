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

import static com.celements.logging.LogUtils.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassPackage;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.context.ModelContext;
import com.google.common.collect.Sets;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

@Singleton
@Component
public class DefaultXClassCreator implements XClassCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXClassCreator.class);

  @Requirement
  protected IModelAccessFacade modelAccess;

  @Requirement
  protected ModelContext context;

  @Requirement
  private List<ClassPackage> classPackages;

  @Override
  public void createXClasses() {
    LOGGER.info("createXClasses for database '{}'", context.getWikiRef());
    for (ClassPackage classPackage : classPackages) {
      if (classPackage.isActivated()) {
        try {
          createXClasses(classPackage);
        } catch (XClassCreateException exc) {
          LOGGER.error("failed to create classes for package '{}'", classPackage.getName(), exc);
        }
      } else {
        LOGGER.info("skipping package '{}'", classPackage.getName());
      }
    }
  }

  @Override
  public void createXClasses(ClassPackage classPackage) throws XClassCreateException {
    LOGGER.debug("createXClasses - checking {}", classPackage.getName());
    for (ClassDefinition classDef : classPackage.getClassDefinitions()) {
      if (!classDef.isBlacklisted()) {
        createXClass(classDef);
      } else {
        LOGGER.info("skipping blacklisted class '{}'", classDef.getName());
      }
    }
  }

  @Override
  public void createXClass(ClassDefinition classDef) throws XClassCreateException {
    LOGGER.debug("createXClass - checking {}", classDef.getName());
    XWikiDocument classDoc = modelAccess.getOrCreateDocument(
        classDef.getClassReference().getDocRef());
    if (hasClassChange(classDef, classDoc)) {
      LOGGER.info("createXClass - {} {}:{}",
          classDoc.isNew() ? "create" : "update",
          classDoc.getDocumentReference().getWikiReference().getName(),
          classDef.getName());
      try {
        classDoc.setXClass(generateXClass(classDef));
        modelAccess.saveDocument(classDoc, "created/updated XClass");
      } catch (DocumentSaveException exc) {
        throw new XClassCreateException(exc);
      }
    }
  }

  @Override
  public BaseClass generateXClass(ClassDefinition classDef) {
    BaseClass bClass = new BaseClass();
    DocumentReference classDocRef = classDef.getClassReference().getDocRef();
    bClass.setDocumentReference(classDocRef);
    bClass.setXClassReference(classDocRef);
    if (classDef.isInternalMapping() && !bClass.hasInternalCustomMapping()) {
      bClass.setCustomMapping("internal");
    }
    for (ClassField<?> field : classDef.getFields()) {
      if (bClass.get(field.getName()) == null) {
        PropertyInterface xField = field.getXField();
        xField.setObject(bClass);
        bClass.addField(field.getName(), xField);
      }
    }
    return bClass;
  }

  private boolean hasClassChange(ClassDefinition classDef, XWikiDocument classDoc) {
    boolean changed = classDoc.isNew();
    if (changed) {
      LOGGER.trace("hasClassChange - new doc: {}", defer(classDoc::getDocumentReference));
    }
    BaseClass bClass = classDoc.getXClass();
    if (classDef.isInternalMapping() != bClass.hasInternalCustomMapping()) {
      changed = true;
      LOGGER.trace("hasClassChange - internal mapping changed: {} -> {}",
          bClass.hasInternalCustomMapping(), classDef.isInternalMapping());
    }
    for (ClassField<?> field : classDef.getFields()) {
      PropertyClass xField = (PropertyClass) bClass.get(field.getName());
      if (xField == null) {
        changed = true;
        LOGGER.trace("hasClassChange - field missing: {}", defer(field::serialize));
        continue;
      }
      var genXField = (PropertyClass) field.getXField();
      genXField.setNumber(xField.getNumber()); // comparison only works with same number
      if (!isFieldEqual(xField, genXField)) {
        changed = true;
        LOGGER.trace("hasClassChange - field {} changed", defer(field::serialize));
      }
    }
    return changed;
  }

  private boolean isFieldEqual(PropertyClass xField, PropertyClass genXField) {
    if (Objects.equals(xField, genXField)) {
      return true;
    }
    var changed = new HashSet<String>();
    var props = Set.copyOf(xField.getPropertyList());
    var genProps = Set.copyOf(genXField.getPropertyList());
    changed.addAll(Sets.symmetricDifference(props, genProps));
    for (var key : Sets.union(props, genProps)) {
      var xProp = (BaseProperty) xField.getField(key);
      var xPropVal = (xProp != null) ? xProp.getValue() : null;
      var genXProp = (BaseProperty) genXField.getField(key);
      var genXPropVal = (genXProp != null) ? genXProp.getValue() : null;
      if (Objects.equals(xProp, genXProp)) {
        continue;
      }
      // workaround for equality check on "defaultValue" always being false
      if ("defaultValue".equals(key)) {
        xField.removeField(key);
        genXField.removeField(key);
        LOGGER.trace("hasClassChange - removing prop '{}' with same value: {}:{}",
            key, xProp, xPropVal);
        continue;
      }
      changed.add(key);
      LOGGER.trace("hasClassChange - prop '{}' changed: {}:{} -> {}:{}",
          key, xProp, xPropVal, genXProp, genXPropVal);
    }
    return changed.isEmpty() && Objects.equals(xField, genXField);
  }

}
