package com.celements.filebase;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.filebase.exceptions.NoValidFileBaseImplFound;

@ComponentRole
public interface IFileBaseAccessRole {

  public static final String FILEBASE_SERVICE_IMPL_CFG = "filebase_service_impl";
  public static final String FILE_BASE_DEFAULT_DOC = "Content_attachments.FileBaseDoc";

  IFileBaseServiceRole getInstance() throws NoValidFileBaseImplFound;

}
