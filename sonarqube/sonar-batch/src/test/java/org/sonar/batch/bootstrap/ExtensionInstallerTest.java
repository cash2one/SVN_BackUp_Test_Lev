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
package org.sonar.batch.bootstrap;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.ClassUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtensionInstallerTest {

  GlobalMode mode;
  BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);

  private static SonarPlugin newPluginInstance(final Object... extensions) {
    return new SonarPlugin() {
      public List getExtensions() {
        return Arrays.asList(extensions);
      }
    };
  }

  @Before
  public void setUp() {
    mode = mock(GlobalMode.class);
  }

  @Test
  public void should_filter_extensions_to_install() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(Foo.class, Bar.class));

    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));
    installer.install(container, new FooMatcher());

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNull();
  }

  @Test
  public void should_execute_extension_provider() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(new FooProvider(), new BarProvider()));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));

    installer.install(container, new FooMatcher());

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNull();
  }

  @Test
  public void should_provide_list_of_extensions() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(new FooBarProvider()));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));

    installer.install(container, new TrueMatcher());

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNotNull();
  }

  @Test
  public void should_not_install_on_unsupported_environment() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(Foo.class, MavenExtension.class, AntExtension.class, new BarProvider()));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));

    installer.install(container, new TrueMatcher());

    assertThat(container.getComponentByType(MavenExtension.class)).isNull();
    assertThat(container.getComponentByType(AntExtension.class)).isNotNull();
    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNotNull();
  }

  private static class FooMatcher implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return extension.equals(Foo.class) || ClassUtils.isAssignable(Foo.class, extension.getClass()) || ClassUtils.isAssignable(FooProvider.class, extension.getClass());
    }
  }

  private static class TrueMatcher implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return true;
    }
  }

  public static class Foo implements BatchExtension {

  }

  public static class Bar implements BatchExtension {

  }

  @SupportedEnvironment("maven")
  public static class MavenExtension implements BatchExtension {

  }

  @SupportedEnvironment("ant")
  public static class AntExtension implements BatchExtension {

  }

  public static class FooProvider extends ExtensionProvider implements BatchExtension {
    @Override
    public Object provide() {
      return new Foo();
    }
  }

  public static class BarProvider extends ExtensionProvider implements BatchExtension {
    @Override
    public Object provide() {
      return new Bar();
    }
  }

  public static class FooBarProvider extends ExtensionProvider implements BatchExtension {
    @Override
    public Object provide() {
      return Arrays.asList(new Foo(), new Bar());
    }
  }

}
