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

package org.sonar.server.source;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OpeningHtmlTagTest {

  @Test
  public void test_getters() throws Exception {
    OpeningHtmlTag openingHtmlTag = new OpeningHtmlTag(3, "tag");
    assertThat(openingHtmlTag.getStartOffset()).isEqualTo(3);
    assertThat(openingHtmlTag.getCssClass()).isEqualTo("tag");
  }

  @Test
  public void test_equals() throws Exception {
    OpeningHtmlTag openingHtmlTag = new OpeningHtmlTag(3, "tag");
    OpeningHtmlTag openingHtmlTagWithSameValues = new OpeningHtmlTag(3, "tag");
    OpeningHtmlTag openingHtmlTagWithDifferentValues = new OpeningHtmlTag(5, "tag2");
    OpeningHtmlTag openingHtmlTagWithNoCssClass = new OpeningHtmlTag(3, null);

    assertThat(openingHtmlTag).isEqualTo(openingHtmlTagWithSameValues);
    assertThat(openingHtmlTag).isEqualTo(openingHtmlTag);
    assertThat(openingHtmlTag).isNotEqualTo(openingHtmlTagWithDifferentValues);
    assertThat(openingHtmlTag).isNotEqualTo(openingHtmlTagWithNoCssClass);
    assertThat(openingHtmlTag).isNotEqualTo(new OpeningHtmlTag(3, "tag"){});
  }

  @Test
  public void test_hashcode() throws Exception {
    OpeningHtmlTag openingHtmlTag = new OpeningHtmlTag(3, "tag");
    OpeningHtmlTag openingHtmlTagWithSameValues = new OpeningHtmlTag(3, "tag");
    OpeningHtmlTag openingHtmlTagWithDifferentValue = new OpeningHtmlTag(5, "tag2");

    assertThat(openingHtmlTag.hashCode()).isEqualTo(openingHtmlTagWithSameValues.hashCode());
    assertThat(openingHtmlTag.hashCode()).isEqualTo(openingHtmlTag.hashCode());
    assertThat(openingHtmlTag.hashCode()).isNotEqualTo(openingHtmlTagWithDifferentValue.hashCode());
  }
}
