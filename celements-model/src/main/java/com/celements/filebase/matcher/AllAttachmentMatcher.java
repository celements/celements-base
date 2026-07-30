package com.celements.filebase.matcher;

import com.xpn.xwiki.doc.XWikiAttachment;

public class AllAttachmentMatcher implements IAttachmentMatcher {

  @Override
  public boolean accept(XWikiAttachment attachment) {
    return true;
  }

}
