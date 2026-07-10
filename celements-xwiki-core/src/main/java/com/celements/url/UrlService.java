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
package com.celements.url;

import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.EntityReference;
import org.springframework.web.util.UriComponentsBuilder;

public interface UrlService {

  @NotNull
  public String getURL(@NotNull EntityReference ref);

  @NotNull
  public String getURL(@NotNull EntityReference ref, @Nullable String action);

  @NotNull
  public String getURL(@NotNull EntityReference ref, @Nullable String action,
      @Nullable String queryString);

  @NotNull
  public String getExternalURL(@NotNull EntityReference ref);

  @NotNull
  public String getExternalURL(@NotNull EntityReference ref, @Nullable String action);

  @NotNull
  public String getExternalURL(@NotNull EntityReference ref, @Nullable String action,
      @Nullable String queryString);

  @NotNull
  UriComponentsBuilder createURIBuilder(@NotNull EntityReference ref);

  @NotNull
  UriComponentsBuilder createURIBuilder(@NotNull EntityReference ref, @Nullable String action);

  /**
   * CAUTION: incomplete implementation. Not all valid strut-actions are recognized
   *
   * @param requestPath
   * @return
   * @throws ExecutionException
   * @deprecated dropped, no replacement
   */
  @Deprecated(since = "6.8.1", forRemoval = true)
  @NotEmpty
  String getActionFromUrl(String requestPath) throws ExecutionException;

}
