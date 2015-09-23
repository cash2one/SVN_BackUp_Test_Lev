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
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;
import java.util.HashSet;
import java.util.Set;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.measure.MapBasedRawMeasureRepository.OverridePolicy;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.metric.ReportMetricValidator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.component.ComponentFunctions.toReportRef;

public class MeasureRepositoryImpl implements MeasureRepository {
  private final MapBasedRawMeasureRepository<Integer> delegate = new MapBasedRawMeasureRepository<>(toReportRef());
  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final BatchMeasureToMeasure batchMeasureToMeasure;
  private final MetricRepository metricRepository;
  private final ReportMetricValidator reportMetricValidator;

  private MeasureDtoToMeasure underTest = new MeasureDtoToMeasure();
  private final Set<Integer> loadedComponents = new HashSet<>();

  public MeasureRepositoryImpl(DbClient dbClient, BatchReportReader reportReader, MetricRepository metricRepository, ReportMetricValidator reportMetricValidator) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.reportMetricValidator = reportMetricValidator;
    this.batchMeasureToMeasure = new BatchMeasureToMeasure();
    this.metricRepository = metricRepository;
  }

  @Override
  public Optional<Measure> getBaseMeasure(Component component, Metric metric) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);

    try (DbSession dbSession = dbClient.openSession(false)) {
      MeasureDto measureDto = dbClient.measureDao().selectByComponentKeyAndMetricKey(dbSession, component.getKey(), metric.getKey());
      Optional<Measure> measureOptional = underTest.toMeasure(measureDto, metric);
      if (measureOptional.isPresent()) {
        checkArgument(measureOptional.get().getCharacteristicId() == null, "Measures with characteristicId are not supported");
        checkArgument(measureOptional.get().getRuleId() == null, "Measures with ruleId are not supported");
      }
      return measureOptional;
    }
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric) {
    Optional<Measure> local = delegate.getRawMeasure(component, metric);
    if (local.isPresent()) {
      return local;
    }

    // look up in batch after loading (if not yet loaded) measures from batch
    loadBatchMeasuresForComponent(component);
    return delegate.getRawMeasure(component, metric);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    delegate.add(component, metric, measure);
  }

  @Override
  public void update(Component component, Metric metric, Measure measure) {
    delegate.update(component, metric, measure);
  }

  @Override
  public Set<Measure> getRawMeasures(Component component, Metric metric) {
    loadBatchMeasuresForComponent(component);
    return delegate.getRawMeasures(component, metric);
  }

  @Override
  public SetMultimap<String, Measure> getRawMeasures(Component component) {
    loadBatchMeasuresForComponent(component);
    return delegate.getRawMeasures(component);
  }

  private void loadBatchMeasuresForComponent(Component component) {
    if (loadedComponents.contains(component.getReportAttributes().getRef())) {
      return;
    }

    try (CloseableIterator<BatchReport.Measure> readIt = reportReader.readComponentMeasures(component.getReportAttributes().getRef())) {
      while (readIt.hasNext()) {
        BatchReport.Measure batchMeasure = readIt.next();
        String metricKey = batchMeasure.getMetricKey();
        if (reportMetricValidator.validate(metricKey)) {
          Metric metric = metricRepository.getByKey(metricKey);
          delegate.add(component, metric, batchMeasureToMeasure.toMeasure(batchMeasure, metric).get(), OverridePolicy.DO_NOT_OVERRIDE);
        }
      }
    }
    loadedComponents.add(component.getReportAttributes().getRef());
  }

}
