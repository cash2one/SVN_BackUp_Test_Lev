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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class RestoreBuiltInActionTest {

  @Mock
  QProfileService profileService;

  @Mock
  I18n i18n;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new RestoreBuiltInAction(profileService)));
  }

  @Test
  public void return_empty_result_when_no_infos_or_warnings() throws Exception {
    WsTester.TestRequest request = tester.newPostRequest("api/qualityprofiles", "restore_built_in").setParam("language", "java");
    request.execute().assertNoContent();
  }
}
