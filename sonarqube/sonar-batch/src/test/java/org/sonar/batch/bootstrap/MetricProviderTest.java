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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricProviderTest {
  @Test
  public void should_provide_at_least_core_metrics() {
    MetricProvider provider = new MetricProvider();
    List<Metric> metrics = provider.provide();

    assertThat(metrics).hasSize(CoreMetrics.getMetrics().size());
    assertThat(metrics).extracting("key").contains("ncloc");
  }

  @Test
  public void should_provide_plugin_metrics() {
    Metrics factory = new Metrics() {
      public List<Metric> getMetrics() {
        return Arrays.<Metric>asList(new Metric.Builder("custom", "Custom", Metric.ValueType.FLOAT).create());
      }
    };
    MetricProvider provider = new MetricProvider(new Metrics[] {factory});
    List<Metric> metrics = provider.provide();

    assertThat(metrics.size()).isEqualTo(1 + CoreMetrics.getMetrics().size());
    assertThat(metrics).extracting("key").contains("custom");
  }
}
