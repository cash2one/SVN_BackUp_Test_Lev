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
package org.sonar.batch.scan;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.util.BatchUtils;

/**
 * Class that creates a project definition based on a set of properties.
 */
public class ProjectReactorBuilder {

  private static final String INVALID_VALUE_OF_X_FOR_Y = "Invalid value of {0} for {1}";

  private static final Logger LOG = Loggers.get(ProjectReactorBuilder.class);

  /**
   * @since 4.1 but not yet exposed in {@link CoreProperties}
   */
  private static final String MODULE_KEY_PROPERTY = "sonar.moduleKey";

  protected static final String PROPERTY_PROJECT_BASEDIR = "sonar.projectBaseDir";
  private static final String PROPERTY_PROJECT_BUILDDIR = "sonar.projectBuildDir";
  private static final String PROPERTY_MODULES = "sonar.modules";

  /**
   * New properties, to be consistent with Sonar naming conventions
   *
   * @since 1.5
   */
  private static final String PROPERTY_SOURCES = "sonar.sources";
  private static final String PROPERTY_TESTS = "sonar.tests";
  private static final String PROPERTY_BINARIES = "sonar.binaries";
  private static final String PROPERTY_LIBRARIES = "sonar.libraries";

  /**
   * Array of all mandatory properties required for a project without child.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT = {
    PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY, CoreProperties.PROJECT_NAME_PROPERTY,
    CoreProperties.PROJECT_VERSION_PROPERTY, PROPERTY_SOURCES
  };

  /**
   * Array of all mandatory properties required for a project with children.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT = {PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY,
    CoreProperties.PROJECT_NAME_PROPERTY, CoreProperties.PROJECT_VERSION_PROPERTY};

  /**
   * Array of all mandatory properties required for a child project before its properties get merged with its parent ones.
   */
  protected static final String[] MANDATORY_PROPERTIES_FOR_CHILD = {MODULE_KEY_PROPERTY, CoreProperties.PROJECT_NAME_PROPERTY};

  /**
   * Properties that must not be passed from the parent project to its children.
   */
  private static final List<String> NON_HERITED_PROPERTIES_FOR_CHILD = Lists.newArrayList(PROPERTY_PROJECT_BASEDIR, CoreProperties.WORKING_DIRECTORY, PROPERTY_MODULES,
    CoreProperties.PROJECT_DESCRIPTION_PROPERTY);

  private static final String NON_ASSOCIATED_PROJECT_KEY = "project";

  private final AnalysisProperties taskProps;
  private final AnalysisMode analysisMode;
  private File rootProjectWorkDir;

  public ProjectReactorBuilder(AnalysisProperties props, AnalysisMode analysisMode) {
    this.taskProps = props;
    this.analysisMode = analysisMode;
  }

  public ProjectReactor execute() {
    Profiler profiler = Profiler.create(LOG).startInfo("Process project properties");
    Map<String, Map<String, String>> propertiesByModuleId = new HashMap<>();
    extractPropertiesByModule(propertiesByModuleId, "", taskProps.properties());
    ProjectDefinition rootProject = defineRootProject(propertiesByModuleId.get(""), null);
    rootProjectWorkDir = rootProject.getWorkDir();
    defineChildren(rootProject, propertiesByModuleId);
    cleanAndCheckProjectDefinitions(rootProject);
    // Since task properties are now empty we should add root module properties
    taskProps.properties().putAll(propertiesByModuleId.get(""));
    profiler.stopDebug();
    return new ProjectReactor(rootProject);
  }

  private static void extractPropertiesByModule(Map<String, Map<String, String>> propertiesByModuleId, String currentModuleId, Map<String, String> parentProperties) {
    if (propertiesByModuleId.containsKey(currentModuleId)) {
      throw new IllegalStateException(String.format("Two modules have the same id: %s. Each module must have a unique id.", currentModuleId));
    }

    Map<String, String> currentModuleProperties = new HashMap<>();
    String prefix = !currentModuleId.isEmpty() ? (currentModuleId + ".") : "";
    int prefixLength = prefix.length();

    // By default all properties starting with module prefix belong to current module
    Iterator<Entry<String, String>> it = parentProperties.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> e = it.next();
      String key = e.getKey();
      if (key.startsWith(prefix)) {
        currentModuleProperties.put(key.substring(prefixLength), e.getValue());
        it.remove();
      }
    }
    String[] moduleIds = getListFromProperty(currentModuleProperties, PROPERTY_MODULES);
    // Sort module by reverse lexicographic order to avoid issue when one module id is a prefix of another one
    Arrays.sort(moduleIds);
    ArrayUtils.reverse(moduleIds);

