/*
 * The MIT License
 *
 * Copyright 2013 Jesse Glick.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.model;

import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.TopLevelItem;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Allows you to add actions to any kind of object at once.
 * @param <T> the type of object to add to; typically an {@link Actionable} subtype,
 *            but may specify a type such as {@link TopLevelItem} most of whose implementations are in fact {@link Actionable}
 * @see Actionable#getAllActions
 * @since 1.548
 */
public abstract class TransientActionFactory<T> implements ExtensionPoint {

    /**
     * The type of object this factory cares about.
     * Declared separately, rather than by having {@link #createFor} do a check-cast,
     * so that method bodies are not loaded until actually needed.
     * @return the type of {@link T}
     */
    public abstract Class<T> type();

    /**
     * Creates actions for a given object.
     * This may be called frequently for the same object, so if your implementation is expensive, do your own caching.
     * @param target an actionable object
     * @return a possible empty set of actions
     */
    public abstract @Nonnull Collection<? extends Action> createFor(@Nonnull T target);

}
