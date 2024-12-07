package com.celements.mandatory;

import static com.celements.execution.XWikiExecutionProp.*;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;

import com.celements.init.DiskDocumentImporter;
import com.xpn.xwiki.XWikiException;

@Component(MandatoryDiskDocuments.NAME)
public class MandatoryDiskDocuments implements IMandatoryDocumentRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(MandatoryDiskDocuments.class);
  private static final String NAME = "celements.mandatory.MandatoryDiskDocuments";

  private final DiskDocumentImporter importer;
  private final Execution execution;

  @Inject
  public MandatoryDiskDocuments(DiskDocumentImporter importer, Execution execution) {
    this.importer = importer;
    this.execution = execution;
  }

  @Override
  public int order() {
    return 10000;
  }

  @Override
  public List<String> dependsOnMandatoryDocuments() {
    return List.of();
  }

  @Override
  public void checkDocuments() throws XWikiException {
    LOGGER.info("check mandatory disk docs");
    execution.getContext().get(WIKI)
        .ifPresent(wikiRef -> importer.importForWiki(wikiRef, false));
  }

}
