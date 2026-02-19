package com.xpn.xwiki.store.hibernate;

import javax.inject.Named;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.store.AttachmentContentStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;

@Component
@Named(HibernateAttachmentContentStore.STORE_NAME)
public class HibernateAttachmentContentStore extends XWikiHibernateBaseStore
    implements AttachmentContentStore {

  public static final String STORE_NAME = "store.attachment.content.hibernate";

  @Override
  public String getStoreName() {
    return STORE_NAME;
  }

  @Override
  public void saveContent(XWikiAttachmentContent content) throws AttachmentContentStoreException {
    logger.info("saveContent - {}", content.getAttachment());
    try {
      Session session = getSession();
      Query query = session.createQuery(
          "select attach.id from XWikiAttachmentContent as attach where attach.id = :id");
      query.setLong("id", content.getId());
      if (query.uniqueResult() == null) {
        session.save(content);
      } else {
        session.update(content);
      }
    } catch (HibernateException e) {
      throw new AttachmentContentStoreException("Failed saving attachment", e);
    }
  }

  @Override
  public void loadContent(XWikiAttachmentContent content) throws AttachmentContentStoreException {
    logger.info("loadContent - {}", content.getAttachment());
    try {
      Session session = getSession();
      session.load(content, Long.valueOf(content.getId()));
    } catch (HibernateException e) {
      throw new AttachmentContentStoreException("Failed loading attachment", e);
    }
  }

  @Override
  public void deleteContent(XWikiAttachment attachment) throws AttachmentContentStoreException {
    logger.info("deleteContent - {}", attachment);
    try {
      Session session = getSession();
      session.createQuery("delete from " + XWikiAttachmentContent.class.getName() + " where id = ?")
          .setLong(0, attachment.getId())
          .executeUpdate();
    } catch (Exception e) {
      throw new AttachmentContentStoreException("Failed deleting attachment", e);
    }
  }

  @Override
  public void deleteContent(XWikiAttachmentContent content) throws AttachmentContentStoreException {
    deleteContent(content.getAttachment());
  }
}
