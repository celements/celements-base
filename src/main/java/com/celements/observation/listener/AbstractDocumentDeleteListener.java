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
package com.celements.observation.listener;

import java.util.Arrays;
import java.util.List;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

public abstract class AbstractDocumentDeleteListener extends AbstractDocumentListener {

  @Override
  public List<Event> getEvents() {
    return Arrays.<Event>asList(new DocumentDeletingEvent(), new DocumentDeletedEvent());
  }

  @Override
  protected Event getNotifyEvent(Event event, XWikiDocument doc) {
    Event notifyEvent = null;
    if (event instanceof DocumentDeletedEvent) {
      doc = doc.getOriginalDocument();
    }
    if ((doc != null) && (getRequiredObj(doc) != null)) {
      if (event instanceof DocumentDeletingEvent) {
        notifyEvent = getDeletingEvent(doc.getDocumentReference());
      } else if (event instanceof DocumentDeletedEvent) {
        notifyEvent = getDeletedEvent(doc.getDocumentReference());
      }
    }
    return notifyEvent;
  }

  @Override
  protected Event getCreatingEvent(DocumentReference docRef) {
    return null;
  }

  @Override
  protected Event getCreatedEvent(DocumentReference docRef) {
    return null;
  }

  @Override
  protected Event getUpdatingEvent(DocumentReference docRef) {
    return null;
  }

  @Override
  protected Event getUpdatedEvent(DocumentReference docRef) {
    return null;
  }

}
