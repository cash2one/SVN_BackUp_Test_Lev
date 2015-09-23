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
package org.sonar.core.issue.workflow;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StateMachineTest {
  @Test
  public void keep_order_of_state_keys() {
    StateMachine machine = StateMachine.builder().states("OPEN", "RESOLVED", "CLOSED").build();

    assertThat(machine.stateKeys()).containsSequence("OPEN", "RESOLVED", "CLOSED");
  }

  @Test
  public void stateKey() {
    StateMachine machine = StateMachine.builder()
      .states("OPEN", "RESOLVED", "CLOSED")
      .transition(Transition.builder("resolve").from("OPEN").to("RESOLVED").build())
      .build();

    assertThat(machine.state("OPEN")).isNotNull();
    assertThat(machine.state("OPEN").transition("resolve")).isNotNull();
  }
}
