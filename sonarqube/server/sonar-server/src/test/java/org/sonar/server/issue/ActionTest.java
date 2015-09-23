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

package org.sonar.server.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionTest {

  @Test
  public void key_should_not_be_empty() {
    try {
      new Action("") {
        @Override
        boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
          return false;
        }

        @Override
        boolean execute(Map<String, Object> properties, Context context) {
          return false;
        }
      };
    } catch (Exception e) {
      assertThat(e).hasMessage("Action key must be set").isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void key_should_not_be_null() {
    try {
      new Action(null) {
        @Override
        boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
          return false;
        }

        @Override
        boolean execute(Map<String, Object> properties, Context context) {
          return false;
        }
      };
    } catch (Exception e) {
      assertThat(e).hasMessage("Action key must be set").isInstanceOf(IllegalArgumentException.class);
    }
  }
}
