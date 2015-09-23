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
package org.sonar.server.computation.measure.qualitygatedetails;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.Test;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.qualitygate.Condition;
import org.sonar.test.JsonAssert;

public class QualityGateDetailsDataTest {
  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_Level_arg_is_null() {
    new QualityGateDetailsData(null, Collections.<EvaluatedCondition>emptyList());
  }

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_Iterable_arg_is_null() {
    new QualityGateDetailsData(Measure.Level.OK, null);
  }

  @Test
  public void verify_json_when_there_is_no_condition() {
    String actualJson = new QualityGateDetailsData(Measure.Level.OK, Collections.<EvaluatedCondition>emptyList()).toJson();

    JsonAssert.assertJson(actualJson).isSimilarTo("{" +
        "\"level\":\"OK\"," +
        "\"conditions\":[]" +
        "}");
  }

  @Test
  public void verify_json_for_each_type_of_condition() {
    String value = "actualValue";
    Condition condition = new Condition(new MetricImpl(1, "key1", "name1", Metric.MetricType.STRING), Condition.Operator.GREATER_THAN.getDbValue(), "errorTh", "warnTh", 10);
    ImmutableList<EvaluatedCondition> evaluatedConditions = ImmutableList.of(
        new EvaluatedCondition(condition, Measure.Level.OK, value),
        new EvaluatedCondition(condition, Measure.Level.WARN, value),
        new EvaluatedCondition(condition, Measure.Level.ERROR, value)
    );
    String actualJson = new QualityGateDetailsData(Measure.Level.OK, evaluatedConditions).toJson();

    JsonAssert.assertJson(actualJson).isSimilarTo("{" +
        "\"level\":\"OK\"," +
        "\"conditions\":[" +
        "  {" +
        "    \"metric\":\"key1\"," +
        "    \"op\":\"GT\"," +
        "    \"period\":10," +
        "    \"warning\":\"warnTh\"," +
        "    \"error\":\"errorTh\"," +
        "    \"actual\":\"actualValue\"," +
        "    \"level\":\"OK\"" +
        "  }," +
        "  {" +
        "    \"metric\":\"key1\"," +
        "    \"op\":\"GT\"," +
        "    \"period\":10," +
        "    \"warning\":\"warnTh\"," +
        "    \"error\":\"errorTh\"," +
        "    \"actual\":\"actualValue\"," +
        "    \"level\":\"WARN\"" +
        "  }," +
        "  {" +
        "    \"metric\":\"key1\"," +
        "    \"op\":\"GT\"," +
        "    \"period\":10," +
        "    \"warning\":\"warnTh\"," +
        "    \"error\":\"errorTh\"," +
        "    \"actual\":\"actualValue\"," +
        "    \"level\":\"ERROR\"" +
        "  }" +
        "]" +
        "}");
  }
}
