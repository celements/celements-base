package com.celements.store.att;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.celements.init.XWikiProvider;
import com.xpn.xwiki.store.HibernateAttachmentContentStore;

/**
 * Policy component that decides whether attachment binary content should be embedded into
 * XML-based structures produced by XWiki, namely the attachment archive (RCS/XML history) and the
 * attachment recycle bin entry (deleted attachment XML snapshot).
 * <p>
 * The decision is based on the currently active {@code AttachmentContentStore} implementation.
 * When the legacy Hibernate content store is used, attachment content is expected to be present in
 * the serialized XML (RCS archive and recycle bin) because the Hibernate-based implementations
 * historically persisted/consumed the bytes from there.
 * <p>
 * When an alternative content storage is used (for example object storage), embedding raw bytes
 * into these XML payloads is typically undesirable (size, duplication) and may be incompatible with
 * the alternative store's lookup strategy. In that case, content should be excluded and retrieved
 * via the configured {@code AttachmentContentStore} instead.
 */
@Component
public class AttachmentContentPolicy {

  private final XWikiProvider xwikiProvider;

  @Inject
  public AttachmentContentPolicy(XWikiProvider xwikiProvider) {
    this.xwikiProvider = xwikiProvider;
  }

  /**
   * Determines whether attachment content should be embedded into the attachment archive XML.
   */
  public boolean includeInArchive() {
    return isHibernateAttachmentContentStore();
  }

  /**
   * Determines whether attachment content should be embedded into recycle bin XML.
   */
  public boolean includeInRecycleBin() {
    return isHibernateAttachmentContentStore();
  }

  private boolean isHibernateAttachmentContentStore() {
    return xwikiProvider.get().orElseThrow(IllegalStateException::new)
        .getAttachmentStore()
        .getContentStore()
        .getStoreName()
        .equals(HibernateAttachmentContentStore.STORE_NAME);
  }

}
