package com.xpn.xwiki.store;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.web.Utils;

public final class StoreFactory {

  private StoreFactory() {}

  public static XWikiStoreInterface getMainStore() {
    try {
      ComponentManager cm = Utils.getComponentManager();
      String hint = cm.lookup(ConfigurationSource.class, "allproperties")
          .getProperty("celements.store.main", "default");
      return cm.lookup(XWikiStoreInterface.class, hint);
    } catch (ComponentLookupException exc) {
      throw new IllegalStateException("failed looking up main store", exc);
    }
  }
}
