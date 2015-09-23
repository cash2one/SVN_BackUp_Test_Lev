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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.SonarPlugin;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.RemotePlugin;
import org.sonar.core.platform.RemotePluginFile;
import org.sonar.home.cache.FileCache;

/**
 * Downloads the plugins installed on server and stores them in a local user cache
 * (see {@link FileCacheProvider}).
 */
public class BatchPluginInstaller implements PluginInstaller {

  private static final Logger LOG = Loggers.get(BatchPluginInstaller.class);
  private static final String PLUGINS_INDEX_URL = "/deploy/plugins/index.txt";

  private final WSLoader wsLoader;
  private final FileCache fileCache;
  private final BatchPluginPredicate pluginPredicate;
  private final ServerClient serverClient;

  public BatchPluginInstaller(WSLoader wsLoader, ServerClient serverClient, FileCache fileCache, BatchPluginPredicate pluginPredicate) {
    this.wsLoader = wsLoader;
    this.fileCache = fileCache;
    this.pluginPredicate = pluginPredicate;
    this.serverClient = serverClient;
  }

  @Override
  public Map<String, PluginInfo> installRemotes() {
    return loadPlugins(listRemotePlugins());
  }

  private Map<String, PluginInfo> loadPlugins(List<RemotePlugin> remotePlugins) {
    Map<String, PluginInfo> infosByKey = new HashMap<>();

    Profiler profiler = Profiler.create(LOG).startDebug("Load plugins");

    for (RemotePlugin remotePlugin : remotePlugins) {
      if (pluginPredicate.apply(remotePlugin.getKey())) {
        File jarFile = download(remotePlugin);
        PluginInfo info = PluginInfo.create(jarFile);
        infosByKey.put(info.getKey(), info);
      }
    }

    profiler.stopDebug();
    return infosByKey;
  }

  /**
   * Returns empty on purpose. This method is used only by tests.
   * @see org.sonar.batch.mediumtest.BatchMediumTester
   */
  @Override
  public Map<String, SonarPlugin> installLocals() {
    return Collections.emptyMap();
  }

  @VisibleForTesting
  File download(final RemotePlugin remote) {
    try {
      final RemotePluginFile file = remote.file();
      return fileCache.get(file.getFilename(), file.getHash(), new FileDownloader(remote.getKey()));
    } catch (Exception e) {
      throw new IllegalStateException("Fail to download plugin: " + remote.getKey(), e);
    }
  }

  /**
   * Gets information about the plugins installed on server (filename, checksum)
   */
  @VisibleForTesting
  List<RemotePlugin> listRemotePlugins() {
    try {
      String pluginIndex = loadPluginIndex();
      String[] rows = StringUtils.split(pluginIndex, CharUtils.LF);
      List<RemotePlugin> result = Lists.newArrayList();
      for (String row : rows) {
        result.add(RemotePlugin.unmarshal(row));
      }
      return result;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to load plugin index: " + PLUGINS_INDEX_URL, e);
    }
  }

  private String loadPluginIndex() {
    Profiler profiler = Profiler.create(LOG).startInfo("Load plugins index");
    WSLoaderResult<String> wsResult = wsLoader.loadString(PLUGINS_INDEX_URL);
    profiler.stopInfo(wsResult.isFromCache());
    return wsResult.get();
  }

  private class FileDownloader implements FileCache.Downloader {
    private String key;

    FileDownloader(String key) {
      this.key = key;
    }

    @Override
    public void download(String filename, File toFile) throws IOException {
      String url = "/deploy/plugins/" + key + "/" + filename;
      if (LOG.isDebugEnabled()) {
        LOG.debug("Download {} to {}", url, toFile.getAbsolutePath());
      } else {
        LOG.info("Download {}", filename);
      }

      serverClient.download(url, toFile);
    }
  }
}
