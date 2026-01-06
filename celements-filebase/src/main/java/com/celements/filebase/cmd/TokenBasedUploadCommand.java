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
 */
package com.celements.filebase.cmd;

import javax.inject.Inject;

import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.UserService;
import com.celements.filebase.IAttachmentServiceRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.ModelContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class TokenBasedUploadCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenBasedUploadCommand.class);

  private final IAttachmentServiceRole attService;
  private final IModelAccessFacade modelAccess;
  private final UserService userService;
  private final ModelContext context;

  @Inject
  public TokenBasedUploadCommand(IAttachmentServiceRole attService, IModelAccessFacade modelAccess,
      UserService userService, ModelContext context) {
    this.attService = attService;
    this.modelAccess = modelAccess;
    this.userService = userService;
    this.context = context;
  }

  public int tokenBasedUploadDocRef(DocumentReference attachToDocRef, String fieldNamePrefix,
      String userToken, boolean createIfNotExists)
      throws XWikiException {
    String username = userService.getUsernameForToken(userToken);
    if (!Strings.isNullOrEmpty(username)) {
      LOGGER.info("tokenBasedUpload: user '{}' identified by userToken.", username);
      context.getXWikiContext().setUser(username);
      if (createIfNotExists || modelAccess.exists(attachToDocRef)) {
        XWikiDocument doc = modelAccess.getOrCreateDocument(attachToDocRef);
        LOGGER.info("tokenBasedUpload: add attachment '{}' to doc '{}'.", fieldNamePrefix,
            attachToDocRef);
        if (LOGGER.isTraceEnabled()) {
          for (XWikiAttachment origAttach : doc.getAttachmentList()) {
            LOGGER.trace("tokenBasedUpload - origialDoc before addAttachments: '{}', '{}'",
                origAttach.getFilename(), origAttach.getVersion());
          }
        }
        return attService.uploadMultipleAttachments(doc, fieldNamePrefix);
      } else {
        LOGGER.warn("tokenBasedUpload: document '{}' does not exist.", attachToDocRef);
      }
    } else {
      LOGGER.warn("tokenBasedUpload: username could not be identified by token");
    }
    return 0;
  }

}
