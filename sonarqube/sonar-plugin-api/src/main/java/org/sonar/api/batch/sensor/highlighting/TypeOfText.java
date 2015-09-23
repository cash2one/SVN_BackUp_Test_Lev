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
package org.sonar.api.batch.sensor.highlighting;

import com.google.common.annotations.Beta;

/**
 * Experimental, do not use.
 * <p/>
 * Possible types for highlighting code. See sonar-colorizer.css
 * @since 5.1
 */
@Beta
public enum TypeOfText {
  ANNOTATION("a"),
  CONSTANT("c"),
  COMMENT("cd"),
  /**
   * @deprecated use {@link #COMMENT}
   */
  @Deprecated
  CPP_DOC("cppd"),
  /**
   * For example Javadoc
   */
  STRUCTURED_COMMENT("j"),
  KEYWORD("k"),
  STRING("s"),
  KEYWORD_LIGHT("h"),
  PREPROCESS_DIRECTIVE("p");

  private final String cssClass;

  private TypeOfText(String cssClass) {
    this.cssClass = cssClass;
  }

  public static TypeOfText forCssClass(String cssClass) {
    for (TypeOfText typeOfText : TypeOfText.values()) {
      if (typeOfText.cssClass().equals(cssClass)) {
        return typeOfText;
      }
    }
    throw new IllegalArgumentException("No TypeOfText for CSS class " + cssClass);
  }

  /**
   * For internal use
   */
  public String cssClass() {
    return cssClass;
  }
}
