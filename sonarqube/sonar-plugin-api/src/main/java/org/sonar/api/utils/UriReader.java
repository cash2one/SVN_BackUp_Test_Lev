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
package org.sonar.api.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.server.ServerSide;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads different types of URI. Supported schemes are http and file.
 *
 * @since 3.2
 */
@BatchSide
@ServerSide
public class UriReader {

  private final Map<String, SchemeProcessor> processorsByScheme = Maps.newHashMap();

  public UriReader(SchemeProcessor[] processors) {
    List<SchemeProcessor> allProcessors = Lists.asList(new FileProcessor(), processors);
    for (SchemeProcessor processor : allProcessors) {
      for (String scheme : processor.getSupportedSchemes()) {
        processorsByScheme.put(scheme.toLowerCase(Locale.ENGLISH), processor);
      }
    }
  }

  /**
   * Reads all bytes from uri. It throws an unchecked exception if an error occurs.
   */
  public byte[] readBytes(URI uri) {
    return searchForSupportedProcessor(uri).readBytes(uri);
  }

  /**
   * Reads all characters from uri, using the given character set.
   * It throws an unchecked exception if an error occurs.
   */
  public String readString(URI uri, Charset charset) {
    return searchForSupportedProcessor(uri).readString(uri, charset);
  }

  /**
   * Returns a detailed description of the given uri. For example HTTP URIs are completed
   * with the configured HTTP proxy.
   */
  public String description(URI uri) {
    SchemeProcessor reader = searchForSupportedProcessor(uri);

    return reader.description(uri);
  }

  @VisibleForTesting
  SchemeProcessor searchForSupportedProcessor(URI uri) {
    SchemeProcessor processor = processorsByScheme.get(uri.getScheme().toLowerCase(Locale.ENGLISH));
    Preconditions.checkArgument(processor != null, "URI schema is not supported: " + uri.getScheme());
    return processor;
  }

  public abstract static class SchemeProcessor {
    protected abstract String[] getSupportedSchemes();

    protected abstract byte[] readBytes(URI uri);

    protected abstract String readString(URI uri, Charset charset);

    protected abstract String description(URI uri);
  }

  /**
   * This implementation is not exposed in API and is kept private.
   */
  private static class FileProcessor extends SchemeProcessor {

    @Override
    public String[] getSupportedSchemes() {
      return new String[] {"file"};
    }

    @Override
    protected byte[] readBytes(URI uri) {
      try {
        return Files.toByteArray(new File(uri));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }

    @Override
    protected String readString(URI uri, Charset charset) {
      try {
        return Files.toString(new File(uri), charset);
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }

    @Override
    protected String description(URI uri) {
      return new File(uri).getAbsolutePath();
    }
  }
}
