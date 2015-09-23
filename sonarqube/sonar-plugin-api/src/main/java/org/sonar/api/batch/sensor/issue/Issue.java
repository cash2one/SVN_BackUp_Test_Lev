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
package org.sonar.api.batch.sensor.issue;

import com.google.common.annotations.Beta;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rule.RuleKey;

/**
 * Represents an issue detected by a {@link Sensor}.
 *
 * @since 5.1
 */
@Beta
public interface Issue {

  interface Flow {
    /**
     * @return Ordered list of locations for the execution flow
     */
    List<IssueLocation> locations();
  }

  /**
   * The {@link RuleKey} of this issue.
   */
  RuleKey ruleKey();

  /**
   * Effort to fix the issue. Used by technical debt model.
   */
  @CheckForNull
  Double effortToFix();

  /**
   * Overridden severity.
   */
  @CheckForNull
  Severity overriddenSeverity();

  /**
   * Primary locations for this issue.
   * @since 5.2
   */
  IssueLocation primaryLocation();

  /**
   * List of flows for this issue. Can be empty.
   * @since 5.2
   */
  List<Flow> flows();

  /**
   * Key/value pair of attributes that are attached to the issue.
   * @since 5.2
   */
  Map<String, String> attributes();

}
