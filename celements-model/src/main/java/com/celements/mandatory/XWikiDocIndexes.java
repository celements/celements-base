package com.celements.mandatory;

import org.xwiki.component.annotation.Component;

@Component(XWikiDocIndexes.NAME)
public class XWikiDocIndexes extends AbstractMandatoryIndex {

  public static final String NAME = "celements.mandatory.XWikiDocIndexes";

  @Override
  protected String getTableName() {
    return "xwikidoc";
  }

  @Override
  protected String getIndexName() {
    return "fulnameIDX";
  }

  @Override
  protected String getAddSql() {
    return "ALTER TABLE " + getTableName()
        + " ADD UNIQUE INDEX `" + getIndexName() + "` (`XWD_FULLNAME`,`XWD_LANGUAGE`),"
        + " ADD INDEX `webNameUNIQUEIDX` (`XWD_WEB`(150),`XWD_NAME`(150),`XWD_LANGUAGE`),"
        + " ADD INDEX `webIDX` (`XWD_WEB`), " + "ADD INDEX `menuSelectINDX`"
        + " (`XWD_FULLNAME`(100),`XWD_TRANSLATION`,`XWD_PARENT`(100),`XWD_WEB`(100)),"
        + " ADD INDEX `languageIDX` (`XWD_LANGUAGE`),"
        + " ADD INDEX `elementsIDX` (`XWD_ELEMENTS`),"
        + " ADD INDEX `classXMLindex` (`XWD_CLASS_XML`(30))";
  }

}
