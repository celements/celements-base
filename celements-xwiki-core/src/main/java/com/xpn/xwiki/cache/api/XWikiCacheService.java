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

package com.xpn.xwiki.cache.api;

import java.util.Properties;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;

@Deprecated
public interface XWikiCacheService {

  /**
   * Initializes the service
   */
  void init(XWiki context);

  /*
   * Returns a local only (never clustered) cache
   */
  XWikiCache newLocalCache() throws XWikiException;

  /*
   * Returns a local only (never clustered) cache with given capacity
   */
  XWikiCache newLocalCache(int capacity) throws XWikiException;

  /*
   * Returns a cache that could be configured to be clustered
   */
  XWikiCache newCache(String cacheName) throws XWikiException;

  /*
   * Returns a cache that could be configured to be clustered with the given capacity
   */
  XWikiCache newCache(String cacheName, int capacity) throws XWikiException;

  /*
   * Returns a custom cache
   */
  XWikiCache newCache(String cacheName, Properties props) throws XWikiException;

  /*
   * Returns a custom local cache
   */
  XWikiCache newLocalCache(Properties props) throws XWikiException;

  /*
   * Returns a custom cache with capacity
   */
  XWikiCache newCache(String cacheName, Properties props, int capacity)
      throws XWikiException;

  /*
   * Returns a custom local cache with capacity
   */
  XWikiCache newLocalCache(Properties props, int capacity) throws XWikiException;

}
