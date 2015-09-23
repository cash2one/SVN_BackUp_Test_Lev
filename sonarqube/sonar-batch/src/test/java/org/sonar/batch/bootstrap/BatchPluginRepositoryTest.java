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
package org.sonar.batch.bootstrap;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sonar.api.SonarPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatchPluginRepositoryTest {

  PluginInstaller installer = mock(PluginInstaller.class);
  PluginLoader loader = mock(PluginLoader.class);
  BatchPluginRepository underTest = new BatchPluginRepository(installer, loader);

  @Test
  public void install_and_load_plugins() {
    PluginInfo info = new PluginInfo("squid");
    ImmutableMap<String, PluginInfo> infos = ImmutableMap.of("squid", info);
    SonarPlugin instance = mock(SonarPlugin.class);
    when(loader.load(infos)).thenReturn(ImmutableMap.of("squid", instance));
    when(installer.installRemotes()).thenReturn(infos);

    underTest.start();

    assertThat(underTest.getPluginInfos()).containsOnly(info);
    assertThat(underTest.getPluginInfo("squid")).isSameAs(info);
    assertThat(underTest.getPluginInstance("squid")).isSameAs(instance);

    underTest.stop();
    verify(loader).unload(anyCollectionOf(SonarPlugin.class));
  }

  @Test
  public void fail_if_requesting_missing_plugin() {
    underTest.start();

    try {
      underTest.getPluginInfo("unknown");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Plugin [unknown] does not exist");
    }
    try {
      underTest.getPluginInstance("unknown");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Plugin [unknown] does not exist");
    }
  }
}
