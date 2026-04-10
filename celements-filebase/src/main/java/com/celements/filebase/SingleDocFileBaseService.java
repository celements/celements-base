package com.celements.filebase;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.filebase.exceptions.FileBaseAddFileException;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.exceptions.FileNotExistsException;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.navigation.cmd.MultilingualMenuNameCommand;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.web.service.IWebUtilsService;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
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
  private final ITreeNodeService treeNodeService;
  private final ModelContext modelContext;
  private final IWebUtilsService webUtilsService;
  private final MultilingualMenuNameCommand menuNameCmd = new MultilingualMenuNameCommand();

  public SingleDocFileBaseService(IAttachmentServiceRole attService, IModelAccessFacade modelAccess,
      ITreeNodeService treeNodeService, ModelUtils modelUtils, ConfigurationSource configuration,
      ModelContext modelContext, IWebUtilsService webUtilsService) {
    this.attService = attService;
    this.modelAccess = modelAccess;
    this.modelUtils = modelUtils;
    this.configuration = configuration;
    this.treeNodeService = treeNodeService;
    this.modelContext = modelContext;
    this.webUtilsService = webUtilsService;
  }

  XWikiDocument getFileBaseDoc() throws FileBaseLoadException {
    var fileBaseDocRefOpt = getFileBaseDocRef();
    if (fileBaseDocRefOpt.isPresent()) {
      try {
        return modelAccess.getOrCreateDocument(fileBaseDocRefOpt.get());
      } catch (DocumentLoadException exp) {
        throw new FileBaseLoadException(modelUtils.serializeRef(fileBaseDocRefOpt.get()), exp);
      }
    } else {
      throw new FileBaseLoadException("Filebase document not configured");
    }
  }

  private Optional<DocumentReference> getFileBaseDocRef() {
    String fileBaseDocFN = configuration.getProperty(FILEBASE_CONFIG_FIELD);
    if (!Strings.isNullOrEmpty(fileBaseDocFN) && !"-".equals(fileBaseDocFN)) {
      return Optional.of(modelUtils.resolveRef(fileBaseDocFN, DocumentReference.class));
    } else {
      return Optional.empty();
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

  @Override
  public int deleteFileList(List<String> filenames) {
    try {
      final DocumentReference fileBaseDocRef = getFileBaseDocRef()
          .orElseThrow(() -> new FileBaseLoadException("Filebase document not configured"));
      return attService.deleteAttachmentList(filenames.stream()
          .filter(fn -> {
            try {
              return existsFileNameEqual(fn);
            } catch (FileBaseLoadException e) {
              LOGGER.error("FileBase loading failed. Skipping delete for " + fn, e);
              return false;
            }
          })
          .map(fn -> new AttachmentReference(fn, fileBaseDocRef))
          .collect(Collectors.toList()));
    } catch (FileBaseLoadException exp) {
      LOGGER.error("FileBase loading failed.", exp);
      return -1;
    }
  }

  @Override
  public List<FileBaseTag> getFileTags() {
    return getFileBaseDocRef()
        .map(fileBaseDocRef -> fileBaseDocRef.getLastSpaceReference())
        .map(tagSpaceRef -> treeNodeService.getSubNodesForParent(tagSpaceRef, "").stream()
            .map(treeNode -> buildFileBaseTag(treeNode.getDocumentReference()))
            .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }


  private FileBaseTag buildFileBaseTag(DocumentReference docRef) {
    XWikiContext ctx = modelContext.getXWikiContext();
    Map<String, String> names = webUtilsService.getAllowedLanguages().stream()
        .collect(LinkedHashMap::new,
            (map, lang) -> map.put(lang,
                menuNameCmd.getMultilingualMenuName(
                    modelUtils.serializeRefLocal(docRef), lang, ctx)),
            Map::putAll);
    return new FileBaseTag(docRef, names, modelAccess, modelUtils, modelContext);
  }

}
