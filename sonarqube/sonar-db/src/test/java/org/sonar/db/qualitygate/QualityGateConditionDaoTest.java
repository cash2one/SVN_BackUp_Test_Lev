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
package org.sonar.db.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class QualityGateConditionDaoTest {

  private static final String[] COLUMNS_WITHOUT_TIMESTAMPS = {
    "id", "qgate_id", "metric_id", "operator", "value_warning", "value_error", "period"
  };

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  QualityGateConditionDao dao = dbTester.getDbClient().gateConditionDao();

  @Test
  public void testInsert() throws Exception {
    dbTester.prepareDbUnit(getClass(), "insert.xml");
    QualityGateConditionDto newCondition = new QualityGateConditionDto()
      .setQualityGateId(1L).setMetricId(2L).setOperator("GT").setWarningThreshold("10").setErrorThreshold("20").setPeriod(3);
    dao.insert(newCondition);
    dbTester.assertDbUnitTable(getClass(), "insert-result.xml", "quality_gate_conditions", "metric_id", "operator", "error_value", "warning_value", "period");
    assertThat(newCondition.getId()).isNotNull();
  }

  @Test
  public void testSelectForQualityGate() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectForQualityGate.xml");
    assertThat(dao.selectForQualityGate(1L)).hasSize(3);
    assertThat(dao.selectForQualityGate(2L)).hasSize(2);
  }

  @Test
  public void testSelectById() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectForQualityGate.xml");
    QualityGateConditionDto selectById = dao.selectById(1L);
    assertThat(selectById).isNotNull();
    assertThat(selectById.getId()).isNotNull().isNotEqualTo(0L);
    assertThat(selectById.getMetricId()).isEqualTo(2L);
    assertThat(selectById.getOperator()).isEqualTo("<");
    assertThat(selectById.getPeriod()).isEqualTo(3);
    assertThat(selectById.getQualityGateId()).isEqualTo(1L);
    assertThat(selectById.getWarningThreshold()).isEqualTo("10");
    assertThat(selectById.getErrorThreshold()).isEqualTo("20");
    assertThat(dao.selectById(42L)).isNull();
  }

  @Test
  public void testDelete() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectForQualityGate.xml");
    dao.delete(new QualityGateConditionDto().setId(1L));
    dbTester.assertDbUnitTable(getClass(), "delete-result.xml", "quality_gate_conditions", COLUMNS_WITHOUT_TIMESTAMPS);
  }

  @Test
  public void testUpdate() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectForQualityGate.xml");
    dao.update(new QualityGateConditionDto().setId(1L).setMetricId(7L).setOperator(">").setPeriod(1).setWarningThreshold("50").setErrorThreshold("80"));
    dbTester.assertDbUnitTable(getClass(), "update-result.xml", "quality_gate_conditions", COLUMNS_WITHOUT_TIMESTAMPS);
  }

  @Test
  public void shouldCleanConditions() {
    dbTester.prepareDbUnit(getClass(), "shouldCleanConditions.xml");
    dao.deleteConditionsWithInvalidMetrics();
    dbTester.assertDbUnit(getClass(), "shouldCleanConditions-result.xml", new String[]{"created_at", "updated_at"}, "quality_gate_conditions");
  }
}
