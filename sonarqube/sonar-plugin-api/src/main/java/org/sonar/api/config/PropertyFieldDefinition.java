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
package org.sonar.api.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.sonar.api.PropertyField;
import org.sonar.api.PropertyType;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.3
 */
public final class PropertyFieldDefinition {
  private final String key;
  private final String name;
  private final String description;
  private final int indicativeSize;
  private final PropertyType type;
  private final List<String> options;

  private PropertyFieldDefinition(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.description = builder.description;
    this.indicativeSize = builder.indicativeSize;
    this.type = builder.type;
    this.options = builder.options;
  }

  static List<PropertyFieldDefinition> create(PropertyField[] fields) {
    List<PropertyFieldDefinition> definitions = new ArrayList<>();
    for (PropertyField field : fields) {
      definitions.add(PropertyFieldDefinition.build(field.key())
        .name(field.name())
        .description(field.description())
        .indicativeSize(field.indicativeSize())
        .type(field.type())
        .options(field.options())
        .build()
        );
    }
    return definitions;
  }

  public static Builder build(String key) {
    return new Builder(key);
  }

  public String key() {
    return key;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public int indicativeSize() {
    return indicativeSize;
  }

  public PropertyType type() {
    return type;
  }

  public List<String> options() {
    return options;
  }

  public PropertyDefinition.Result validate(@Nullable String value) {
    return PropertyDefinition.validate(type, value, options);
  }

  public static class Builder {
    private String key;
    private String name;
    private String description;
    private int indicativeSize;
    private PropertyType type;
    private List<String> options;

    private Builder(String key) {
      this.key = key;
      this.name = "";
      this.description = "";
      this.indicativeSize = 20;
      this.type = PropertyType.STRING;
      this.options = new ArrayList<>();
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder indicativeSize(int indicativeSize) {
      this.indicativeSize = indicativeSize;
      return this;
    }

    public Builder type(PropertyType type) {
      this.type = type;
      return this;
    }

    public Builder options(String... options) {
      this.options.addAll(ImmutableList.copyOf(options));
      return this;
    }

    public Builder options(List<String> options) {
      this.options.addAll(ImmutableList.copyOf(options));
      return this;
    }

    public PropertyFieldDefinition build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Key must be set");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name must be set");
      return new PropertyFieldDefinition(this);
    }
  }
}
