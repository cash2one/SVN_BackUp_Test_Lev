/*
 * Copyright (c) 2010 HtmlUnit team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.htmlunit.proxy;

import net.sourceforge.htmlunit.corejs.javascript.Node;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstNode;
import net.sourceforge.htmlunit.corejs.javascript.ast.Block;
import net.sourceforge.htmlunit.corejs.javascript.ast.FunctionNode;

/**
 * Logs function entry.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5595 $
 */
public class JavaScriptFunctionLogger extends JavaScriptBeautifier {

    @Override
    protected void print(final Block node, final int depth) {
        if (node.getParent() instanceof FunctionNode) {
            final StringBuilder sb = getBuffer();
            final FunctionNode parent = (FunctionNode) node.getParent();
            if (parent.getFunctionName() != null) {
                makeIndent(depth);
                sb.append("{\n");
                makeIndent(depth + 1);
                if (!parent.getParams().isEmpty()) {
                    sb.append("if (!window.top.__HtmlUnitLogging) {\n");
                    makeIndent(depth + 1);
                    sb.append("  window.top.__HtmlUnitLogging = true;\n");
                    makeIndent(depth + 2);
                    sb.append("try{");
                }
                sb.append(getLogginFunctionName()).append("('Entering Function: ");
                print(parent.getFunctionName(), 0);
                sb.append("(");
                if (!parent.getParams().isEmpty()) {
                    sb.append("' + ");
                    final int max = parent.getParams().size();
                    int count = 0;
                    for (final AstNode item : parent.getParams()) {
                        sb.append("'");
                        print(item, 0);
                        sb.append(":' + ");
                        print(item, 0);
                        if (count++ < max - 1) {
                            sb.append(" + ',' + ");
                        }
                    }
                    sb.append(" + '");
                }
                sb.append(")');");
                if (!parent.getParams().isEmpty()) {
                    sb.append("}catch(htmlunitExp){");
                    sb.append(getLogginFunctionName()).append("('Entering Function: ");
                    print(parent.getFunctionName(), 0);
                    sb.append("(error printing parameters)');");
                    sb.append("}\n");
                    makeIndent(depth + 2);
                    sb.append("window.top.__HtmlUnitLogging = false;\n");
                    makeIndent(depth + 1);
                    sb.append("}");
                }
                sb.append("\n");
                for (final Node kid : node) {
                    print(kid, depth + 1);
                }
                makeIndent(depth);
                sb.append("}\n");
                return;
            }
        }
        super.print(node, depth);
    }
}
