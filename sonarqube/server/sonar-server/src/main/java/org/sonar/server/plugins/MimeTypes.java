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
package org.sonar.server.plugins;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FilenameUtils;

import java.util.Locale;
import java.util.Map;

/**
 * @since 3.1
 */
public final class MimeTypes {

  public static final String JSON = "application/json";
  public static final String XML = "application/xml";
  public static final String TXT = "text/plain";
  public static final String PROTOBUF = "application/x-protobuf";
  public static final String DEFAULT = "application/octet-stream";

  private static final Map<String, String> MAP = new ImmutableMap.Builder<String, String>()
    .put("json", JSON)
    .put("zip", "application/zip")
    .put("tgz", "application/tgz")
    .put("ps", "application/postscript")
    .put("jnlp", "application/jnlp")
    .put("jar", "application/java-archive")
    .put("xls", "application/vnd.ms-excel")
    .put("ppt", "application/vnd.ms-powerpoint")
    .put("tar", "application/x-tar")
    .put("xml", XML)
    .put("dtd", "application/xml-dtd")
    .put("xslt", "application/xslt+xml")
    .put("bmp", "image/bmp")
    .put("gif", "image/gif")
    .put("jpg", "image/jpeg")
    .put("jpeg", "image/jpeg")
    .put("tiff", "image/tiff")
    .put("png", "image/png")
    .put("svg", "image/svg+xml")
    .put("ico", "image/x-icon")
    .put("txt", TXT)
    .put("csv", "text/csv")
    .put("properties", "text/plain")
    .put("rtf", "text/rtf")
    .put("html", "text/html")
    .put("css", "text/css")
    .put("tsv", "text/tab-separated-values")
    .build();

  private MimeTypes() {
    // only static methods
  }

  public static String getByFilename(String filename) {
    String extension = FilenameUtils.getExtension(filename);
    String mime = null;
    if (!Strings.isNullOrEmpty(extension)) {
      mime = MAP.get(extension.toLowerCase(Locale.ENGLISH));
    }
    return mime != null ? mime : DEFAULT;
  }
}
