/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.junit.testDiscovery;

import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.testDiscovery.TestDiscoveryConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JUnitTestDiscoveryConfigurationType implements ConfigurationType {
    private final ConfigurationFactory myFactory;

    public JUnitTestDiscoveryConfigurationType() {
        myFactory = new ConfigurationFactoryEx(this) {
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new JUnitTestDiscoveryConfiguration("", project, this);
            }

            @Override
            public void onNewConfigurationCreated(@NotNull RunConfiguration configuration) {
                ((ModuleBasedConfiguration)configuration).onNewConfigurationCreated();
            }
        };
    }

    @Override
    public String getDisplayName() {
        return "JUnit Test Discovery";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Runs junit tests which passed changed code";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.RunConfigurations.Junit;
    }

    @NotNull
    @Override
    public String getId() {
        return "JUnitTestDiscovery";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[] {myFactory};
    }
}
