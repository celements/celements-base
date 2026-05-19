package com.celements.filebase;

import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;
import com.celements.filebase.exceptions.FileBaseAddFileException;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.exceptions.FileBaseTagCreateException;
import com.celements.filebase.exceptions.FileBaseTagDeleteException;
import com.celements.filebase.exceptions.FileBaseTagRenameException;
import com.celements.filebase.exceptions.FileNotExistsException;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.xpn.xwiki.doc.XWikiAttachment;

@ComponentRole
public interface IFileBaseServiceRole {

  public static final String FILEBASE_CONFIG_FIELD = "cel_centralfilebase";

  public boolean hasListingRight(@NotNull String dirPath, @Nullable User user);

  public boolean hasUploadRight(@NotNull String dirPath, @Nullable User user);

  public boolean existsFileNameEqual(@NotNull String filename) throws FileBaseLoadException;

  @NotNull
  public XWikiAttachment getFileNameEqual(@NotNull String filename) throws FileNotExistsException,
      FileBaseLoadException;

  @NotNull
  public List<XWikiAttachment> getFilesNameMatch(@NotNull IAttachmentMatcher attMatcher)
      throws FileBaseLoadException;

  @NotNull
  public XWikiAttachment addFile(@NotNull InputStream in, @NotNull String filename,
      @Nullable String username, @Nullable String comment) throws FileBaseAddFileException;

  @NotNull
  public XWikiAttachment addFile(@NotNull InputStream in, @NotNull String filename,
      @Nullable String comment) throws FileBaseAddFileException;

  public int deleteFileList(@NotNull List<String> files);

  @NotNull
  public List<FileBaseTag> getFileTags();

  @NotNull
  public DocumentReference createFileTag(@NotNull String label) throws FileBaseTagCreateException;

  public void deleteFileTag(@NotNull DocumentReference tagRef) throws FileBaseTagDeleteException;

  public void renameFileTag(@NotNull DocumentReference tagRef, @NotNull String newLabel)
      throws FileBaseTagRenameException;
}
