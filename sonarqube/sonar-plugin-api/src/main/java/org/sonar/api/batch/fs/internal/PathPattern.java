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
package org.sonar.api.batch.fs.internal;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.WildcardPattern;

public abstract class PathPattern {

  final WildcardPattern pattern;

  PathPattern(String pattern) {
    this.pattern = WildcardPattern.create(pattern);
  }

  public abstract boolean match(InputFile inputFile);

  public abstract boolean match(InputFile inputFile, boolean caseSensitiveFileExtension);

  public static PathPattern create(String s) {
    String trimmed = StringUtils.trim(s);
    if (StringUtils.startsWithIgnoreCase(trimmed, "file:")) {
      return new AbsolutePathPattern(StringUtils.substring(trimmed, "file:".length()));
    }
    return new RelativePathPattern(trimmed);
  }

  public static PathPattern[] create(String[] s) {
    PathPattern[] result = new PathPattern[s.length];
    for (int i = 0; i < s.length; i++) {
      result[i] = create(s[i]);
    }
    return result;
  }

  private static class AbsolutePathPattern extends PathPattern {
    private AbsolutePathPattern(String pattern) {
      super(pattern);
    }

    @Override
    public boolean match(InputFile inputFile) {
      return match(inputFile, true);
    }

    @Override
    public boolean match(InputFile inputFile, boolean caseSensitiveFileExtension) {
      String path = inputFile.absolutePath();
      if (!caseSensitiveFileExtension) {
        String extension = sanitizeExtension(FilenameUtils.getExtension(inputFile.file().getName()));
        if (StringUtils.isNotBlank(extension)) {
          path = StringUtils.removeEndIgnoreCase(path, extension);
          path = path + extension;
        }
      }
      return pattern.match(path);
    }

    @Override
    public String toString() {
      return "file:" + pattern.toString();
    }
  }

  /**
   * Path relative to module basedir
   */
  private static class RelativePathPattern extends PathPattern {
    private RelativePathPattern(String pattern) {
      super(pattern);
    }

    @Override
    public boolean match(InputFile inputFile) {
      return match(inputFile, true);
    }

    @Override
    public boolean match(InputFile inputFile, boolean caseSensitiveFileExtension) {
      String path = inputFile.relativePath();
      if (!caseSensitiveFileExtension) {
        String extension = sanitizeExtension(FilenameUtils.getExtension(inputFile.file().getName()));
        if (StringUtils.isNotBlank(extension)) {
          path = StringUtils.removeEndIgnoreCase(path, extension);
          path = path + extension;
        }
      }
      return path != null && pattern.match(path);
    }

    @Override
    public String toString() {
      return pattern.toString();
    }
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
