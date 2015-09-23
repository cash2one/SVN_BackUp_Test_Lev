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
package org.sonar.server.startup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.platform.RemotePlugin;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

@ServerSide
public final class GeneratePluginIndex {

  private DefaultServerFileSystem fileSystem;
  private PluginRepository repository;

  public GeneratePluginIndex(DefaultServerFileSystem fileSystem, PluginRepository repository) {
    this.fileSystem = fileSystem;
    this.repository = repository;
  }

  public void start() throws IOException {
    writeIndex(fileSystem.getPluginIndex());
  }

  void writeIndex(File indexFile) throws IOException {
    FileUtils.forceMkdir(indexFile.getParentFile());
    Writer writer = new OutputStreamWriter(new FileOutputStream(indexFile), StandardCharsets.UTF_8);
    try {
      for (PluginInfo pluginInfo : repository.getPluginInfos()) {
        writer.append(RemotePlugin.create(pluginInfo).marshal());
        writer.append(CharUtils.LF);
      }
      writer.flush();

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }
}
