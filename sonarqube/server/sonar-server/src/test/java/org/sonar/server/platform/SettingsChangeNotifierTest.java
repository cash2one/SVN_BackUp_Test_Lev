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
package org.sonar.server.platform;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.config.GlobalPropertyChangeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SettingsChangeNotifierTest {
  @Test
  public void onGlobalPropertyChange() {
    GlobalPropertyChangeHandler handler = mock(GlobalPropertyChangeHandler.class);
    SettingsChangeNotifier notifier = new SettingsChangeNotifier(new GlobalPropertyChangeHandler[] {handler});

    notifier.onGlobalPropertyChange("foo", "bar");

    verify(handler).onChange(argThat(new ArgumentMatcher<GlobalPropertyChangeHandler.PropertyChange>() {
      @Override
      public boolean matches(Object o) {
        GlobalPropertyChangeHandler.PropertyChange change = (GlobalPropertyChangeHandler.PropertyChange) o;
        return change.getKey().equals("foo") && change.getNewValue().equals("bar");
      }
    }));
  }

  @Test
  public void no_handlers() {
    SettingsChangeNotifier notifier = new SettingsChangeNotifier();

    assertThat(notifier.changeHandlers).isEmpty();

    // does not fail
    notifier.onGlobalPropertyChange("foo", "bar");
  }
}
