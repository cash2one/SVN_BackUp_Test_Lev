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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.Response.Stream;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportAction implements QProfileWsAction {

  private static final String PARAM_PROFILE_NAME = "name";
  private static final String PARAM_LANGUAGE = "language";
  private static final String PARAM_FORMAT = "exporterKey";

  private final DbClient dbClient;

  private final QProfileFactory profileFactory;

  private final QProfileBackuper backuper;

  private final QProfileExporters exporters;

  private final Languages languages;

  public ExportAction(DbClient dbClient, QProfileFactory profileFactory, QProfileBackuper backuper, QProfileExporters exporters, Languages languages) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.backuper = backuper;
    this.exporters = exporters;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction create = controller.createAction("export")
      .setSince("5.2")
      .setDescription("Export a quality profile.")
      .setHandler(this);

    create.createParam(PARAM_PROFILE_NAME)
      .setDescription("The name of the quality profile to export. If left empty, will export the default profile for the language.")
      .setExampleValue("My Sonar way");

    create.createParam(PARAM_LANGUAGE)
      .setDescription("The language for the quality profile.")
      .setExampleValue(LanguageParamUtils.getExampleValue(languages))
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setRequired(true);

    List<String> exporterKeys = Lists.newArrayList();
    for (Language lang : languages.all()) {
      for (ProfileExporter exporter : exporters.exportersForLanguage(lang.getKey())) {
        exporterKeys.add(exporter.getKey());
      }
    }
    if (!exporterKeys.isEmpty()) {
      create.createParam(PARAM_FORMAT)
        .setDescription("Output format. If left empty, the same format as api/qualityprofiles/backup is used. " +
          "Possible values are described by api/qualityprofiles/exporters.")
        .setPossibleValues(exporterKeys);
    }
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String name = request.param(PARAM_PROFILE_NAME);
    String language = request.mandatoryParam(PARAM_LANGUAGE);
    String format = null;
    if (!exporters.exportersForLanguage(language).isEmpty()) {
      format = request.param(PARAM_FORMAT);
    }

    DbSession dbSession = dbClient.openSession(false);
    Stream stream = response.stream();
    OutputStream output = stream.output();
    Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);

    try {
      QualityProfileDto profile;
      if (name == null) {
        profile = profileFactory.getDefault(dbSession, language);
      } else {
        profile = profileFactory.getByNameAndLanguage(dbSession, name, language);
      }
      if (profile == null) {
        throw new NotFoundException(String.format("Could not find profile with name '%s' for language '%s'", name, language));
      }

      String profileKey = profile.getKey();
      if (format == null) {
        stream.setMediaType(MimeTypes.XML);
        backuper.backup(profileKey, writer);
      } else {
        stream.setMediaType(exporters.mimeType(format));
        exporters.export(profileKey, format, writer);
      }
    } finally {
      IOUtils.closeQuietly(writer);
      IOUtils.closeQuietly(output);
      dbSession.close();
    }
  }
}
