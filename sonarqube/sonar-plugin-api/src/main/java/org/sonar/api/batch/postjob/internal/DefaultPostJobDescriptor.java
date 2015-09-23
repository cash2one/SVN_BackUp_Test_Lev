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
package org.sonar.api.batch.postjob.internal;

import org.sonar.api.batch.postjob.PostJobDescriptor;

import java.util.Arrays;
import java.util.Collection;

public class DefaultPostJobDescriptor implements PostJobDescriptor {

  private String name;
  private String[] properties = new String[0];
  private boolean disabledInIssues = false;

  public String name() {
    return name;
  }

  public Collection<String> properties() {
    return Arrays.asList(properties);
  }

  public boolean isDisabledInIssues() {
    return disabledInIssues;
  }

  @Override
  public DefaultPostJobDescriptor name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public DefaultPostJobDescriptor requireProperty(String... propertyKey) {
    return requireProperties(propertyKey);
  }

  @Override
  public DefaultPostJobDescriptor requireProperties(String... propertyKeys) {
    this.properties = propertyKeys;
    return this;
  }

  @Override
  public DefaultPostJobDescriptor disabledInIssues() {
    this.disabledInIssues = true;
    return this;
  }

}
