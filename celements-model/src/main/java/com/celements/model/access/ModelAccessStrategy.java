package com.celements.model.access;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentSaveException;
import com.xpn.xwiki.doc.XWikiDocument;

@ComponentRole
public interface ModelAccessStrategy {

  boolean exists(@NotNull DocumentReference docRef);

  @NotNull
  XWikiDocument getDocument(@NotNull DocumentReference docRef, @NotNull String lang);

  void saveDocument(@NotNull XWikiDocument doc) throws DocumentSaveException;

  void deleteDocument(@NotNull XWikiDocument doc, boolean totrash)
      throws DocumentDeleteException;

  @NotNull
  List<String> getTranslations(@NotNull DocumentReference docRef);

}
