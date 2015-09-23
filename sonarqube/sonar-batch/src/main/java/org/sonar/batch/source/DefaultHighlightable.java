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
package org.sonar.batch.source;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.source.Highlightable;

/**
 * @since 3.6
 */
public class DefaultHighlightable implements Highlightable {

  private static final HighlightingBuilder NO_OP_BUILDER = new NoOpHighlightingBuilder();
  private final DefaultInputFile inputFile;
  private final SensorStorage sensorStorage;
  private final AnalysisMode analysisMode;

  public DefaultHighlightable(DefaultInputFile inputFile, SensorStorage sensorStorage, AnalysisMode analysisMode) {
    this.inputFile = inputFile;
    this.sensorStorage = sensorStorage;
    this.analysisMode = analysisMode;
  }

  @Override
  public HighlightingBuilder newHighlighting() {
    if (analysisMode.isIssues()) {
      return NO_OP_BUILDER;
    }
    DefaultHighlighting defaultHighlighting = new DefaultHighlighting(sensorStorage);
    defaultHighlighting.onFile(inputFile);
    return new DefaultHighlightingBuilder(defaultHighlighting);
  }

  private static final class NoOpHighlightingBuilder implements HighlightingBuilder {
    @Override
    public HighlightingBuilder highlight(int startOffset, int endOffset, String typeOfText) {
      // Do nothing
      return this;
    }

    @Override
    public void done() {
      // Do nothing
    }
  }

  private static class DefaultHighlightingBuilder implements HighlightingBuilder {

    private final DefaultHighlighting defaultHighlighting;

    public DefaultHighlightingBuilder(DefaultHighlighting defaultHighlighting) {
      this.defaultHighlighting = defaultHighlighting;
    }

    @Override
    public HighlightingBuilder highlight(int startOffset, int endOffset, String typeOfText) {
      TypeOfText type = org.sonar.api.batch.sensor.highlighting.TypeOfText.forCssClass(typeOfText);
      defaultHighlighting.highlight(startOffset, endOffset, type);
      return this;
    }

    @Override
    public void done() {
      defaultHighlighting.save();
    }
  }
}
