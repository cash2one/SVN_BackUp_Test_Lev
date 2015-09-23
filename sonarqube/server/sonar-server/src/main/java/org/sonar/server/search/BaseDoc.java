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
package org.sonar.server.search;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Base implementation for business objects based on elasticsearch document
 */
public abstract class BaseDoc {

  protected final Map<String, Object> fields;

  protected BaseDoc() {
    this.fields = newHashMap();
  }

  protected BaseDoc(Map<String, Object> fields) {
    this.fields = fields;
  }

  public String keyField() {
    return (String) fields.get("key");
  }

  /**
   * Use this method when field value can be null. See warning in {@link #getField(String)}
   */
  @CheckForNull
  public <K> K getNullableField(String key) {
    if (!fields.containsKey(key)) {
      throw new IllegalStateException(String.format("Field %s not specified in query options", key));
    }
    return (K) fields.get(key);
  }

  @CheckForNull
  public Date getNullableFieldAsDate(String key) {
    Object val = getNullableField(key);
    if (val != null) {
      if (val instanceof Date) {
        return (Date)val;
      }
      return IndexUtils.parseDateTime((String) val);
    }
    return null;
  }

  /**
   * Use this method when you are sure that the value can't be null in ES document.
   * <p/>
   * Warning with numbers - even if mapping declares long field, value can be an Integer
   * instead of an expected Long. The reason is that ES delegates the deserialization of JSON
   * to Jackson, which doesn't know the field type declared in mapping. See
   * https://groups.google.com/forum/#!searchin/elasticsearch/getsource$20integer$20long/elasticsearch/jxIY22TmA8U/PyqZPPyYQ0gJ
   * for more details. Workaround is to cast to java.lang.Number and then to call {@link Number#longValue()}
   */
  public <K> K getField(String key) {
    K value = getNullableField(key);
    if (value == null) {
      throw new IllegalStateException("Value of index field is null: " + key);
    }
    return value;
  }

  public Date getFieldAsDate(String key) {
    Object value = getField(key);
    if (value instanceof Date) {
      return (Date)value;
    }
    return IndexUtils.parseDateTime((String)value);
  }

  public void setField(String key, @Nullable Object value) {
    fields.put(key, value);
  }

  public Map<String, Object> getFields() {
    return fields;
  }
}
