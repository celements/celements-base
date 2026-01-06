package com.celements.filebase;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.celements.filebase.exceptions.FileBaseAddFileException;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.exceptions.FileNotExistsException;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Strings;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

@Component(SingleDocFileBaseService.FILEBASE_SINGLE_DOC)
public class SingleDocFileBaseService implements IFileBaseServiceRole {

  public static final String FILEBASE_SINGLE_DOC = "filebase.singleDoc";

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleDocFileBaseService.class);

  private final IAttachmentServiceRole attService;
  private final IModelAccessFacade modelAccess;
  private final ModelUtils modelUtils;
  private final ConfigurationSource configuration;

  public SingleDocFileBaseService(IAttachmentServiceRole attService, IModelAccessFacade modelAccess,
      ModelUtils modelUtils, ConfigurationSource configuration) {
    this.attService = attService;
    this.modelAccess = modelAccess;
    this.modelUtils = modelUtils;
    this.configuration = configuration;
  }

  XWikiDocument getFileBaseDoc() throws FileBaseLoadException {
    String fileBaseDocFN = configuration.getProperty(FILEBASE_CONFIG_FIELD);
    if (!Strings.isNullOrEmpty(fileBaseDocFN) && !"-".equals(fileBaseDocFN)) {
      try {
        DocumentReference fileBaseDocRef = modelUtils.resolveRef(fileBaseDocFN,
            DocumentReference.class);
        return modelAccess.getOrCreateDocument(fileBaseDocRef);
      } catch (DocumentLoadException exp) {
        throw new FileBaseLoadException(fileBaseDocFN, exp);
      }
    } else {
      throw new FileBaseLoadException(fileBaseDocFN);
    }
  }

  @Override
  public boolean existsFileNameEqual(String filename) throws FileBaseLoadException {
    return attService.existsAttachmentNameEqual(getFileBaseDoc(), filename);
  }

  @Override
  public XWikiAttachment getFileNameEqual(String filename) throws FileNotExistsException,
      FileBaseLoadException {
    try {
      return attService.getAttachmentNameEqual(getFileBaseDoc(), filename);
    } catch (AttachmentNotExistsException attNotExistsExp) {
      LOGGER.trace("failed to get file in filebase. ", attNotExistsExp);
      throw new FileNotExistsException(filename);
    }
  }

  @Override
  public List<XWikiAttachment> getFilesNameMatch(IAttachmentMatcher attMatcher)
      throws FileBaseLoadException {
    return attService.getAttachmentsNameMatch(getFileBaseDoc(), attMatcher);
  }

  @Override
  public XWikiAttachment addFile(InputStream in, String filename, String username, String comment)
      throws FileBaseAddFileException {
    try {
      String safeName = attService.clearFileName(filename);
      return attService.addAttachment(getFileBaseDoc(), in, safeName, username, comment);
    } catch (DocumentSaveException | AttachmentToBigException
        | AddingAttachmentContentFailedException | FileBaseLoadException exp) {
      throw new FileBaseAddFileException("Failed to add file '" + filename + "'", exp);
    }
  }

  @Override
  public XWikiAttachment addFile(InputStream in, String filename, String comment)
      throws FileBaseAddFileException {
    return addFile(in, filename, null, comment);
  }

}
