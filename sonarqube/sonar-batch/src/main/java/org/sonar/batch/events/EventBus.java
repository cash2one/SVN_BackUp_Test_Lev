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
package org.sonar.batch.events;

import com.google.common.collect.Lists;
import org.sonar.api.batch.events.EventHandler;

import java.util.List;

/**
 * Dispatches {@link BatchEvent}s. Eases decoupling by allowing objects to interact without having direct dependencies upon one another, and
 * without requiring event sources to deal with maintaining handler lists.
 */
public class EventBus {

  private EventHandler[] registeredHandlers;

  public EventBus(EventHandler[] handlers) {
    this.registeredHandlers = handlers;
  }

  /**
   * Fires the given event.
   */
  public void fireEvent(BatchEvent event) {
    doFireEvent(event);
  }

  private void doFireEvent(BatchEvent event) {
    List<EventHandler> handlers = getDispatchList(event.getType());
    for (EventHandler handler : handlers) {
      event.dispatch(handler);
    }
  }

  private List<EventHandler> getDispatchList(Class<? extends EventHandler> handlerType) {
    List<EventHandler> result = Lists.newArrayList();
    for (EventHandler handler : registeredHandlers) {
      if (handlerType.isAssignableFrom(handler.getClass())) {
        result.add(handler);
      }
    }
    return result;
  }

}
