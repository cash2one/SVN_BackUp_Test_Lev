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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DeleteActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient;

  private QualityProfileDao qualityProfileDao;

  private ComponentDao componentDao;

  private Language xoo1;
  private Language xoo2;

  private WsTester tester;

  private DbSession session;

  System2 system2 = mock(System2.class);

  @Before
  public void setUp() {
    dbTester.truncateTables();
    qualityProfileDao = new QualityProfileDao(dbTester.myBatis(), mock(System2.class));
    componentDao = new ComponentDao();

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), qualityProfileDao, new ActiveRuleDao(qualityProfileDao, new RuleDao(system2), system2));
    session = dbClient.openSession(false);

    xoo1 = LanguageTesting.newLanguage("xoo1");
    xoo2 = LanguageTesting.newLanguage("xoo2");

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new DeleteAction(new Languages(xoo1, xoo2), new QProfileFactory(dbClient), dbClient, userSessionRule)));
  }

  @After
  public void teadDown() {
    session.close();
  }

  @Test
  public void delete_nominal_with_key() throws Exception {
    String profileKey = "sonar-way-xoo1-12345";

    ComponentDto project = ComponentTesting.newProjectDto("polop");
    componentDao.insert(session, project);
    qualityProfileDao.insert(session, QualityProfileDto.createFor(profileKey).setLanguage(xoo1.getKey()).setName("Sonar way"));
    qualityProfileDao.insertProjectProfileAssociation(project.uuid(), profileKey, session);
    session.commit();

    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "delete").setParam("profileKey", "sonar-way-xoo1-12345").execute().assertNoContent();

    assertThat(qualityProfileDao.selectByKey(session, "sonar-way-xoo1-12345")).isNull();
    assertThat(qualityProfileDao.selectProjects("Sonar way", xoo1.getName())).isEmpty();
  }

  @Test
  public void delete_nominal_with_language_and_name() throws Exception {
    String profileKey = "sonar-way-xoo1-12345";

    ComponentDto project = ComponentTesting.newProjectDto("polop");
    componentDao.insert(session, project);
    qualityProfileDao.insert(session, QualityProfileDto.createFor(profileKey).setLanguage(xoo1.getKey()).setName("Sonar way"));
    qualityProfileDao.insertProjectProfileAssociation(project.uuid(), profileKey, session);
    session.commit();

    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "delete").setParam("profileName", "Sonar way").setParam("language", xoo1.getKey()).execute().assertNoContent();

    assertThat(qualityProfileDao.selectByKey(session, "sonar-way-xoo1-12345")).isNull();
    assertThat(qualityProfileDao.selectProjects("Sonar way", xoo1.getName())).isEmpty();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    userSessionRule.login("obiwan");
    tester.newPostRequest("api/qualityprofiles", "delete").execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_arguments() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    tester.newPostRequest("api/qualityprofiles", "delete").execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_language() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    tester.newPostRequest("api/qualityprofiles", "delete").setParam("profileName", "Polop").execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_name() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    tester.newPostRequest("api/qualityprofiles", "delete").setParam("language", xoo1.getKey()).execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_too_many_arguments() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    tester.newPostRequest("api/qualityprofiles", "delete").setParam("profileName", "Polop").setParam("language", xoo1.getKey()).setParam("profileKey", "polop").execute();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unexisting_profile() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    tester.newPostRequest("api/qualityprofiles", "delete").setParam("profileName", "Polop").setParam("language", xoo1.getKey()).execute();
  }
}
