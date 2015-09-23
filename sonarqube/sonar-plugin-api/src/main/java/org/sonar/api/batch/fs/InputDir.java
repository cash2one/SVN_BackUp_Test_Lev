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
package org.sonar.api.batch.fs;

import java.io.File;
import java.nio.file.Path;

/**
 * Layer over {@link java.io.File} for directories.
 *
 * @since 4.5
 */
public interface InputDir extends InputPath {

  /**
   * Path relative to module base directory. Path is unique and identifies directory
   * within given <code>{@link FileSystem}</code>. File separator is the forward
   * slash ('/'), even on Microsoft Windows.
   * <p/>
   * Returns <code>src/main/java/com</code> if module base dir is
   * <code>/path/to/module</code> and if directory is
   * <code>/path/to/module/src/main/java/com</code>.
   * <p/>
   * Relative path is not null and is normalized ('foo/../foo' is replaced by 'foo').
   */
  @Override
  String relativePath();

  /**
   * Normalized absolute path. File separator is forward slash ('/'), even on Microsoft Windows.
   * <p/>
   * This is not canonical path. Symbolic links are not resolved. For example if /project/src links
   * to /tmp/src and basedir is /project, then this method returns /project/src. Use
   * {@code file().getCanonicalPath()} to resolve symbolic link.
   */
  @Override
  String absolutePath();

  /**
   * The underlying absolute {@link java.io.File}
   */
  @Override
  File file();

  /**
   * The underlying absolute {@link Path}
   */
  @Override
  Path path();

}
