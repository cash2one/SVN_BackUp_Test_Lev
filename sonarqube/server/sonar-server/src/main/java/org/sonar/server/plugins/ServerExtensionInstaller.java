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
package org.sonar.server.plugins;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Map;
import org.sonar.api.Extension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.SonarPlugin;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

/**
 * Loads the plugins server extensions and injects them to DI container
 */
@ServerSide
public class ServerExtensionInstaller {

  private final PluginRepository pluginRepository;

  public ServerExtensionInstaller(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  public void installExtensions(ComponentContainer container) {
    ListMultimap<PluginInfo, Object> installedExtensionsByPlugin = ArrayListMultimap.create();

    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      try {
        String pluginKey = pluginInfo.getKey();
        SonarPlugin plugin = pluginRepository.getPluginInstance(pluginKey);
        container.addExtension(pluginInfo, plugin);

        for (Object extension : plugin.getExtensions()) {
          if (installExtension(container, pluginInfo, extension, true) != null) {
            installedExtensionsByPlugin.put(pluginInfo, extension);
          } else {
            container.declareExtension(pluginInfo, extension);
          }
        }
      } catch (Throwable e) {
        // catch Throwable because we want to catch Error too (IncompatibleClassChangeError, ...)
        throw new IllegalStateException(String.format("Fail to load plugin %s [%s]", pluginInfo.getName(), pluginInfo.getKey()), e);
      }
    }
    for (Map.Entry<PluginInfo, Object> entry : installedExtensionsByPlugin.entries()) {
      PluginInfo pluginInfo = entry.getKey();
      try {
        Object extension = entry.getValue();
        if (isExtensionProvider(extension)) {
          ExtensionProvider provider = (ExtensionProvider) container.getComponentByKey(extension);
          installProvider(container, pluginInfo, provider);
        }
      } catch (Throwable e) {
        // catch Throwable because we want to catch Error too (IncompatibleClassChangeError, ...)
        throw new IllegalStateException(String.format("Fail to load plugin %s [%s]", pluginInfo.getName(), pluginInfo.getKey()), e);
      }
    }
  }

  private void installProvider(ComponentContainer container, PluginInfo pluginInfo, ExtensionProvider provider) {
    Object obj = provider.provide();
    if (obj != null) {
      if (obj instanceof Iterable) {
        for (Object ext : (Iterable) obj) {
          installExtension(container, pluginInfo, ext, false);
        }
      } else {
        installExtension(container, pluginInfo, obj, false);
      }
    }
  }

  Object installExtension(ComponentContainer container, PluginInfo pluginInfo, Object extension, boolean acceptProvider) {
    if (AnnotationUtils.getAnnotation(extension, ServerSide.class) != null) {
      if (!acceptProvider && isExtensionProvider(extension)) {
        throw new IllegalStateException("ExtensionProvider can not include providers itself: " + extension);
      }
      container.addExtension(pluginInfo, extension);
      return extension;
    }
    return null;
  }

  static boolean isExtensionProvider(Object extension) {
    return isType(extension, ExtensionProvider.class) || extension instanceof ExtensionProvider;
  }

  static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = extension instanceof Class ? (Class) extension : extension.getClass();
    return extensionClass.isAssignableFrom(clazz);
  }
}
