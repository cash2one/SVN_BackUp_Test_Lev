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
package org.sonar.batch.repository.language;

import org.sonar.api.resources.Languages;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Languages repository using {@link Languages}
 * @since 4.4
 */
public class DefaultLanguagesRepository implements LanguagesRepository {

  private Languages languages;

  public DefaultLanguagesRepository(Languages languages) {
    this.languages = languages;
  }

  /**
   * Get language.
   */
  @Override
  @CheckForNull
  public Language get(String languageKey) {
    org.sonar.api.resources.Language language = languages.get(languageKey);
    return language != null ? new Language(language.getKey(), language.getName(), language.getFileSuffixes()) : null;
  }

  /**
   * Get list of all supported languages.
   */
  @Override
  public Collection<Language> all() {
    org.sonar.api.resources.Language[] all = languages.all();
    Collection<Language> result = new ArrayList<>(all.length);
    for (org.sonar.api.resources.Language language : all) {
      result.add(new Language(language.getKey(), language.getName(), language.getFileSuffixes()));
    }
    return result;
  }

}
