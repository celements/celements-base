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
package com.xpn.xwiki.internal.event;

import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

/**
 * Base class for all comment {@link org.xwiki.observation.event.Event events}.
 *
 * @version $Id$
 * @since 2.6RC2
 */
public abstract class AbstractCommentEvent extends AbstractDocumentEvent {

  /**
   * The version identifier for this Serializable class. Increment only if the <i>serialized</i>
   * form of the class
   * changes.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The comment that is used in events.
   */
  private String identifier;

  /**
   * Constructor initializing the event filter with an
   * {@link org.xwiki.observation.event.filter.AlwaysMatchingEventFilter}, meaning that this event
   * will match any
   * other comment event (add, update, delete).
   */
  public AbstractCommentEvent() {}

  /**
   * Constructor initializing the event filter with a
   * {@link org.xwiki.observation.event.filter.FixedNameEventFilter},
   * meaning that this event will match only comment events affecting the document matching the
   * passed document name.
   *
   * @param documentName
   *          the name of the updated document to match
   * @param identifier
   *          the identifier of the comment added/updated/deleted
   */
  public AbstractCommentEvent(String documentName, String identifier) {
    // TODO: depreciate this constructor and add a constructor taking a document reference instead
    super(new FixedNameEventFilter(documentName));
    this.identifier = identifier;
  }

  /**
   * Constructor using a custom {@link EventFilter}.
   *
   * @param eventFilter
   *          the filter to use for matching events
   */
  public AbstractCommentEvent(EventFilter eventFilter) {
    super(eventFilter);
  }

  /**
   * Retrieves the content of the comment added/updated/deleted in the event.
   *
   * @return comment identifier
   */
  public String getIdentifier() {
    return identifier;
  }

}
