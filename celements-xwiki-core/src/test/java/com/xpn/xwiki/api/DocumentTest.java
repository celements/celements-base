package com.xpn.xwiki.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractComponentTest;

import junit.framework.Assert;

public class DocumentTest extends AbstractComponentTest {

  @Test
  public void test_toString_returnsFullName() {
    XWikiDocument doc = new XWikiDocument("Space", "Page");
    assertEquals("Space.Page", new Document(doc, new XWikiContext()).toString());
    assertEquals("Main.WebHome", new Document(new XWikiDocument(), new XWikiContext())
        .toString());
  }

  @Test
  public void test_getObjects() throws XWikiException {
    XWikiContext context = new XWikiContext();
    XWikiDocument doc = new XWikiDocument("Wiki", "Space", "Page");

    doc.getxWikiClass().addNumberField("prop", "prop", 5, "long");
    BaseObject obj = (BaseObject) doc.getxWikiClass().newObject(context);
    obj.setLongValue("prop", 1);
    doc.addObject(doc.getFullName(), obj);

    assertEquals(obj, doc.getObject(doc.getFullName(), "prop", "1"));
    assertNull(doc.getObject(doc.getFullName(), "prop", "2"));

    Document adoc = new Document(doc, context);
    List<Object> lst = adoc.getObjects(adoc.getFullName(), "prop", "1");
    assertEquals(1, lst.size());
    assertEquals(obj, lst.get(0).getBaseObject());

    lst = adoc.getObjects(adoc.getFullName(), "prop", "0");
    assertEquals(0, lst.size());

    lst = adoc.getObjects(adoc.getFullName());
    assertEquals(1, lst.size());
  }

  @Test
  public void test_removeObject_doesntCauseDataLoss() throws XWikiException {
    BaseClass c = new BaseClass();
    c.setDocumentReference(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
    c.addTextAreaField("comment", "comment", 60, 20);
    expect(getWikiMock().getXClass(anyObject(DocumentReference.class), same(getContext())))
        .andReturn(c).anyTimes();
    replayDefault();

    XWikiDocument doc = new XWikiDocument("Wiki", "Space", "Page");

    for (int i = 0; i < 10; ++i) {
      doc.newObject("XWiki.XWikiComments", getContext());
    }

    Document adoc = new Document(doc, getContext());

    for (Object obj : adoc.getObjects("XWiki.XWikiComments")) {
      obj.set("comment", "Comment");
      if (obj.getNumber() == 4) {
        adoc.removeObject(obj);
      }
    }

    // Let's make sure the original document wasn't changed
    for (BaseObject obj : doc.getObjects("XWiki.XWikiComments")) {
      Assert.assertNull(obj.get("comment"));
    }

    // Let's make sure the cloned document was changed everywhere
    for (BaseObject obj : adoc.getDoc().getObjects("XWiki.XWikiComments")) {
      if (obj != null) {
        Assert.assertEquals("Comment", ((BaseProperty) obj.get("comment")).getValue());
      }
    }
    verifyDefault();
  }
}
