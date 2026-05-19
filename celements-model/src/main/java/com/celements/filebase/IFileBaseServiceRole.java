package com.celements.filebase;

import java.io.InputStream;
import java.util.List;

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

        public boolean hasListingRight(String dirPath, User user);

        public boolean hasUploadRight(String dirPath, User user);

        public boolean existsFileNameEqual(String filename) throws FileBaseLoadException;

        public XWikiAttachment getFileNameEqual(String filename) throws FileNotExistsException,
                        FileBaseLoadException;

        public List<XWikiAttachment> getFilesNameMatch(IAttachmentMatcher attMatcher)
                        throws FileBaseLoadException;

        public XWikiAttachment addFile(InputStream in, String filename, String username,
                        String comment)
                        throws FileBaseAddFileException;

        public XWikiAttachment addFile(InputStream in, String filename, String comment)
                        throws FileBaseAddFileException;

        public int deleteFileList(List<String> files);

        public List<FileBaseTag> getFileTags();

        public DocumentReference createFileTag(String label) throws FileBaseTagCreateException;

        public void deleteFileTag(DocumentReference tagRef) throws FileBaseTagDeleteException;

        public void renameFileTag(DocumentReference tagRef, String newLabel)
                        throws FileBaseTagRenameException;
}
