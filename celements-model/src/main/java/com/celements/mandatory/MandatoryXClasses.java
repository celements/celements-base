package com.celements.mandatory;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.celements.common.classes.IClassesCompositorComponent;
import com.xpn.xwiki.XWikiException;

@Component(MandatoryXClasses.NAME)
public class MandatoryXClasses implements IMandatoryDocumentRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(MandatoryXClasses.class);
  private static final String NAME = "celements.mandatory.MandatoryXClasses";

  private IClassesCompositorComponent classesCompositor;

  @Inject
  public MandatoryXClasses(IClassesCompositorComponent classesCompositor) {
    this.classesCompositor = classesCompositor;
  }

  @Override
  public List<String> dependsOnMandatoryDocuments() {
    return List.of();
  }

  @Override
  public void checkDocuments() throws XWikiException {
    LOGGER.info("check xClasses");
    classesCompositor.checkClasses();
  }

}
