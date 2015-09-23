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
package org.sonar.batch.protocol.input;

import org.junit.Test;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalRepositoriesTest {

  @Test
  public void to_json() {
    GlobalRepositories ref = new GlobalRepositories();
    ref.addMetric(new Metric(1, "ncloc", "INT", "Description", -1, "NCLOC", true, false, 2.0, 1.0, true));
    ref.addGlobalSetting("prop", "value");
    ref.setTimestamp(10);

    JsonAssert
      .assertJson(ref.toJson())
      .isSimilarTo(getClass().getResource("GlobalRepositoriesTest/expected.json"));
  }

  @Test
  public void from_json() {
    GlobalRepositories ref = GlobalRepositories
      .fromJson(
      "{timestamp:1,"
        + "metrics:[{id:1,key:ncloc,valueType:DATA,description:Description,direction:-1,name:NCLOC,qualitative:true,userManaged:false,worstValue:2.0,bestValue:1.0,optimizedBestValue:true}],"
        + "globalSettings:{prop:value}}");

    assertThat(ref.timestamp()).isEqualTo(1);
    Metric metric = ref.metrics().iterator().next();
    assertThat(metric.id()).isEqualTo(1);
    assertThat(metric.key()).isEqualTo("ncloc");
    assertThat(metric.valueType()).isEqualTo("DATA");
    assertThat(metric.description()).isEqualTo("Description");
    assertThat(metric.direction()).isEqualTo(-1);
    assertThat(metric.name()).isEqualTo("NCLOC");
    assertThat(metric.isQualitative()).isTrue();
    assertThat(metric.isUserManaged()).isFalse();
    assertThat(metric.worstValue()).isEqualTo(2.0);
    assertThat(metric.bestValue()).isEqualTo(1.0);
    assertThat(metric.isOptimizedBestValue()).isTrue();

    assertThat(ref.globalSettings()).containsEntry("prop", "value");
  }
}
