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
package org.sonar.batch.sensor;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.SyntaxHighlightingRule;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Symbol;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.report.BatchReportUtils;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.sensor.coverage.CoverageExclusions;
import org.sonar.batch.source.DefaultSymbol;

public class DefaultSensorStorage implements SensorStorage {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSensorStorage.class);

  private static final List<Metric> INTERNAL_METRICS = Arrays.<Metric>asList(
    // Computed by CpdSensor
    CoreMetrics.DUPLICATED_FILES,
    CoreMetrics.DUPLICATED_LINES,
    CoreMetrics.DUPLICATED_BLOCKS,
    // Computed by LinesSensor
    CoreMetrics.LINES);

  private static final List<String> DEPRECATED_METRICS_KEYS = Arrays.<String>asList(
    CoreMetrics.DEPENDENCY_MATRIX_KEY,
    CoreMetrics.DIRECTORY_CYCLES_KEY,
    CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY,
    CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY,
    CoreMetrics.DIRECTORY_TANGLE_INDEX_KEY,
    CoreMetrics.DIRECTORY_TANGLES_KEY,
    CoreMetrics.FILE_CYCLES_KEY,
    CoreMetrics.FILE_EDGES_WEIGHT_KEY,
    CoreMetrics.FILE_FEEDBACK_EDGES_KEY,
    CoreMetrics.FILE_TANGLE_INDEX_KEY,
    CoreMetrics.FILE_TANGLES_KEY,
    CoreMetrics.DUPLICATIONS_DATA_KEY);

  private final MetricFinder metricFinder;
  private final ModuleIssues moduleIssues;
  private final CoverageExclusions coverageExclusions;
  private final DuplicationCache duplicationCache;
  private final BatchComponentCache resourceCache;
  private final ReportPublisher reportPublisher;
  private final MeasureCache measureCache;

  public DefaultSensorStorage(MetricFinder metricFinder, ModuleIssues moduleIssues,
    Settings settings, FileSystem fs, ActiveRules activeRules, DuplicationCache duplicationCache,
    CoverageExclusions coverageExclusions, BatchComponentCache resourceCache, ReportPublisher reportPublisher, MeasureCache measureCache) {
    this.metricFinder = metricFinder;
    this.moduleIssues = moduleIssues;
    this.coverageExclusions = coverageExclusions;
    this.duplicationCache = duplicationCache;
    this.resourceCache = resourceCache;
    this.reportPublisher = reportPublisher;
    this.measureCache = measureCache;
  }

  private Metric findMetricOrFail(String metricKey) {
    Metric m = (Metric) metricFinder.findByKey(metricKey);
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    return m;
  }

  @Override
  public void store(Measure newMeasure) {
    DefaultMeasure<?> measure = (DefaultMeasure<?>) newMeasure;
    org.sonar.api.measures.Metric m = findMetricOrFail(measure.metric().key());
    org.sonar.api.measures.Measure measureToSave = new org.sonar.api.measures.Measure(m);
    setValueAccordingToMetricType(newMeasure, m, measureToSave);
    measureToSave.setFromCore(measure.isFromCore());
    InputComponent inputComponent = newMeasure.inputComponent();
    Resource resource = resourceCache.get(inputComponent).resource();
    if (coverageExclusions.accept(resource, measureToSave)) {
      saveMeasure(resource, measureToSave);
    }
  }

  public org.sonar.api.measures.Measure saveMeasure(Resource resource, org.sonar.api.measures.Measure measure) {
    if (DEPRECATED_METRICS_KEYS.contains(measure.getMetricKey())) {
      // Ignore deprecated metrics
      return null;
    }
    org.sonar.api.batch.measure.Metric metric = metricFinder.findByKey(measure.getMetricKey());
    if (metric == null) {
      throw new SonarException("Unknown metric: " + measure.getMetricKey());
    }
    if (!measure.isFromCore() && INTERNAL_METRICS.contains(metric)) {
      LOG.debug("Metric " + metric.key() + " is an internal metric computed by SonarQube. Provided value is ignored.");
      return measure;
    }
    if (measureCache.contains(resource, measure)) {
      throw new SonarException("Can not add the same measure twice on " + resource + ": " + measure);
    }
    measureCache.put(resource, measure);
    return measure;
  }

  private void setValueAccordingToMetricType(Measure<?> measure, org.sonar.api.measures.Metric<?> m, org.sonar.api.measures.Measure measureToSave) {
    switch (m.getType()) {
      case BOOL:
        measureToSave.setValue(Boolean.TRUE.equals(measure.value()) ? 1.0 : 0.0);
        break;
      case INT:
      case MILLISEC:
      case WORK_DUR:
      case FLOAT:
      case PERCENT:
      case RATING:
        measureToSave.setValue(((Number) measure.value()).doubleValue());
        break;
      case STRING:
      case LEVEL:
      case DATA:
      case DISTRIB:
        measureToSave.setData((String) measure.value());
        break;
      default:
        throw new UnsupportedOperationException("Unsupported type :" + m.getType());
    }
  }

  @Override
  public void store(Issue issue) {
    moduleIssues.initAndAddIssue(issue);
  }

  private File getFile(InputFile file) {
    BatchComponent r = resourceCache.get(file);
    if (r == null) {
      throw new IllegalStateException("Provided input file is not indexed");
    }
    return (File) r.resource();
  }

  @Override
  public void store(Duplication duplication) {
    duplicationCache.put(duplication.originBlock().resourceKey(), (DefaultDuplication) duplication);
  }

  @Override
  public void store(DefaultHighlighting highlighting) {
    BatchReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) highlighting.inputFile();
    writer.writeComponentSyntaxHighlighting(resourceCache.get(inputFile).batchId(),
      Iterables.transform(highlighting.getSyntaxHighlightingRuleSet(), new BuildSyntaxHighlighting()));
  }

  public void store(DefaultInputFile inputFile, Map<Symbol, Set<TextRange>> referencesBySymbol) {
    BatchReportWriter writer = reportPublisher.getWriter();
    writer.writeComponentSymbols(resourceCache.get(inputFile).batchId(),
      Iterables.transform(referencesBySymbol.entrySet(), new Function<Map.Entry<Symbol, Set<TextRange>>, BatchReport.Symbol>() {
        private BatchReport.Symbol.Builder builder = BatchReport.Symbol.newBuilder();
        private BatchReport.TextRange.Builder rangeBuilder = BatchReport.TextRange.newBuilder();

        @Override
        public BatchReport.Symbol apply(Map.Entry<Symbol, Set<TextRange>> input) {
          builder.clear();
          rangeBuilder.clear();
          DefaultSymbol symbol = (DefaultSymbol) input.getKey();
          builder.setDeclaration(rangeBuilder.setStartLine(symbol.range().start().line())
            .setStartOffset(symbol.range().start().lineOffset())
            .setEndLine(symbol.range().end().line())
            .setEndOffset(symbol.range().end().lineOffset())
            .build());
          for (TextRange reference : input.getValue()) {
            builder.addReference(rangeBuilder.setStartLine(reference.start().line())
              .setStartOffset(reference.start().lineOffset())
              .setEndLine(reference.end().line())
              .setEndOffset(reference.end().lineOffset())
              .build());
          }
          return builder.build();
        }

      }));
  }

  @Override
  public void store(DefaultCoverage defaultCoverage) {
    File file = getFile(defaultCoverage.inputFile());
    if (coverageExclusions.hasMatchingPattern(file)) {
      return;
    }
    CoverageType type = defaultCoverage.type();
    if (defaultCoverage.linesToCover() > 0) {
      saveMeasure(file, new org.sonar.api.measures.Measure(type.linesToCover(), (double) defaultCoverage.linesToCover()));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.uncoveredLines(), (double) (defaultCoverage.linesToCover() - defaultCoverage.coveredLines())));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.lineHitsData()).setData(KeyValueFormat.format(defaultCoverage.hitsByLine())));
    }
    if (defaultCoverage.conditions() > 0) {
      saveMeasure(file, new org.sonar.api.measures.Measure(type.conditionsToCover(), (double) defaultCoverage.conditions()));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.uncoveredConditions(), (double) (defaultCoverage.conditions() - defaultCoverage.coveredConditions())));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.coveredConditionsByLine()).setData(KeyValueFormat.format(defaultCoverage.coveredConditionsByLine())));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.conditionsByLine()).setData(KeyValueFormat.format(defaultCoverage.conditionsByLine())));
    }
  }

  private static class BuildSyntaxHighlighting implements Function<SyntaxHighlightingRule, BatchReport.SyntaxHighlighting> {
    private BatchReport.SyntaxHighlighting.Builder builder = BatchReport.SyntaxHighlighting.newBuilder();
    private BatchReport.TextRange.Builder rangeBuilder = BatchReport.TextRange.newBuilder();

    @Override
    public BatchReport.SyntaxHighlighting apply(@Nonnull SyntaxHighlightingRule input) {
      builder.setRange(rangeBuilder.setStartLine(input.range().start().line())
        .setStartOffset(input.range().start().lineOffset())
        .setEndLine(input.range().end().line())
        .setEndOffset(input.range().end().lineOffset())
        .build());
      builder.setType(BatchReportUtils.toProtocolType(input.getTextType()));
      return builder.build();
    }
  }
}
