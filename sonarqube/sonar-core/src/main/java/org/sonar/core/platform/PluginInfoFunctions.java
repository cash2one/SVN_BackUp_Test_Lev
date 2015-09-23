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

package org.sonar.core.platform;

import com.google.common.base.Function;
import javax.annotation.Nonnull;

public final class PluginInfoFunctions {

  private PluginInfoFunctions() {
    // utility class
  }

  public static Function<PluginInfo, String> toName() {
    return PluginInfoToName.INSTANCE;
  }

  public static Function<PluginInfo, String> toKey() {
    return PluginInfoToKey.INSTANCE;
  }

  private enum PluginInfoToName implements Function<PluginInfo, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull PluginInfo input) {
      return input.getName();
    }
  }

  private enum PluginInfoToKey implements Function<PluginInfo, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull PluginInfo input) {
      return input.getKey();
    }
  }
}
