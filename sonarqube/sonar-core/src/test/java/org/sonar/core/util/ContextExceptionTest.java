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
package org.sonar.core.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextExceptionTest {

  public static final String LABEL = "something wrong";

  @Test
  public void only_label() throws Exception {
    ContextException e = ContextException.of(LABEL);
    assertThat(e.getMessage()).isEqualTo(LABEL);
    assertThat(e.getRawMessage()).isEqualTo(LABEL);
    assertThat(e.getCause()).isNull();
  }

  @Test
  public void only_cause() throws Exception {
    Exception cause = new Exception("cause");
    ContextException e = ContextException.of(cause);
    assertThat(e.getMessage()).isEqualTo("java.lang.Exception: cause");
    assertThat(e.getRawMessage()).isEqualTo("java.lang.Exception: cause");
    assertThat(e.getCause()).isSameAs(cause);
  }

  @Test
  public void cause_and_message() throws Exception {
    Exception cause = new Exception("cause");
    ContextException e = ContextException.of(LABEL, cause);
    assertThat(e.getMessage()).isEqualTo(LABEL);
    assertThat(e.getRawMessage()).isEqualTo(LABEL);
    assertThat(e.getCause()).isSameAs(cause);
  }

  @Test
  public void addContext() throws Exception {
    ContextException e = ContextException.of(LABEL)
      .addContext("K1", "V1")
      .addContext("K2", "V2");
    assertThat(e).hasMessage(LABEL + " | K1=V1 | K2=V2");
  }

  @Test
  public void setContext() throws Exception {
    ContextException e = ContextException.of(LABEL)
      .addContext("K1", "V1")
      .setContext("K1", "V2");
    assertThat(e).hasMessage(LABEL + " | K1=V2");
  }

  @Test
  public void multiple_context_values() throws Exception {
    ContextException e = ContextException.of(LABEL)
      .addContext("K1", "V1")
      .addContext("K1", "V2");
    assertThat(e).hasMessage(LABEL + " | K1=[V1,V2]");
  }

  @Test
  public void merge_ContextException() throws Exception {
    ContextException cause = ContextException.of("cause").addContext("K1", "V1");
    ContextException e = ContextException.of(cause)
      .addContext("K1", "V11")
      .addContext("K2", "V2");
    assertThat(e.getContext("K1")).containsExactly("V1", "V11");
    assertThat(e.getContext("K2")).containsOnly("V2");
    assertThat(e).hasMessage("K1=[V1,V11] | K2=V2");
  }

  @Test
  public void merge_ContextException_with_new_message() throws Exception {
    ContextException cause = ContextException.of("cause").addContext("K1", "V1");
    ContextException e = ContextException.of(LABEL, cause).addContext("K2", "V2");
    assertThat(e.getContext("K1")).containsOnly("V1");
    assertThat(e.getContext("K2")).containsOnly("V2");
    assertThat(e).hasMessage(LABEL + " | K1=V1 | K2=V2");
  }
}
