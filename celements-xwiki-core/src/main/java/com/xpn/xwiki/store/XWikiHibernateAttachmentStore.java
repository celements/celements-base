package com.xpn.xwiki.store;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.spring.context.SpringContextProvider.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.Session;
import org.xwiki.component.annotation.Component;

import com.celements.store.StoreFactory;
import com.google.common.base.Suppliers;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class XWikiHibernateAttachmentStore extends XWikiHibernateBaseStore
    implements XWikiAttachmentStoreInterface {

  private final Supplier<AttachmentContentStore> contentStore = Suppliers
      .memoize(StoreFactory::getAttachmentContentStore);
  private final Supplier<Optional<AttachmentContentStore>> contentStoreFallback = Suppliers
      .memoize(() -> Optional.of(getBeanFactory()
          .getBean(HibernateAttachmentContentStore.STORE_NAME, AttachmentContentStore.class))
          .filter(store -> !store.getStoreName().equals(contentStore.get().getStoreName())));
  private final Supplier<AttachmentVersioningStore> versioningStore = Suppliers
      .memoize(StoreFactory::getAttachmentVersioningStore);

  /**
   * Empty constructor needed for component manager.
   */
  public XWikiHibernateAttachmentStore() {}

  @Override
  public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    saveAttachmentContent(attachment, true, context, bTransaction);
  }

  @Override
  public void saveAttachmentContent(XWikiAttachment attachment, boolean parentUpdate,
      XWikiContext context, boolean bTransaction) throws XWikiException {
    try {
      XWikiAttachmentContent content = attachment.getAttachment_content();
      if (content.isContentDirty()) {
        attachment.updateContentArchive();
      }
      if (bTransaction) {
        bTransaction = beginTransaction();
      }
      String db = context.getDatabase();
      String attachdb = (attachment.getDoc() == null) ? null : attachment.getDoc().getDatabase();
      try {
        if (attachdb != null) {
          context.setDatabase(attachdb);
        }
        getContentStore().saveContent(content);
        if (attachment.getAttachment_archive() == null) {
          attachment.loadArchive();
        }
        getVersioningStore().saveArchive(attachment.getAttachment_archive(), false);
        if (parentUpdate) {
          context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, true);
        }
      } finally {
        context.setDatabase(db);
      }
      if (bTransaction) {
        endTransaction(true);
      }
    } catch (Exception e) {
      Object[] args = { attachment };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
          "Exception while saving {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(false);
        }
      } catch (Exception e) {}
    }

  }

  @Override
  public void saveAttachmentsContent(List<XWikiAttachment> attachments, XWikiDocument doc,
      boolean bParentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException {
    if (attachments == null) {
      return;
    }
    try {
      if (bTransaction) {
        bTransaction = beginTransaction();
      }
      for (XWikiAttachment att : attachments) {
        saveAttachmentContent(att, false, context, false);
      }
      if (bParentUpdate) {
        context.getWiki().getStore().saveXWikiDoc(doc, context, false);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
          "Exception while saving attachments", e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(false);
        }
      } catch (Exception e) {}
    }

  }

  @Override
  public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    try {
      if (bTransaction) {
        bTransaction = beginTransaction();
      }
      String db = context.getDatabase();
      String attachdb = (attachment.getDoc() == null) ? null : attachment.getDoc().getDatabase();
      try {
        if (attachdb != null) {
          context.setDatabase(attachdb);
        }
        XWikiAttachmentContent content = new XWikiAttachmentContent(attachment);
        attachment.setAttachment_content(content);
        try {
          getContentStore().loadContent(content);
        } catch (Exception e) {
          // content load failed, try legacy fallback store first before rethrowing
          contentStoreFallback.get().ifPresent(rethrowConsumer(s -> s.loadContent(content)));
          contentStoreFallback.get().orElseThrow(() -> e);
        }
        content.setContentDirty(false);
      } finally {
        context.setDatabase(db);
      }
      if (bTransaction) {
        endTransaction(false);
      }
    } catch (Exception e) {
      Object[] args = { attachment };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
          "Exception while loading {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(false);
        }
      } catch (Exception e) {}
    }
  }

  @Override
  public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    deleteXWikiAttachment(attachment, true, context, bTransaction);
  }

  @Override
  public void deleteXWikiAttachment(XWikiAttachment attachment, boolean parentUpdate,
      XWikiContext context, boolean bTransaction) throws XWikiException {
    try {
      if (bTransaction) {
        bTransaction = beginTransaction();
      }
      Session session = getSession();
      String db = context.getDatabase();
      String attachdb = (attachment.getDoc() == null) ? null : attachment.getDoc().getDatabase();
      try {
        if (attachdb != null) {
          context.setDatabase(attachdb);
        }
        // delete attachment content
        try {
          loadAttachmentContent(attachment, context, false);
          var content = attachment.getAttachment_content();
          try {
            getContentStore().deleteContent(content);
          } catch (Exception e) {
            logger.info("Error deleting content for {}", attachment);
          }
          try {
            contentStoreFallback.get().ifPresent(rethrowConsumer(s -> s.deleteContent(content)));
          } catch (Exception e) {
            logger.info("Error deleting content for {}", attachment);
          }
        } catch (Exception e) {
          logger.warn("Error loading content when deleting {}", attachment);
        }
        // delete attachment archive
        getVersioningStore().deleteArchive(attachment, false);
        // delete attachment meta data
        try {
          session.delete(attachment);
        } catch (Exception e) {
          logger.warn("Error deleting meta data for {}", attachment);
        }
      } finally {
        context.setDatabase(db);
      }
      // update parent document
      try {
        if (parentUpdate) {
          var iter = attachment.getDoc().getAttachmentList().iterator();
          while (iter.hasNext()) {
            if (attachment.getFilename().equals(iter.next().getFilename())) {
              iter.remove();
              break;
            }
          }
          context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
        }
      } catch (Exception e) {
        logger.warn("Error updating document when deleting {}", attachment);
      }
      if (bTransaction) {
        endTransaction(true);
      }
    } catch (Exception e) {
      Object[] args = { attachment };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_ATTACHMENT,
          "Exception while deleting {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(false);
        }
      } catch (Exception e) {}
    }
  }

  @Override
  public AttachmentContentStore getContentStore() {
    return contentStore.get();
  }

  @Override
  public AttachmentVersioningStore getVersioningStore() {
    return versioningStore.get();
  }
}
