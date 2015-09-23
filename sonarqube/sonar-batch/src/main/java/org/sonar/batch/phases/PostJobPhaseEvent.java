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
package org.sonar.batch.phases;

import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.events.PostJobsPhaseHandler;

import java.util.List;

class PostJobPhaseEvent extends AbstractPhaseEvent<PostJobsPhaseHandler>
    implements org.sonar.api.batch.events.PostJobsPhaseHandler.PostJobsPhaseEvent {

  private final List<PostJob> postJobs;

  PostJobPhaseEvent(List<PostJob> postJobs, boolean start) {
    super(start);
    this.postJobs = postJobs;
  }

  @Override
  public List<PostJob> getPostJobs() {
    return postJobs;
  }

  @Override
  protected void dispatch(PostJobsPhaseHandler handler) {
    handler.onPostJobsPhase(this);
  }

  @Override
  protected Class getType() {
    return PostJobsPhaseHandler.class;
  }

}
