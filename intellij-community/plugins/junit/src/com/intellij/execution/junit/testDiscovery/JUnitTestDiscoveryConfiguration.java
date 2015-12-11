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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.junit.TestObject;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testDiscovery.TestDiscoveryConfiguration;
import com.intellij.execution.testDiscovery.TestDiscoverySearchHelper;
import com.intellij.execution.testframework.SearchForTestsTask;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class JUnitTestDiscoveryConfiguration extends TestDiscoveryConfiguration {

  public JUnitTestDiscoveryConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, new JavaRunConfigurationModule(project, false), factory,
          new JUnitConfiguration("", project, JUnitConfigurationType.getInstance().getConfigurationFactories()[0]));
  }

  @Override
  protected ModuleBasedConfiguration createInstance() {
    final JUnitTestDiscoveryConfigurationType configurationType =
      ConfigurationTypeUtil.findConfigurationType(JUnitTestDiscoveryConfigurationType.class);
    final ConfigurationFactory[] factories = configurationType.getConfigurationFactories();
    return new JUnitTestDiscoveryConfiguration(getName(), getProject(), factories[0]);
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
    return new JUnitTestDiscoveryRunnableState(environment);
  }

  @Nullable
  @Override
  public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
    return null;
  }

  @NotNull
  @Override
  public String getFrameworkPrefix() {
    return "j";
  }

  private class JUnitTestDiscoveryRunnableState extends TestObject {
    public JUnitTestDiscoveryRunnableState(ExecutionEnvironment environment) {
      super(((JUnitConfiguration)myDelegate), environment);
    }
    
    @Override
    public SearchForTestsTask createSearchingForTestsTask() {
      return new SearchForTestsTask(getProject(), myServerSocket) {
        @Override
        protected void search() throws ExecutionException {
          final Set<String> patterns = TestDiscoverySearchHelper.search(getProject(), getPosition(), getChangeList(), getFrameworkPrefix());
          addClassesListToJavaParameters(patterns, FunctionUtil.<String>id(), "", false, getJavaParameters());
        }

        @Override
        protected void onFound() {}
      };
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
      final JavaParameters javaParameters = super.createJavaParameters();
      createTempFiles(javaParameters);

      createServerSocket(javaParameters);
      return javaParameters;
    }

    @Override
    public String suggestActionName() {
      return "";
    }

    @Override
    public RefactoringElementListener getListener(PsiElement element, JUnitConfiguration configuration) {
      return null;
    }

    @Override
    public boolean isConfiguredByElement(JUnitConfiguration configuration,
                                         PsiClass testClass,
                                         PsiMethod testMethod,
                                         PsiPackage testPackage,
                                         PsiDirectory testDir) {
      return false;
    }
  }
}
