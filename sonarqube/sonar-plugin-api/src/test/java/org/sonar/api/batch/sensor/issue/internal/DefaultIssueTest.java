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
package org.sonar.api.batch.sensor.issue.internal;

import java.io.StringReader;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultIssueTest {

  private DefaultInputFile inputFile = new DefaultInputFile("foo", "src/Foo.php").initMetadata(new FileMetadata().readMetadata(new StringReader("Foo\nBar\n")));

  @Test
  public void build_file_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .effortToFix(10.0);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(1);
    assertThat(issue.effortToFix()).isEqualTo(10.0);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_directory_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(storage)
      .at(new DefaultIssueLocation()
        .on(new DefaultInputDir("foo", "src"))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .overrideSeverity(Severity.BLOCKER);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(new DefaultInputDir("foo", "src"));
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");
    assertThat(issue.overriddenSeverity()).isEqualTo(Severity.BLOCKER);

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_project_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(storage)
      .at(new DefaultIssueLocation()
        .on(new DefaultInputModule("foo"))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .effortToFix(10.0);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(new DefaultInputModule("foo"));
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.effortToFix()).isEqualTo(10.0);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

}
