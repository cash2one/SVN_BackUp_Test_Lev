/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.application;

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;

/**
 * @author max
 */
public class ConfigImportHelper {
  /**
   * Holds name of the system property that is supposed to hold <code>'true'</code> value when IDE settings have been
   * imported on the current startup
   */
  @NonNls public static final String CONFIG_IMPORTED_IN_CURRENT_SESSION_KEY = "intellij.config.imported.in.current.session";
  
  @NonNls private static final String BUILD_NUMBER_FILE = SystemInfo.isMac ? "/Resources/build.txt" : "build.txt";
  @NonNls private static final String PLUGINS_PATH = "plugins";
  @NonNls private static final String BIN_FOLDER = "bin";
  @NonNls private static final String CONFIG_RELATED_PATH = SystemInfo.isMac ? "" : "config/";
  @NonNls private static final String OPTIONS_XML = "options/options.xml";

  private ConfigImportHelper() {
  }

  public static void importConfigsTo(@NotNull String newConfigPath) {
    ConfigImportSettings settings = getConfigImportSettings();

    File newConfigDir = new File(newConfigPath);
    File oldConfigDir = findOldConfigDir(newConfigDir, settings.getCustomPathsSelector());
    do {
      ImportOldConfigsPanel dialog = new ImportOldConfigsPanel(oldConfigDir, settings);
      dialog.setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
      AppUIUtil.updateWindowIcon(dialog);
      dialog.setVisible(true);
      if (dialog.isImportEnabled()) {
        File installationHome = dialog.getSelectedFile();
        oldConfigDir = getOldConfigDir(installationHome, settings);
        if (!validateOldConfigDir(installationHome, oldConfigDir, settings)) {
          continue;
        }

        assert oldConfigDir != null;
        doImport(newConfigDir, oldConfigDir, settings, installationHome);
        settings.importFinished(newConfigPath);
        System.setProperty(CONFIG_IMPORTED_IN_CURRENT_SESSION_KEY, Boolean.TRUE.toString());
      }

      break;
    }
    while (true);
  }

  @NotNull
  private static ConfigImportSettings getConfigImportSettings() {
    try {
      @SuppressWarnings("unchecked")
      Class<ConfigImportSettings> customProviderClass =
        (Class<ConfigImportSettings>)Class.forName("com.intellij.openapi.application." + PlatformUtils.getPlatformPrefix() + "ConfigImportSettings");
      if (ConfigImportSettings.class.isAssignableFrom(customProviderClass)) {
        return ReflectionUtil.newInstance(customProviderClass);
      }
    }
    catch (ClassNotFoundException ignored) {
    }
    catch (RuntimeException ignored) {
    }
    return new ConfigImportSettings();
  }

