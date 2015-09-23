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
package org.sonar.batch.postjob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.resources.Project;

public class PostJobWrapper implements org.sonar.api.batch.PostJob, CheckProject {

  private static final Logger LOG = LoggerFactory.getLogger(PostJobWrapper.class);

  private PostJob wrappedPostJob;
  private PostJobContext adaptor;
  private DefaultPostJobDescriptor descriptor;
  private PostJobOptimizer optimizer;

  public PostJobWrapper(PostJob newPostJob, PostJobContext adaptor, PostJobOptimizer optimizer) {
    this.wrappedPostJob = newPostJob;
    this.optimizer = optimizer;
    this.descriptor = new DefaultPostJobDescriptor();
    newPostJob.describe(descriptor);
    this.adaptor = adaptor;
  }

  public PostJob wrappedPostJob() {
    return wrappedPostJob;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return optimizer.shouldExecute(descriptor);
  }

  @Override
  public void executeOn(Project project, org.sonar.api.batch.SensorContext context) {
    wrappedPostJob.execute(adaptor);
  }

  @Override
  public String toString() {
    return descriptor.name() + (LOG.isDebugEnabled() ? " (wrapped)" : "");
  }
}
