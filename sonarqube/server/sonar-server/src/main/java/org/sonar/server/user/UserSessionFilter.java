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
package org.sonar.server.user;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.platform.Platform;

/**
 * @since 3.6
 */
public class UserSessionFilter implements Filter {
  private final Platform platform;

  public UserSessionFilter() {
    this.platform = Platform.getInstance();
  }

  public UserSessionFilter(Platform platform) {
    this.platform = platform;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // nothing to do
  }

  @Override
  public void destroy() {
    // nothing to do
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    try {
      chain.doFilter(servletRequest, servletResponse);
    } finally {
      ThreadLocalUserSession userSession = platform.getContainer().getComponentByType(ThreadLocalUserSession.class);
      if (userSession == null) {
        Loggers.get(UserSessionFilter.class).error("Can not retrieve ThreadLocalUserSession from Platform");
      } else {
        userSession.remove();
      }
    }
  }
}
