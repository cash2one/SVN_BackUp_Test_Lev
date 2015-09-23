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
package org.sonar.api;

/**
 * Factory of extensions. It allows to dynamically create extensions depending upon runtime context. A use-case is
 * to create one rule repository by language.
 *
 * <p>Notes :
 * <ul>
 * <li>the provider is declared in Plugin.getExtensions()</li>
 * <li>the provider must also implement ServerExtension and/or BatchExtension</li>
 * <li>the provider can accept dependencies (parameters) in its constructors.</li>
 * <li>the method provide() is executed once by sonar</li>
 * <li>the method provide() must return an object, a class or an Iterable of objects. <strong>Arrays are excluded</strong>.</li>
 * </ul>
 * </p>
 *
 * <p>Example:
 * <pre>
 * public class RuleRepositoryProvider extends ExtensionProvider implements ServerExtension {
 *   private Language[] languages;
 *
 *   public RuleRepositoryProvider(Language[] languages) {
 *     this.languages = languages;
 *   }
 *
 *   public List<RuleRepository> provide() {
 *     List<RuleRepository> result = new ArrayList<RuleRepository>();
 *     for(Language language: languages) {
 *       result.add(new RuleRepository(..., language, ...));
 *     }
 *     return result;
 *   }
 * }
 * </pre>
 * </p>
 *
 * @since 2.3
 */
public abstract class ExtensionProvider implements Extension {

  public abstract Object provide();
}
