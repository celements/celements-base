/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package com.xpn.xwiki.doc;

import static com.celements.spring.context.SpringContextProvider.*;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;

import com.celements.store.att.AttachmentContentPolicy;
import com.xpn.xwiki.XWikiException;

public class XWikiAttachmentArchive implements Cloneable {

  private static final Logger LOG = LoggerFactory.getLogger(XWikiAttachmentArchive.class);

  private XWikiAttachment attachment;

  public long getId() {
    return this.attachment.getId();
  }

  public void setId(long id) {}

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() {
    XWikiAttachmentArchive attachmentarchive = null;
    try {
      attachmentarchive = getClass().newInstance();
    } catch (Exception e) {
      // This should not happen
      LOG.error("Error while attachmentArchive.clone()", e);
    }

    attachmentarchive.setAttachment(getAttachment());
    attachmentarchive.setRCSArchive(getRCSArchive());

    return attachmentarchive;
  }

  // Document Archive
  private Archive archive;

  /**
   * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
   */
  @Deprecated
  public Archive getRCSArchive() {
    return this.archive;
  }

  /**
   * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
   */
  @Deprecated
  public void setRCSArchive(Archive archive) {
    this.archive = archive;
  }

  public byte[] getArchive() throws XWikiException {
    if (this.archive == null) {
      return new byte[0];
    } else {
      return this.archive.toByteArray();
    }
  }

  public void setArchive(byte[] data) throws XWikiException {
    if ((data == null) || (data.length == 0)) {
      this.archive = null;
    } else {
      try {
        // attachment.fromXML(data.toString());
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        this.archive = new Archive(getAttachment().getFilename(), is);

      } catch (Exception e) {
        Object[] args = { getAttachment().getFilename() };
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
            "Exception while manipulating the archive for file {0}", e, args);
      }
    }
  }

  /**
   * Update the archive.
   *
   * @param data
   *          not used for anything, the data is loaded from the attachment included with this
   *          archive.
   * @param context
   *          the XWikiContext for the request used to load the correct attachment content from the
   *          database.
   */
  public void updateArchive() throws XWikiException {
    try {
      this.attachment.incrementVersion();
      this.attachment.setDate(new Date());
      boolean includeContent = getAttachmentContentPolicy().includeInArchive();
      var xml = this.attachment.toStringXML(includeContent, false)
          .lines().toArray(String[]::new);
      if (this.archive != null) {
        this.archive.addRevision(xml, "");
      } else {
        this.archive = new Archive(xml, getAttachment().getFilename(),
            getAttachment().getVersion());
      }
    } catch (Exception e) {
      Object[] args = { getAttachment().getFilename() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
          "Exception while manipulating the archive for file {0}", e, args);
    }
  }

  public void rebuildArchive(boolean includeContent) throws XWikiException {
    if (archive == null) {
      return;
    }
    try {
      Archive rebuilt = null;
      for (Version v : getVersions()) {
        var xml = getRevision(v).toStringXML(includeContent, false)
            .lines().toArray(String[]::new);
        if (rebuilt == null) {
          rebuilt = new Archive(xml, getAttachment().getFilename(), v.toString());
        } else {
          rebuilt.addRevision(xml, "");
        }
      }
      this.archive = rebuilt;
    } catch (Exception e) {
      Object[] args = { getAttachment().getFilename() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
          "Exception while manipulating the archive for file {0}", e, args);
    }
  }

  public XWikiAttachment getAttachment() {
    return this.attachment;
  }

  public void setAttachment(XWikiAttachment attachment) {
    this.attachment = attachment;
  }

  public Version[] getVersions() {
    Node[] nodes = (archive != null) ? archive.changeLog() : new Node[0];
    Version[] versions = new Version[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      versions[i] = nodes[i].getVersion();
    }
    return versions;
  }

  public XWikiAttachment getRevision(String rev) throws XWikiException {
    if ((rev == null) || (archive == null)) {
      return null;
    }
    return getRevision(archive.getRevisionVersion(rev));
  }

  public XWikiAttachment getRevision(Version v) throws XWikiException {
    try {
      if (Objects.equals(v, attachment.getRCSVersion())) {
        return attachment;
      } else if ((v == null) || (archive == null)) {
        return null;
      }
      String xml = Stream.of(archive.getRevision(v))
          .map(Object::toString)
          .collect(Collectors.joining("\n"));
      XWikiAttachment revattach = new XWikiAttachment();
      revattach.fromXML(xml);
      revattach.setDoc(attachment.getDoc());
      revattach.setVersion(v.toString());
      return revattach;
    } catch (Exception e) {
      Object[] args = { attachment.getFilename() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
          "Exception while manipulating the archive for file {0}", e, args);
    }
  }

  private AttachmentContentPolicy getAttachmentContentPolicy() {
    return getBeanFactory().getBean(AttachmentContentPolicy.class);
  }
}
