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
package com.intellij.refactoring.typeMigration.inspections;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.typeMigration.*;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.refactoring.typeMigration.rules.guava.BaseGuavaTypeConversionRule;
import com.intellij.refactoring.typeMigration.rules.guava.GuavaFluentIterableConversionRule;
import com.intellij.refactoring.typeMigration.rules.guava.GuavaFunctionConversionRule;
import com.intellij.refactoring.typeMigration.rules.guava.GuavaOptionalConversionRule;
import com.intellij.reference.SoftLazyValue;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.HashMap;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
@SuppressWarnings("DialogTitleCapitalization")
public class GuavaInspection extends BaseJavaLocalInspectionTool {
//public class GuavaInspection extends BaseJavaBatchLocalInspectionTool {
  private final static Logger LOG = Logger.getInstance(GuavaInspection.class);

  public final static String PROBLEM_DESCRIPTION_FOR_VARIABLE = "Guava's functional primitives can be replaced by Java API";
  public final static String PROBLEM_DESCRIPTION_FOR_METHOD_CHAIN = "Guava's FluentIterable method chain can be replaced by Java API";

  private final static SoftLazyValue<Set<String>> FLUENT_ITERABLE_STOP_METHODS = new SoftLazyValue<Set<String>>() {
    @NotNull
    @Override
    protected Set<String> compute() {
      return ContainerUtil.newHashSet("append", "cycle", "uniqueIndex", "index");
    }
  };

  public boolean checkVariables = true;
  public boolean checkChains = true;
  public boolean checkReturnTypes = true;

