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
package org.sonar.db.issue;

import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;

public class IssueChangeDtoTest {

  @Test
  public void create_from_comment() {
    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");

    IssueChangeDto dto = IssueChangeDto.of(comment);

    assertThat(dto.getChangeData()).isEqualTo("the comment");
    assertThat(dto.getChangeType()).isEqualTo("comment");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
    assertThat(dto.getIssueChangeCreationDate()).isNotNull();
    assertThat(dto.getIssueKey()).isEqualTo("ABCDE");
    assertThat(dto.getUserLogin()).isEqualTo("emmerik");
  }

  @Test
  public void create_from_comment_with_created_at() {
    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    comment.setCreatedAt(parseDate("2015-01-13"));

    IssueChangeDto dto = IssueChangeDto.of(comment);

    assertThat(dto.getIssueChangeCreationDate()).isEqualTo(parseDate("2015-01-13").getTime());
  }

  @Test
  public void create_from_diff() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "INFO", "BLOCKER");
    diffs.setUserLogin("emmerik");

    IssueChangeDto dto = IssueChangeDto.of("ABCDE", diffs);

    assertThat(dto.getChangeData()).isEqualTo("severity=INFO|BLOCKER");
    assertThat(dto.getChangeType()).isEqualTo("diff");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
    assertThat(dto.getIssueChangeCreationDate()).isNull();
    assertThat(dto.getIssueKey()).isEqualTo("ABCDE");
    assertThat(dto.getUserLogin()).isEqualTo("emmerik");
  }

  @Test
  public void create_from_diff_with_created_at() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "INFO", "BLOCKER");
    diffs.setUserLogin("emmerik");
    diffs.setCreationDate(parseDate("2015-01-13"));

    IssueChangeDto dto = IssueChangeDto.of("ABCDE", diffs);

    assertThat(dto.getIssueChangeCreationDate()).isEqualTo(parseDate("2015-01-13").getTime());
  }

  @Test
  public void to_comment() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserLogin("emmerik")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setCreatedAt(System2.INSTANCE.now())
      .setUpdatedAt(System2.INSTANCE.now());

    DefaultIssueComment comment = changeDto.toComment();
    assertThat(comment.key()).isEqualTo("EFGH");
    assertThat(comment.markdownText()).isEqualTo("Some text");
    assertThat(comment.createdAt()).isNotNull();
    assertThat(comment.updatedAt()).isNotNull();
    assertThat(comment.userLogin()).isEqualTo("emmerik");
    assertThat(comment.issueKey()).isEqualTo("ABCDE");
  }

  @Test
  public void to_field_diffs_with_issue_creation_date() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserLogin("emmerik")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setIssueChangeCreationDate(System2.INSTANCE.now());

    FieldDiffs diffs = changeDto.toFieldDiffs();
    assertThat(diffs.userLogin()).isEqualTo("emmerik");
    assertThat(diffs.issueKey()).isEqualTo("ABCDE");
    assertThat(diffs.creationDate()).isNotNull();
  }

  @Test
  public void to_field_diffs_with_create_at() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserLogin("emmerik")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setCreatedAt(System2.INSTANCE.now());

    FieldDiffs diffs = changeDto.toFieldDiffs();
    assertThat(diffs.userLogin()).isEqualTo("emmerik");
    assertThat(diffs.issueKey()).isEqualTo("ABCDE");
    assertThat(diffs.creationDate()).isNotNull();
  }

  @Test
  public void to_string() {
    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    IssueChangeDto dto = IssueChangeDto.of(comment);
    assertThat(dto.toString()).contains("ABCDE");
  }
}
