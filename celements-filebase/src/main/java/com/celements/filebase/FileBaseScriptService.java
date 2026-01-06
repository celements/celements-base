package com.celements.filebase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.celements.filebase.cmd.TokenBasedUploadCommand;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.exceptions.FileNotExistsException;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.celements.rights.access.exceptions.NoAccessRightsException;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;

@Component("filebase")
public class FileBaseScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileBaseScriptService.class);

  private final IAttachmentServiceRole attachmentService;
  private final IFileBaseServiceRole filebaseService;
  private final TokenBasedUploadCommand tokenbasedUploadCmd;

  @Inject
  public FileBaseScriptService(IAttachmentServiceRole attachmentService,
      @Named(SingleDocFileBaseService.FILEBASE_SINGLE_DOC) IFileBaseServiceRole filebaseService,
      TokenBasedUploadCommand tokenbasedUploadCmd) {
    this.attachmentService = attachmentService;
    this.filebaseService = filebaseService;
    this.tokenbasedUploadCmd = tokenbasedUploadCmd;
  }

  public String clearFileName(String fileName) {
    return attachmentService.clearFileName(fileName);
  }

  public int tokenBasedUpload(DocumentReference attachToDocRef, String fieldName,
      String userToken) {
    return tokenBasedUpload(attachToDocRef, fieldName, userToken, false);
  }

  public int tokenBasedUpload(DocumentReference attachToDocRef, String fieldName, String userToken,
      Boolean createIfNotExists) {
    try {
      return tokenbasedUploadCmd.tokenBasedUploadDocRef(attachToDocRef, fieldName,
          userToken, createIfNotExists);
    } catch (XWikiException exp) {
      LOGGER.error("token based attachment upload failed: ", exp);
    }
    return 0;
  }

  public int deleteAttachmentList(List<AttachmentReference> attachmentRefList) {
    return attachmentService.deleteAttachmentList(attachmentRefList);
  }

  public boolean existsFileNameEqual(String filename) throws FileBaseLoadException {
    return filebaseService.existsFileNameEqual(filename);
  }

  public Attachment getFileNameEqual(String filename) throws FileBaseLoadException {
    try {
      XWikiAttachment xwikiAtt = filebaseService.getFileNameEqual(filename);
      return attachmentService.getApiAttachment(xwikiAtt);
    } catch (FileNotExistsException e) {
      LOGGER.trace("Filebase could not find file [{}]", filename);
    } catch (NoAccessRightsException nare) {
      LOGGER.info("User {} was refused {} access on file base document {}", nare.getUser(),
          nare.getExpectedAccessLevel(), nare.getEntityRef());
    }
    return null;
  }

  public List<Attachment> getFilesNameMatch(IAttachmentMatcher attMatcher)
      throws FileBaseLoadException {
    List<XWikiAttachment> xwikiAttList = filebaseService.getFilesNameMatch(attMatcher);
    List<Attachment> attList = new ArrayList<>();
    for (XWikiAttachment xwikiAtt : xwikiAttList) {
      try {
        attList.add(attachmentService.getApiAttachment(xwikiAtt));
      } catch (NoAccessRightsException nare) {
        LOGGER.info("User {} was refused {} access on file base document {}", nare.getUser(),
            nare.getExpectedAccessLevel(), nare.getEntityRef());
      }
    }
    return attList;
  }
}
