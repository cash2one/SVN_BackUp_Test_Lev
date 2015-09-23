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

package org.sonar.server.component;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.db.component.ComponentDtoFunctions.toKey;

@ServerSide
public class ComponentService {

  private final DbClient dbClient;

  private final I18n i18n;
  private final UserSession userSession;
  private final System2 system2;
  private final ComponentFinder componentFinder;

  public ComponentService(DbClient dbClient, I18n i18n, UserSession userSession, System2 system2, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.userSession = userSession;
    this.system2 = system2;
    this.componentFinder = componentFinder;
  }

  public ComponentDto getByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return getByKey(session, key);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public ComponentDto getNullableByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      Optional<ComponentDto> component = dbClient.componentDao().selectByKey(session, key);
      return component.isPresent() ? component.get() : null;
    } finally {
      session.close();
    }
  }

  public ComponentDto getNonNullByUuid(String uuid) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().selectOrFailByUuid(session, uuid);
    } finally {
      session.close();
    }
  }

  public Optional<ComponentDto> getByUuid(String uuid) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().selectByUuid(session, uuid);
    } finally {
      session.close();
    }
  }

  public void updateKey(String projectOrModuleKey, String newKey) {
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto projectOrModule = getByKey(session, projectOrModuleKey);
      userSession.checkProjectUuidPermission(UserRole.ADMIN, projectOrModule.projectUuid());
      dbClient.resourceKeyUpdaterDao().updateKey(projectOrModule.getId(), newKey);
      session.commit();

      session.commit();
    } finally {
      session.close();
    }
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(String projectKey, String stringToReplace, String replacementString) {
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto project = getByKey(projectKey);
      userSession.checkProjectUuidPermission(UserRole.ADMIN, project.projectUuid());
      return dbClient.resourceKeyUpdaterDao().checkModuleKeysBeforeRenaming(project.getId(), stringToReplace, replacementString);
    } finally {
      session.close();
    }
  }

  public void bulkUpdateKey(String projectKey, String stringToReplace, String replacementString) {
    // Open a batch session
    DbSession session = dbClient.openSession(true);
    try {
      ComponentDto project = getByKey(session, projectKey);
      userSession.checkProjectUuidPermission(UserRole.ADMIN, project.projectUuid());
      dbClient.resourceKeyUpdaterDao().bulkUpdateKey(session, project.getId(), stringToReplace, replacementString);
      session.commit();
    } finally {
      session.close();
    }
  }

  public ComponentDto create(NewComponent newComponent) {
    userSession.checkGlobalPermission(GlobalPermissions.PROVISIONING);

    DbSession session = dbClient.openSession(false);
    try {
      checkKeyFormat(newComponent.qualifier(), newComponent.key());
      checkBranchFormat(newComponent.qualifier(), newComponent.branch());
      String keyWithBranch = ComponentKeys.createKey(newComponent.key(), newComponent.branch());

      ComponentDto existingComponent = getNullableByKey(keyWithBranch);
      if (existingComponent != null) {
        throw new BadRequestException(formatMessage("Could not create %s, key already exists: %s", newComponent.qualifier(), keyWithBranch));
      }

      String uuid = Uuids.create();
      ComponentDto component = new ComponentDto()
        .setUuid(uuid)
        .setModuleUuid(null)
        .setModuleUuidPath(ComponentDto.MODULE_UUID_PATH_SEP + uuid + ComponentDto.MODULE_UUID_PATH_SEP)
        .setProjectUuid(uuid)
        .setKey(keyWithBranch)
        .setDeprecatedKey(keyWithBranch)
        .setName(newComponent.name())
        .setLongName(newComponent.name())
        .setScope(Scopes.PROJECT)
        .setQualifier(newComponent.qualifier())
        .setCreatedAt(new Date(system2.now()));
      dbClient.componentDao().insert(session, component);
      dbClient.componentIndexDao().indexResource(session, component.getId());
      session.commit();

      return component;
    } finally {
      dbClient.closeSession(session);
    }
  }

  public Collection<String> componentUuids(@Nullable Collection<String> componentKeys) {
    DbSession session = dbClient.openSession(false);
    try {
      return componentUuids(session, componentKeys, false);
    } finally {
      session.close();
    }
  }

  public Collection<String> componentUuids(DbSession session, @Nullable Collection<String> componentKeys, boolean ignoreMissingComponents) {
    Collection<String> componentUuids = newArrayList();
    if (componentKeys != null && !componentKeys.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByKeys(session, componentKeys);

      if (!ignoreMissingComponents && components.size() < componentKeys.size()) {
        Collection<String> foundKeys = Collections2.transform(components, toKey());
        Set<String> missingKeys = Sets.newHashSet(componentKeys);
        missingKeys.removeAll(foundKeys);
        throw new NotFoundException("The following component keys do not match any component:\n" +
          Joiner.on('\n').join(missingKeys));
      }

      for (ComponentDto component : components) {
        componentUuids.add(component.uuid());
      }
    }
    return componentUuids;
  }

  public Set<String> getDistinctQualifiers(DbSession session, @Nullable Collection<String> componentUuids) {
    Set<String> componentQualifiers = Sets.newHashSet();
    if (componentUuids != null && !componentUuids.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByUuids(session, componentUuids);

      for (ComponentDto component : components) {
        componentQualifiers.add(component.qualifier());
      }
    }
    return componentQualifiers;
  }

  public Collection<ComponentDto> getByUuids(DbSession session, Collection<String> componentUuids) {
    Set<ComponentDto> directoryPaths = Sets.newHashSet();
    if (componentUuids != null && !componentUuids.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByUuids(session, componentUuids);

      for (ComponentDto component : components) {
        directoryPaths.add(component);
      }
    }
    return directoryPaths;
  }

  private void checkKeyFormat(String qualifier, String kee) {
    if (!ComponentKeys.isValidModuleKey(kee)) {
      throw new BadRequestException(formatMessage("Malformed key for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.",
        qualifier, kee));
    }
  }

  private void checkBranchFormat(String qualifier, @Nullable String branch) {
    if (branch != null && !ComponentKeys.isValidBranch(branch)) {
      throw new BadRequestException(formatMessage("Malformed branch for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.",
        qualifier, branch));
    }
  }

  private String formatMessage(String message, String qualifier, String key) {
    return String.format(message, i18n.message(Locale.getDefault(), "qualifier." + qualifier, "Project"), key);
  }

  private ComponentDto getByKey(DbSession session, String key) {
    return componentFinder.getByKey(session, key);
  }
}
