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

import java.util.List;

import net.sourceforge.htmlunit.corejs.javascript.Node;
import net.sourceforge.htmlunit.corejs.javascript.Parser;
import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;
import net.sourceforge.htmlunit.corejs.javascript.Token;
import net.sourceforge.htmlunit.corejs.javascript.ast.ArrayLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstNode;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstRoot;
import net.sourceforge.htmlunit.corejs.javascript.ast.Block;
import net.sourceforge.htmlunit.corejs.javascript.ast.BreakStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.CatchClause;
import net.sourceforge.htmlunit.corejs.javascript.ast.ConditionalExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.ContinueStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.DoLoop;
import net.sourceforge.htmlunit.corejs.javascript.ast.ElementGet;
import net.sourceforge.htmlunit.corejs.javascript.ast.EmptyExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.ExpressionStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.ForInLoop;
import net.sourceforge.htmlunit.corejs.javascript.ast.ForLoop;
import net.sourceforge.htmlunit.corejs.javascript.ast.FunctionCall;
import net.sourceforge.htmlunit.corejs.javascript.ast.FunctionNode;
import net.sourceforge.htmlunit.corejs.javascript.ast.IfStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.InfixExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.KeywordLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.Label;
import net.sourceforge.htmlunit.corejs.javascript.ast.LabeledStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.Loop;
import net.sourceforge.htmlunit.corejs.javascript.ast.Name;
import net.sourceforge.htmlunit.corejs.javascript.ast.NewExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.NumberLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.ObjectLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.ObjectProperty;
import net.sourceforge.htmlunit.corejs.javascript.ast.ParenthesizedExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.PropertyGet;
import net.sourceforge.htmlunit.corejs.javascript.ast.RegExpLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.ReturnStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.Scope;
import net.sourceforge.htmlunit.corejs.javascript.ast.StringLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.SwitchCase;
import net.sourceforge.htmlunit.corejs.javascript.ast.SwitchStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.ThrowStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.TryStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.UnaryExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.VariableDeclaration;
import net.sourceforge.htmlunit.corejs.javascript.ast.VariableInitializer;
import net.sourceforge.htmlunit.corejs.javascript.ast.WhileLoop;

/**
 * Beautifier.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5470 $
 */
public class JavaScriptBeautifier {

    private StringBuilder buffer_ = new StringBuilder();
    private String methodName_;

    /**
     * Beautifies the given JavaScript source.
     * @param source the source code
     * @return the beautified source
     */
    public String beautify(final String source) {
        buffer_.setLength(0);
        final Parser parser = new Parser();
        final AstRoot root = parser.parse(source, "<cmd>", 0);
        print(root, 0);
        return buffer_.toString();
    }

    /**
     * Sets the JavaScript method name that is called for logging events.
     * @param methoName the method name
     */
    public void setLoggingMethodName(final String methoName) {
        methodName_ = methoName;
    }

    /**
     * Gets the JavaScript method name that is called for logging events.
     * @return the method name
     */
    public String getLogginFunctionName() {
        return methodName_;
    }

