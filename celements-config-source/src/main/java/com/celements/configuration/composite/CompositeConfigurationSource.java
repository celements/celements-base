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
package com.celements.configuration.composite;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.MoreOptional;
import com.google.common.collect.ImmutableList;

/**
 * Allows composing (aka chaining) several Configuration Sources. The order of sources is important.
 * Sources located before other sources take priority.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class CompositeConfigurationSource implements ConfigurationSource {

  /**
   * The order of sources is important. Sources located before other sources take priority.
   */
  private final List<ConfigurationSource> sources;

  public CompositeConfigurationSource(List<ConfigurationSource> sources) {
    this.sources = ImmutableList.copyOf(sources);
  }

  public List<ConfigurationSource> getSources() {
    return sources;
  }

  @Override
  public boolean containsKey(String key) {
    return getSources().stream().anyMatch(source -> source.containsKey(key));
  }

  @Override
  public <T> Stream<T> stream(String key, Class<T> type) {
    return getSources().stream()
        .filter(source -> source.containsKey(key))
        .flatMap(src -> src.<T>stream(key, type))
        .filter(Objects::nonNull);
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    return getSources().stream()
        .filter(source -> source.containsKey(key))
        .map(src -> src.<T>get(key, type))
        .flatMap(MoreOptional::stream)
        .findFirst();
  }

  @Override
  public List<String> getKeys() {
    return getSources().stream()
        .flatMap(source -> source.getKeys().stream())
        .distinct()
        .collect(toList());
  }

  @Override
  public boolean isEmpty() {
    return getSources().stream().allMatch(ConfigurationSource::isEmpty);
  }

}
