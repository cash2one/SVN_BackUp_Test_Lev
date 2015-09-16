/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package org.jvnet.hudson.test.recipes;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRecipe;
import org.jvnet.hudson.test.JenkinsRule;
import org.apache.commons.io.FileUtils;

import hudson.util.JenkinsReloadFailed;

import java.io.File;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.net.URL;

/**
 * Times out the test after the specified number of seconds.
 *
 * All the tests time out after some number of seconds by default, but this recipe
 * allows you to override that value.
 *
 * @author Kohsuke Kawaguchi
 */
@Documented
@Recipe(WithTimeout.RunnerImpl.class)
@JenkinsRecipe(WithTimeout.RuleRunnerImpl.class)
@Target(METHOD)
@Retention(RUNTIME)
public @interface WithTimeout {
    /**
     * Number of seconds.
     *
     * 0 to indicate the test should never time out.
     */
    int value();

    class RunnerImpl extends Recipe.Runner<WithTimeout> {
        @Override
        public void setup(HudsonTestCase testCase, WithTimeout recipe) throws Exception {
            testCase.timeout = recipe.value();
        }
    }

    class RuleRunnerImpl extends JenkinsRecipe.Runner<WithTimeout> {
        @Override
        public void setup(JenkinsRule jenkinsRule, WithTimeout recipe) throws Exception {
            jenkinsRule.timeout = recipe.value();
        }
    }
}
