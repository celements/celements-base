package com.celements.filebase;

import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.auth.user.User;
import com.celements.filebase.exceptions.FileBaseAddFileException;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.exceptions.FileBaseTagCreateException;
import com.celements.filebase.exceptions.FileBaseTagDeleteException;
import com.celements.filebase.exceptions.FileBaseTagRenameException;
import com.celements.filebase.exceptions.FileNotExistsException;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.context.ModelContext;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ModelUtils;
import com.celements.navigation.INavigationClassConfig;
import com.celements.navigation.cmd.MultilingualMenuNameCommand;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.web.service.IWebUtilsService;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component(SingleDocFileBaseService.FILEBASE_SINGLE_DOC)
public class SingleDocFileBaseService implements IFileBaseServiceRole {

  public static final String FILEBASE_SINGLE_DOC = "filebase.singleDoc";

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleDocFileBaseService.class);

  private final IAttachmentServiceRole attService;
  private final IModelAccessFacade modelAccess;
  private final IRightsAccessFacadeRole rightsAccess;
  private final ModelUtils modelUtils;
  private final ConfigurationSource configuration;
  private final ITreeNodeService treeNodeService;
  private final ModelContext modelContext;
  private final IWebUtilsService webUtilsService;
  private final INextFreeDocRole nextFreeDoc;
  private final MultilingualMenuNameCommand menuNameCmd = new MultilingualMenuNameCommand();

  public SingleDocFileBaseService(IAttachmentServiceRole attService, IModelAccessFacade modelAccess,
      IRightsAccessFacadeRole rightsAccess, ITreeNodeService treeNodeService, ModelUtils modelUtils,
      ConfigurationSource configuration, ModelContext modelContext,
      IWebUtilsService webUtilsService,
      INextFreeDocRole nextFreeDoc) {
    this.attService = attService;
    this.modelAccess = modelAccess;
    this.rightsAccess = rightsAccess;
    this.modelUtils = modelUtils;
    this.configuration = configuration;
    this.treeNodeService = treeNodeService;
    this.modelContext = modelContext;
    this.webUtilsService = webUtilsService;
    this.nextFreeDoc = nextFreeDoc;
  }

  XWikiDocument getFileBaseDoc() throws FileBaseLoadException {
    DocumentReference fileBaseDocRef = getFileBaseDocRef()
        .orElseThrow(() -> new FileBaseLoadException("Filebase document not configured"));
    try {
      return modelAccess.getOrCreateDocument(fileBaseDocRef);
    } catch (DocumentLoadException exp) {
      throw new FileBaseLoadException(modelUtils.serializeRef(fileBaseDocRef), exp);
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
  public boolean hasListingRight(String dirPath, @Nullable User user) {
    boolean hasListingRight = hasRight(user, EAccessLevel.VIEW);
    if (!hasListingRight) {
      String fileBaseDocFN = configuration.getProperty(FILEBASE_CONFIG_FIELD);
      LOGGER.debug(
          "Listing denied for filebase path '{}' and user '{}' on filebase document '{}'",
          dirPath, user, fileBaseDocFN);
    }
    return hasListingRight;
  }

  @Override
  public boolean hasUploadRight(String dirPath, User user) {
    return hasRight(user, EAccessLevel.EDIT);
  }

  @Override
  public boolean hasDeleteRight(String dirPath, User user) {
    return hasRight(user, EAccessLevel.DELETE);
  }

  private boolean hasRight(User user, EAccessLevel accessLevel) {
    return getFileBaseDocRef()
        .filter(docRef -> rightsAccess.hasAccessLevel(docRef, accessLevel, user))
        .isPresent();
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
          .filter(rethrowPredicate(this::existsFileNameEqual))
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

  @Override
  public DocumentReference createFileTag(String label) throws FileBaseTagCreateException {
    try {
      DocumentReference fileBaseDocRef = getFileBaseDocRef()
          .orElseThrow(() -> new FileBaseTagCreateException("Filebase document not configured"));
      SpaceReference spaceRef = fileBaseDocRef.getLastSpaceReference();
      DocumentReference tagDocRef = nextFreeDoc.getNextTitledPageDocRef(spaceRef, "tag");
      XWikiDocument tagDoc = modelAccess.getOrCreateDocument(tagDocRef);
      BaseObject menuItemObj = XWikiObjectEditor.on(tagDoc)
          .filter(INavigationClassConfig.MENU_ITEM_CLASS_REF)
          .createFirstIfNotExists();
      menuItemObj.setIntValue(INavigationClassConfig.MENU_POSITION_FIELD, getFileTags().size());
      menuItemObj.setStringValue("menu_parent", "");
      menuItemObj.setStringValue(INavigationClassConfig.PART_NAME_FIELD, "");
      for (String lang : webUtilsService.getAllowedLanguages()) {
        ClassField<String> langField = new StringField.Builder(
            INavigationClassConfig.MENU_NAME_CLASS_REF, INavigationClassConfig.MENU_NAME_LANG_FIELD)
            .build();
        BaseObject menuNameObj = XWikiObjectEditor.on(tagDoc)
            .filter(INavigationClassConfig.MENU_NAME_CLASS_REF)
            .filter(langField, lang)
            .createFirstIfNotExists();
        menuNameObj.setStringValue(INavigationClassConfig.MENU_NAME_LANG_FIELD, lang);
        menuNameObj.setStringValue(INavigationClassConfig.MENU_NAME_FIELD, label);
      }
      modelAccess.saveDocument(tagDoc, "Tag created via MediaLib");
      return tagDocRef;
    } catch (Exception e) {
      throw new FileBaseTagCreateException("Failed to create tag: " + label, e);
    }
  }

  @Override
  public void deleteFileTag(DocumentReference tagRef) throws FileBaseTagDeleteException {
    try {
      modelAccess.deleteDocument(tagRef, true);
    } catch (Exception e) {
      throw new FileBaseTagDeleteException("Failed to delete tag", e);
    }
  }

  @Override
  public void renameFileTag(DocumentReference tagRef, String newLabel)
      throws FileBaseTagRenameException {
    try {
      XWikiDocument tagDoc = modelAccess.getOrCreateDocument(tagRef);
      for (String lang : webUtilsService.getAllowedLanguages()) {
        ClassField<String> langField = new StringField.Builder(
            INavigationClassConfig.MENU_NAME_CLASS_REF, INavigationClassConfig.MENU_NAME_LANG_FIELD)
            .build();
        BaseObject menuNameObj = XWikiObjectEditor.on(tagDoc)
            .filter(INavigationClassConfig.MENU_NAME_CLASS_REF)
            .filter(langField, lang)
            .createFirstIfNotExists();
        menuNameObj.setStringValue(INavigationClassConfig.MENU_NAME_LANG_FIELD, lang);
        menuNameObj.setStringValue(INavigationClassConfig.MENU_NAME_FIELD, newLabel);
      }
      modelAccess.saveDocument(tagDoc, "Tag renamed via MediaLib");
    } catch (Exception e) {
      throw new FileBaseTagRenameException("Failed to rename tag", e);
    }
  }

}
