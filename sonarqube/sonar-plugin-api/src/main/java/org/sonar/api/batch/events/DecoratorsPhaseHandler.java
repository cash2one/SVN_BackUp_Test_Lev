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
package org.sonar.api.batch.events;

import java.util.List;
import org.sonar.api.batch.Decorator;

/**
 * @since 2.8
 * @deprecated since 5.2 no more decorator
 */
@Deprecated
public interface DecoratorsPhaseHandler extends EventHandler {

  /**
   * This interface is not intended to be implemented by clients.
   */
  interface DecoratorsPhaseEvent {

    /**
     * @return list of Decorators in the order of execution
     */
    List<Decorator> getDecorators();

    boolean isStart();

    boolean isEnd();

  }

  /**
   * Called before and after execution of all {@link Decorator}s.
   */
  void onDecoratorsPhase(DecoratorsPhaseEvent event);

}
