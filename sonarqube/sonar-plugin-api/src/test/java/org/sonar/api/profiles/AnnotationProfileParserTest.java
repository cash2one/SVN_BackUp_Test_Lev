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
package org.sonar.api.profiles;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnnotationProfileParserTest {

  @Test
  public void shouldParseAnnotatedClasses() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock iom) throws Throwable {
        return Rule.create((String) iom.getArguments()[0], (String) iom.getArguments()[1], (String) iom.getArguments()[1]);
      }
    });

    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = new AnnotationProfileParser(ruleFinder).parse("squid", "Foo way", "java", Lists.<Class> newArrayList(FakeRule.class), messages);

    assertThat(profile.getName(), is("Foo way"));
    assertThat(profile.getLanguage(), is("java"));
    assertThat(profile.getActiveRule("squid", "fake").getSeverity(), is(RulePriority.BLOCKER));
    assertThat(messages.hasErrors(), is(false));
  }

  @Test
  public void shouldParseOnlyWantedProfile() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock iom) throws Throwable {
        return Rule.create((String) iom.getArguments()[0], (String) iom.getArguments()[1], (String) iom.getArguments()[1]);
      }
    });

    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = new AnnotationProfileParser(ruleFinder).parse("squid", "Foo way", "java", Lists.<Class> newArrayList(FakeRule.class, RuleOnOtherProfile.class), messages);

    assertNotNull(profile.getActiveRule("squid", "fake"));
    assertNull(profile.getActiveRule("squid", "other"));
  }
}

@BelongsToProfile(title = "Other profile", priority = Priority.BLOCKER)
@org.sonar.check.Rule(key = "other", priority = Priority.CRITICAL)
class RuleOnOtherProfile {
}

@BelongsToProfile(title = "Foo way", priority = Priority.BLOCKER)
@org.sonar.check.Rule(key = "fake", priority = Priority.CRITICAL)
class FakeRule {
}
