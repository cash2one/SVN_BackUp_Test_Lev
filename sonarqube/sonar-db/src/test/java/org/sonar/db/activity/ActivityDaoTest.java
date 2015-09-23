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
package org.sonar.db.activity;

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class ActivityDaoTest {

  System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  ActivityDao underTest = dbTester.getDbClient().activityDao();

  @Test
  public void insert() {
    when(system.now()).thenReturn(1_500_000_000_000L);
    ActivityDto dto = new ActivityDto()
      .setKey("UUID_1").setAction("THE_ACTION").setType("THE_TYPE")
      .setAuthor("THE_AUTHOR").setData("THE_DATA");
    underTest.insert(dto);

    Map<String, Object> map = dbTester.selectFirst("select created_at as \"createdAt\", log_action as \"action\", data_field as \"data\" from activities where log_key='UUID_1'");
    assertThat(map.get("action")).isEqualTo("THE_ACTION");
    // not possible to check exact date yet. dbTester#selectFirst() uses ResultSet#getObject(), which returns
    // non-JDBC interface in Oracle driver.
    assertThat(map.get("createdAt")).isNotNull();
    assertThat(map.get("data")).isEqualTo("THE_DATA");
  }
}
