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

package org.sonar.db.event;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventDtoTest {

  @Test
  public void test_getters_and_setters() throws Exception {
    EventDto dto = new EventDto()
      .setId(1L)
      .setName("1.0")
      .setCategory("Version")
      .setDescription("Version 1.0")
      .setData("some data")
      .setDate(1413407091086L)
      .setComponentUuid("ABCD")
      .setSnapshotId(1000L)
      .setCreatedAt(1225630680000L);

    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getName()).isEqualTo("1.0");
    assertThat(dto.getCategory()).isEqualTo("Version");
    assertThat(dto.getDescription()).isEqualTo("Version 1.0");
    assertThat(dto.getData()).isEqualTo("some data");
    assertThat(dto.getComponentUuid()).isEqualTo("ABCD");
    assertThat(dto.getSnapshotId()).isEqualTo(1000L);
    assertThat(dto.getCreatedAt()).isEqualTo(1225630680000L);
  }

}
