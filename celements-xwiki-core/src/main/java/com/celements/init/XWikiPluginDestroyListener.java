package com.celements.init;

import javax.inject.Inject;

import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class XWikiPluginDestroyListener
    implements ApplicationListener<CelementsStoppedEvent>, Ordered {

  public static final int ORDER = 1000;

  private final XWikiProvider xwikiProvider;

  @Inject
  public XWikiPluginDestroyListener(XWikiProvider xwikiProvider) {
    this.xwikiProvider = xwikiProvider;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void onApplicationEvent(CelementsStoppedEvent event) {
    xwikiProvider.get().ifPresent(xwiki -> xwiki.getPluginManager().destroy());
  }

}
