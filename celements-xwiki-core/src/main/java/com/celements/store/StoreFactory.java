package com.celements.store;

import static com.celements.spring.context.SpringContextProvider.*;

import java.util.Optional;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

import com.google.common.primitives.Ints;
import com.xpn.xwiki.store.AttachmentContentStore;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentContentStore;
import com.xpn.xwiki.web.Utils;

public final class StoreFactory {

  private StoreFactory() {}

  public static XWikiStoreInterface getMainStore() {
    try {
      String hint = getConfigSource().getProperty("celements.store.main");
      return getComponentManager().lookup(XWikiStoreInterface.class, hint);
    } catch (ComponentLookupException exc) {
      throw new IllegalStateException("failed looking up main store", exc);
    }
  }

  public static Optional<XWikiRecycleBinStoreInterface> getRecycleBinStore() {
    return getOptionalStore(XWikiRecycleBinStoreInterface.class, "celements.store.recyclebin");
  }

  public static XWikiAttachmentStoreInterface getAttachmentStore() {
    try {
      String hint = getConfigSource().getProperty("celements.store.attachment");
      return getComponentManager().lookup(XWikiAttachmentStoreInterface.class, hint);
    } catch (ComponentLookupException exc) {
      throw new IllegalStateException("failed looking up attachment store", exc);
    }
  }

  public static AttachmentContentStore getAttachmentContentStore() {
    String beanName = getConfigSource().getProperty("celements.store.attachment.content",
        HibernateAttachmentContentStore.STORE_NAME);
    return getBeanFactory().getBean(beanName, AttachmentContentStore.class);
  }

  public static AttachmentVersioningStore getAttachmentVersioningStore() {
    try {
      return getComponentManager().lookup(AttachmentVersioningStore.class,
          getConfigSource().getProperty("celements.store.attachment.versioning", "default"));
    } catch (ComponentLookupException exc) {
      throw new IllegalStateException("failed looking up attachment versioning store", exc);
    }
  }

  private static <T> Optional<T> getOptionalStore(Class<T> type, String key) {
    try {
      String enabled = getConfigSource().getProperty(key + ".enabled", "false").toLowerCase();
      if ("true".equals(enabled) || (0 != Optional.ofNullable(Ints.tryParse(enabled)).orElse(0))) {
        return Optional.of(getComponentManager().lookup(type,
            getConfigSource().getProperty(key + ".hint", "default")));
      } else {
        return Optional.empty();
      }
    } catch (ComponentLookupException exc) {
      throw new IllegalStateException("failed looking up " + key, exc);
    }
  }

  private static ConfigurationSource getConfigSource() {
    return Utils.getComponent(ConfigurationSource.class, "allproperties");
  }

  private static ComponentManager getComponentManager() {
    return Utils.getComponentManager();
  }
}
