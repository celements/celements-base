package com.celements.mandatory;

import org.xwiki.component.annotation.Component;

@Component(XWikiObjectIndexes.NAME)
public class XWikiObjectIndexes extends AbstractMandatoryIndex {

  static final String NAME = "celements.mandatory.XWikiObjectIndexes";

  @Override
  protected String getTableName() {
    return "xwikiobjects";
  }

  @Override
  protected String getIndexName() {
    return "docnameIDX";
  }

  @Override
  protected String getAddSql() {
    return "ALTER TABLE " + getTableName()
        + " ADD INDEX `" + getIndexName() + "` (`XWO_NAME`,`XWO_NUMBER`),"
        + " ADD INDEX `classnameIDX` (`XWO_CLASSNAME`),"
        + " ADD INDEX `selectNewsIDX` (`XWO_CLASSNAME`(150),`XWO_NAME`(150))";
  }

}
