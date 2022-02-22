package com.xpn.xwiki.validation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class XWikiDefaultValidation implements XWikiValidationInterface {

  @Override
  public boolean validateDocument(XWikiDocument doc, XWikiContext context) {
    return true;
  }

  @Override
  public boolean validateObject(BaseObject object, XWikiContext context) {
    return true;
  }
}
