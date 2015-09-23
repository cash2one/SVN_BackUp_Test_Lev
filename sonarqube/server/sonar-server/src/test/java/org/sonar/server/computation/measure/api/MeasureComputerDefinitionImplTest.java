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

package org.sonar.server.computation.measure.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.MeasureComputer;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureComputerDefinitionImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_measure_computer_definition() throws Exception {
    String inputMetric = "ncloc";
    String outputMetric = "comment_density";
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics(inputMetric)
      .setOutputMetrics(outputMetric)
      .build();

    assertThat(measureComputer.getInputMetrics()).containsOnly(inputMetric);
    assertThat(measureComputer.getOutputMetrics()).containsOnly(outputMetric);
  }

  @Test
  public void build_measure_computer_with_multiple_metrics() throws Exception {
    String[] inputMetrics = {"ncloc", "comment"};
    String[] outputMetrics = {"comment_density_1", "comment_density_2"};
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics(inputMetrics)
      .setOutputMetrics(outputMetrics)
      .build();

    assertThat(measureComputer.getInputMetrics()).containsOnly(inputMetrics);
    assertThat(measureComputer.getOutputMetrics()).containsOnly(outputMetrics);
  }

  @Test
  public void input_metrics_can_be_empty() throws Exception {
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics()
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();

    assertThat(measureComputer.getInputMetrics()).isEmpty();
  }

  @Test
  public void input_metrics_is_empty_when_not_set() throws Exception {
    MeasureComputer.MeasureComputerDefinition measureComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();

    assertThat(measureComputer.getInputMetrics()).isEmpty();
  }

  @Test
  public void fail_with_NPE_when_null_input_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Input metrics cannot be null");

    new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics(null)
      .setOutputMetrics("comment_density_1", "comment_density_2");
  }

  @Test
  public void fail_with_NPE_when_one_input_metric_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Null metric is not allowed");

    new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", null)
      .setOutputMetrics("comment_density_1", "comment_density_2");
  }

  @Test
  public void fail_with_NPE_when_no_output_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Output metrics cannot be null");

    new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .build();
  }

  @Test
  public void fail_with_NPE_when_null_output_metrics() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Output metrics cannot be null");

    new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics(null);
  }

  @Test
  public void fail_with_NPE_when_one_output_metric_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Null metric is not allowed");

    new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", null);
  }

  @Test
  public void fail_with_IAE_with_empty_output_metrics() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("At least one output metric must be defined");

    new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics();
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    MeasureComputer.MeasureComputerDefinition computer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build();

    MeasureComputer.MeasureComputerDefinition sameComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("comment", "ncloc")
      .setOutputMetrics("comment_density_2", "comment_density_1")
      .build();

    MeasureComputer.MeasureComputerDefinition anotherComputer = new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("comment")
      .setOutputMetrics("debt")
      .build();

    assertThat(computer).isEqualTo(computer);
    assertThat(computer).isEqualTo(sameComputer);
    assertThat(computer).isNotEqualTo(anotherComputer);
    assertThat(computer).isNotEqualTo(null);

    assertThat(computer.hashCode()).isEqualTo(computer.hashCode());
    assertThat(computer.hashCode()).isEqualTo(sameComputer.hashCode());
    assertThat(computer.hashCode()).isNotEqualTo(anotherComputer.hashCode());
  }

  @Test
  public void test_to_string() throws Exception {
    assertThat(new MeasureComputerDefinitionImpl.BuilderImpl()
      .setInputMetrics("ncloc", "comment")
      .setOutputMetrics("comment_density_1", "comment_density_2")
      .build().toString())
      .isEqualTo("MeasureComputerDefinitionImpl{inputMetricKeys=[ncloc, comment], outputMetrics=[comment_density_1, comment_density_2]}");
  }

}
