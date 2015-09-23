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

package org.sonar.db.component;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class ComponentDao implements Dao {

  public ComponentDto selectOrFailById(DbSession session, long id) {
    Optional<ComponentDto> componentDto = selectById(session, id);
    if (!componentDto.isPresent()) {
      throw new RowNotFoundException(String.format("Component id does not exist: %d", id));
    }
    return componentDto.get();
  }

  public Optional<ComponentDto> selectById(DbSession session, long id) {
    return Optional.fromNullable(mapper(session).selectById(id));
  }

  public Optional<ComponentDto> selectByUuid(DbSession session, String uuid) {
    return Optional.fromNullable(mapper(session).selectByUuid(uuid));
  }

  public ComponentDto selectOrFailByUuid(DbSession session, String uuid) {
    Optional<ComponentDto> componentDto = selectByUuid(session, uuid);
    if (!componentDto.isPresent()) {
      throw new RowNotFoundException(String.format("Component with uuid '%s' not found", uuid));
    }
    return componentDto.get();
  }

  public boolean existsById(Long id, DbSession session) {
    return mapper(session).countById(id) > 0;
  }

  public List<ComponentDto> selectSubProjectsByComponentUuids(DbSession session, Collection<String> keys) {
    if (keys.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper(session).selectSubProjectsByComponentUuids(keys);
  }

  public List<ComponentDto> selectDescendantModules(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantModules(rootComponentUuid, Scopes.PROJECT, false);
  }

  public List<ComponentDto> selectEnabledDescendantModules(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantModules(rootComponentUuid, Scopes.PROJECT, true);
  }

  public List<FilePathWithHashDto> selectEnabledDescendantFiles(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantFiles(rootComponentUuid, Scopes.FILE, true);
  }

  public List<FilePathWithHashDto> selectEnabledFilesFromProject(DbSession session, String rootComponentUuid) {
    return mapper(session).selectEnabledFilesFromProject(rootComponentUuid);
  }

  public List<ComponentDto> selectByIds(final DbSession session, Collection<Long> ids) {
    return DatabaseUtils.executeLargeInputs(ids, new Function<List<Long>, List<ComponentDto>>() {
      @Override
      public List<ComponentDto> apply(@Nonnull List<Long> partition) {
        return mapper(session).selectByIds(partition);
      }
    });
  }

  public List<ComponentDto> selectByUuids(final DbSession session, Collection<String> uuids) {
    return DatabaseUtils.executeLargeInputs(uuids, new Function<List<String>, List<ComponentDto>>() {
      @Override
      public List<ComponentDto> apply(@Nonnull List<String> partition) {
        return mapper(session).selectByUuids(partition);
      }
    });
  }

  public List<String> selectExistingUuids(final DbSession session, Collection<String> uuids) {
    return DatabaseUtils.executeLargeInputs(uuids, new Function<List<String>, List<String>>() {
      @Override
      public List<String> apply(@Nonnull List<String> partition) {
        return mapper(session).selectExistingUuids(partition);
      }
    });
  }

  /**
   * Return all components of a project (including disable ones)
   */
  public List<ComponentDto> selectAllComponentsFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectComponentsFromProjectKeyAndScope(projectKey, null, false);
  }

  public List<ComponentDto> selectEnabledModulesFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectComponentsFromProjectKeyAndScope(projectKey, Scopes.PROJECT, true);
  }

  public List<ComponentDto> selectByKeys(DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, new KeyToDto(mapper(session)));
  }

  private static class KeyToDto implements Function<List<String>, List<ComponentDto>> {
    private final ComponentMapper mapper;

    private KeyToDto(ComponentMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<ComponentDto> apply(@Nonnull List<String> partitionOfKeys) {
      return mapper.selectByKeys(partitionOfKeys);
    }
  }

  public ComponentDto selectOrFailByKey(DbSession session, String key) {
    Optional<ComponentDto> component = selectByKey(session, key);
    if (!component.isPresent()) {
      throw new RowNotFoundException(String.format("Component key '%s' not found", key));
    }
    return component.get();
  }

  public Optional<ComponentDto> selectByKey(DbSession session, String key) {
    return Optional.fromNullable(mapper(session).selectByKey(key));
  }

  public List<UuidWithProjectUuidDto> selectAllViewsAndSubViews(DbSession session) {
    return mapper(session).selectUuidsForQualifiers(Qualifiers.VIEW, Qualifiers.SUBVIEW);
  }

  public List<String> selectProjectsFromView(DbSession session, String viewUuid, String projectViewUuid) {
    return mapper(session).selectProjectsFromView("%." + viewUuid + ".%", projectViewUuid);
  }

  /**
   * Returns all projects (Scope {@link org.sonar.api.resources.Scopes#PROJECT} and qualifier
   * {@link org.sonar.api.resources.Qualifiers#PROJECT}) which are enabled.
   *
   * Used by Views.
   */
  public List<ComponentDto> selectProjects(DbSession session) {
    return mapper(session).selectProjects();
  }

  /**
   * Does not return component copies
   */
  public List<ComponentDto> selectComponents(DbSession session, Collection<String> qualifiers, int offset, int limit, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);
    addQualifiers(parameters, qualifiers);

    return mapper(session).selectComponents(parameters, new RowBounds(offset, limit));
  }

  public int countRootComponents(DbSession session, Collection<String> qualifiers, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addPartialQueryParameterIfNotNull(parameters, query);
    addQualifiers(parameters, qualifiers);

    return mapper(session).countRootComponents(parameters);
  }

  public List<ComponentDto> selectProvisionedProjects(DbSession session, int offset, int limit, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).selectProvisionedProjects(parameters, new RowBounds(offset, limit));
  }

  public int countProvisionedProjects(DbSession session, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).countProvisionedProjects(parameters);
  }

  public List<ComponentDto> selectGhostProjects(DbSession session, int offset, int limit, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).selectGhostProjects(parameters, new RowBounds(offset, limit));
  }

  public long countGhostProjects(DbSession session, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).countGhostProjects(parameters);
  }

  /**
   * Retrieves all components with a specific root project Uuid, no other filtering is done by this method.
   *
   * Used by Views plugin
   */
  public List<ComponentDto> selectByProjectUuid(String projectUuid, DbSession dbSession) {
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }

  private static void addPartialQueryParameterIfNotNull(Map<String, Object> parameters, @Nullable String keyOrNameFilter) {
    if (keyOrNameFilter != null) {
      parameters.put("query", "%" + keyOrNameFilter.toUpperCase() + "%");
    }
  }

  private static void addProjectQualifier(Map<String, Object> parameters) {
    parameters.put("qualifier", Qualifiers.PROJECT);
  }

  private static void addQualifiers(Map<String, Object> parameters, Collection<String> qualifiers) {
    parameters.put("qualifiers", qualifiers);
  }

  public void insert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
  }

  public void insert(DbSession session, Collection<ComponentDto> items) {
    for (ComponentDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, ComponentDto item, ComponentDto... others) {
    insert(session, Lists.asList(item, others));
  }

  public void update(DbSession session, ComponentDto item) {
    mapper(session).update(item);
  }

  private ComponentMapper mapper(DbSession session) {
    return session.getMapper(ComponentMapper.class);
  }

}