    /**
     * Returns the current buffer.
     * @return the current buffer
     */
    protected StringBuilder getBuffer() {
        return buffer_;
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final Node node, final int depth) {
        if (node instanceof AstRoot) {
            print((AstRoot) node, depth);
        }
        else if (node instanceof FunctionNode) {
            print((FunctionNode) node, depth);
        }
        else if (node instanceof Name) {
            print((Name) node, depth);
        }
        else if (node instanceof Block) {
            print((Block) node, depth);
        }
        else if (node instanceof IfStatement) {
            print((IfStatement) node, depth);
        }
        else if (node instanceof UnaryExpression) {
            print((UnaryExpression) node, depth);
        }
        else if (node instanceof PropertyGet) {
            print((PropertyGet) node, depth);
        }
        else if (node instanceof TryStatement) {
            print((TryStatement) node, depth);
        }
        else if (node instanceof ExpressionStatement) {
            print((ExpressionStatement) node, depth);
        }
        else if (node instanceof NewExpression) {
            print((NewExpression) node, depth);
        }
        else if (node instanceof FunctionCall) {
            print((FunctionCall) node, depth);
        }
        else if (node instanceof CatchClause) {
            print((CatchClause) node, depth);
        }
        else if (node instanceof ReturnStatement) {
            print((ReturnStatement) node, depth);
        }
        else if (node instanceof StringLiteral) {
            print((StringLiteral) node, depth);
        }
        else if (node instanceof NumberLiteral) {
            print((NumberLiteral) node, depth);
        }
        else if (node instanceof ConditionalExpression) {
            print((ConditionalExpression) node, depth);
        }
        else if (node instanceof ObjectLiteral) {
            print((ObjectLiteral) node, depth);
        }
        else if (node instanceof ObjectProperty) {
            print((ObjectProperty) node, depth);
        }
        else if (node instanceof KeywordLiteral) {
            print((KeywordLiteral) node, depth);
        }
        else if (node instanceof InfixExpression) {
            print((InfixExpression) node, depth);
        }
        else if (node instanceof VariableDeclaration) {
            print((VariableDeclaration) node, depth);
        }
        else if (node instanceof VariableInitializer) {
            print((VariableInitializer) node, depth);
        }
        else if (node instanceof ElementGet) {
            print((ElementGet) node, depth);
        }
        else if (node instanceof ForLoop) {
            print((ForLoop) node, depth);
        }
        else if (node instanceof ForInLoop) {
            print((ForInLoop) node, depth);
        }
        else if (node instanceof WhileLoop) {
            print((WhileLoop) node, depth);
        }
        else if (node instanceof DoLoop) {
            print((DoLoop) node, depth);
        }
        else if (node instanceof Scope) {
            print((Scope) node, depth);
        }
        else if (node instanceof ParenthesizedExpression) {
            print((ParenthesizedExpression) node, depth);
        }
        else if (node instanceof ArrayLiteral) {
            print((ArrayLiteral) node, depth);
        }
        else if (node instanceof BreakStatement) {
            print((BreakStatement) node, depth);
        }
        else if (node instanceof RegExpLiteral) {
            print((RegExpLiteral) node, depth);
        }
        else if (node instanceof EmptyExpression) {
            print((EmptyExpression) node, depth);
        }
        else if (node instanceof ContinueStatement) {
            print((ContinueStatement) node, depth);
        }
        else if (node instanceof ThrowStatement) {
            print((ThrowStatement) node, depth);
        }
        else if (node instanceof SwitchStatement) {
            print((SwitchStatement) node, depth);
        }
        else if (node instanceof SwitchCase) {
            print((SwitchCase) node, depth);
        }
        else if (node instanceof LabeledStatement) {
            print((LabeledStatement) node, depth);
        }
        else if (node instanceof Label) {
            print((Label) node, depth);
        }
        else {
            throw new RuntimeException("Unknown " + node.getClass().getName());
        }
    }

    /**
     * Adds an indentation.
     * @param indent the depth of indent needed
     */
    protected void makeIndent(final int indent) {
        for (int i = 0; i < indent; i++) {
            buffer_.append("  ");
        }
    }

