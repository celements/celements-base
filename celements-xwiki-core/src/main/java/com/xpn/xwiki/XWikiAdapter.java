package com.xpn.xwiki;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

@ComponentRole
public interface XWikiAdapter {

  boolean exists(DocumentReference documentReference);

  XWikiDocument getDocument(XWikiDocument doc) throws XWikiException;

  void saveDocument(XWikiDocument doc, String comment, boolean isMinorEdit) throws XWikiException;

  void deleteDocument(XWikiDocument doc, boolean totrash) throws XWikiException;

}
