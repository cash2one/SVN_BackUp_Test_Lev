/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion("DEV")
    .addPlugin(ItUtils.xooPlugin())
    .setContext("/")

    .addPlugin(ItUtils.pluginArtifact("batch-plugin"))
    // Java is only used in convert_library_into_module test
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE").addPlugin("java")

    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/batch/BatchTest/one-issue-per-line.xml"));
  }

  /** 
   * SONAR-3718
   */
  @Test
  public void should_scan_branch_with_forward_slash() {
    scan("shared/xoo-multi-modules-sample");
    scan("shared/xoo-multi-modules-sample", "sonar.branch", "branch/0.x");

    Sonar sonar = orchestrator.getServer().getWsClient();
    assertThat(sonar.findAll(new ResourceQuery().setQualifiers("TRK"))).hasSize(2);

    Resource master = sonar.find(new ResourceQuery("com.sonarsource.it.samples:multi-modules-sample"));
    assertThat(master.getName()).isEqualTo("Sonar :: Integration Tests :: Multi-modules Sample");

    Resource branch = sonar.find(new ResourceQuery("com.sonarsource.it.samples:multi-modules-sample:branch/0.x"));
    assertThat(branch.getName()).isEqualTo("Sonar :: Integration Tests :: Multi-modules Sample branch/0.x");
  }

  /**
   * SONAR-2907
   */
  @Test
  public void branch_should_load_own_settings_from_database() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");
    scan("shared/xoo-multi-modules-sample");
    assertThat(getResource("com.sonarsource.it.samples:multi-modules-sample:module_b")).isNotNull();

    Sonar sonar = orchestrator.getServer().getAdminWsClient();
    // The parameter skippedModule considers key after first colon
    sonar.update(new PropertyUpdateQuery("sonar.skippedModules", "multi-modules-sample:module_b",
      "com.sonarsource.it.samples:multi-modules-sample"));

    try {
      scan("shared/xoo-multi-modules-sample");
      assertThat(getResource("com.sonarsource.it.samples:multi-modules-sample:module_b")).isNull();

      scan("shared/xoo-multi-modules-sample",
        "sonar.branch", "mybranch");

      assertThat(getResource("com.sonarsource.it.samples:multi-modules-sample:module_b:mybranch")).isNotNull();
    } finally {
      sonar.delete(new PropertyDeleteQuery("sonar.skippedModules", "com.sonarsource.it.samples:multi-modules-sample"));
    }
  }

  // SONAR-4680
  @Test
  public void module_should_load_own_settings_from_database() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    Sonar sonar = orchestrator.getServer().getAdminWsClient();
    String propKey = "myFakeProperty";
    String rootModuleKey = "com.sonarsource.it.samples:multi-modules-sample";
    String moduleBKey = rootModuleKey + ":module_b";
    sonar.delete(new PropertyDeleteQuery(propKey, rootModuleKey));
    sonar.delete(new PropertyDeleteQuery(propKey, moduleBKey));

    BuildResult result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).doesNotContain(rootModuleKey + ":" + propKey);
    assertThat(result.getLogs()).doesNotContain(moduleBKey + ":" + propKey);

    // Set property only on root project
    sonar.update(new PropertyUpdateQuery(propKey, "project", rootModuleKey));

    result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = project");

    // Override property on moduleB
    sonar.update(new PropertyUpdateQuery(propKey, "moduleB", moduleBKey));

    result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = moduleB");
  }

  /**
   * SONAR-3116
   */
  @Test
  public void should_not_exclude_root_module() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    thrown.expect(BuildFailureException.class);
    scan("shared/xoo-multi-modules-sample",
      "sonar.skippedModules", "multi-modules-sample");
  }

  /**
   * SONAR-3024
   */
  @Test
  public void should_support_source_files_with_same_deprecated_key() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.projects.batch:duplicate-source", "exclusions");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.projects.batch:duplicate-source", "xoo", "one-issue-per-line");
    scan("batch/duplicate-source");

    Sonar sonar = orchestrator.getServer().getAdminWsClient();
    Resource project = sonar.find(new ResourceQuery("com.sonarsource.it.projects.batch:duplicate-source").setMetrics("files", "directories"));
    // 2 main files and 1 test file all with same deprecated key
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("directories")).isEqualTo(3);
  }

  /**
   * SONAR-3125
   */
  @Test
  public void should_display_explicit_message_when_no_plugin_language_available() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.language", "foo",
      "sonar.profile", "");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "You must install a plugin that supports the language 'foo'");
  }

  @Test
  public void should_display_explicit_message_when_wrong_profile() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.profile", "unknow");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "sonar.profile was set to 'unknow' but didn't match any profile for any language. Please check your configuration.");
  }

  @Test
  public void should_honor_sonarUserHome() {
    File userHome = temp.getRoot();

    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarRunner runner = configureRunner("shared/xoo-sample",
      "sonar.verbose", "true");
    runner.setEnvironmentVariable("SONAR_USER_HOME", "/dev/null");
    BuildResult buildResult = orchestrator.executeBuildQuietly(runner);
    assertThat(buildResult.getStatus()).isEqualTo(1);

    buildResult = scan("shared/xoo-sample",
      "sonar.verbose", "true",
      "sonar.userHome", userHome.getAbsolutePath());
    assertThat(buildResult.isSuccess()).isTrue();
  }

  @Test
  public void should_authenticate_when_needed() {
    try {
      orchestrator.getServer().provisionProject("sample", "xoo-sample");
      orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      BuildResult buildResult = scanQuietly("shared/xoo-sample",
        "sonar.login", "",
        "sonar.password", "");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");

      // SONAR-4048
      buildResult = scanQuietly("shared/xoo-sample",
        "sonar.login", "wrong_login",
        "sonar.password", "wrong_password");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Please check the properties sonar.login and sonar.password.");

      buildResult = scan("shared/xoo-sample",
        "sonar.login", "admin",
        "sonar.password", "admin");
      assertThat(buildResult.getStatus()).isEqualTo(0);

    } finally {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "false"));
    }
  }

  /**
   * SONAR-4211 Test Sonar Runner when server requires authentication
   */
  @Test
  public void sonar_runner_with_secured_server() {
    try {
      orchestrator.getServer().provisionProject("sample", "xoo-sample");
      orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      BuildResult buildResult = scanQuietly("shared/xoo-sample");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");

      buildResult = scanQuietly("shared/xoo-sample",
        "sonar.login", "wrong_login",
        "sonar.password", "wrong_password");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Please check the properties sonar.login and sonar.password.");

      buildResult = scan("shared/xoo-sample",
        "sonar.login", "admin",
        "sonar.password", "admin");
      assertThat(buildResult.getStatus()).isEqualTo(0);

    } finally {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "false"));
    }
  }

  /**
   * SONAR-2291
   */
  @Test
  public void batch_should_cache_plugin_jars() throws IOException {
    File userHome = temp.newFolder();

    BuildResult result = scan("shared/xoo-sample",
      "sonar.userHome", userHome.getAbsolutePath());

    File cache = new File(userHome, "cache");
    assertThat(cache).exists().isDirectory();
    int cachedFiles = FileUtils.listFiles(cache, new String[] {"jar"}, true).size();
    assertThat(cachedFiles).isGreaterThan(5);
    assertThat(result.getLogs()).contains("User cache: " + cache.getAbsolutePath());
    assertThat(result.getLogs()).contains("Download sonar-xoo-plugin-");

    result = scan("shared/xoo-sample",
      "sonar.userHome", userHome.getAbsolutePath());
    assertThat(cachedFiles).isEqualTo(cachedFiles);
    assertThat(result.getLogs()).contains("User cache: " + cache.getAbsolutePath());
    assertThat(result.getLogs()).doesNotContain("Download sonar-xoo-plugin-");
  }

  /**
   * SONAR-4239
   */
  @Test
  public void should_display_project_url_after_analysis() throws IOException {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("3.6"));

    BuildResult result = scan("shared/xoo-multi-modules-sample");

    assertThat(result.getLogs()).contains("/dashboard/index/com.sonarsource.it.samples:multi-modules-sample");

    result = scan("shared/xoo-multi-modules-sample",
      "sonar.branch", "mybranch");

    assertThat(result.getLogs()).contains("/dashboard/index/com.sonarsource.it.samples:multi-modules-sample:mybranch");

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", "http://foo:123/sonar"));

    result = scan("shared/xoo-multi-modules-sample");

    assertThat(result.getLogs()).contains("http://foo:123/sonar/dashboard/index/com.sonarsource.it.samples:multi-modules-sample");
  }

  /**
   * SONAR-4188, SONAR-5178, SONAR-5915
   */
  @Test
  public void should_display_explicit_message_when_invalid_project_key_or_branch() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", "ar g$l:");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"ar g$l:\" is not a valid project or module key")
      .contains("Allowed characters");

    // SONAR-4629
    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", "12345");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"12345\" is not a valid project or module key")
      .contains("Allowed characters");

    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.branch", "ar g$l:");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"ar g$l:\" is not a valid branch")
      .contains("Allowed characters");
  }

  /**
   * SONAR-4547
   */
  @Test
  public void display_MessageException_without_stacktrace() throws Exception {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    BuildResult result = scanQuietly("shared/xoo-sample", "raiseMessageException", "true");
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs())
      // message
      .contains("Error message from plugin")

      // but not stacktrace
      .doesNotContain("at com.sonarsource.RaiseMessageException");
  }

  /**
   * SONAR-4751
   */
  @Test
  public void file_extensions_are_case_insensitive() throws Exception {
    orchestrator.getServer().provisionProject("case-sensitive-file-extensions", "Case Sensitive");
    orchestrator.getServer().associateProjectToQualityProfile("case-sensitive-file-extensions", "xoo", "one-issue-per-line");
    scan("batch/case-sensitive-file-extensions");

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("case-sensitive-file-extensions", "files", "ncloc"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("ncloc")).isEqualTo(5 + 2);
  }

  /**
   * SONAR-4876
   */
  @Test
  public void custom_module_key() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");
    scan("batch/custom-module-key");
    assertThat(getResource("com.sonarsource.it.samples:moduleA")).isNotNull();
    assertThat(getResource("com.sonarsource.it.samples:moduleB")).isNotNull();
  }

  /**
   * SONAR-4692
   */
  @Test
  @Ignore("This test should be moved to a Medium test of the Compute Engine")
  public void prevent_same_module_key_in_two_projects() {
    orchestrator.getServer().provisionProject("projectAB", "project AB");
    orchestrator.getServer().associateProjectToQualityProfile("projectAB", "xoo", "one-issue-per-line");
    scan("batch/prevent-common-module/projectAB");
    assertThat(getResource("com.sonarsource.it.samples:moduleA")).isNotNull();
    assertThat(getResource("com.sonarsource.it.samples:moduleB")).isNotNull();

    orchestrator.getServer().provisionProject("projectAC", "project AC");
    orchestrator.getServer().associateProjectToQualityProfile("projectAC", "xoo", "one-issue-per-line");

    BuildResult result = scanQuietly("batch/prevent-common-module/projectAC");
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("Module \"com.sonarsource.it.samples:moduleA\" is already part of project \"projectAB\"");
  }

  /**
   * SONAR-4235
   */
  @Test
  public void test_project_creation_date() {
    long before = new Date().getTime() - 2000l;
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    orchestrator.executeBuild(SonarRunner.create(ItUtils.projectDir("shared/xoo-sample")));
    long after = new Date().getTime() + 2000l;
    Resource xooSample = orchestrator.getServer().getWsClient().find(new ResourceQuery().setResourceKeyOrId("sample"));
    assertThat(xooSample.getCreationDate().getTime()).isGreaterThan(before).isLessThan(after);
  }

  /**
   * SONAR-4334
   */
  @Test
  @Ignore("Should be move to CE IT/MT")
  public void fail_if_project_date_is_older_than_latest_snapshot() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarRunner analysis = SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"));
    analysis.setProperty("sonar.projectDate", "2014-01-01");
    orchestrator.executeBuild(analysis);

    analysis.setProperty("sonar.projectDate", "2000-10-19");
    BuildResult result = orchestrator.executeBuildQuietly(analysis);

    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("'sonar.projectDate' property cannot be older than the date of the last known quality snapshot on this project. Value: '2000-10-19'. " +
      "Latest quality snapshot: ");
    assertThat(result.getLogs()).contains("This property may only be used to rebuild the past in a chronological order.");
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines"));
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuild(runner);
  }

  private BuildResult scanQuietly(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuildQuietly(runner);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.projectDir(projectPath))
      .setProperties(props);
    return runner;
  }

}
