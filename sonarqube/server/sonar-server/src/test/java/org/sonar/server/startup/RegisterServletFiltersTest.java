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
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.platform.MasterServletFilter;

import javax.servlet.ServletException;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RegisterServletFiltersTest {
  @Test
  public void should_not_fail_if_master_filter_is_not_up() throws ServletException {
    MasterServletFilter.INSTANCE = null;
    new RegisterServletFilters(new ServletFilter[2]).start();
  }

  @Test
  public void should_register_filters_if_master_filter_is_up() throws ServletException {
    MasterServletFilter.INSTANCE = mock(MasterServletFilter.class);
    new RegisterServletFilters(new ServletFilter[2]).start();

    verify(MasterServletFilter.INSTANCE).initFilters(anyListOf(ServletFilter.class));
  }

  @Test
  public void filters_should_be_optional() throws ServletException {
    MasterServletFilter.INSTANCE = mock(MasterServletFilter.class);
    new RegisterServletFilters().start();
    // do not fail
    verify(MasterServletFilter.INSTANCE).initFilters(anyListOf(ServletFilter.class));
  }
}
