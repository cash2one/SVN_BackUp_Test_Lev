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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.sonar.api.server.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryContext;

import javax.annotation.CheckForNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sonar.server.user.UserSession;

@ServerSide
public class QProfileLoader {

  private final DbClient dbClient;
  private final IndexClient index;
  private final UserSession userSession;

  public QProfileLoader(DbClient dbClient, IndexClient index, UserSession userSession) {
    this.dbClient = dbClient;
    this.index = index;
    this.userSession = userSession;
  }

  /**
   * Returns all Quality profiles as DTOs. This is a temporary solution as long as
   * profiles are not indexed and declared as a business object
   */
  public List<QualityProfileDto> findAll() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.qualityProfileDao().selectAll(dbSession);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByKey(String key) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.qualityProfileDao().selectByKey(dbSession, key);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByLangAndName(String lang, String name) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.qualityProfileDao().selectByNameAndLanguage(name, lang, dbSession);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public ActiveRule getActiveRule(ActiveRuleKey key) {
    return index.get(ActiveRuleIndex.class).getNullableByKey(key);
  }

  public List<ActiveRule> findActiveRulesByRule(RuleKey key) {
    return index.get(ActiveRuleIndex.class).findByRule(key);
  }

  public Iterator<ActiveRule> findActiveRulesByProfile(String key) {
    return index.get(ActiveRuleIndex.class).findByProfile(key);
  }

  public long countActiveRulesByProfile(String key) {
    return index.get(ActiveRuleIndex.class).countByQualityProfileKey(key);
  }

  public Map<String, Long> countAllActiveRules() {
    Map<String, Long> counts = new HashMap<>();
    for (Map.Entry<String, Long> entry : index.get(ActiveRuleIndex.class).countAllByQualityProfileKey().entrySet()) {
      counts.put(entry.getKey(), entry.getValue());
    }
    return counts;
  }

  public Multimap<String, FacetValue> getStatsByProfile(String key) {
    return index.get(ActiveRuleIndex.class).getStatsByProfileKey(key);
  }

  public Map<String, Multimap<String, FacetValue>> getAllProfileStats() {
    List<String> keys = Lists.newArrayList();
    for (QualityProfileDto profile : this.findAll()) {
      keys.add(profile.getKey());
    }
    return index.get(ActiveRuleIndex.class).getStatsByProfileKeys(keys);
  }

  public long countDeprecatedActiveRulesByProfile(String key) {
    return index.get(RuleIndex.class).search(
      new RuleQuery()
        .setQProfileKey(key)
        .setActivation(true)
        .setStatuses(Lists.newArrayList(RuleStatus.DEPRECATED)),
      new QueryContext(userSession).setLimit(0)).getTotal();
  }

}
