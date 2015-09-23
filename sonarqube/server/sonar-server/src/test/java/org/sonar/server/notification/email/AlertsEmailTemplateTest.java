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
package org.sonar.server.notification.email;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlertsEmailTemplateTest {

  private AlertsEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings configuration = mock(EmailSettings.class);
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new AlertsEmailTemplate(configuration);
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message, nullValue());
  }

  @Test
  public void shouldFormatAlertWithSeveralMessages() {
    Notification notification = createNotification("Orange (was Red)", "violations > 4, coverage < 75%", "WARN", "false");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("Quality gate status changed on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Quality gate status: Orange (was Red)\n" +
      "\n" +
      "Quality gate thresholds:\n" +
      "  - violations > 4\n" +
      "  - coverage < 75%\n" +
      "\n" +
      "See it in SonarQube: http://nemo.sonarsource.org/dashboard/index/org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatNewAlertWithSeveralMessages() {
    Notification notification = createNotification("Orange (was Red)", "violations > 4, coverage < 75%", "WARN", "true");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("New quality gate threshold reached on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Quality gate status: Orange (was Red)\n" +
      "\n" +
      "New quality gate thresholds:\n" +
      "  - violations > 4\n" +
      "  - coverage < 75%\n" +
      "\n" +
      "See it in SonarQube: http://nemo.sonarsource.org/dashboard/index/org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatNewAlertWithOneMessage() {
    Notification notification = createNotification("Orange (was Red)", "violations > 4", "WARN", "true");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("New quality gate threshold reached on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Quality gate status: Orange (was Red)\n" +
      "\n" +
      "New quality gate threshold: violations > 4\n" +
      "\n" +
      "See it in SonarQube: http://nemo.sonarsource.org/dashboard/index/org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatBackToGreenMessage() {
    Notification notification = createNotification("Green (was Red)", "", "OK", "false");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("\"Foo\" is back to green"));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Quality gate status: Green (was Red)\n" +
      "\n" +
      "\n" +
      "See it in SonarQube: http://nemo.sonarsource.org/dashboard/index/org.sonar.foo:foo"));
  }

  private Notification createNotification(String alertName, String alertText, String alertLevel, String isNewAlert) {
    Notification notification = new Notification("alerts")
        .setFieldValue("projectName", "Foo")
        .setFieldValue("projectKey", "org.sonar.foo:foo")
        .setFieldValue("projectId", "45")
        .setFieldValue("alertName", alertName)
        .setFieldValue("alertText", alertText)
        .setFieldValue("alertLevel", alertLevel)
        .setFieldValue("isNewAlert", isNewAlert);
    return notification;
  }

}
