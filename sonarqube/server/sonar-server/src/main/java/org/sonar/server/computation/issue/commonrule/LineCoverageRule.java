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
package org.sonar.server.computation.issue.commonrule;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolder;
import org.sonar.server.rule.CommonRuleKeys;

import static java.lang.String.format;

public class LineCoverageRule extends AbstractCoverageRule {

  public LineCoverageRule(ActiveRulesHolder activeRulesHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    super(activeRulesHolder, CommonRuleKeys.INSUFFICIENT_LINE_COVERAGE, CommonRuleKeys.INSUFFICIENT_LINE_COVERAGE_PROPERTY,
      measureRepository,
      metricRepository.getByKey(CoreMetrics.LINE_COVERAGE_KEY),
      metricRepository.getByKey(CoreMetrics.UNCOVERED_LINES_KEY),
      metricRepository.getByKey(CoreMetrics.LINES_TO_COVER_KEY));
  }

  @Override
  protected String formatMessage(int effortToFix, double minCoverage) {
    // TODO declare min threshold as int but not float ?
    return format("%d more lines of code need to be covered by unit tests to reach the minimum threshold of %s%% lines coverage.", effortToFix, minCoverage);
  }
}
