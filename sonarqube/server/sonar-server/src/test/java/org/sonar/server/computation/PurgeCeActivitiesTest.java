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
package org.sonar.server.computation;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PurgeCeActivitiesTest {

  TestSystem2 system2 = new TestSystem2();

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  PurgeCeActivities underTest = new PurgeCeActivities(dbTester.getDbClient(), system2);

  @Test
  public void delete_older_than_6_months() throws Exception {
    insertWithDate("VERY_OLD", 1_000_000_000_000L);
    insertWithDate("RECENT", 1_500_000_000_000L);
    system2.setNow(1_500_000_000_100L);

    underTest.onServerStart(mock(Server.class));

    assertThat(dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), "VERY_OLD").isPresent()).isFalse();
    assertThat(dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), "RECENT").isPresent()).isTrue();
  }

  private void insertWithDate(String uuid, long date) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType("fake");

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    system2.setNow(date);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
  }
}
