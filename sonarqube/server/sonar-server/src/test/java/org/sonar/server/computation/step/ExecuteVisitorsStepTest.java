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

package org.sonar.server.computation.step;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class ExecuteVisitorsStepTest {

  private static final String TEST_METRIC_KEY = "test";

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(1, NCLOC)
    .add(new MetricImpl(2, TEST_METRIC_KEY, "name", Metric.MetricType.INT));

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF).setKey("project")
        .addChildren(
          builder(MODULE, MODULE_REF).setKey("module")
            .addChildren(
              builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
                .addChildren(
                  builder(FILE, FILE_1_REF).setKey("file1").build(),
                  builder(FILE, FILE_2_REF).setKey("file2").build()
                ).build()
            ).build()
        ).build());
  }

  @Test
  public void execute_with_type_aware_visitor() throws Exception {
    ExecuteVisitorsStep underStep = new ExecuteVisitorsStep(treeRootHolder, Arrays.<ComponentVisitor>asList(new TestTypeAwareVisitor()));

    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(3));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(3));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(3));

    underStep.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(3);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(4);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(4);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(4);
  }

  @Test
  public void execute_with_path_aware_visitor() throws Exception {
    ExecuteVisitorsStep underStep = new ExecuteVisitorsStep(treeRootHolder, Arrays.<ComponentVisitor>asList(new TestPathAwareVisitor()));

    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(1));

    underStep.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_METRIC_KEY).get().getIntValue()).isEqualTo(2);
  }

  private class TestTypeAwareVisitor extends TypeAwareVisitorAdapter {

    public TestTypeAwareVisitor() {
      super(CrawlerDepthLimit.FILE, ComponentVisitor.Order.POST_ORDER);
    }

    @Override
    public void visitAny(Component any) {
      int ncloc = measureRepository.getRawMeasure(any, metricRepository.getByKey(NCLOC_KEY)).get().getIntValue();
      measureRepository.add(any, metricRepository.getByKey(TEST_METRIC_KEY), newMeasureBuilder().create(ncloc + 1));
    }
  }

  private class TestPathAwareVisitor extends PathAwareVisitorAdapter<Counter> {

    public TestPathAwareVisitor() {
      super(CrawlerDepthLimit.FILE, ComponentVisitor.Order.POST_ORDER, new SimpleStackElementFactory<Counter>() {
        @Override
        public Counter createForAny(Component component) {
          return new Counter();
        }
      });
    }

    @Override
    public void visitProject(Component project, Path<Counter> path) {
      computeAndSaveMeasures(project, path);
    }

    @Override
    public void visitModule(Component module, Path<Counter> path) {
      computeAndSaveMeasures(module, path);
    }

    @Override
    public void visitDirectory(Component directory, Path<Counter> path) {
      computeAndSaveMeasures(directory, path);
    }

    @Override
    public void visitFile(Component file, Path<Counter> path) {
      int ncloc = measureRepository.getRawMeasure(file, metricRepository.getByKey(NCLOC_KEY)).get().getIntValue();
      path.current().add(ncloc);
      computeAndSaveMeasures(file, path);
    }

    private void computeAndSaveMeasures(Component component, Path<Counter> path) {
      measureRepository.add(component, metricRepository.getByKey(TEST_METRIC_KEY), newMeasureBuilder().create(path.current().getValue()));
      increaseParentValue(path);
    }

    private void increaseParentValue(Path<Counter> path) {
      if (!path.isRoot()) {
        path.parent().add(path.current().getValue());
      }
    }
  }

  public class Counter {
    private int value = 0;

    public void add(int value) {
      this.value += value;
    }

    public int getValue() {
      return value;
    }
  }
}
