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
import java.util.Collection;

/**
 * Factory of {@link org.sonar.api.batch.fs.FilePredicate}
 *
 * @since 4.2
 */
public interface FilePredicates {
  /**
   * Predicate that always evaluates to true
   */
  FilePredicate all();

  /**
   * Predicate that always evaluates to false
   */
  FilePredicate none();

  /**
   * Predicate that gets a file by its absolute path. The parameter
   * accepts forward/back slashes as separator and non-normalized values
   * (<code>/path/to/../foo.txt</code> is same as <code>/path/foo.txt</code>).
   * <p/>
   * Warning - not efficient because absolute path is not indexed yet.
   */
  FilePredicate hasAbsolutePath(String s);

  /**
   * Predicate that gets a file by its relative path. The parameter
   * accepts forward/back slashes as separator and non-normalized values
   * (<code>foo/../bar.txt</code> is same as <code>bar.txt</code>). It must
   * not be <code>null</code>.
   */
  FilePredicate hasRelativePath(String s);

  /**
   * Predicate that gets the files which relative or absolute path matches a wildcard pattern.
   * <p/>
   * If the parameter starts with <code>file:</code>, then absolute path is used, else relative path. Pattern
   * is case-sensitive, except for file extension.
   * <p/>
   * Supported wildcards are <code>&#42;</code> and <code>&#42;&#42;</code>, but not <code>?</code>.
   * <p/>
   * Examples:
   * <ul>
   *   <li><code>&#42;&#42;/&#42;Foo.java</code> matches Foo.java, src/Foo.java and src/java/SuperFoo.java</li>
   *   <li><code>&#42;&#42;/&#42;Foo&#42;.java</code> matches src/Foo.java, src/BarFoo.java, src/FooBar.java
   *   and src/BarFooBaz.java</li>
   *   <li><code>&#42;&#42;/&#42;FOO.JAVA</code> matches FOO.java and FOO.JAVA but not Foo.java</li>
   *   <li><code>file:&#42;&#42;/src/&#42;Foo.java</code> matches /path/to/src/Foo.java on unix and c:\path\to\Foo.java on MSWindows</li>
   * </ul>
   */
  FilePredicate matchesPathPattern(String inclusionPattern);

  /**
   * Predicate that gets the files matching at least one wildcard pattern. No filter is applied when
   * zero wildcard patterns (similar to {@link #all()}.
   * @see #matchesPathPattern(String)
   */
  FilePredicate matchesPathPatterns(String[] inclusionPatterns);

  /**
   * Predicate that gets the files that do not match the given wildcard pattern.
   * @see #matchesPathPattern(String)
   */
  FilePredicate doesNotMatchPathPattern(String exclusionPattern);

  /**
   * Predicate that gets the files that do not match any of the given wildcard patterns. No filter is applied when
   * zero wildcard patterns (similar to {@link #all()}.
   * @see #matchesPathPattern(String)
   */
  FilePredicate doesNotMatchPathPatterns(String[] exclusionPatterns);

  /**
   * if the parameter represents an absolute path for the running environment, then
   * returns {@link #hasAbsolutePath(String)}, else {@link #hasRelativePath(String)}
   */
  FilePredicate hasPath(String s);

  FilePredicate is(File ioFile);

  FilePredicate hasLanguage(String language);

  FilePredicate hasLanguages(Collection<String> languages);

  FilePredicate hasLanguages(String... languages);

  FilePredicate hasStatus(InputFile.Status status);

  FilePredicate hasType(InputFile.Type type);

  FilePredicate not(FilePredicate p);

  FilePredicate or(Collection<FilePredicate> or);

  FilePredicate or(FilePredicate... or);

  FilePredicate or(FilePredicate first, FilePredicate second);

  FilePredicate and(Collection<FilePredicate> and);

  FilePredicate and(FilePredicate... and);

  FilePredicate and(FilePredicate first, FilePredicate second);

}
