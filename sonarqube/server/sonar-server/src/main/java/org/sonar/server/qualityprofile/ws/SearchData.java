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

package org.sonar.server.qualityprofile.ws;

import java.util.List;
import java.util.Map;
import org.sonar.server.qualityprofile.QProfile;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableMap.copyOf;

public class SearchData {
  private List<QProfile> profiles;
  private Map<String, Long> activeRuleCountByProfileKey;
  private Map<String, Long> projectCountByProfileKey;

  public List<QProfile> getProfiles() {
    return profiles;
  }

  public SearchData setProfiles(List<QProfile> profiles) {
    this.profiles = copyOf(profiles);
    return this;
  }

  public SearchData setActiveRuleCountByProfileKey(Map<String, Long> activeRuleCountByProfileKey) {
    this.activeRuleCountByProfileKey = copyOf(activeRuleCountByProfileKey);
    return this;
  }

  public SearchData setProjectCountByProfileKey(Map<String, Long> projectCountByProfileKey) {
    this.projectCountByProfileKey = copyOf(projectCountByProfileKey);
    return this;
  }

  public long getActiveRuleCount(String profileKey) {
    return firstNonNull(activeRuleCountByProfileKey.get(profileKey), 0L);
  }

  public long getProjectCount(String profileKey) {
    return firstNonNull(projectCountByProfileKey.get(profileKey), 0L);
  }
}
