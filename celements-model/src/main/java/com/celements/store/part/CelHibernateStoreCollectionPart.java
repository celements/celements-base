package com.celements.store.part;

import static com.xpn.xwiki.XWikiException.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

import com.celements.store.CelHibernateStore;
import com.google.common.base.Preconditions;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

//TODO CELDEV-626 - CelHibernateStore refactoring
public class CelHibernateStoreCollectionPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  public CelHibernateStoreCollectionPart(CelHibernateStore store) {
    this.store = Preconditions.checkNotNull(store);
  }

  public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException, HibernateException {
    if (object == null) {
      return;
    }
    if (!object.hasValidId()) {
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
          "Unable to save object with invalid id: " + object);
    }
    boolean commit = false;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(context);
      }
      Session session = store.getSession(context);

      // Verify if the property already exists
      Query query = session.createQuery("select obj.id from BaseObject as obj where obj.id = :id");
      query.setLong("id", object.getId());
      if (query.uniqueResult() == null) {
        session.save("com.xpn.xwiki.objects.BaseObject", object);
      } else {
        session.update("com.xpn.xwiki.objects.BaseObject", object);
      }
      /*
       * if (stats) session.saveOrUpdate(object); else
       * session.saveOrUpdate((String)"com.xpn.xwiki.objects.BaseObject", (Object)object);
       */
      BaseClass bclass = object.getXClass(context);
      List<String> handledProps = new ArrayList<>();
      if ((bclass != null) && (bclass.hasCustomMapping())
          && context.getWiki().hasCustomMappings()) {
        // save object using the custom mapping
        Map<String, Object> objmap = object.getCustomMappingMap();
        handledProps = bclass.getCustomMappingPropertyList(context);
        Session dynamicSession = session.getSession(EntityMode.MAP);
        query = session.createQuery("select obj.id from " + bclass.getName()
            + " as obj where obj.id = :id");
        query.setLong("id", object.getId());
        if (query.uniqueResult() == null) {
          dynamicSession.save(bclass.getName(), objmap);
        } else {
          dynamicSession.update(bclass.getName(), objmap);
        }
        // dynamicSession.saveOrUpdate((String) bclass.getName(), objmap);
      }

      if (object.getXClassReference() != null) {
        // Remove all existing properties
        if (object.getFieldsToRemove().size() > 0) {
          for (Object element : object.getFieldsToRemove()) {
            BaseProperty prop = (BaseProperty) element;
            if (!handledProps.contains(prop.getName())) {
              session.delete(prop);
            }
          }
          object.setFieldsToRemove(new ArrayList<BaseProperty>());
        }

        for (String key : object.getPropertyList()) {
          BaseProperty prop = (BaseProperty) object.getField(key);
          if (!prop.getName().equals(key)) {
            throw new XWikiException(MODULE_XWIKI_CLASSES, ERROR_XWIKI_CLASSES_FIELD_INVALID,
                "Field '" + key + "' has an invalid name in object: " + object);
          }

          String pname = prop.getName();
          if ((pname != null) && !pname.trim().equals("") && !handledProps.contains(pname)) {
            store.saveXWikiProperty(prop, context, false);
          }
        }
      }

      commit = true;
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

  public void loadXWikiCollection(BaseCollection object1, XWikiDocument doc, XWikiContext context,
      boolean bTransaction, boolean alreadyLoaded) throws XWikiException, HibernateException {
    BaseCollection object = object1;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(false, context);
      }
      Session session = store.getSession(context);

      if (!alreadyLoaded) {
        session.load(object, new Long(object1.getId()));
      }

      DocumentReference classReference = object.getXClassReference();

      // If the class reference is null in the loaded object then skip loading properties
      if (classReference != null) {

        BaseClass bclass = null;
        if (!classReference.equals(object.getDocumentReference())) {
          // Let's check if the class has a custom mapping
          bclass = object.getXClass(context);
        } else {
          // We need to get it from the document otherwise
          // we will go in an endless loop
          if (doc != null) {
            bclass = doc.getXClass();
          }
        }

        List<String> handledProps = new ArrayList<>();
        try {
          if ((bclass != null) && (bclass.hasCustomMapping())
              && context.getWiki().hasCustomMappings()) {
            Session dynamicSession = session.getSession(EntityMode.MAP);
            Map<String, ?> map = (Map<String, ?>) dynamicSession.load(bclass.getName(), new Long(
                object.getId()));
            // Let's make sure to look for null fields in the dynamic mapping
            bclass.fromValueMap(map, object);
            for (String prop : bclass.getCustomMappingPropertyList(context)) {
              if (map.get(prop) != null) {
                handledProps.add(prop);
              }
            }
          }
        } catch (HibernateException exc) {
          LOGGER.error("Failed loading custom mapping for doc [{}], class [{}], nb [{}]",
              object.getDocumentReference(), object.getXClassReference(), object.getNumber(), exc);
        }

        // Load strings, integers, dates all at once

        Query query = session.createQuery(
            "select prop.name, prop.classType from BaseProperty as prop where prop.id.id = :id");
        query.setLong("id", object.getId());
        List<?> list = query.list();
        for (Object obj : list) {
          Object[] result = (Object[]) obj;
          String name = (String) result[0];
          // No need to load fields already loaded from
          // custom mapping
          if (handledProps.contains(name)) {
            continue;
          }
          String classType = (String) result[1];
          BaseProperty property = null;

          try {
            property = (BaseProperty) Class.forName(classType).newInstance();
            property.setObject(object);
            property.setName(name);
            store.loadXWikiProperty(property, context, false);
          } catch (Exception e) {
            // TODO CELDEV-626 what exception are we actually handling here?
            // WORKAROUND IN CASE OF MIXMATCH BETWEEN STRING AND LARGESTRING
            try {
              if (property instanceof StringProperty) {
                LargeStringProperty property2 = new LargeStringProperty();
                property2.setObject(object);
                property2.setName(name);
                store.loadXWikiProperty(property2, context, false);
                property.setValue(property2.getValue());
                if (bclass != null) {
                  if (bclass.get(name) instanceof TextAreaClass) {
                    property = property2;
                  }
                }
              } else if (property instanceof LargeStringProperty) {
                StringProperty property2 = new StringProperty();
                property2.setObject(object);
                property2.setName(name);
                store.loadXWikiProperty(property2, context, false);
                property.setValue(property2.getValue());
                if (bclass != null) {
                  if (bclass.get(name) instanceof StringClass) {
                    property = property2;
                  }
                }
              } else {
                throw e;
              }
            } catch (Exception e2) {
              throw new XWikiException(MODULE_XWIKI_STORE,
                  ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT, "Exception while loading property '"
                      + name + "' for object: " + object,
                  e);
            }
          }

          object.addField(name, property);
        }
      }
    } catch (ObjectNotFoundException exc) { // there is no object data saved
      LOGGER.warn("loadXWikiCollection - no data for object: {} {}_{}", object1.getId(),
          object1.getClassName(), object1.getNumber(), exc);
    } finally {
      if (bTransaction) {
        store.endTransaction(context, false, false);
      }
    }
  }

  public void deleteXWikiCollection(BaseCollection object, XWikiContext context,
      boolean bTransaction, boolean evict) throws XWikiException, HibernateException {
    if (object == null) {
      return;
    }
    boolean commit = false;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(context);
      }
      Session session = store.getSession(context);

      // Let's check if the class has a custom mapping
      BaseClass bclass = object.getXClass(context);
      List<String> handledProps = new ArrayList<>();
      if ((bclass != null) && (bclass.hasCustomMapping())
          && context.getWiki().hasCustomMappings()) {
        handledProps = bclass.getCustomMappingPropertyList(context);
        Session dynamicSession = session.getSession(EntityMode.MAP);
        Object map = dynamicSession.get(bclass.getName(), new Long(object.getId()));
        if (map != null) {
          if (evict) {
            dynamicSession.evict(map);
          }
          dynamicSession.delete(map);
        }
      }

      if (object.getXClassReference() != null) {
        for (Object name : object.getFieldList()) {
          BaseElement property = (BaseElement) name;
          if (!handledProps.contains(property.getName())) {
            if (evict) {
              session.evict(property);
            }
            if (session.get(property.getClass(), property) != null) {
              session.delete(property);
            }
          }
        }
      }

      // In case of custom class we need to force it as BaseObject to delete the xwikiobject row
      if (!"".equals(bclass.getCustomClass())) {
        BaseObject cobject = new BaseObject();
        cobject.setDocumentReference(object.getDocumentReference());
        cobject.setClassName(object.getClassName());
        cobject.setNumber(object.getNumber());
        if (object instanceof BaseObject) {
          cobject.setGuid(((BaseObject) object).getGuid());
        }
        cobject.setId(object.getId(), object.getIdVersion());
        if (evict) {
          session.evict(cobject);
        }
        session.delete(cobject);
      } else {
        if (evict) {
          session.evict(object);
        }
        session.delete(object);
      }

      commit = true;
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

}
