package com.xpn.xwiki.store;

import java.util.Optional;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.web.Utils;

public final class StoreFactory {

  private StoreFactory() {}

  public static XWikiStoreInterface getMainStore() {
    try {
      return getComponentManager().lookup(XWikiStoreInterface.class,
          getConfigSource().getProperty("celements.store.main", "default"));
    } catch (ComponentLookupException exc) {
      throw new IllegalStateException("failed looking up main store", exc);
    }
  }

  public static Optional<XWikiRecycleBinStoreInterface> getRecycleBinStore() {
    try {
      return Optional.of(getComponentManager().lookup(XWikiRecycleBinStoreInterface.class,
          getConfigSource().getProperty("celements.store.recyclebin", "default")));
    } catch (ComponentLookupException exc) {
      return Optional.empty();
    }
  }

  private static ConfigurationSource getConfigSource() throws ComponentLookupException {
    return getComponentManager().lookup(ConfigurationSource.class, "allproperties");
  }

  private static ComponentManager getComponentManager() {
    return Utils.getComponentManager();
  }
}
