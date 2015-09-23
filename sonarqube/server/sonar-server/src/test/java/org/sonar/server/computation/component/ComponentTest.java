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
package org.sonar.server.computation.component;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;

public class ComponentTest {
  @Test
  public void verify_type_is_deeper_than_when_comparing_to_itself() {
    for (Component.Type type : Component.Type.values()) {
      assertThat(type.isDeeperThan(type)).isFalse();
    }
  }

  @Test
  public void FILE_type_is_deeper_than_all_other_types() {
    assertThat(Component.Type.FILE.isDeeperThan(DIRECTORY)).isTrue();
    assertThat(Component.Type.FILE.isDeeperThan(MODULE)).isTrue();
    assertThat(Component.Type.FILE.isDeeperThan(PROJECT)).isTrue();
  }

  @Test
  public void DIRECTORY_type_is_deeper_than_MODULE_and_PROJECT() {
    assertThat(Component.Type.DIRECTORY.isDeeperThan(MODULE)).isTrue();
    assertThat(Component.Type.DIRECTORY.isDeeperThan(PROJECT)).isTrue();
  }

  @Test
  public void MODULE_type_is_deeper_than_PROJECT() {
    assertThat(Component.Type.MODULE.isDeeperThan(PROJECT)).isTrue();
  }

  @Test
  public void FILE_type_is_higher_than_no_other_types() {
    assertThat(Component.Type.FILE.isHigherThan(DIRECTORY)).isFalse();
    assertThat(Component.Type.FILE.isHigherThan(MODULE)).isFalse();
    assertThat(Component.Type.FILE.isHigherThan(PROJECT)).isFalse();
  }

  @Test
  public void DIRECTORY_type_is_higher_than_FILE() {
    assertThat(Component.Type.DIRECTORY.isHigherThan(FILE)).isTrue();
  }

  @Test
  public void MODULE_type_is_higher_than_FILE_AND_DIRECTORY() {
    assertThat(Component.Type.MODULE.isHigherThan(FILE)).isTrue();
    assertThat(Component.Type.MODULE.isHigherThan(DIRECTORY)).isTrue();
  }

  @Test
  public void PROJECT_type_is_higher_than_all_other_types() {
    assertThat(Component.Type.PROJECT.isHigherThan(FILE)).isTrue();
    assertThat(Component.Type.PROJECT.isHigherThan(DIRECTORY)).isTrue();
    assertThat(Component.Type.PROJECT.isHigherThan(MODULE)).isTrue();
  }

  @Test
  public void any_type_is_not_higher_than_itself() {
    assertThat(Component.Type.FILE.isHigherThan(FILE)).isFalse();
    assertThat(Component.Type.DIRECTORY.isHigherThan(DIRECTORY)).isFalse();
    assertThat(Component.Type.MODULE.isHigherThan(MODULE)).isFalse();
    assertThat(Component.Type.PROJECT.isHigherThan(PROJECT)).isFalse();
  }

  @Test
  public void PROJECT_MODULE_DIRECTORY_and_FILE_are_report_types_and_not_views_types() {
    for (Component.Type type : ImmutableList.of(PROJECT, MODULE, DIRECTORY, FILE)) {
      assertThat(type.isReportType()).isTrue();
      assertThat(type.isViewsType()).isFalse();
    }
  }

  @Test
  public void VIEW_SUBVIEW_and_PROJECT_VIEW_are_views_types_and_not_report_types() {
    for (Component.Type type : ImmutableList.of(VIEW, SUBVIEW, PROJECT_VIEW)) {
      assertThat(type.isViewsType()).isTrue();
      assertThat(type.isReportType()).isFalse();
    }

  }
}
