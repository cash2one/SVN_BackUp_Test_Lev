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

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.AbstractIterableAssert;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.formula.counter.IntVariationValue;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepoEntry;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_IT_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ViewsComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class ViewsFormulaExecutorComponentVisitorTest {
  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_1_REF = 11;
  private static final int SUB_SUBVIEW_REF = 111;
  private static final int PROJECT_VIEW_1_REF = 1111;
  private static final int PROJECT_VIEW_2_REF = 1113;
  private static final int SUBVIEW_2_REF = 12;
  private static final int PROJECT_VIEW_3_REF = 121;
  private static final int PROJECT_VIEW_4_REF = 13;

  private static final ViewsComponent BALANCED_COMPONENT_TREE = ViewsComponent.builder(VIEW, ROOT_REF)
    .addChildren(
      ViewsComponent.builder(SUBVIEW, SUBVIEW_1_REF)
        .addChildren(
            ViewsComponent.builder(SUBVIEW, SUB_SUBVIEW_REF)
                .addChildren(
                    builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                    builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
                .build())
        .build(),
      ViewsComponent.builder(SUBVIEW, SUBVIEW_2_REF)
        .addChildren(
            builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
        .build(),
      builder(PROJECT_VIEW, PROJECT_VIEW_4_REF).build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES)
    .add(CoreMetrics.NCLOC)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_IT_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule()
    .setPeriods(new Period(2, "some mode", null, 95l, 756l), new Period(5, "some other mode", null, 756L, 956L));

  FormulaExecutorComponentVisitor underTest = FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
    .withVariationSupport(periodsHolder)
    .buildFor(ImmutableList.<Formula>of(new FakeFormula(), new FakeVariationFormula()));

  @Test
  public void verify_aggregation_on_value() throws Exception {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);
    addRawMeasure(PROJECT_VIEW_1_REF, 1, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_2_REF, 2, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_3_REF, 3, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_4_REF, 4, LINES_KEY);

    new PathAwareCrawler<>(underTest).visit(BALANCED_COMPONENT_TREE);

    verifyProjectViewsHasNoAddedRawMeasures();
    verifySingleMetricValue(SUB_SUBVIEW_REF, 3);
    verifySingleMetricValue(SUBVIEW_1_REF, 3);
    verifySingleMetricValue(SUBVIEW_2_REF, 3);
    verifySingleMetricValue(ROOT_REF, 10);
  }

  private MeasureRepositoryRule addRawMeasure(int componentRef, int value, String metricKey) {
    return measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  @Test
  public void verify_multi_metric_formula_support_and_aggregation() throws Exception {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);
    addRawMeasure(PROJECT_VIEW_1_REF, 1, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_2_REF, 2, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_3_REF, 5, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_4_REF, 4, LINES_KEY);

    FormulaExecutorComponentVisitor underTest = FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .withVariationSupport(periodsHolder)
      .buildFor(ImmutableList.<Formula>of(new FakeMultiMetricFormula()));
    new PathAwareCrawler<>(underTest).visit(BALANCED_COMPONENT_TREE);

    verifyProjectViewsHasNoAddedRawMeasures();
    verifyMultiMetricValues(SUB_SUBVIEW_REF, 13, 103);
    verifyMultiMetricValues(SUBVIEW_1_REF, 13, 103);
    verifyMultiMetricValues(SUBVIEW_2_REF, 15, 105);
    verifyMultiMetricValues(ROOT_REF, 22, 112);
  }

  @Test
  public void verify_aggregation_on_variations() throws Exception {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    addRawMeasureWithVariation(PROJECT_VIEW_1_REF, NEW_LINES_TO_COVER_KEY, 10, 20);
    addRawMeasureWithVariation(PROJECT_VIEW_2_REF, NEW_LINES_TO_COVER_KEY, 8, 16);
    addRawMeasureWithVariation(PROJECT_VIEW_3_REF, NEW_LINES_TO_COVER_KEY, 2, 4);
    addRawMeasureWithVariation(PROJECT_VIEW_4_REF, NEW_LINES_TO_COVER_KEY, 3, 7);

    new PathAwareCrawler<>(underTest).visit(BALANCED_COMPONENT_TREE);

    verifyProjectViewsHasNoAddedRawMeasures();
    verifySingleMetricWithVariations(SUB_SUBVIEW_REF, 18, 36);
    verifySingleMetricWithVariations(SUBVIEW_1_REF, 18, 36);
    verifySingleMetricWithVariations(SUBVIEW_2_REF, 2, 4);
    verifySingleMetricWithVariations(ROOT_REF, 23, 47);
  }

  private AbstractIterableAssert<?, ? extends Iterable<? extends MeasureRepoEntry>, MeasureRepoEntry> verifySingleMetricWithVariations(int componentRef, int variation2Value, int variation5Value) {
    return assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(entryOf(NEW_IT_COVERAGE_KEY, createMeasureWithVariation(variation2Value, variation5Value)));
  }

  private MeasureRepositoryRule addRawMeasureWithVariation(int componentRef, String metricKey, int variation2Value, int variation5Value) {
    return measureRepository.addRawMeasure(componentRef, metricKey, createMeasureWithVariation(variation2Value, variation5Value));
  }

  private static Measure createMeasureWithVariation(double variation2Value, double variation5Value) {
    return newMeasureBuilder().setVariations(new MeasureVariations(null, variation2Value, null, null, variation5Value)).createNoValue();
  }

  @Test
  public void add_no_measure() throws Exception {
    ViewsComponent project = ViewsComponent.builder(VIEW, ROOT_REF)
      .addChildren(
        ViewsComponent.builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            ViewsComponent.builder(SUBVIEW, SUB_SUBVIEW_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(underTest).visit(project);

    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(SUBVIEW_1_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(SUB_SUBVIEW_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(PROJECT_VIEW_1_REF)).isEmpty();
  }

  @Test
  public void add_no_measure_when_no_file() throws Exception {
    ViewsComponent project = ViewsComponent.builder(VIEW, ROOT_REF)
      .addChildren(
        ViewsComponent.builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            ViewsComponent.builder(SUBVIEW, SUB_SUBVIEW_REF).build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(underTest).visit(project);

    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(SUBVIEW_1_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(SUB_SUBVIEW_REF)).isEmpty();
  }

  private class FakeFormula implements Formula<FakeCounter> {

    @Override
    public FakeCounter createNewCounter() {
      return new FakeCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric()).isSameAs(metricRepository.getByKey(NCLOC_KEY));

      // simplest computation
      if (counter.value <= 0) {
        return Optional.absent();
      }
      return Optional.of(Measure.newMeasureBuilder().create(counter.value));
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NCLOC_KEY};
    }
  }

  private class FakeMultiMetricFormula implements Formula<FakeCounter> {

    @Override
    public FakeCounter createNewCounter() {
      return new FakeCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric())
        .isIn(metricRepository.getByKey(NEW_LINES_TO_COVER_KEY), metricRepository.getByKey(NEW_IT_COVERAGE_KEY));

      // simplest computation
      if (counter.value <= 0) {
        return Optional.absent();
      }
      return Optional.of(Measure.newMeasureBuilder().create(counter.value + metricOffset(context.getMetric())));
    }

    private int metricOffset(Metric metric) {
      if (metric.getKey().equals(NEW_LINES_TO_COVER_KEY)) {
        return 10;
      }
      if (metric.getKey().equals(NEW_IT_COVERAGE_KEY)) {
        return 100;
      }
      throw new IllegalArgumentException("Unsupported metric " + metric);
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_TO_COVER_KEY, NEW_IT_COVERAGE_KEY};
    }
  }

  private class FakeCounter implements Counter<FakeCounter> {
    private int value = 0;

    @Override
    public void aggregate(FakeCounter counter) {
      this.value += counter.value;
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      verifyLeafContext(context);

      Optional<Measure> measureOptional = context.getMeasure(LINES_KEY);
      if (measureOptional.isPresent()) {
        value += measureOptional.get().getIntValue();
      }
    }
  }

  private class FakeVariationFormula implements Formula<FakeVariationCounter> {

    @Override
    public FakeVariationCounter createNewCounter() {
      return new FakeVariationCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeVariationCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric()).isSameAs(metricRepository.getByKey(NEW_IT_COVERAGE_KEY));

      Optional<MeasureVariations> measureVariations = counter.values.toMeasureVariations();
      if (measureVariations.isPresent()) {
        return Optional.of(
          newMeasureBuilder()
            .setVariations(measureVariations.get())
            .createNoValue());
      }
      return Optional.absent();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_IT_COVERAGE_KEY};
    }
  }

  private class FakeVariationCounter implements Counter<FakeVariationCounter> {
    private final IntVariationValue.Array values = IntVariationValue.newArray();

    @Override
    public void aggregate(FakeVariationCounter counter) {
      values.incrementAll(counter.values);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      verifyLeafContext(context);

      Optional<Measure> measureOptional = context.getMeasure(NEW_LINES_TO_COVER_KEY);
      if (!measureOptional.isPresent()) {
        return;
      }
      for (Period period : context.getPeriods()) {
        this.values.increment(
          period,
          (int) measureOptional.get().getVariations().getVariation(period.getIndex()));
      }
    }
  }

  private void verifyProjectViewsHasNoAddedRawMeasures() {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(PROJECT_VIEW_1_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(PROJECT_VIEW_2_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(PROJECT_VIEW_3_REF))).isEmpty();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(PROJECT_VIEW_4_REF))).isEmpty();
  }

  private void verifySingleMetricValue(int componentRef, int measureValue) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(entryOf(NCLOC_KEY, newMeasureBuilder().create(measureValue)));
  }

  private void verifyMultiMetricValues(int componentRef, int valueLinesToCover, int valueItCoverage) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
        .containsOnly(
            entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(valueLinesToCover)),
            entryOf(NEW_IT_COVERAGE_KEY, newMeasureBuilder().create(valueItCoverage)));
  }

  private void verifyLeafContext(CounterInitializationContext context) {
    assertThat(context.getLeaf().getKey()).isIn(String.valueOf(PROJECT_VIEW_1_REF), String.valueOf(PROJECT_VIEW_2_REF), String.valueOf(PROJECT_VIEW_3_REF),
      String.valueOf(PROJECT_VIEW_4_REF));
    assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
  }

}