    /**
     * Prints a comma-separated item list.
     * @param items a list to print
     * @param <T> the type of node
     */
    protected <T extends AstNode> void printList(final List<T> items) {
        final int max = items.size();
        int count = 0;
        for (final AstNode item : items) {
            print(item, 0);
            if (count++ < max - 1) {
                buffer_.append(", ");
            }
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final AstRoot node, final int depth) {
        for (final Node child : node) {
            print(child, depth);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final FunctionNode node, final int depth) {
        makeIndent(depth);
        buffer_.append("function");
        if (node.getFunctionName() != null) {
            buffer_.append(" ");
            print(node.getFunctionName(), 0);
        }
        if (node.getParams().isEmpty()) {
            buffer_.append("() ");
        }
        else {
            buffer_.append("(");
            printList(node.getParams());
            buffer_.append(") ");
        }
        if (node.isExpressionClosure()) {
            buffer_.append(" ");
            print(node.getBody(), 0);
        }
        else {
            final int initLength = buffer_.length();
            print(node.getBody(), 0);
            buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
        }
        if (node.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
            buffer_.append("\n");
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final Name node, final int depth) {
        makeIndent(depth);
        buffer_.append(node.getIdentifier() == null ? "<null>" : node.getIdentifier());
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final Block node, final int depth) {
        makeIndent(depth);
        buffer_.append("{\n");
        for (final Node kid : node) {
            print(kid, depth + 1);
        }
        makeIndent(depth);
        buffer_.append("}\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final IfStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("if (");
        print(node.getCondition(), 0);
        buffer_.append(") ");
        if (!(node.getThenPart() instanceof Block)) {
            buffer_.append("\n");
            makeIndent(depth);
        }
        int initLength = buffer_.length();
        print(node.getThenPart(), depth);
        buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());

        if (node.getElsePart() instanceof IfStatement) {
            buffer_.append(" else ");
            initLength = buffer_.length();
            print(node.getElsePart(), depth);
            buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
        }
        else if (node.getElsePart() != null) {
            buffer_.append(" else ");
            initLength = buffer_.length();
            print(node.getElsePart(), depth);
            buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
        }
        buffer_.append("\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final UnaryExpression node, final int depth) {
        makeIndent(depth);
        if (!node.isPostfix()) {
            buffer_.append(AstNode.operatorToString(node.getType()));
            if (node.getType() == Token.TYPEOF || node.getType() == Token.DELPROP) {
                buffer_.append(" ");
            }
        }
        print(node.getOperand(), 0);
        if (node.isPostfix()) {
            buffer_.append(AstNode.operatorToString(node.getType()));
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final Scope node, final int depth) {
        if (node.getClass() != Scope.class) {
            throw new RuntimeException("Printing Scope called with " + node.getClass().getName());
        }
        makeIndent(depth);
        buffer_.append("{\n");
        for (Node kid : node) {
            print(kid, depth + 1);
        }
        makeIndent(depth);
        buffer_.append("}\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final PropertyGet node, final int depth) {
        makeIndent(depth);
        print(node.getLeft(), 0);
        buffer_.append(".");
        print(node.getRight(), 0);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final TryStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("try ");
        final int previousLength = buffer_.length();
        print(node.getTryBlock(), depth);
        buffer_.replace(previousLength, buffer_.length(), buffer_.substring(previousLength, buffer_.length()).trim());

        for (CatchClause cc : node.getCatchClauses()) {
            print(cc, depth);
        }
        if (node.getFinallyBlock() != null) {
            buffer_.append(" finally ");
            print(node.getFinallyBlock(), depth);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ExpressionStatement node, final int depth) {
        print(node.getExpression(), depth);
        buffer_.append(";\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final FunctionCall node, final int depth) {
        if (node.getClass() != FunctionCall.class) {
            throw new RuntimeException("Printing FunctionCall called with " + node.getClass().getName());
        }
        makeIndent(depth);
        print(node.getTarget(), 0);
        buffer_.append("(");
        printList(node.getArguments());
        buffer_.append(")");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final CatchClause node, final int depth) {
        makeIndent(depth);
        buffer_.append("catch (");
        print(node.getVarName(), 0);
        if (node.getCatchCondition() != null) {
            buffer_.append(" if ");
            print(node.getCatchCondition(), 0);
        }
        buffer_.append(") ");
        print(node.getBody(), 0);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ReturnStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("return");
        if (node.getReturnValue() != null) {
            buffer_.append(" ");
            print(node.getReturnValue(), 0);
        }
        buffer_.append(";\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final StringLiteral node, final int depth) {
        makeIndent(depth);
        buffer_.append(node.getQuoteCharacter())
            .append(ScriptRuntime.escapeString(node.getValue(), node.getQuoteCharacter()))
            .append(node.getQuoteCharacter());
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final NumberLiteral node, final int depth) {
        makeIndent(depth);
        buffer_.append(node.getValue() == null ? "<null>" : node.getValue());
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ConditionalExpression node, final int depth) {
        makeIndent(depth);
        print(node.getTestExpression(), depth);
        buffer_.append(" ? ");
        print(node.getTrueExpression(), 0);
        buffer_.append(" : ");
        print(node.getFalseExpression(), 0);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ObjectLiteral node, final int depth) {
        makeIndent(depth);
        buffer_.append("{");
        printList(node.getElements());
        buffer_.append("}");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ObjectProperty node, final int depth) {
        makeIndent(depth);
        if (node.isGetter()) {
            buffer_.append("get ");
        }
        else if (node.isSetter()) {
            buffer_.append("set ");
        }
        print(node.getLeft(), 0);
        if (node.getType() == Token.COLON) {
            buffer_.append(": ");
        }
        print(node.getRight(), 0);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final KeywordLiteral node, final int depth) {
        makeIndent(depth);
        switch (node.getType()) {
            case Token.THIS:
                buffer_.append("this");
                break;
            case Token.NULL:
                buffer_.append("null");
                break;
            case Token.TRUE:
                buffer_.append("true");
                break;
            case Token.FALSE:
                buffer_.append("false");
                break;
            case Token.DEBUGGER:
                buffer_.append("debugger");
                break;
            default:
                throw new IllegalStateException("Invalid keyword literal type: " + node.getType());
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final InfixExpression node, final int depth) {
        makeIndent(depth);
        print(node.getLeft(), 0);
        buffer_.append(" ");
        buffer_.append(AstNode.operatorToString(node.getType()));
        buffer_.append(" ");
        print(node.getRight(), 0);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final VariableDeclaration node, final int depth) {
        makeIndent(depth);
        buffer_.append(Token.typeToName(node.getType()).toLowerCase());
        buffer_.append(" ");
        printList(node.getVariables());
        if (!(node.getParent() instanceof Loop)) {
            buffer_.append(";\n");
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final VariableInitializer node, final int depth) {
        makeIndent(depth);
        print(node.getTarget(), 0);
        if (node.getInitializer() != null) {
            buffer_.append(" = ");
            print(node.getInitializer(), 0);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ElementGet node, final int depth) {
        makeIndent(depth);
        print(node.getTarget(), 0);
        buffer_.append("[");
        print(node.getElement(), 0);
        buffer_.append("]");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ForInLoop node, final int depth) {
        makeIndent(depth);
        buffer_.append("for ");
        if (node.isForEach()) {
            buffer_.append("each ");
        }
        buffer_.append("(");
        print(node.getIterator(), 0);
        buffer_.append(" in ");
        print(node.getIteratedObject(), 0);
        buffer_.append(") ");
        if (node.getBody() instanceof Block) {
            final int initLength = buffer_.length();
            print(node.getBody(), depth);
            buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
            buffer_.append("\n");
        }
        else {
            buffer_.append("\n");
            print(node.getBody(), depth + 1);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ForLoop node, final int depth) {
        makeIndent(depth);
        buffer_.append("for (");
        print(node.getInitializer(), 0);
        buffer_.append("; ");
        print(node.getCondition(), 0);
        buffer_.append("; ");
        print(node.getIncrement(), 0);
        buffer_.append(") ");
        if (node.getBody() instanceof Block) {
            final int initLength = buffer_.length();
            print(node.getBody(), depth);
            buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
            buffer_.append("\n");
        }
        else {
            buffer_.append("\n");
            print(node.getBody(), depth + 1);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ParenthesizedExpression node, final int depth) {
        makeIndent(depth);
        buffer_.append('(');
        print(node.getExpression(), 0);
        buffer_.append(')');
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final NewExpression node, final int depth) {
        makeIndent(depth);
        buffer_.append("new ");
        print(node.getTarget(), 0);
        buffer_.append("(");
        printList(node.getArguments());
        buffer_.append(")");
        if (node.getInitializer() != null) {
            buffer_.append(" ");
            print(node.getInitializer(), 0);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ArrayLiteral node, final int depth) {
        makeIndent(depth);
        buffer_.append("[");
        printList(node.getElements());
        buffer_.append("]");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final BreakStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("break");
        if (node.getBreakLabel() != null) {
            buffer_.append(" ");
            print(node.getBreakLabel(), 0);
        }
        buffer_.append(";\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final RegExpLiteral node, final int depth) {
        makeIndent(depth);
        buffer_.append('/').append(node.getValue()).append('/');
        if (node.getFlags() != null) {
            buffer_.append(node.getFlags());
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final EmptyExpression node, final int depth) {
        makeIndent(depth);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ContinueStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("continue");
        if (node.getLabel() != null) {
            buffer_.append(" ");
            print(node.getLabel(), 0);
        }
        buffer_.append(";\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final ThrowStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("throw");
        buffer_.append(" ");
        print(node.getExpression(), 0);
        buffer_.append(";\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final SwitchStatement node, final int depth) {
        makeIndent(depth);
        buffer_.append("switch (");
        print(node.getExpression(), 0);
        buffer_.append(") {\n");
        for (final SwitchCase sc : node.getCases()) {
            print(sc, depth + 1);
        }
        makeIndent(depth);
        buffer_.append("}\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final SwitchCase node, final int depth) {
        makeIndent(depth);
        if (node.getExpression() == null) {
            buffer_.append("default:\n");
        }
        else {
            buffer_.append("case ");
            print(node.getExpression(), 0);
            buffer_.append(":\n");
        }
        if (node.getStatements() != null) {
            for (final AstNode s : node.getStatements()) {
                print(s, depth + 1);
            }
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final WhileLoop node, final int depth) {
        makeIndent(depth);
        buffer_.append("while (");
        print(node.getCondition(), 0);
        buffer_.append(") ");
        if (node.getBody() instanceof Block) {
            final int initLength = buffer_.length();
            print(node.getBody(), depth);
            buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
            buffer_.append("\n");
        }
        else {
            buffer_.append("\n");
            print(node.getBody(), depth + 1);
        }
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final LabeledStatement node, final int depth) {
        for (final Label label : node.getLabels()) {
            print(label, depth);
        }
        print(node.getStatement(), depth + 1);
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final Label node, final int depth) {
        makeIndent(depth);
        buffer_.append(node.getName()).append(":\n");
    }

    /**
     * Prints the specified node.
     * @param node the node
     * @param depth the current recursion depth
     */
    protected void print(final DoLoop node, final int depth) {
        buffer_.append("do ");
        final int initLength = buffer_.length();
        print(node.getBody(), depth);
        buffer_.replace(initLength, buffer_.length(), buffer_.substring(initLength, buffer_.length()).trim());
        buffer_.append(" while (");
        print(node.getCondition(), 0);
        buffer_.append(");\n");
    }
}
