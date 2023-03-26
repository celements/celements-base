package com.celements.filebase;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.filebase.exceptions.NoValidFileBaseImplFound;

@ComponentRole
public interface IFileBaseAccessRole {

  public static final String FILEBASE_SERVICE_IMPL_CFG = "filebase_service_impl";
  public static final String FILE_BASE_DEFAULT_DOC_SPACE = "Content_attachments";
  public static final String FILE_BASE_DEFAULT_DOC_NAME = "FileBaseDoc";
  public static final String FILE_BASE_DEFAULT_DOC_FN = FILE_BASE_DEFAULT_DOC_SPACE + "."
      + FILE_BASE_DEFAULT_DOC_NAME;

  IFileBaseServiceRole getInstance() throws NoValidFileBaseImplFound;

}
