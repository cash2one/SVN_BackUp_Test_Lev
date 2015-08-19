/*
 * Copyright (c) 2002-2015 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit.javascript.host.dom;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.html.DomDocumentFragment;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.impl.SimpleRange;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstant;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLDocument;

import net.sourceforge.htmlunit.corejs.javascript.Context;

/**
 * The JavaScript object that represents a Range.
 *
 * @see <a href="http://www.xulplanet.com/references/objref/Range.html">XULPlanet</a>
 * @see <a href="http://www.w3.org/TR/DOM-Level-2-Traversal-Range/ranges.html">
 * DOM-Level-2-Traversal-Range</a>
 * @version $Revision: 10896 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Daniel Gredler
 * @author James Phillpotts
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
public class Range extends SimpleScriptable {

    /** Comparison mode for compareBoundaryPoints. */
    @JsxConstant
    public static final short START_TO_START = 0;

    /** Comparison mode for compareBoundaryPoints. */
    @JsxConstant
    public static final short START_TO_END = 1;

    /** Comparison mode for compareBoundaryPoints. */
    @JsxConstant
    public static final short END_TO_END = 2;

    /** Comparison mode for compareBoundaryPoints. */
    @JsxConstant
    public static final short END_TO_START = 3;

    /** Node before. */
    @JsxConstant(@WebBrowser(CHROME))
    public static final short NODE_BEFORE = 0;

    /** Node after. */
    @JsxConstant(@WebBrowser(CHROME))
    public static final short NODE_AFTER = 1;

    /** Node before and after. */
    @JsxConstant(@WebBrowser(CHROME))
    public static final short NODE_BEFORE_AND_AFTER = 2;

    /** Node inside. */
    @JsxConstant(@WebBrowser(CHROME))
    public static final short NODE_INSIDE = 3;

    private Node startContainer_, endContainer_;
    private int startOffset_, endOffset_;

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public Range() {
    }

    /**
     * Creates a new instance.
     * @param document the HTML document creating the range
     */
    public Range(final HTMLDocument document) {
        startContainer_ = document;
        endContainer_ = document;
    }

    Range(final org.w3c.dom.ranges.Range w3cRange) {
        final DomNode domNodeStartContainer = (DomNode) w3cRange.getStartContainer();
        startContainer_ = (Node) domNodeStartContainer.getScriptObject();
        startOffset_ = w3cRange.getStartOffset();

        final DomNode domNodeEndContainer = (DomNode) w3cRange.getEndContainer();
        endContainer_ = (Node) domNodeEndContainer.getScriptObject();
        endOffset_ = w3cRange.getEndOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDefaultValue(final Class<?> hint) {
        if (getPrototype() == null) {
            return super.getDefaultValue(hint);
        }
        return toW3C().toString();
    }

    /**
     * Gets the node within which the Range begins.
     * @return <code>undefined</code> if not initialized
     */
    @JsxGetter
    public Object getStartContainer() {
        if (startContainer_ == null) {
            return Context.getUndefinedValue();
        }
        return startContainer_;
    }

    /**
     * Gets the node within which the Range ends.
     * @return <code>undefined</code> if not initialized
     */
    @JsxGetter
    public Object getEndContainer() {
        if (endContainer_ == null) {
            return Context.getUndefinedValue();
        }
        return endContainer_;
    }

    /**
     * Gets the offset within the starting node of the Range.
     * @return <code>0</code> if not initialized
     */
    @JsxGetter
    public int getStartOffset() {
        return startOffset_;
    }

    /**
     * Gets the offset within the end node of the Range.
     * @return <code>0</code> if not initialized
     */
    @JsxGetter
    public int getEndOffset() {
        return endOffset_;
    }

    /**
     * Sets the attributes describing the start of a Range.
     * @param refNode the reference node
     * @param offset the offset value within the node
     */
    @JsxFunction
    public void setStart(final Node refNode, final int offset) {
        if (refNode == null) {
            throw Context.reportRuntimeError("It is illegal to call Range.setStart() with a null node.");
        }
        startContainer_ = refNode;
        startOffset_ = offset;
    }

    /**
     * Sets the start of the range to be after the node.
     * @param refNode the reference node
     */
    @JsxFunction
    public void setStartAfter(final Node refNode) {
        if (refNode == null) {
            throw Context.reportRuntimeError("It is illegal to call Range.setStartAfter() with a null node.");
        }
        startContainer_ = refNode.getParent();
        startOffset_ = getPositionInContainer(refNode) + 1;
    }

    /**
     * Sets the start of the range to be before the node.
     * @param refNode the reference node
     */
    @JsxFunction
    public void setStartBefore(final Node refNode) {
        if (refNode == null) {
            throw Context.reportRuntimeError("It is illegal to call Range.setStartBefore() with a null node.");
        }
        startContainer_ = refNode.getParent();
        startOffset_ = getPositionInContainer(refNode);
    }

    private int getPositionInContainer(final Node refNode) {
        int i = 0;
        Node node = refNode;
        while (node.getPreviousSibling() != null) {
            node = node.getPreviousSibling();
            ++i;
        }
        return i;
    }

    /**
     * Indicates if the range is collapsed.
     * @return {@code true} if the range is collapsed
     */
    @JsxGetter
    public boolean getCollapsed() {
        return startContainer_ == endContainer_ && startOffset_ == endOffset_;
    }

    /**
     * Sets the attributes describing the end of a Range.
     * @param refNode the reference node
     * @param offset the offset value within the node
     */
    @JsxFunction
    public void setEnd(final Node refNode, final int offset) {
        if (refNode == null) {
            throw Context.reportRuntimeError("It is illegal to call Range.setEnd() with a null node.");
        }
        endContainer_ = refNode;
        endOffset_ = offset;
    }

    /**
     * Sets the end of the range to be after the node.
     * @param refNode the reference node
     */
    @JsxFunction
    public void setEndAfter(final Node refNode) {
        if (refNode == null) {
            throw Context.reportRuntimeError("It is illegal to call Range.setEndAfter() with a null node.");
        }
        endContainer_ = refNode.getParent();
        endOffset_ = getPositionInContainer(refNode) + 1;
    }

    /**
     * Sets the end of the range to be before the node.
     * @param refNode the reference node
     */
    @JsxFunction
    public void setEndBefore(final Node refNode) {
        if (refNode == null) {
            throw Context.reportRuntimeError("It is illegal to call Range.setEndBefore() with a null node.");
        }
        startContainer_ = refNode.getParent();
        startOffset_ = getPositionInContainer(refNode);
    }

    /**
     * Select the contents within a node.
     * @param refNode Node to select from
     */
    @JsxFunction
    public void selectNodeContents(final Node refNode) {
        startContainer_ = refNode;
        startOffset_ = 0;
        endContainer_ = refNode;
        endOffset_ = refNode.getChildNodes().getLength();
    }

    /**
     * Selects a node and its contents.
     * @param refNode the node to select
     */
    @JsxFunction
    public void selectNode(final Node refNode) {
        setStartBefore(refNode);
        setEndAfter(refNode);
    }

    /**
     * Collapse a Range onto one of its boundaries.
     * @param toStart if {@code true}, collapses the Range onto its start; else collapses it onto its end
     */
    @JsxFunction
    public void collapse(final boolean toStart) {
        if (toStart) {
            endContainer_ = startContainer_;
            endOffset_ = startOffset_;
        }
        else {
            startContainer_ = endContainer_;
            startOffset_ = endOffset_;
        }
    }

    /**
     * Returns the deepest common ancestor container of the Range's two boundary points.
     * @return the deepest common ancestor container of the Range's two boundary points
     */
    @JsxGetter
    public Object getCommonAncestorContainer() {
        final Node ancestor = getCommonAncestor();
        if (ancestor == null) {
            return Context.getUndefinedValue();
        }
        return ancestor;
    }

    /**
     * Returns the deepest common ancestor container of the Range's two boundary points.
     * @return the deepest common ancestor container of the Range's two boundary points
     */
    @SuppressWarnings("unchecked")
    private Node getCommonAncestor() {
        final List<Node> startAncestors = getAncestorsAndSelf(startContainer_);
        final List<Node> endAncestors = getAncestorsAndSelf(endContainer_);
        final List<Node> commonAncestors = ListUtils.intersection(startAncestors, endAncestors);
        if (commonAncestors.isEmpty()) {
            return null;
        }
        return commonAncestors.get(0);
    }

    /**
     * Returns the ancestors of the specified node.
     * @param node the node to start with
     * @return the ancestors of the specified node
     */
    private List<Node> getAncestorsAndSelf(final Node node) {
        final List<Node> ancestors = new ArrayList<>();
        Node ancestor = node;
        while (ancestor != null) {
            ancestors.add(ancestor);
            ancestor = ancestor.getParent();
        }
        return ancestors;
    }

    /**
     * Parses an HTML snippet.
     * @param valueAsString text that contains text and tags to be converted to a document fragment
     * @return a document fragment
     * @see <a href="https://developer.mozilla.org/en-US/docs/DOM/range.createContextualFragment">Mozilla
     * documentation</a>
     */
    @JsxFunction
    public Object createContextualFragment(final String valueAsString) {
        final SgmlPage page = startContainer_.getDomNodeOrDie().getPage();
        final DomDocumentFragment fragment = new DomDocumentFragment(page);
        try {
            HTMLParser.parseFragment(fragment, startContainer_.getDomNodeOrDie(), valueAsString);
        }
        catch (final Exception e) {
            LogFactory.getLog(Range.class).error("Unexpected exception occurred in createContextualFragment", e);
            throw Context.reportRuntimeError("Unexpected exception occurred in createContextualFragment: "
                    + e.getMessage());
        }

        return fragment.getScriptObject();
    }

    /**
     * Moves this range's contents from the document tree into a document fragment.
     * @return the new document fragment containing the range contents
     */
    @JsxFunction
    public Object extractContents() {
        return toW3C().extractContents().getScriptObject();
    }

    /**
     * Returns a W3C {@link org.w3c.dom.ranges.Range} version of this object.
     * @return a W3C {@link org.w3c.dom.ranges.Range} version of this object
     */
    public SimpleRange toW3C() {
        return new SimpleRange(startContainer_.getDomNodeOrNull(), startOffset_,
            endContainer_.getDomNodeOrDie(), endOffset_);
    }

    /**
     * Compares the boundary points of two Ranges.
     * @param how a constant describing the comparison method
     * @param sourceRange the Range to compare boundary points with this range
     * @return -1, 0, or 1, indicating whether the corresponding boundary-point of range is respectively before,
     * equal to, or after the corresponding boundary-point of sourceRange.
     */
    @JsxFunction
    public Object compareBoundaryPoints(final int how, final Range sourceRange) {
        final Node nodeForThis;
        final int offsetForThis;
        final int containingMoficator;
        if (START_TO_START == how || END_TO_START == how) {
            nodeForThis = startContainer_;
            offsetForThis = startOffset_;
            containingMoficator = 1;
        }
        else {
            nodeForThis = endContainer_;
            offsetForThis = endOffset_;
            containingMoficator = -1;
        }

        final Node nodeForOther;
        final int offsetForOther;
        if (START_TO_END == how || START_TO_START == how) {
            nodeForOther = sourceRange.startContainer_;
            offsetForOther = sourceRange.startOffset_;
        }
        else {
            nodeForOther = sourceRange.endContainer_;
            offsetForOther = sourceRange.endOffset_;
        }

        if (nodeForThis == nodeForOther) {
            if (offsetForThis < offsetForOther) {
                return Integer.valueOf(-1);
            }
            else if (offsetForThis < offsetForOther) {
                return Integer.valueOf(1);
            }
            return Integer.valueOf(0);
        }

        final byte nodeComparision = (byte) nodeForThis.compareDocumentPosition(nodeForOther);
        if ((nodeComparision & Node.DOCUMENT_POSITION_CONTAINED_BY) != 0) {
            return Integer.valueOf(-1 * containingMoficator);
        }
        else if ((nodeComparision & Node.DOCUMENT_POSITION_PRECEDING) != 0) {
            return Integer.valueOf(-1);
        }
        // TODO: handle other cases!
        return Integer.valueOf(1);
    }

    /**
     * Returns a clone of the range in a document fragment.
     * @return a clone
     */
    @JsxFunction
    public Object cloneContents() {
        return toW3C().cloneContents().getScriptObject();
    }

    /**
     * Deletes the contents of the range.
     */
    @JsxFunction
    public void deleteContents() {
        toW3C().deleteContents();
    }

    /**
     * Inserts a new node at the beginning of the range. If the range begins at an offset, the node is split.
     * @param newNode The node to insert
     * @see <a href="https://developer.mozilla.org/en/DOM/range">https://developer.mozilla.org/en/DOM/range</a>
     */
    @JsxFunction
    public void insertNode(final Node newNode) {
        toW3C().insertNode(newNode.getDomNodeOrDie());
    }

    /**
     * Surrounds the contents of the range in a new node.
     * @param newNode The node to surround the range in
     */
    @JsxFunction
    public void surroundContents(final Node newNode) {
        toW3C().surroundContents(newNode.getDomNodeOrDie());
    }

    /**
     * Returns a clone of the range.
     * @return a clone of the range
     */
    @JsxFunction
    public Object cloneRange() {
        return new Range(toW3C().cloneRange());
    }

    /**
     * Releases Range from use to improve performance.
     */
    @JsxFunction
    public void detach() {
        // Java garbage collection should take care of this for us
    }

    /**
     * Returns the text of the Range.
     * @return the text
     */
    @Override
    @JsxFunction
    public String toString() {
        return toW3C().toString();
    }

    @Override
    protected Object equivalentValues(final Object value) {
        if (!(value instanceof Range)) {
            return false;
        }
        final Range other = (Range) value;
        return startContainer_ == other.startContainer_
                && endContainer_ == other.endContainer_
                && startOffset_ == other.startOffset_
                && endOffset_ == other.endOffset_;
    }
}
