package com.celements.store;

import java.util.Optional;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

import com.google.common.primitives.Ints;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
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

  private static ConfigurationSource getConfigSource() throws ComponentLookupException {
    return getComponentManager().lookup(ConfigurationSource.class, "allproperties");
  }

  private static ComponentManager getComponentManager() {
    return Utils.getComponentManager();
  }
}
