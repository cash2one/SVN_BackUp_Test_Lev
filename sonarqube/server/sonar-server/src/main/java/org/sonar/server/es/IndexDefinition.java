/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.es;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.sonar.api.server.ServerSide;

import java.util.Map;

@ServerSide
public interface IndexDefinition {

  public static class IndexDefinitionContext {
    private final Map<String, NewIndex> byKey = Maps.newHashMap();

    public NewIndex create(String key) {
      Preconditions.checkArgument(!byKey.containsKey(key), String.format("Index already exists: %s", key));
      NewIndex index = new NewIndex(key);
      byKey.put(key, index);
      return index;
    }

    public Map<String, NewIndex> getIndices() {
      return byKey;
    }
  }

  void define(IndexDefinitionContext context);

}
