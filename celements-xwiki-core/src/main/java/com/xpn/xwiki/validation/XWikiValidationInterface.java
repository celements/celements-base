package com.xpn.xwiki.validation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public interface XWikiValidationInterface {

  boolean validateDocument(XWikiDocument doc, XWikiContext context);

  boolean validateObject(BaseObject object, XWikiContext context);
}