  @SuppressWarnings("Duplicates")
  @Override
  public JComponent createOptionsPanel() {
    final MultipleCheckboxOptionsPanel panel = new MultipleCheckboxOptionsPanel(this);
    panel.addCheckbox("Report variables", "checkVariables");
    panel.addCheckbox("Report method chains", "checkChains");
    panel.addCheckbox("Report return types", "checkReturnTypes");
    return panel;
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    if (!PsiUtil.isLanguageLevel8OrHigher(holder.getFile())) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    return new JavaElementVisitor() {
      private final AtomicNotNullLazyValue<Map<String, PsiClass>> myGuavaClassConversions =
        new AtomicNotNullLazyValue<Map<String, PsiClass>>() {
          @NotNull
          @Override
          protected Map<String, PsiClass> compute() {
            Map<String, PsiClass> map = new HashMap<String, PsiClass>();
            for (TypeConversionRule rule : TypeConversionRule.EP_NAME.getExtensions()) {
              if (rule instanceof BaseGuavaTypeConversionRule) {
                final String fromClass = ((BaseGuavaTypeConversionRule)rule).ruleFromClass();
                final String toClass = ((BaseGuavaTypeConversionRule)rule).ruleToClass();

                final Project project = holder.getProject();
                final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                final PsiClass targetClass = javaPsiFacade.findClass(toClass, GlobalSearchScope.allScope(project));

                if (targetClass != null) {
                  map.put(fromClass, targetClass);
                }

              }
            }
            return map;
          }
        };

      @Override
      public void visitVariable(PsiVariable variable) {
        if (!checkVariables) return;
        final PsiType type = variable.getType();
        PsiType targetType = getConversionClassType(type);
        if (targetType != null) {
          holder.registerProblem(variable,
                                 PROBLEM_DESCRIPTION_FOR_VARIABLE,
                                 TypeMigrationVariableTypeFixProvider.createTypeMigrationFix(variable, targetType, true));
        }
      }

      @Override
      public void visitMethod(PsiMethod method) {
        super.visitMethod(method);
        if (!checkReturnTypes) return;
        final PsiType targetType = getConversionClassType(method.getReturnType());
        if (targetType != null) {
          final PsiTypeElement typeElement = method.getReturnTypeElement();
          if (typeElement != null) {
            holder.registerProblem(typeElement,
                                   PROBLEM_DESCRIPTION_FOR_VARIABLE,
                                   new MigrateMethodReturnTypeFix(method, targetType));
          }
        }
      }

      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        if (!checkChains) return;
        if (!isFluentIterableFromCall(expression)) return;

        final PsiMethodCallExpression chain = findGuavaMethodChain(expression);
        if (chain == null) {
          return;
        }

        final PsiElement maybeLocalVariable = chain.getParent();
        if (maybeLocalVariable instanceof PsiLocalVariable) {
          final PsiClass aClass = PsiUtil.resolveClassInType(((PsiLocalVariable)maybeLocalVariable).getType());
          if (aClass != null && (GuavaFluentIterableConversionRule.FLUENT_ITERABLE.equals(aClass.getQualifiedName()) ||
                                 GuavaOptionalConversionRule.GUAVA_OPTIONAL.equals(aClass.getQualifiedName()))) {
            return;
          }
        }

        PsiClassType initialType = (PsiClassType)expression.getType();
        LOG.assertTrue(initialType != null);
        PsiClass resolvedClass = initialType.resolve();
        PsiClass target;
        if (resolvedClass == null || (target = myGuavaClassConversions.getValue().get(resolvedClass.getQualifiedName())) == null) {
          return;
        }
        PsiClassType targetType = addTypeParameters(initialType, initialType.resolveGenerics(), target);

        holder.registerProblem(chain, PROBLEM_DESCRIPTION_FOR_METHOD_CHAIN, new MigrateFluentIterableChainQuickFix(chain, initialType, targetType));
      }

      private PsiType getConversionClassType(PsiType initialType) {
        if (initialType == null) return null;
        final PsiType type = initialType.getDeepComponentType();
        if (type instanceof PsiClassType) {
          final PsiClassType.ClassResolveResult resolveResult = ((PsiClassType)type).resolveGenerics();
          final PsiClass psiClass = resolveResult.getElement();
          if (psiClass != null) {
            final String qName = psiClass.getQualifiedName();
            final PsiClass targetClass = myGuavaClassConversions.getValue().get(qName);
            if (targetClass != null) {
              final PsiClassType createdType = addTypeParameters(type, resolveResult, targetClass);
              return initialType instanceof PsiArrayType ? wrapAsArray((PsiArrayType)initialType, createdType) : createdType;
            }
          }
        }
        return null;
      };

      private PsiType wrapAsArray(PsiArrayType initial, PsiType created) {
        PsiArrayType result = new PsiArrayType(created);
        while (initial.getComponentType() instanceof PsiArrayType) {
          initial = (PsiArrayType)initial.getComponentType();
          result = new PsiArrayType(result);
        }
        return result;
      }

      private boolean isFluentIterableFromCall(PsiMethodCallExpression expression) {
        PsiMethod method = expression.resolveMethod();
        if (method == null || !GuavaFluentIterableConversionRule.CHAIN_HEAD_METHODS.contains(method.getName())) {
          return false;
        }
        PsiClass aClass = method.getContainingClass();
        return aClass != null && GuavaFluentIterableConversionRule.FLUENT_ITERABLE.equals(aClass.getQualifiedName());
      }

      private PsiMethodCallExpression findGuavaMethodChain(PsiMethodCallExpression expression) {
        PsiMethodCallExpression chain = expression;
        while (true) {
          final PsiMethodCallExpression current = PsiTreeUtil.getParentOfType(chain, PsiMethodCallExpression.class);
          if (current != null && current.getMethodExpression().getQualifierExpression() == chain) {
            final PsiMethod method = current.resolveMethod();
            if (method == null) {
              return chain;
            }
            if (FLUENT_ITERABLE_STOP_METHODS.getValue().contains(method.getName())) {
              return null;
            }
            final PsiClass containingClass = method.getContainingClass();
            if (containingClass == null || ! (GuavaFluentIterableConversionRule.FLUENT_ITERABLE.equals(containingClass.getQualifiedName())
                                              || GuavaOptionalConversionRule.GUAVA_OPTIONAL.equals(containingClass.getQualifiedName()))) {
              return chain;
            }
          } else {
            return chain;
          }
          chain = current;
        }
      }

      @NotNull
      private PsiClassType addTypeParameters(PsiType currentType, PsiClassType.ClassResolveResult currentTypeResolveResult, PsiClass targetClass) {
        final Map<PsiTypeParameter, PsiType> substitutionMap = currentTypeResolveResult.getSubstitutor().getSubstitutionMap();
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(holder.getProject());
        if (substitutionMap.size() == 1) {
          return elementFactory.createType(targetClass, ContainerUtil.getFirstItem(substitutionMap.values()));
        } else {
          LOG.assertTrue(substitutionMap.size() == 2);
          LOG.assertTrue(GuavaFunctionConversionRule.JAVA_UTIL_FUNCTION_FUNCTION.equals(targetClass.getQualifiedName()));
          final PsiType returnType = LambdaUtil.getFunctionalInterfaceReturnType(currentType);
          final List<PsiType> types = new ArrayList<PsiType>(substitutionMap.values());
          types.remove(returnType);
          final PsiType parameterType = types.get(0);
          return elementFactory.createType(targetClass, parameterType, returnType);
        }
      }
    };
  }

  public static class MigrateFluentIterableChainQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final PsiClassType myInitialType;
    private final PsiClassType myTargetType;

    private MigrateFluentIterableChainQuickFix(@Nullable PsiElement element, PsiClassType initialType, PsiClassType targetType) {
      super(element);
      myInitialType = initialType;
      myTargetType = targetType;
    }

    @Override
    public void invoke(@NotNull final Project project,
                       @NotNull final PsiFile file,
                       @Nullable("is null when called from inspection") Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
      if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
      try {
        final PsiMethodCallExpression expr = (PsiMethodCallExpression)startElement;
        final boolean isIterableAssignment = isIterable(expr);
        final TypeMigrationRules rules = new TypeMigrationRules();
        rules.setBoundScope(GlobalSearchScope.fileScope(file));
        final TypeConversionDescriptorBase conversion =
          rules.findConversion(myInitialType, myTargetType, expr.resolveMethod(), expr, new TypeMigrationLabeler(rules, myTargetType));
        LOG.assertTrue(conversion != null);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            PsiElement replacedExpression = TypeMigrationReplacementUtil.replaceExpression(expr, project, conversion);
            if (isIterableAssignment) {
              final String expressionText = replacedExpression.getText() + ".collect(java.util.stream.Collectors.toList())";
              replacedExpression = replacedExpression.replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText(expressionText, replacedExpression));
            }
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(replacedExpression);
            UndoUtil.markPsiFileForUndo(file);
          }
        });
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    private static boolean isIterable(PsiMethodCallExpression expression) {
      final PsiElement parent = expression.getParent();
      final PsiMethod method = expression.resolveMethod();
      if (method != null) {
        final PsiClass returnClass = PsiTypesUtil.getPsiClass(method.getReturnType());
        if (returnClass == null || !GuavaFluentIterableConversionRule.FLUENT_ITERABLE.equals(returnClass.getQualifiedName())) {
          return false;
        }
      }
      if (parent instanceof PsiLocalVariable) {
        return isIterable(((PsiLocalVariable)parent).getType());
      }
      else if (parent instanceof PsiReturnStatement) {
        final PsiElement methodOrLambda = PsiTreeUtil.getParentOfType(parent, PsiMethod.class, PsiLambdaExpression.class);
        PsiType methodReturnType = null;
        if (methodOrLambda instanceof PsiMethod) {
          methodReturnType = ((PsiMethod)methodOrLambda).getReturnType();
        }
        else if (methodOrLambda instanceof PsiLambdaExpression) {
          methodReturnType = LambdaUtil.getFunctionalInterfaceReturnType((PsiFunctionalExpression)methodOrLambda);
        }
        return isIterable(methodReturnType);
      }
      return false;
    }

    private static boolean isIterable(@Nullable PsiType type) {
      PsiClass aClass;
      return (aClass = PsiTypesUtil.getPsiClass(type)) != null && CommonClassNames.JAVA_LANG_ITERABLE.equals(aClass.getQualifiedName());
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    @NotNull
    @Override
    public String getText() {
      return getFamilyName();
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return "Replace FluentIterable method chain by Java API";
    }
  }

  public static class MigrateMethodReturnTypeFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    private final PsiType myTargetType;

    private MigrateMethodReturnTypeFix(@NotNull PsiMethod method, PsiType targetType) {
      super(method);
      myTargetType = targetType;
    }

    @Override
    public void invoke(@NotNull Project project,
                       @NotNull PsiFile file,
                       @Nullable("is null when called from inspection") Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
      if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
      try {
        PsiMethod method = (PsiMethod)getStartElement();
        final TypeMigrationRules rules = new TypeMigrationRules();
        rules.setBoundScope(method.getUseScope());
        TypeMigrationProcessor.runHighlightingTypeMigration(project, editor, rules, method, myTargetType, true);
        UndoUtil.markPsiFileForUndo(file);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    @Override
    protected boolean isAvailable() {
      return super.isAvailable() && myTargetType.isValid();
    }

    @NotNull
    @Override
    public String getText() {
      return !myTargetType.isValid() ? "Migrate method return type" : "Migrate method return type to '" + myTargetType.getCanonicalText(false) + "'";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return "Migrate method return type";
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }
  }
}