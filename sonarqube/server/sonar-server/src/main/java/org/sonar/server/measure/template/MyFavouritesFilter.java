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
package org.sonar.server.measure.template;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterColumn;
import org.sonar.api.web.FilterTemplate;

/**
 * Default filter for looking for user favourite resources.
 *
 * @since 3.1
 */
public class MyFavouritesFilter extends FilterTemplate {
  public static final String NAME = "My favourites";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Filter createFilter() {
    return Filter.create()
      .setDisplayAs(Filter.LIST)
      .setFavouritesOnly(true)
      .add(FilterColumn.create("metric", CoreMetrics.ALERT_STATUS_KEY, FilterColumn.DESC, false))
      .add(FilterColumn.create("name", null, FilterColumn.ASC, false))
      .add(FilterColumn.create("date", null, FilterColumn.DESC, false));
  }
}
