package com.celements.mandatory;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.filebase.IFileBaseAccessRole;
import com.celements.model.reference.RefBuilder;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component(FileBaseDefaultDoc.HINT_NAME)
public class FileBaseDefaultDoc extends AbstractMandatoryDocument {

  public static final String HINT_NAME = "celements.mandatory.filebase_defaultdoc";

  private static final Logger LOGGER = LoggerFactory.getLogger(FileBaseDefaultDoc.class);

  @Override
  public List<String> dependsOnMandatoryDocuments() {
    return Collections.emptyList();
  }

  @Override
  public String getName() {
    return "FileBaseDefaultDoc";
  }

  @Override
  protected boolean skip() {
    return modelContext.isMainWiki();
  }

  @Override
  protected DocumentReference getDocRef() {
    return new RefBuilder().with(modelContext.getWikiRef())
        .space(IFileBaseAccessRole.FILE_BASE_DEFAULT_DOC_SPACE)
        .doc(IFileBaseAccessRole.FILE_BASE_DEFAULT_DOC_NAME).build(DocumentReference.class);
  }

  @Override
  protected boolean checkDocuments(XWikiDocument doc) throws XWikiException {
    boolean isDirty = !modelContext.isMainWiki() && !modelAccess.exists(getDocRef());
    LOGGER.debug("FileBaseDefaultDoc: checkDocuments returing {}", isDirty);
    return isDirty;
  }

  @Override
  protected boolean checkDocumentsMain(XWikiDocument doc) throws XWikiException {
    return false;
  }

  @Override
  public Logger getLogger() {
    return LOGGER;
  }

}
