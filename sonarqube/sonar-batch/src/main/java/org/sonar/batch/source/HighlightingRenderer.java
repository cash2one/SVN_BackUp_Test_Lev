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

import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;
import org.sonar.colorizer.HtmlCodeBuilder;
import org.sonar.colorizer.TokenizerDispatcher;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class HighlightingRenderer {

  public void render(Reader code, List<? extends Channel<HtmlCodeBuilder>> tokenizers, NewHighlighting highlighting) {
    List<Channel<HtmlCodeBuilder>> allTokenizers = new ArrayList<>();
    HighlightingCodeBuilder codeBuilder = new HighlightingCodeBuilder(highlighting);

    allTokenizers.addAll(tokenizers);

    new TokenizerDispatcher(allTokenizers).colorize(new CodeReader(code), codeBuilder);
    highlighting.save();
  }
}
