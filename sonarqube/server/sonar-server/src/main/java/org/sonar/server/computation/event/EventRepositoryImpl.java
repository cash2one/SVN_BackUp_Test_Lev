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
package org.sonar.server.computation.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.sonar.server.computation.component.Component;

import static java.util.Objects.requireNonNull;

public class EventRepositoryImpl implements EventRepository {
  private final Multimap<Integer, Event> events = HashMultimap.create();

  @Override
  public void add(Component component, Event event) {
    events.put(component.getReportAttributes().getRef(), requireNonNull(event));
  }

  @Override
  public Iterable<Event> getEvents(Component component) {
    return this.events.get(component.getReportAttributes().getRef());
  }
}
