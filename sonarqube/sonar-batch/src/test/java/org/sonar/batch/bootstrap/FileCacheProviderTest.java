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

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.home.cache.FileCache;
import static org.assertj.core.api.Assertions.assertThat;

public class FileCacheProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void provide() {
    FileCacheProvider provider = new FileCacheProvider();
    FileCache cache = provider.provide(new Settings());

    assertThat(cache).isNotNull();
    assertThat(cache.getDir()).isNotNull().exists();
  }

  @Test
  public void keep_singleton_instance() {
    FileCacheProvider provider = new FileCacheProvider();
    Settings settings = new Settings();
    FileCache cache1 = provider.provide(settings);
    FileCache cache2 = provider.provide(settings);

    assertThat(cache1).isSameAs(cache2);
  }

  @Test
  public void honor_sonarUserHome() throws IOException {
    FileCacheProvider provider = new FileCacheProvider();
    Settings settings = new Settings();
    File f = temp.newFolder();
    settings.appendProperty("sonar.userHome", f.getAbsolutePath());
    FileCache cache = provider.provide(settings);

    assertThat(cache.getDir()).isEqualTo(new File(f, "cache"));
  }
}
