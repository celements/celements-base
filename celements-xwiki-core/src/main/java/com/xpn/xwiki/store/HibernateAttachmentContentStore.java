package com.xpn.xwiki.store;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.xpn.xwiki.doc.XWikiAttachmentContent;

@Component
public class HibernateAttachmentContentStore extends XWikiHibernateBaseStore
    implements AttachmentContentStore {

  @Override
  public void saveContent(XWikiAttachmentContent content) throws AttachmentContentStoreException {
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
    try {
      Session session = getSession();
      session.load(content, Long.valueOf(content.getId()));
    } catch (HibernateException e) {
      throw new AttachmentContentStoreException("Failed loading attachment", e);
    }
  }

  @Override
  public void deleteContent(XWikiAttachmentContent content) throws AttachmentContentStoreException {
    try {
      Session session = getSession();
      session.delete(content);
    } catch (Exception e) {
      throw new AttachmentContentStoreException("Failed deleting attachment", e);
    }
  }
}
