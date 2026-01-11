package com.xpn.xwiki.store;

import org.xwiki.component.annotation.ComponentRole;

import com.xpn.xwiki.doc.XWikiAttachmentContent;

@ComponentRole
public interface AttachmentContentStore {

  void saveContent(XWikiAttachmentContent content) throws AttachmentContentStoreException;

  void loadContent(XWikiAttachmentContent content) throws AttachmentContentStoreException;

  void deleteContent(XWikiAttachmentContent content) throws AttachmentContentStoreException;

  public static class AttachmentContentStoreException extends Exception {

    private static final long serialVersionUID = 1L;

    public AttachmentContentStoreException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
