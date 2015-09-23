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
package org.sonar.server.rule.db;

import com.google.common.base.Preconditions;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMapper;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.IndexDefinition;

import javax.annotation.CheckForNull;

import java.util.List;

public class RuleDao extends BaseDao<RuleMapper, RuleDto, RuleKey> {

  public RuleDao(System2 system) {
    super(IndexDefinition.RULE, RuleMapper.class, system);
  }

  @Override
  public RuleDto doGetNullableByKey(DbSession session, RuleKey key) {
    return mapper(session).selectByKey(key);
  }

  public RuleDto getByName(String name, DbSession session) {
    return mapper(session).selectByName(name);
  }

  @Override
  protected RuleDto doInsert(DbSession session, RuleDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected RuleDto doUpdate(DbSession session, RuleDto item) {
    mapper(session).update(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(DbSession session, RuleKey key) {
    throw new UnsupportedOperationException("Rules cannot be deleted");
  }

  /**
   * @deprecated use keys.
   */
  @CheckForNull
  @Deprecated
  public RuleDto selectById(DbSession session, int id) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public RuleDto selectTemplate(RuleDto rule, DbSession session) {
    Preconditions.checkNotNull(rule.getTemplateId(), "Rule has no persisted template!");
    return mapper(session).selectById(rule.getTemplateId());
  }

  /**
   * Finder methods for Rules
   */

  public List<RuleDto> selectByNonManual(DbSession session) {
    return mapper(session).selectNonManual();
  }

  public List<RuleDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  /**
   * Nested DTO RuleParams
   */

  public void insertRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    Preconditions.checkNotNull(rule.getId(), "Rule id must be set");
    param.setRuleId(rule.getId());
    mapper(session).insertParameter(param);
    this.enqueueInsert(param, rule.getKey(), session);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    Preconditions.checkNotNull(rule.getId(), "Rule id must be set");
    Preconditions.checkNotNull(param.getId(), "Param is not yet persisted must be set");
    param.setRuleId(rule.getId());
    mapper(session).updateParameter(param);
    this.enqueueUpdate(param, rule.getKey(), session);
    return param;
  }

  public void deleteRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    Preconditions.checkNotNull(param.getId(), "Param is not persisted");
    mapper(session).deleteParameter(param.getId());
    this.enqueueDelete(param, rule.getKey(), session);
  }

  /**
   * Finder methods for RuleParams
   */

  public List<RuleParamDto> selectAllRuleParams(DbSession session) {
    return mapper(session).selectAllParams();
  }

  public List<RuleParamDto> selectRuleParamsByRuleKey(DbSession session, RuleKey key) {
    return mapper(session).selectParamsByRuleKey(key);
  }

  public List<RuleDto> selectRulesByDebtSubCharacteristicId(DbSession session, int id) {
    return mapper(session).selectBySubCharacteristicId(id);
  }

  public List<RuleDto> selectEnabledAndNonManual(DbSession session) {
    return mapper(session).selectEnabledAndNonManual();
  }
}