  @Nullable
  private static File findOldConfigDir(@NotNull File configDir, @Nullable String customPathSelector) {
    final File selectorDir = CONFIG_RELATED_PATH.isEmpty() ? configDir : configDir.getParentFile();
    final File parent = selectorDir.getParentFile();
    if (parent == null || !parent.exists()) {
      return null;
    }
    File maxFile = null;
    long lastModified = 0;
    final String selector = PathManager.getPathsSelector() != null ? PathManager.getPathsSelector() : selectorDir.getName();

    final String prefix = getPrefixFromSelector(selector);
    final String customPrefix = customPathSelector != null ? getPrefixFromSelector(customPathSelector) : null;
    for (File file : parent.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(@NotNull File file, @NotNull String name) {
        return StringUtil.startsWithIgnoreCase(name, prefix) ||
               customPrefix != null && StringUtil.startsWithIgnoreCase(name, customPrefix);
      }
    })) {
      File options = new File(file, CONFIG_RELATED_PATH + OPTIONS_XML);
      if (!options.exists()) {
        continue;
      }

      long modified = options.lastModified();
      if (modified > lastModified) {
        lastModified = modified;
        maxFile = file;
      }
    }
    return maxFile != null ? new File(maxFile, CONFIG_RELATED_PATH) : null;
  }

  private static String getPrefixFromSelector(String selector) {
    return (SystemInfo.isMac ? "" : ".") + selector.replaceAll("\\d", "");
  }

  private static void doImport(@NotNull File newConfigDir, @NotNull File oldConfigDir, ConfigImportSettings settings, File installationHome) {
    try {
      copy(oldConfigDir, newConfigDir, settings, installationHome);
    }
    catch (IOException e) {
      JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                    ApplicationBundle.message("error.unable.to.import.settings", e.getMessage()),
                                    ApplicationBundle.message("title.settings.import.failed"), JOptionPane.WARNING_MESSAGE);
    }
  }

  private static boolean validateOldConfigDir(@Nullable File installationHome, @Nullable File oldConfigDir, @NotNull ConfigImportSettings settings) {
    if (oldConfigDir == null) {
      if (installationHome != null) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                      ApplicationBundle.message("error.invalid.installation.home", installationHome.getAbsolutePath(), settings.getProductName(ThreeState.YES)));
      }
      return false;
    }

    if (!oldConfigDir.exists()) {
      JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                    ApplicationBundle.message("error.no.settings.path",
                                                              oldConfigDir.getAbsolutePath()),
                                    ApplicationBundle.message("title.settings.import.failed"), JOptionPane.WARNING_MESSAGE);
      return false;
    }
    return true;
  }

  private static void copy(@NotNull File src, @NotNull File dest, ConfigImportSettings settings, File oldInstallationHome) throws IOException {
    src = src.getCanonicalFile();
    dest = dest.getCanonicalFile();
    if (!src.isDirectory()) {
      throw new IOException(ApplicationBundle.message("config.import.invalid.directory.error", src.getAbsolutePath()));
    }
    if (!dest.isDirectory()) {
      throw new IOException(ApplicationBundle.message("config.import.invalid.directory.error", dest.getAbsolutePath()));
    }
    if (FileUtil.filesEqual(src, dest)) {
      return;
    }

    FileUtil.ensureExists(dest);

    // Copy old plugins. If some of them are incompatible PluginManager will deal with it
    FileUtil.copyDir(src, dest);

    File oldPluginsDir = new File(src, PLUGINS_PATH);
    if (!oldPluginsDir.isDirectory() && SystemInfo.isMac) {
      oldPluginsDir = getSettingsPath(oldInstallationHome, settings, PathManager.PROPERTY_PLUGINS_PATH,
                                      new Function<String, String>() {
                                        @Override
                                        public String fun(String pathSelector) {
                                          return PathManager.getDefaultPluginPathFor(pathSelector);
                                        }
                                      });
      if (oldPluginsDir == null) {
        //e.g. installation home referred to config home. Try with default selector, same as config name
        oldPluginsDir = new File(PathManager.getDefaultPluginPathFor(src.getName()));
      }

      File newPluginsDir = new File(PathManager.getPluginsPath());
      FileUtil.copyDir(oldPluginsDir, newPluginsDir);
    }
    loadOldPlugins(oldPluginsDir, dest);
  }

  private static boolean loadOldPlugins(File plugins, File dest) throws IOException {
    if (plugins.exists()) {
      List<IdeaPluginDescriptorImpl> descriptors = new SmartList<IdeaPluginDescriptorImpl>();
      PluginManagerCore.loadDescriptors(plugins, descriptors, null, 0);
      List<String> oldPlugins = new SmartList<String>();
      for (IdeaPluginDescriptorImpl descriptor : descriptors) {
        // check isBundled also - probably plugin is bundled in new IDE version
        if (descriptor.isEnabled() && !descriptor.isBundled()) {
          oldPlugins.add(descriptor.getPluginId().getIdString());
        }
      }
      if (!oldPlugins.isEmpty()) {
        PluginManagerCore.savePluginsList(oldPlugins, false, new File(dest, PluginManager.INSTALLED_TXT));
      }
      return true;
    }
    return false;
  }

  @Nullable
  public static File getOldConfigDir(@Nullable File oldInstallHome, ConfigImportSettings settings) {
    if (oldInstallHome == null) {
      return null;
    }

    // check if it's already config dir
    if (new File(oldInstallHome, OPTIONS_XML).exists()) {
      return oldInstallHome;
    }
    if (new File(oldInstallHome, CONFIG_RELATED_PATH + OPTIONS_XML).exists()) {
      return new File(oldInstallHome, CONFIG_RELATED_PATH);
    }

    int oldBuildNumber = getBuildNumber(oldInstallHome);

    if (oldBuildNumber != -1 && oldBuildNumber <= 600) { // Pandora
      //noinspection HardCodedStringLiteral
      return new File(oldInstallHome, "config");
    }

    return getSettingsPath(oldInstallHome, settings, PathManager.PROPERTY_CONFIG_PATH, new Function<String, String>() {
      @Override
      public String fun(String pathsSelector) {
        return PathManager.getDefaultConfigPathFor(pathsSelector);
      }
    });
  }

  private static File getSettingsPath(final File installHome,
                                      final ConfigImportSettings settings,
                                      final String propertyName,
                                      final Function<String, String> fromPathSelector) {
    final File[] launchFileCandidates = getLaunchFilesCandidates(installHome, settings);

    // custom config folder
    for (File candidate : launchFileCandidates) {
      if (candidate.exists()) {
        String configDir = PathManager.substituteVars(getPropertyFromLaxFile(candidate, propertyName),
                                                      installHome.getPath());
        if (configDir != null) {
          File probableConfig = new File(configDir);
          if (probableConfig.exists()) return probableConfig;
        }
      }
    }

    // custom config folder not found - use paths selector
    for (File candidate : launchFileCandidates) {
      if (candidate.exists()) {
        final String pathsSelector = getPropertyFromLaxFile(candidate, PathManager.PROPERTY_PATHS_SELECTOR);
        if (pathsSelector != null) {
          final String configDir = fromPathSelector.fun(pathsSelector);
          final File probableConfig = new File(configDir);
          if (probableConfig.exists()) {
            return probableConfig;
          }
        }
      }
    }

    return null;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static File[] getLaunchFilesCandidates(@NotNull final File instHome, @NotNull final ConfigImportSettings settings) {
    final File bin = new File(instHome, BIN_FOLDER);
    final List<File> files = new ArrayList<File>();
    if (SystemInfo.isMac) {
      // Info.plist
      files.add(new File(new File(instHome, "Contents"), "Info.plist"));

      files.add(new File(new File(new File(bin, "idea.app"), "Contents"), "Info.plist"));
      files.add(new File(new File(new File(instHome, "idea.app"), "Contents"), "Info.plist"));
    }
    // idea.properties
    files.add(new File(bin, PathManager.PROPERTIES_FILE_NAME));

    
    // other binary scripts
    final String executableName = StringUtil.toLowerCase(settings.getExecutableName());
    // * defaults:
    addLaunchExecutableScriptsCandidates(files, executableName, bin);
    // * customized files:
    files.addAll(settings.getCustomLaunchFilesCandidates(instHome, bin));
    // * legacy support:
    if (!"idea".equals(executableName)) {
      // for compatibility with some platform-base IDEs with wrong executable names
      addLaunchExecutableScriptsCandidates(files, "idea", bin);
    }
    return files.toArray(new File[files.size()]);
  }

  private static void addLaunchExecutableScriptsCandidates(final List<File> files,
                                                           final String executableName,
                                                           final File binFolder) {
    files.add(new File(binFolder, executableName + ".lax"));
    files.add(new File(binFolder, executableName + ".bat"));
    files.add(new File(binFolder, executableName + ".sh"));
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Nullable
  public static String getPropertyFromLaxFile(@NotNull final File file,
                                              @NotNull final String propertyName) {
    if (file.getName().endsWith(".properties")) {
      try {
        PropertyResourceBundle bundle;
        InputStream fis = new BufferedInputStream(new FileInputStream(file));
        try {
          bundle = new PropertyResourceBundle(fis);
        }
        finally {
          fis.close();
        }
        if (bundle.containsKey(propertyName)) {
          return bundle.getString(propertyName);
        } 
        return null;
      }
      catch (IOException e) {
        return null;
      }
    }
    
    final String fileContent = getContent(file);

    // try to find custom config path
    final String propertyValue = findProperty(propertyName, fileContent);
    if (!StringUtil.isEmpty(propertyValue)) {
      return propertyValue;
    }

    return null;
  }

  @Nullable
  private static String findProperty(final String propertyName, 
                                     final String fileContent) {
    String param = propertyName + "=";
    int idx = fileContent.indexOf(param);
    if (idx == -1) {
      param = "<key>" + propertyName + "</key>";
      idx = fileContent.indexOf(param);
      if (idx == -1) return null;
      idx = fileContent.indexOf("<string>", idx);
      if (idx == -1) return null;
      idx += "<string>".length();
      return fixDirName(fileContent.substring(idx, fileContent.indexOf("</string>", idx)), true);
    }
    else {
      String configDir = "";
      idx += param.length();
      if (fileContent.length() > idx) {
        if (fileContent.charAt(idx) == '"') {
          idx++;
          while ((fileContent.length() > idx) && (fileContent.charAt(idx) != '"') && (fileContent.charAt(idx) != '\n') &&
                 (fileContent.charAt(idx) != '\r')) {
            configDir += fileContent.charAt(idx);
            idx++;
          }
        }
        else {
          while ((fileContent.length() > idx) && (!Character.isSpaceChar(fileContent.charAt(idx))) &&
                 (fileContent.charAt(idx) != '\n') &&
                 (fileContent.charAt(idx) != '\r')) {
            configDir += fileContent.charAt(idx);
            idx++;
          }
        }
      }
      configDir = fixDirName(configDir, true);
      if (configDir.length() > 0) {
        configDir = (new File(configDir)).getPath();
      }
      return configDir;
    }
  }

  @Nullable
  private static String getContent(File file) {
    try {
      StringBuilder content = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      try {
        do {
          String line = reader.readLine();
          if (line == null) break;
          content.append(line);
          content.append('\n');
        }
        while (true);
      }
      finally {
        reader.close();
      }

      return content.toString();
    }
    catch (Exception e) {
      return null;
    }
  }

  public static String fixDirName(String dir, boolean replaceUserHome) {
    if (StringUtil.startsWithChar(dir, '\"') && StringUtil.endsWithChar(dir, '\"')) {
      dir = dir.substring(1, dir.length() - 1);
    }
    if (replaceUserHome) {
      dir = FileUtil.expandUserHome(dir);
    }
    return dir;
  }

  public static boolean isInstallationHomeOrConfig(@NotNull final String installationHome, @NotNull final ConfigImportSettings settings) {
    if (new File(installationHome, OPTIONS_XML).exists()) return true;
    if (new File(installationHome, CONFIG_RELATED_PATH + OPTIONS_XML).exists()) return true;

    if (!new File(installationHome, BIN_FOLDER).exists()) {
      return false;
    }

    File libFolder = new File(installationHome, "lib");
    boolean quickTest = false;
    String[] mainJarNames = settings.getMainJarNames();
    for (String name : mainJarNames) {
      String mainJarName = StringUtil.toLowerCase(name) + ".jar";
      //noinspection HardCodedStringLiteral
      if (new File(libFolder, mainJarName).exists()) {
        quickTest = true;
        break;
      }
    }
    if (!quickTest) return false;

    File[] files = getLaunchFilesCandidates(new File(installationHome), settings);
    for (File file : files) {
      if (file.exists()) return true;
    }

    return false;
  }

  private static int getBuildNumber(File installDirectory) {
    installDirectory = installDirectory.getAbsoluteFile();

    File buildTxt = new File(installDirectory, BUILD_NUMBER_FILE);
    if ((!buildTxt.exists()) || (buildTxt.isDirectory())) {
      buildTxt = new File(new File(installDirectory, BIN_FOLDER), BUILD_NUMBER_FILE);
    }

    if (buildTxt.exists() && !buildTxt.isDirectory()) {
      int buildNumber = -1;
      String buildNumberText = getContent(buildTxt);
      if (buildNumberText != null) {
        try {
          if (buildNumberText.length() > 1) {
            buildNumberText = buildNumberText.trim();
            buildNumber = Integer.parseInt(buildNumberText);
          }
        }
        catch (Exception e) {
          // OK
        }
      }
      return buildNumber;
    }

    return -1;
  }
}
