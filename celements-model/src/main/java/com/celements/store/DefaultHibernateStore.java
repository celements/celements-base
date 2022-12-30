package com.celements.store;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.store.XWikiStoreInterface;

@Component(DefaultHibernateStore.NAME)
public class DefaultHibernateStore extends CelHibernateStore implements XWikiStoreInterface {

  public static final String NAME = "hibernate";

}
