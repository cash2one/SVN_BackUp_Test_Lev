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

import org.junit.Test;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.QueryContext;
import org.sonar.test.JsonAssert;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchOptionsTest {

  @Test
  public void defaults() {
    SearchOptions options = new SearchOptions();

    assertThat(options.getFacets()).isEmpty();
    assertThat(options.getFields()).isEmpty();
    assertThat(options.getOffset()).isEqualTo(0);
    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getPage()).isEqualTo(1);
  }

  @Test
  public void page_shortcut_for_limit_and_offset() {
    SearchOptions options = new SearchOptions().setPage(3, 10);

    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getOffset()).isEqualTo(20);
  }

  @Test
  public void page_starts_at_one() {
    SearchOptions options = new SearchOptions().setPage(1, 10);
    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getOffset()).isEqualTo(0);
    assertThat(options.getPage()).isEqualTo(1);
  }

  @Test
  public void with_zero_page_size() {
    SearchOptions options = new SearchOptions().setPage(1, 0);
    assertThat(options.getLimit()).isEqualTo(SearchOptions.MAX_LIMIT);
    assertThat(options.getOffset()).isEqualTo(0);
    assertThat(options.getPage()).isEqualTo(1);
  }

  @Test
  public void page_must_be_strictly_positive() {
    try {
      new SearchOptions().setPage(0, 10);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Page must be greater or equal to 1 (got 0)");
    }
  }

  @Test
  public void use_max_limit_if_negative() {
    SearchOptions options = new SearchOptions().setPage(2, -1);
    assertThat(options.getLimit()).isEqualTo(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void max_limit() {
    SearchOptions options = new SearchOptions().setLimit(42);
    assertThat(options.getLimit()).isEqualTo(42);

    options.setLimit(SearchOptions.MAX_LIMIT + 10);
    assertThat(options.getLimit()).isEqualTo(QueryContext.MAX_LIMIT);
  }

  @Test
  public void disable_limit() {
    SearchOptions options = new SearchOptions().disableLimit();
    assertThat(options.getLimit()).isEqualTo(999999);
  }

  @Test
  public void max_page_size() {
    SearchOptions options = new SearchOptions().setPage(3, QueryContext.MAX_LIMIT + 10);
    assertThat(options.getOffset()).isEqualTo(QueryContext.MAX_LIMIT * 2);
    assertThat(options.getLimit()).isEqualTo(QueryContext.MAX_LIMIT);
  }

  @Test
  public void hasField() {
    // parameter is missing -> all the fields are returned by default
    SearchOptions options = new SearchOptions();
    assertThat(options.hasField("repo")).isTrue();

    // parameter is set to empty -> all the fields are returned by default
    options = new SearchOptions().addFields("");
    assertThat(options.hasField("repo")).isTrue();

    // parameter is set -> return only the selected fields
    options = new SearchOptions().addFields("name", "repo");
    assertThat(options.hasField("name")).isTrue();
    assertThat(options.hasField("repo")).isTrue();
    assertThat(options.hasField("severity")).isFalse();
  }

  @Test
  public void writeJson() {
    SearchOptions options = new SearchOptions().setPage(3, 10);
    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json).beginObject();
    options.writeJson(jsonWriter, 42L);
    jsonWriter.endObject().close();

    JsonAssert.assertJson(json.toString()).isSimilarTo("{\"total\": 42, \"p\": 3, \"ps\": 10}");
  }

  @Test
  public void writeDeprecatedJson() {
    SearchOptions options = new SearchOptions().setPage(3, 10);
    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json).beginObject();
    options.writeDeprecatedJson(jsonWriter, 42L);
    jsonWriter.endObject().close();

    JsonAssert.assertJson(json.toString()).isSimilarTo("{\"paging\": {\"pageIndex\": 3, \"pageSize\": 10, \"total\": 42, \"fTotal\": \"42\", \"pages\": 5}}");
  }

  @Test
  public void writeDeprecatedJson_exact_nb_of_pages() {
    SearchOptions options = new SearchOptions().setPage(3, 10);
    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json).beginObject();
    options.writeDeprecatedJson(jsonWriter, 30L);
    jsonWriter.endObject().close();

    JsonAssert.assertJson(json.toString()).isSimilarTo("{\"paging\": {\"pageIndex\": 3, \"pageSize\": 10, \"total\": 30, \"fTotal\": \"30\", \"pages\": 3}}");
  }
}
