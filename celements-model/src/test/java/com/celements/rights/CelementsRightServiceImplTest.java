package com.celements.rights;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.rights.publication.IPublicationServiceRole;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.objects.BaseObject;

public class CelementsRightServiceImplTest extends AbstractComponentTest {

  private IPublicationServiceRole publicationService;
  private CelementsRightServiceImpl rightService;

  @Before
  public void prepareTest() throws Exception {
    publicationService = registerComponentMock(IPublicationServiceRole.class);
    rightService = new CelementsRightServiceImpl();
  }

  @Test
  public void test_checkRight_celDocumentAppliesPublicationCheck() throws Exception {
    getXContext().setDatabase("wiki");
    XWikiDocument document = new XWikiDocument(
        new DocumentReference("wiki", "Space", "Page"));
    BaseObject rightsObject = new BaseObject();
    rightsObject.setXClassReference(
        new DocumentReference("wiki", "XWiki", "XWikiRights"));
    rightsObject.setStringValue("levels", "view");
    rightsObject.setStringValue("users", "XWiki.User");
    rightsObject.setIntValue("allow", 1);
    document.addXObject(rightsObject);
    CelDocument.Default celDocument = CelDocument.Default.from(document);

    expect(publicationService.isPublishActive()).andReturn(true);
    expect(publicationService.isRestrictedRightsAction("view")).andReturn(true);
    expect(publicationService.isPubOverride()).andReturn(false);
    expect(publicationService.isUnpubOverride()).andReturn(false);
    expect(publicationService.isPublished(same(celDocument))).andReturn(true);

    replayDefault();
    assertTrue(rightService.checkRight("XWiki.User", celDocument, "view", true, true, false,
        getXContext()));
    verifyDefault();
  }
}