    propertiesByModuleId.put(currentModuleId, currentModuleProperties);

    for (String moduleId : moduleIds) {
      extractPropertiesByModule(propertiesByModuleId, moduleId, currentModuleProperties);
    }
  }

  private static void prepareNonAssociatedProject(Map<String, String> props, AnalysisMode mode) {
    if (mode.isIssues() && !props.containsKey(CoreProperties.PROJECT_KEY_PROPERTY)) {
      props.put(CoreProperties.PROJECT_KEY_PROPERTY, NON_ASSOCIATED_PROJECT_KEY);
    }
  }

  protected ProjectDefinition defineRootProject(Map<String, String> rootProperties, @Nullable ProjectDefinition parent) {
    prepareNonAssociatedProject(rootProperties, analysisMode);

    if (rootProperties.containsKey(PROPERTY_MODULES)) {
      checkMandatoryProperties(rootProperties, MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT);
    } else {
      checkMandatoryProperties(rootProperties, MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT);
    }
    File baseDir = new File(rootProperties.get(PROPERTY_PROJECT_BASEDIR));
    final String projectKey = rootProperties.get(CoreProperties.PROJECT_KEY_PROPERTY);
    File workDir;
    if (parent == null) {
      validateDirectories(rootProperties, baseDir, projectKey);
      workDir = initRootProjectWorkDir(baseDir, rootProperties);
    } else {
      workDir = initModuleWorkDir(baseDir, rootProperties);
    }

    return ProjectDefinition.create().setProperties(rootProperties)
      .setBaseDir(baseDir)
      .setWorkDir(workDir)
      .setBuildDir(initModuleBuildDir(baseDir, rootProperties));
  }

  @VisibleForTesting
  protected File initRootProjectWorkDir(File baseDir, Map<String, String> rootProperties) {
    String workDir = rootProperties.get(CoreProperties.WORKING_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      return new File(baseDir, CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE);
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(baseDir, customWorkDir.getPath());
  }

  @VisibleForTesting
  protected File initModuleWorkDir(File moduleBaseDir, Map<String, String> moduleProperties) {
    String workDir = moduleProperties.get(CoreProperties.WORKING_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      return new File(rootProjectWorkDir, BatchUtils.cleanKeyForFilename(moduleProperties.get(CoreProperties.PROJECT_KEY_PROPERTY)));
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(moduleBaseDir, customWorkDir.getPath());
  }

  @CheckForNull
  private static File initModuleBuildDir(File moduleBaseDir, Map<String, String> moduleProperties) {
    String buildDir = moduleProperties.get(PROPERTY_PROJECT_BUILDDIR);
    if (StringUtils.isBlank(buildDir)) {
      return null;
    }

    File customBuildDir = new File(buildDir);
    if (customBuildDir.isAbsolute()) {
      return customBuildDir;
    }
    return new File(moduleBaseDir, customBuildDir.getPath());
  }

  private void defineChildren(ProjectDefinition parentProject, Map<String, Map<String, String>> propertiesByModuleId) {
    Map<String, String> parentProps = parentProject.properties();
    if (parentProps.containsKey(PROPERTY_MODULES)) {
      for (String moduleId : getListFromProperty(parentProps, PROPERTY_MODULES)) {
        Map<String, String> moduleProps = propertiesByModuleId.get(moduleId);
        ProjectDefinition childProject = loadChildProject(parentProject, moduleProps, moduleId);
        // check the uniqueness of the child key
        checkUniquenessOfChildKey(childProject, parentProject);
        // the child project may have children as well
        defineChildren(childProject, propertiesByModuleId);
        // and finally add this child project to its parent
        parentProject.addSubProject(childProject);
      }
    }
  }

  protected ProjectDefinition loadChildProject(ProjectDefinition parentProject, Map<String, String> moduleProps, String moduleId) {
    final File baseDir;
    if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
      baseDir = resolvePath(parentProject.getBaseDir(), moduleProps.get(PROPERTY_PROJECT_BASEDIR));
      setProjectBaseDir(baseDir, moduleProps, moduleId);
    } else {
      baseDir = new File(parentProject.getBaseDir(), moduleId);
      setProjectBaseDir(baseDir, moduleProps, moduleId);
    }

    setModuleKeyAndNameIfNotDefined(moduleProps, moduleId, parentProject.getKey());

    // and finish
    checkMandatoryProperties(moduleProps, MANDATORY_PROPERTIES_FOR_CHILD);
    validateDirectories(moduleProps, baseDir, moduleId);

    mergeParentProperties(moduleProps, parentProject.properties());

    return defineRootProject(moduleProps, parentProject);
  }

  @VisibleForTesting
  protected static void setModuleKeyAndNameIfNotDefined(Map<String, String> childProps, String moduleId, String parentKey) {
    if (!childProps.containsKey(MODULE_KEY_PROPERTY)) {
      if (!childProps.containsKey(CoreProperties.PROJECT_KEY_PROPERTY)) {
        childProps.put(MODULE_KEY_PROPERTY, parentKey + ":" + moduleId);
      } else {
        String childKey = childProps.get(CoreProperties.PROJECT_KEY_PROPERTY);
        childProps.put(MODULE_KEY_PROPERTY, parentKey + ":" + childKey);
      }
    }
    if (!childProps.containsKey(CoreProperties.PROJECT_NAME_PROPERTY)) {
      childProps.put(CoreProperties.PROJECT_NAME_PROPERTY, moduleId);
    }
    // For backward compatibility with ProjectDefinition
    childProps.put(CoreProperties.PROJECT_KEY_PROPERTY, childProps.get(MODULE_KEY_PROPERTY));
  }

  @VisibleForTesting
  protected static void checkUniquenessOfChildKey(ProjectDefinition childProject, ProjectDefinition parentProject) {
    for (ProjectDefinition definition : parentProject.getSubProjects()) {
      if (definition.getKey().equals(childProject.getKey())) {
        throw new IllegalStateException("Project '" + parentProject.getKey() + "' can't have 2 modules with the following key: " + childProject.getKey());
      }
    }
  }

  protected static void setProjectBaseDir(File baseDir, Map<String, String> childProps, String moduleId) {
    if (!baseDir.isDirectory()) {
      throw new IllegalStateException("The base directory of the module '" + moduleId + "' does not exist: " + baseDir.getAbsolutePath());
    }
    childProps.put(PROPERTY_PROJECT_BASEDIR, baseDir.getAbsolutePath());
  }

  @VisibleForTesting
  protected static void checkMandatoryProperties(Map<String, String> props, String[] mandatoryProps) {
    StringBuilder missing = new StringBuilder();
    for (String mandatoryProperty : mandatoryProps) {
      if (!props.containsKey(mandatoryProperty)) {
        if (missing.length() > 0) {
          missing.append(", ");
        }
        missing.append(mandatoryProperty);
      }
    }
    String moduleKey = StringUtils.defaultIfBlank(props.get(MODULE_KEY_PROPERTY), props.get(CoreProperties.PROJECT_KEY_PROPERTY));
    if (missing.length() != 0) {
      throw new IllegalStateException("You must define the following mandatory properties for '" + (moduleKey == null ? "Unknown" : moduleKey) + "': " + missing);
    }
  }

  protected static void validateDirectories(Map<String, String> props, File baseDir, String projectId) {
    if (!props.containsKey(PROPERTY_MODULES)) {
      // SONARPLUGINS-2285 Not an aggregator project so we can validate that paths are correct if defined

      // We need to resolve patterns that may have been used in "sonar.libraries"
      for (String pattern : getListFromProperty(props, PROPERTY_LIBRARIES)) {
        File[] files = getLibraries(baseDir, pattern);
        if (files == null || files.length == 0) {
          LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, PROPERTY_LIBRARIES, projectId));
          throw new IllegalStateException("No files nor directories matching '" + pattern + "' in directory " + baseDir);
        }
      }

      // Check sonar.tests
      String[] testPaths = getListFromProperty(props, PROPERTY_TESTS);
      checkExistenceOfPaths(projectId, baseDir, testPaths, PROPERTY_TESTS);

      // Check sonar.binaries
      String[] binDirs = getListFromProperty(props, PROPERTY_BINARIES);
      checkExistenceOfDirectories(projectId, baseDir, binDirs, PROPERTY_BINARIES);
    }
  }

  @VisibleForTesting
  protected static void cleanAndCheckProjectDefinitions(ProjectDefinition project) {
    if (project.getSubProjects().isEmpty()) {
      cleanAndCheckModuleProperties(project);
    } else {
      cleanAndCheckAggregatorProjectProperties(project);

      // clean modules properties as well
      for (ProjectDefinition module : project.getSubProjects()) {
        cleanAndCheckProjectDefinitions(module);
      }
    }
  }

  @VisibleForTesting
  protected static void cleanAndCheckModuleProperties(ProjectDefinition project) {
    Map<String, String> properties = project.properties();

    // We need to check the existence of source directories
    String[] sourcePaths = getListFromProperty(properties, PROPERTY_SOURCES);
    checkExistenceOfPaths(project.getKey(), project.getBaseDir(), sourcePaths, PROPERTY_SOURCES);

    // And we need to resolve patterns that may have been used in "sonar.libraries"
    List<String> libPaths = Lists.newArrayList();
    for (String pattern : getListFromProperty(properties, PROPERTY_LIBRARIES)) {
      for (File file : getLibraries(project.getBaseDir(), pattern)) {
        libPaths.add(file.getAbsolutePath());
      }
    }
    properties.remove(PROPERTY_LIBRARIES);
    properties.put(PROPERTY_LIBRARIES, StringUtils.join(libPaths, ","));
  }

  @VisibleForTesting
  protected static void cleanAndCheckAggregatorProjectProperties(ProjectDefinition project) {
    Map<String, String> properties = project.properties();

    // SONARPLUGINS-2295
    String[] sourceDirs = getListFromProperty(properties, PROPERTY_SOURCES);
    for (String path : sourceDirs) {
      File sourceFolder = resolvePath(project.getBaseDir(), path);
      if (sourceFolder.isDirectory()) {
        LOG.warn("/!\\ A multi-module project can't have source folders, so '{}' won't be used for the analysis. " +
          "If you want to analyse files of this folder, you should create another sub-module and move them inside it.",
          sourceFolder.toString());
      }
    }

    // "aggregator" project must not have the following properties:
    properties.remove(PROPERTY_SOURCES);
    properties.remove(PROPERTY_TESTS);
    properties.remove(PROPERTY_BINARIES);
    properties.remove(PROPERTY_LIBRARIES);
  }

  @VisibleForTesting
  protected static void mergeParentProperties(Map<String, String> childProps, Map<String, String> parentProps) {
    for (Map.Entry<String, String> entry : parentProps.entrySet()) {
      String key = (String) entry.getKey();
      if ((!childProps.containsKey(key) || childProps.get(key).equals(entry.getValue()))
        && !NON_HERITED_PROPERTIES_FOR_CHILD.contains(key)) {
        childProps.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @VisibleForTesting
  protected static void checkExistenceOfDirectories(String moduleRef, File baseDir, String[] dirPaths, String propName) {
    for (String path : dirPaths) {
      File sourceFolder = resolvePath(baseDir, path);
      if (!sourceFolder.isDirectory()) {
        LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, propName, moduleRef));
        throw new IllegalStateException("The folder '" + path + "' does not exist for '" + moduleRef +
          "' (base directory = " + baseDir.getAbsolutePath() + ")");
      }
    }

  }

  @VisibleForTesting
  protected static void checkExistenceOfPaths(String moduleRef, File baseDir, String[] paths, String propName) {
    for (String path : paths) {
      File sourceFolder = resolvePath(baseDir, path);
      if (!sourceFolder.exists()) {
        LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, propName, moduleRef));
        throw new IllegalStateException("The folder '" + path + "' does not exist for '" + moduleRef +
          "' (base directory = " + baseDir.getAbsolutePath() + ")");
      }
    }

  }

  /**
   * Returns files matching specified pattern.
   */
  @VisibleForTesting
  protected static File[] getLibraries(File baseDir, String pattern) {
    final int i = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
    final String dirPath;
    final String filePattern;
    if (i == -1) {
      dirPath = ".";
      filePattern = pattern;
    } else {
      dirPath = pattern.substring(0, i);
      filePattern = pattern.substring(i + 1);
    }
    List<IOFileFilter> filters = new ArrayList<>();
    if (pattern.indexOf('*') >= 0) {
      filters.add(FileFileFilter.FILE);
    }
    filters.add(new WildcardFileFilter(filePattern));
    File dir = resolvePath(baseDir, dirPath);
    File[] files = dir.listFiles((FileFilter) new AndFileFilter(filters));
    if (files == null) {
      files = new File[0];
    }
    return files;
  }

  protected static File resolvePath(File baseDir, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(baseDir, path).getCanonicalFile();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to resolve path \"" + path + "\"", e);
      }
    }
    return file;
  }

  /**
   * Transforms a comma-separated list String property in to a array of trimmed strings.
   *
   * This works even if they are separated by whitespace characters (space char, EOL, ...)
   *
   */
  static String[] getListFromProperty(Map<String, String> properties, String key) {
    return (String[]) ObjectUtils.defaultIfNull(StringUtils.stripAll(StringUtils.split(properties.get(key), ',')), new String[0]);
  }

}
