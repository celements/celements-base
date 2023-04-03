package com.celements.mandatory;

import org.xwiki.component.annotation.Component;

@Component(XWikiAttachmentIndexes.NAME)
public class XWikiAttachmentIndexes extends AbstractMandatoryIndex {

  public static final String NAME = "celements.mandatory.XWikiAttachmentIndexes";

  @Override
  protected String getTableName() {
    return "xwikiattachment";
  }

  @Override
  protected String getIndexName() {
    return "docIdIDX";
  }

  @Override
  protected String getAddSql() {
    return "ALTER TABLE xwikiattachment"
        + " ADD index docIdIDX (XWA_DOC_ID)";
  }

}
