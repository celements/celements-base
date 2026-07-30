package com.celements.mandatory;

import org.xwiki.component.annotation.Component;

@Component(XWikiObjectUniqueConstraint.NAME)
public class XWikiObjectUniqueConstraint extends AbstractMandatoryIndex {

  public static final String NAME = "XWikiObjectUniqueConstraint";

  @Override
  protected String getTableName() {
    return "xwikiobjects";
  }

  @Override
  protected String getIndexName() {
    return "uniqueObjIDX";
  }

  @Override
  protected String getAddSql() {
    return "alter table " + getTableName() + " add constraint `" + getIndexName()
        + "` unique (XWO_NAME, XWO_CLASSNAME, XWO_NUMBER)";
  }

}
