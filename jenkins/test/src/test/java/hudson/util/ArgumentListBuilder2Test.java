/*
 * The MIT License
 *
 * Copyright (c) 2010, Kohsuke Kawaguchi
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
package hudson.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import hudson.Functions;
import hudson.Launcher.LocalLauncher;
import hudson.Launcher.RemoteLauncher;
import hudson.Proc;
import hudson.model.Slave;

import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.JenkinsRule;

import com.google.common.base.Joiner;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class ArgumentListBuilder2Test {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Makes sure {@link RemoteLauncher} properly masks arguments.
     */
    @Test
    @Email("http://n4.nabble.com/Password-masking-when-running-commands-on-a-slave-tp1753033p1753033.html")
    public void slaveMask() throws Exception {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("java");
        args.addMasked("-version");

        Slave s = j.createSlave();
        s.toComputer().connect(false).get();

        StringWriter out = new StringWriter();
        assertEquals(0,s.createLauncher(new StreamTaskListener(out)).launch().cmds(args).join());
        System.out.println(out);
        assertTrue(out.toString().contains("$ java ********"));
    }

    @Test
    public void ensureArgumentsArePassedViaCmdExeUnmodified() throws Exception {
        assumeTrue(Functions.isWindows());

        String[] specials = new String[] {
                "~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
                "_", "+", "{", "}", "[", "]", ":", ";", "\"", "'", "\\", "|",
                "<", ">", ",", ".", "/", "?", " "
        };

        String out = echoArgs(specials);

        String expected = String.format("%n%s", Joiner.on(" ").join(specials));
        assertThat(out, containsString(expected));
    }

    public String echoArgs(String... arguments) throws Exception {
        ArgumentListBuilder args = new ArgumentListBuilder(JavaEnvUtils.getJreExecutable("java"), "-cp", "target/test-classes/", "hudson.util.EchoCommand");
        args.add(arguments);
        args = args.toWindowsCommand();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final StreamTaskListener listener = new StreamTaskListener(out);
        Proc p = new LocalLauncher(listener)
                .launch()
                .stderr(System.err)
                .stdout(out)
                .cmds(args)
                .start()
        ;
        int code = p.join();
        listener.close();

        assertThat(code, equalTo(0));
        return out.toString();
    }
}
