package com.celements.wiki.classes;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.celements.common.classes.XClassCreateException;
import com.celements.common.classes.XClassCreator;
import com.celements.init.MainXClassInitializer;

@Component
public class XWikiServerClassInitializer implements MainXClassInitializer {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(XWikiServerClassInitializer.class);

  private final XClassCreator xClassCreator;
  private final XWikiServerClass serverClass;

  @Inject
  public XWikiServerClassInitializer(
      XClassCreator xClassCreator,
      XWikiServerClass serverClass) {
    this.xClassCreator = xClassCreator;
    this.serverClass = serverClass;
  }

  @Override
  public void run() throws ExecutionException {
    LOGGER.trace("MainWikiClassPackageInitializer run");
    try {
      xClassCreator.createXClass(serverClass);
    } catch (XClassCreateException exc) {
      throw new ExecutionException(exc);
    }
  }

}
