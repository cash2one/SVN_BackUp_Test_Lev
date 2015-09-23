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
package org.sonar.server.ws;

import com.google.common.base.Objects;
import org.jruby.RubyFile;
import org.sonar.api.server.ws.internal.ValidatingRequest;

import javax.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.util.Map;

public class ServletRequest extends ValidatingRequest {

  private final HttpServletRequest source;
  private final Map<String, Object> params;

  public ServletRequest(HttpServletRequest source, Map<String, Object> params) {
    this.source = source;
    this.params = params;
  }

  @Override
  public String method() {
    return source.getMethod();
  }

  @Override
  public String getMediaType() {
    return Objects.firstNonNull(source.getContentType(), "application/octet-stream");
  }

  @Override
  public boolean hasParam(String key) {
    return source.getParameterMap().containsKey(key) || params.keySet().contains(key);
  }

  @Override
  protected String readParam(String key) {
    String value = source.getParameter(key);
    if (value == null) {
      Object string = params.get(key);
      if (string != null && string instanceof String) {
        value = (String) string;
      }
    }
    return value;
  }

  @Override
  protected InputStream readInputStreamParam(String key) {
    Object file = params.get(key);
    if (file != null && file instanceof RubyFile) {
      return ((RubyFile) file).getInStream();
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuffer url = source.getRequestURL();
    String query = source.getQueryString();
    if (query != null) {
      url.append("?").append(query);
    }
    return url.toString();
  }
}
