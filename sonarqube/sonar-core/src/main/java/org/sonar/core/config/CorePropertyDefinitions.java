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
package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class CorePropertyDefinitions {

  private CorePropertyDefinitions() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    List<PropertyDefinition> defs = Lists.newArrayList();
    defs.addAll(IssueExclusionProperties.all());
    defs.addAll(ExclusionProperties.all());
    defs.addAll(SecurityProperties.all());
    defs.addAll(DebtProperties.all());
    defs.addAll(PurgeProperties.all());

    defs.addAll(ImmutableList.of(
      PropertyDefinition.builder(CoreProperties.SERVER_BASE_URL)
        .name("Server base URL")
        .description("HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE)
        .build(),

      PropertyDefinition.builder(CoreProperties.LINKS_HOME_PAGE)
        .name("Project Home Page")
        .description("HTTP URL of the home page of the project.")
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_CI)
        .name("CI server")
        .description("HTTP URL of the continuous integration server.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_ISSUE_TRACKER)
        .name("Issue Tracker")
        .description("HTTP URL of the issue tracker.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_SOURCES)
        .name("SCM server")
        .description("HTTP URL of the server which hosts the sources of the project.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_SOURCES_DEV)
        .name("SCM connection for developers")
        .description("HTTP URL used by developers to connect to the SCM server for the project.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.ANALYSIS_MODE)
        .name("Analysis mode")
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Arrays.asList(CoreProperties.ANALYSIS_MODE_ANALYSIS, CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_INCREMENTAL))
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.ANALYSIS_MODE_ANALYSIS)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.PREVIEW_INCLUDE_PLUGINS)
        .name("Plugins accepted for Preview mode")
        .description("Comma-separated list of plugin keys. Those plugins will be used during preview analyses.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE)
        .build(),
      PropertyDefinition.builder(CoreProperties.PREVIEW_EXCLUDE_PLUGINS)
        .name("Plugins excluded for Preview mode")
        .description("Comma-separated list of plugin keys. Those plugins will not be used during preview analyses.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE)
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_REALM)
        .name("Security Realm")
        .hidden()
        .build(),
      PropertyDefinition.builder("sonar.security.savePassword")
        .name("Save external password")
        .hidden()
        .build(),
      PropertyDefinition.builder("sonar.authenticator.downcase")
        .name("Downcase login")
        .description("Downcase login during user authentication, typically for Active Directory")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS)
        .name("Create user accounts")
        .description("Create accounts when authenticating users via an external system")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_UPDATE_USER_ATTRIBUTES)
        .name("Update user attributes")
        .description("When using the LDAP or OpenID plugin, at each login, the user attributes (name, email, ...) are re-synchronized")
        .hidden()
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE)
        .name("Ignore failures during authenticator startup")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),
      PropertyDefinition.builder("sonar.enableFileVariation")
        .name("Enable file variation")
        .hidden()
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_LOCAL_USERS)
        .name("Local/technical users")
        .description("Comma separated list of user logins that will always be authenticated using SonarQube database. "
          + "When using the LDAP plugin, for these accounts, the user attributes (name, email, ...) are not re-synchronized")
        .type(PropertyType.STRING)
        .multiValues(true)
        .defaultValue("admin")
        .build(),
      PropertyDefinition.builder(CoreProperties.SCM_DISABLED_KEY)
        .name("Disable the SCM Sensor")
        .description("Disable the retrieval of blame information from Source Control Manager")
        .category(CoreProperties.CATEGORY_SCM)
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(String.valueOf(false))
        .build(),
      PropertyDefinition.builder(CoreProperties.SCM_PROVIDER_KEY)
        .name("Key of the SCM provider for this project")
        .description("Force the provider to be used to get SCM information for this project. By default auto-detection is done. Example: svn, git.")
        .category(CoreProperties.CATEGORY_SCM)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),

      // WEB LOOK&FEEL
      PropertyDefinition.builder("sonar.lf.logoUrl")
        .deprecatedKey("sonar.branding.image")
        .name("Logo URL")
        .description("URL to logo image. Any standard format is accepted.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.logoWidthPx")
        .deprecatedKey("sonar.branding.image.width")
        .name("Width of image in pixels")
        .description("Width in pixels, given that the height of the the image is constrained to 30px")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.enableGravatar")
        .name("Enable support of gravatars")
        .description("Gravatars are profile pictures of users based on their email.")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.gravatarServerUrl")
        .name("Gravatar URL")
        .description("Optional URL of custom Gravatar service. Accepted variables are {EMAIL_MD5} for MD5 hash of email and {SIZE} for the picture size in pixels.")
        .defaultValue("https://secure.gravatar.com/avatar/{EMAIL_MD5}.jpg?s={SIZE}&d=identicon")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),

      // ISSUES
      PropertyDefinition.builder(CoreProperties.DEFAULT_ISSUE_ASSIGNEE)
        .name("Default Assignee")
        .description("New issues will be assigned to this user each time it is not possible to determine the user who is the author of the issue.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.USER_LOGIN)
        .build(),

      // BATCH

      PropertyDefinition.builder(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY)
        .defaultValue("en")
        .name("Locale used for issue messages")
        .description("Deprecated property. Keep default value for backward compatibility.")
        .hidden()
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 1)
        .name("Period 1")
        .description("Period used to compare measures and track new issues. Values are : <ul class='bullet'><li>Number of days before " +
          "analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, for example 2010-12-25</li><li>'previous_analysis' to " +
          "compare to previous analysis</li><li>'previous_version' to compare to the previous version in the project history</li>" +
          "<li>A version, for example '1.2' or 'BASELINE'</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is " +
          " the first one available inside the corresponding time range. </p>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_1)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 2)
        .name("Period 2")
        .description("See the property 'Period 1'")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_2)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 3)
        .name("Period 3")
        .description("See the property 'Period 1'")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_3)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 4)
        .name("Period 4")
        .description("Period used to compare measures and track new issues. This property is specific to the project. Values are : " +
          "<ul class='bullet'><li>Number of days before analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, " +
          "for example 2010-12-25</li><li>'previous_analysis' to compare to previous analysis</li>" +
          "<li>'previous_version' to compare to the previous version in the project history</li><li>A version, for example '1.2' or 'BASELINE'</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is the first one available inside the corresponding time range. </p>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_4)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 5)
        .name("Period 5")
        .description("See the property 'Period 4'")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_5)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      // CPD
      PropertyDefinition.builder(CoreProperties.CPD_CROSS_PROJECT)
        .defaultValue(Boolean.toString(CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE))
        .name("Cross project duplication detection")
        .description("By default, SonarQube detects duplications at sub-project level. This means that a block "
          + "duplicated on two sub-projects of the same project won't be reported. Setting this parameter to \"true\" "
          + "allows to detect duplicates across sub-projects and more generally across projects. Note that activating "
          + "this property will slightly increase each SonarQube analysis time.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_SKIP_PROPERTY)
        .defaultValue(String.valueOf(false))
        .name("Skip")
        .description("Disable detection of duplications")
        .hidden()
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_EXCLUSIONS)
        .defaultValue("")
        .name("Duplication Exclusions")
        .description("Patterns used to exclude some source files from the duplication detection mechanism. " +
          "See below to know how to use wildcards to specify this property.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS_EXCLUSIONS)
        .multiValues(true)
        .build()));
    return defs;
  }
}
