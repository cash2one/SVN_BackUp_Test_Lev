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
package org.sonar.server.computation.metric;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.picocontainer.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

public class MetricRepositoryImpl implements MetricRepository, Startable {

  private final DbClient dbClient;
  @CheckForNull
  private Map<String, Metric> metricsByKey;
  @CheckForNull
  private Map<Long, Metric> metricsById;

  public MetricRepositoryImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<MetricDto> metricList = dbClient.metricDao().selectEnabled(dbSession);
      this.metricsByKey = from(metricList).transform(MetricDtoToMetric.INSTANCE).uniqueIndex(MetricToKey.INSTANCE);
      this.metricsById = from(metricList).transform(MetricDtoToMetric.INSTANCE).uniqueIndex(MetricToId.INSTANCE);
    } finally {
      dbSession.close();
    }
  }

  @Override
  public void stop() {
    // nothing to do when stopping
  }

  @Override
  public Metric getByKey(String key) {
    requireNonNull(key);
    verifyMetricsInitialized();

    Metric res = this.metricsByKey.get(key);
    if (res == null) {
      throw new IllegalStateException(String.format("Metric with key '%s' does not exist", key));
    }
    return res;
  }

  @Override
  public Metric getById(long id) {
    verifyMetricsInitialized();

    Metric res = this.metricsById.get(id);
    if (res == null) {
      throw new IllegalStateException(String.format("Metric with id '%s' does not exist", id));
    }
    return res;
  }

  @Override
  public Iterable<Metric> getAll() {
    return FluentIterable.from(metricsByKey.values()).toSet();
  }

  private void verifyMetricsInitialized() {
    if (this.metricsByKey == null) {
      throw new IllegalStateException("Metric cache has not been initialized");
    }
  }

  private enum MetricToKey implements Function<Metric, String> {
    INSTANCE;

    @Override
    @Nonnull
    public String apply(@Nonnull Metric metric) {
      return metric.getKey();
    }
  }

  private enum MetricToId implements Function<Metric, Long> {
    INSTANCE;

    @Override
    @Nonnull
    public Long apply(@Nonnull Metric metric) {
      return (long) metric.getId();
    }
  }

}
