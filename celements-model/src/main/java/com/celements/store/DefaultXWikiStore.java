package com.celements.store;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.store.XWikiStoreInterface;

@Component
public class DefaultXWikiStore extends DefaultHibernateStore implements XWikiStoreInterface {}
