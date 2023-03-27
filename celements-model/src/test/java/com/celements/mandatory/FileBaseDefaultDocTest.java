package com.celements.mandatory;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.filebase.IFileBaseAccessRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.reference.RefBuilder;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class FileBaseDefaultDocTest extends AbstractComponentTest {

  private FileBaseDefaultDoc mandatory;
  private WikiReference wikiRef;
  private IModelAccessFacade modelAccessMock;

  @Before
  public void prepareTest() throws Exception {
    modelAccessMock = registerComponentMock(IModelAccessFacade.class);
    mandatory = (FileBaseDefaultDoc) Utils.getComponent(IMandatoryDocumentRole.class,
        FileBaseDefaultDoc.NAME);
    wikiRef = new WikiReference("wiki");
    getContext().setDatabase(wikiRef.getName());
  }

  @Test
  public void test_dependsOnMandatoryDocuments() throws Exception {
    assertEquals(0, mandatory.dependsOnMandatoryDocuments().size());
  }

  @Test
  public void test_checkDocuments() throws Exception {
    DocumentReference docRef = new RefBuilder().wiki(getContext().getDatabase())
        .space(IFileBaseAccessRole.FILE_BASE_DEFAULT_DOC_SPACE)
        .doc(IFileBaseAccessRole.FILE_BASE_DEFAULT_DOC_NAME).build(DocumentReference.class);
    XWikiDocument doc = new XWikiDocument(docRef);
    expect(modelAccessMock.exists(eq(docRef))).andReturn(false).once();
    replayDefault();
    assertTrue(mandatory.checkDocuments(doc));
    verifyDefault();
  }

  @Test
  public void test_getNameName() throws Exception {
    assertEquals("FileBaseDefaultDoc", mandatory.getName());
  }

}
