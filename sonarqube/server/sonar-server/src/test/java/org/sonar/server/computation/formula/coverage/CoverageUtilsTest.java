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
package org.sonar.server.computation.formula.coverage;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.formula.CounterInitializationContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.formula.coverage.CoverageUtils.getLongMeasureValue;
import static org.sonar.server.computation.formula.coverage.CoverageUtils.getLongVariation;
import static org.sonar.server.computation.formula.coverage.CoverageUtils.getMeasureVariations;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class CoverageUtilsTest {

  private static final String SOME_METRIC_KEY = "some key";
  public static final MeasureVariations DEFAULT_VARIATIONS = new MeasureVariations(0d, 0d, 0d, 0d, 0d);

  @Rule
  public CounterInitializationContextRule fileAggregateContext = new CounterInitializationContextRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void verify_calculate_coverage() {
    assertThat(CoverageUtils.calculateCoverage(5, 10)).isEqualTo(50d);
  }

  @Test
  public void getLongMeasureValue_returns_0_if_measure_does_not_exist() {
    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(0L);
  }

  @Test
  public void getLongMeasureValue_returns_0_if_measure_is_NO_VALUE() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().createNoValue());

    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(0L);
  }

  @Test
  public void getLongMeasureValue_returns_value_if_measure_is_INT() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().create(152));

    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(152L);
  }

  @Test
  public void getLongMeasureValue_returns_value_if_measure_is_LONG() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().create(152L));

    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(152L);
  }

  @Test
  public void getLongMeasureValue_throws_ISE_if_measure_is_DOUBLE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("value can not be converted to long because current value type is a DOUBLE");

    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().create(152d));

    getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY);
  }

  @Test
  public void getMeasureVariations_returns_0_in_all_MeasureVariations_if_there_is_no_measure() {
    assertThat(getMeasureVariations(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(DEFAULT_VARIATIONS);
  }

  @Test
  public void getMeasureVariations_returns_0_in_all_MeasureVariations_if_there_is_measure_has_no_variations() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().createNoValue());

    assertThat(getMeasureVariations(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(DEFAULT_VARIATIONS);
  }

  @Test
  public void getMeasureVariations_returns_MeasureVariations_of_measure_when_it_has_one() {
    MeasureVariations measureVariations = new MeasureVariations(null, 5d, null, null);
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().setVariations(measureVariations).createNoValue());

    assertThat(getMeasureVariations(fileAggregateContext, SOME_METRIC_KEY)).isSameAs(measureVariations);
  }

  @Test
  public void getLongVariation_returns_0_if_MeasureVariation_has_none_for_the_specified_period() {
    MeasureVariations variations = new MeasureVariations(null, 2d, null, null, 5d);

    assertThat(getLongVariation(variations, createPeriod(1))).isEqualTo(0L);
    assertThat(getLongVariation(variations, createPeriod(2))).isEqualTo(2L);
    assertThat(getLongVariation(variations, createPeriod(3))).isEqualTo(0L);
    assertThat(getLongVariation(variations, createPeriod(4))).isEqualTo(0L);
    assertThat(getLongVariation(variations, createPeriod(5))).isEqualTo(5L);
  }

  private Period createPeriod(int periodIndex) {
    return new Period(periodIndex, "mode" + periodIndex, null, 963L + periodIndex, 9865L + periodIndex);
  }

  private static class CounterInitializationContextRule extends ExternalResource implements CounterInitializationContext {
    private final Map<String, Measure> measures = new HashMap<>();

    public CounterInitializationContextRule put(String metricKey, Measure measure) {
      checkNotNull(metricKey);
      checkNotNull(measure);
      checkState(!measures.containsKey(metricKey));
      measures.put(metricKey, measure);
      return this;
    }

    @Override
    protected void after() {
      measures.clear();
    }

    @Override
    public Component getLeaf() {
      throw new UnsupportedOperationException("getFile is not supported");
    }

    @Override
    public Optional<Measure> getMeasure(String metricKey) {
      return Optional.fromNullable(measures.get(metricKey));
    }

    @Override
    public List<Period> getPeriods() {
      throw new UnsupportedOperationException("getPeriods is not supported");
    }
  }
}
