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
package org.sonar.server.issue;

import com.google.common.collect.Maps;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.db.rule.RuleTesting;

public class IssueTesting {

  /**
   * Full IssueDto used to feed database with fake data. Tests must not rely on the
   * field contents declared here. They should override the fields they need to test,
   * for example:
   * <pre>
   *   issueDao.insert(dbSession, IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE));
   * </pre>
   */
  public static IssueDto newDto(RuleDto rule, ComponentDto file, ComponentDto project) {
    return new IssueDto()
      .setKee(Uuids.create())
      .setRule(rule)
      .setComponent(file)
      .setProject(project)
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setSeverity(Severity.MAJOR)
      .setDebt(10L)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setCreatedAt(1400000000000L)
      .setUpdatedAt(1400000000000L);
  }

  public static IssueDoc newDoc() {
    IssueDoc doc = new IssueDoc(Maps.<String, Object>newHashMap());
    doc.setKey("ABC");
    doc.setRuleKey(RuleTesting.XOO_X1.toString());
    doc.setActionPlanKey(null);
    doc.setReporter(null);
    doc.setAssignee("steve");
    doc.setAuthorLogin("roger");
    doc.setLanguage("xoo");
    doc.setComponentUuid("FILE_1");
    doc.setEffortToFix(3.14);
    doc.setFilePath("src/Foo.xoo");
    doc.setDirectoryPath("/src");
    doc.setMessage("the message");
    doc.setModuleUuid("MODULE_1");
    doc.setModuleUuidPath("MODULE_1");
    doc.setProjectUuid("PROJECT_1");
    doc.setLine(42);
    doc.setAttributes(null);
    doc.setStatus(Issue.STATUS_OPEN);
    doc.setResolution(null);
    doc.setSeverity(Severity.MAJOR);
    doc.setManualSeverity(true);
    doc.setDebt(10L);
    doc.setChecksum("12345");
    doc.setFuncCreationDate(DateUtils.parseDate("2014-09-04"));
    doc.setFuncUpdateDate(DateUtils.parseDate("2014-12-04"));
    doc.setFuncCloseDate(null);
    doc.setTechnicalUpdateDate(DateUtils.parseDate("2014-12-04"));
    return doc;
  }

  public static IssueDoc newDoc(String key, ComponentDto componentDto) {
    return newDoc()
      .setKey(key)
      .setComponentUuid(componentDto.uuid())
      .setModuleUuid(!componentDto.scope().equals(Scopes.PROJECT) ? componentDto.moduleUuid() : componentDto.uuid())
      .setModuleUuidPath(componentDto.moduleUuidPath())
      .setProjectUuid(componentDto.projectUuid())
      // File path make no sens on modules and projects
      .setFilePath(!componentDto.scope().equals(Scopes.PROJECT) ? componentDto.path() : null);
  }
}
