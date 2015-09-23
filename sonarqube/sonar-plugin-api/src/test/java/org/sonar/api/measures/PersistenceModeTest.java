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
package org.sonar.api.measures;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PersistenceModeTest {

  @Test
  public void useMemory() {
    assertThat(PersistenceMode.MEMORY.useMemory(), is(true));
    assertThat(PersistenceMode.DATABASE.useMemory(), is(false));
    assertThat(PersistenceMode.FULL.useMemory(), is(true));
  }

  @Test
  public void useDatabase() {
    assertThat(PersistenceMode.MEMORY.useDatabase(), is(false));
    assertThat(PersistenceMode.DATABASE.useDatabase(), is(true));
    assertThat(PersistenceMode.FULL.useDatabase(), is(true));
  }
}
