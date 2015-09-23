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
package org.sonar.server.es;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Various Elasticsearch request options: paging, fields and facets
 */
public class SearchOptions {

  public static final int DEFAULT_OFFSET = 0;
  public static final int DEFAULT_LIMIT = 10;
  public static final int MAX_LIMIT = 500;

  private int offset = DEFAULT_OFFSET;
  private int limit = DEFAULT_LIMIT;
  private final Set<String> facets = new LinkedHashSet<>();
  private final Set<String> fieldsToReturn = new HashSet<>();

  /**
   * Offset of the first result to return. Defaults to {@link #DEFAULT_OFFSET}
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Sets the offset of the first result to return (zero-based).
   */
  public SearchOptions setOffset(int offset) {
    Preconditions.checkArgument(offset >= 0, "Offset must be positive");
    this.offset = offset;
    return this;
  }

  /**
   * Set offset and limit according to page approach. If pageSize is negative, then
   * {@link #MAX_LIMIT} is used.
   */
  public SearchOptions setPage(int page, int pageSize) {
    Preconditions.checkArgument(page >= 1, "Page must be greater or equal to 1 (got " + page + ")");
    setLimit(pageSize);
    setOffset((page * this.limit) - this.limit);
    return this;
  }

  public int getPage() {
    return limit > 0 ? (int) Math.ceil((double) (offset + 1) / (double) limit) : 0;
  }

  /**
   * Limit on the number of results to return. Defaults to {@link #DEFAULT_LIMIT}.
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Sets the limit on the number of results to return.
   */
  public SearchOptions setLimit(int limit) {
    if (limit <= 0) {
      this.limit = MAX_LIMIT;
    } else {
      this.limit = Math.min(limit, MAX_LIMIT);
    }
    return this;
  }

  /**
   * WARNING - dangerous
   */
  @Deprecated
  public SearchOptions disableLimit() {
    this.limit = 999999;
    return this;
  }

  /**
   * Lists selected facets.
   */
  public Collection<String> getFacets() {
    return facets;
  }

  /**
   * Selects facets to return for the domain.
   */
  public SearchOptions addFacets(@Nullable Collection<String> f) {
    if (f != null) {
      this.facets.addAll(f);
    }
    return this;
  }

  public SearchOptions addFacets(String... array) {
    Collections.addAll(facets, array);
    return this;
  }

  public Set<String> getFields() {
    return fieldsToReturn;
  }

  public boolean hasField(String key) {
    return fieldsToReturn.isEmpty() || fieldsToReturn.contains(key);
  }

  public SearchOptions addFields(@Nullable Collection<String> c) {
    if (c != null) {
      for (String s : c) {
        if (StringUtils.isNotBlank(s)) {
          fieldsToReturn.add(s);
        }
      }
    }
    return this;
  }

  public SearchOptions addFields(String... array) {
    return addFields(Arrays.asList(array));
  }

  public SearchOptions writeJson(JsonWriter json, long totalHits) {
    json.prop("total", totalHits);
    json.prop(WebService.Param.PAGE, getPage());
    json.prop(WebService.Param.PAGE_SIZE, getLimit());
    return this;
  }

  @Deprecated
  public SearchOptions writeDeprecatedJson(JsonWriter json, long totalHits) {
    int pages = 0;
    if (limit > 0) {
      pages = (int) (totalHits / limit);
      if (totalHits % limit > 0) {
        pages++;
      }
    }
    json.name("paging").beginObject()
      .prop("pageIndex", getPage())
      .prop("pageSize", getLimit())
      .prop("total", totalHits)
      .prop("fTotal", String.valueOf(totalHits))
      .prop("pages", pages)
      .endObject();
    return this;
  }
}
