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
package org.sonar.api.test;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;

public interface MutableTestCase extends TestCase {
  MutableTestCase setStatus(@Nullable Status s);

  MutableTestCase setDurationInMs(@Nullable Long l);

  MutableTestCase setMessage(@Nullable String s);

  MutableTestCase setStackTrace(@Nullable String s);

  /**
   * @deprecated since 5.2 not used
   */
  @Deprecated
  MutableTestCase setType(@Nullable String s);

  /**
   * @deprecated since 5.2. Use {@link #setCoverageBlock(InputFile, List)}
   */
  @Deprecated
  MutableTestCase setCoverageBlock(Testable testable, List<Integer> lines);

  MutableTestCase setCoverageBlock(InputFile mainFile, List<Integer> lines);
}
