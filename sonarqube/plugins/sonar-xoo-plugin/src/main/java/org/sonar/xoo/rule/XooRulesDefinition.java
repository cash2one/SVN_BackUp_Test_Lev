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
package org.sonar.xoo.rule;

import org.sonar.xoo.Xoo2;

import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.xoo.Xoo;

/**
 * Define all the coding rules that are supported on the repositories named "xoo" and "xoo2"
 */
public class XooRulesDefinition implements RulesDefinition {

  public static final String XOO_REPOSITORY = "xoo";
  public static final String XOO2_REPOSITORY = "xoo2";

  @Override
  public void define(Context context) {
    defineRulesXoo(context);
    defineRulesXoo2(context);
  }

  private static void defineRulesXoo2(Context context) {
    NewRepository repo = context.createRepository(XOO2_REPOSITORY, Xoo2.KEY).setName("Xoo2");

    NewRule hasTag = repo.createRule(HasTagSensor.RULE_KEY).setName("Has Tag")
      .setHtmlDescription("Search for a given tag in Xoo files");

    NewRule oneIssuePerLine = repo.createRule(OneIssuePerLineSensor.RULE_KEY).setName("One Issue Per Line")
      .setHtmlDescription("Generate an issue on each line of a file. It requires the metric \"lines\".");
    oneIssuePerLine.setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().linear("1min"))
      .setEffortToFixDescription("It takes about 1 minute to an experienced software craftsman to remove a line of code");
    
    repo.done();
  }

  private static void defineRulesXoo(Context context) {
    NewRepository repo = context.createRepository(XOO_REPOSITORY, Xoo.KEY).setName("Xoo");
    
    NewRule hasTag = repo.createRule(HasTagSensor.RULE_KEY).setName("Has Tag")
      .setHtmlDescription("Search for a given tag in Xoo files");
    hasTag.setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().constantPerIssue("2min"));
    hasTag.createParam("tag")
      .setDefaultValue("xoo")
      .setDescription("The tag to search for");

    NewRule ruleWithParameters = repo.createRule("RuleWithParameters").setName("Rule with parameters")
      .setHtmlDescription("Rule containing parameter of different types : boolean, integer, etc. For information, no issue will be linked to this rule.");
    ruleWithParameters.createParam("string").setType(RuleParamType.STRING);
    ruleWithParameters.createParam("text").setType(RuleParamType.TEXT);
    ruleWithParameters.createParam("boolean").setType(RuleParamType.BOOLEAN);
    ruleWithParameters.createParam("integer").setType(RuleParamType.INTEGER);
    ruleWithParameters.createParam("float").setType(RuleParamType.FLOAT);

    NewRule oneIssuePerLine = repo.createRule(OneIssuePerLineSensor.RULE_KEY).setName("One Issue Per Line")
      .setHtmlDescription("Generate an issue on each line of a file. It requires the metric \"lines\".");
    oneIssuePerLine.setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().linear("1min"))
      .setEffortToFixDescription("It takes about 1 minute to an experienced software craftsman to remove a line of code");

    repo.createRule(OneIssueOnDirPerFileSensor.RULE_KEY).setName("One Issue On Dir Per File")
      .setHtmlDescription("Generate issues on directories");

    NewRule oneIssuePerFile = repo.createRule(OneIssuePerFileSensor.RULE_KEY).setName("One Issue Per File")
      .setHtmlDescription("Generate an issue on each file");
    oneIssuePerFile.setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_CHANGEABILITY)
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().linear("10min"));

    NewRule oneIssuePerModule = repo.createRule(OneIssuePerModuleSensor.RULE_KEY).setName("One Issue Per Module")
      .setHtmlDescription("Generate an issue on each module");
    oneIssuePerModule.setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.API_ABUSE)
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().linearWithOffset("25min", "1h"))
      .setEffortToFixDescription("A certified architect will need roughly half an hour to start working on removal of modules, " +
        "then it's about one hour per module.");

    repo.createRule(OneBlockerIssuePerFileSensor.RULE_KEY).setName("One Blocker Issue Per File")
      .setHtmlDescription("Generate a blocker issue on each file, whatever the severity declared in the Quality profile");

    repo.createRule(CustomMessageSensor.RULE_KEY).setName("Issue With Custom Message")
      .setHtmlDescription("Generate an issue on each file with a custom message");

    repo.createRule(RandomAccessSensor.RULE_KEY).setName("One Issue Per File with Random Access")
      .setHtmlDescription("This issue is generated on each file");

    repo.createRule(DeprecatedResourceApiSensor.RULE_KEY).setName("Issue created using deprecated API")
      .setHtmlDescription("Issue created using deprecated API");

    repo.createRule(MultilineIssuesSensor.RULE_KEY).setName("Creates issues with ranges/multiple locations")
      .setHtmlDescription("Issue with range and multiple locations");

    repo.done();

  }

}
