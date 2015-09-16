/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Jean-Baptiste Quenot, Seiji Sogabe, Tom Huybrechts
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
package hudson.model;

import hudson.Util;
import hudson.EnvVars;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.Queue.QueueAction;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.queue.SubTask;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Records the parameter values used for a build.
 *
 * <P>
 * This object is associated with the build record so that we remember what parameters
 * were used for what build. It is also attached to the queue item to remember parameter
 * that were specified when scheduling.
 */
@ExportedBean
public class ParametersAction implements Action, Iterable<ParameterValue>, QueueAction, EnvironmentContributingAction, LabelAssignmentAction {

    private final List<ParameterValue> parameters;

    /**
     * @deprecated since 1.283; kept to avoid warnings loading old build data, but now transient.
     */
    @Deprecated
    private transient AbstractBuild<?, ?> build;

    public ParametersAction(List<ParameterValue> parameters) {
        this.parameters = parameters;
    }
    
    public ParametersAction(ParameterValue... parameters) {
        this(Arrays.asList(parameters));
    }

    public void createBuildWrappers(AbstractBuild<?,?> build, Collection<? super BuildWrapper> result) {
        for (ParameterValue p : parameters) {
            if (p == null) continue;
            BuildWrapper w = p.createBuildWrapper(build);
            if(w!=null) result.add(w);
        }
    }

    public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
        for (ParameterValue p : parameters) {
            if (p == null) continue;
            p.buildEnvironment(build, env); 
        }
    }

    // TODO do we need an EnvironmentContributingAction variant that takes Run so this can implement it?

    /**
     * Performs a variable substitution to the given text and return it.
     */
    public String substitute(AbstractBuild<?,?> build, String text) {
        return Util.replaceMacro(text,createVariableResolver(build));
    }

    /**
     * Creates an {@link VariableResolver} that aggregates all the parameters.
     *
     * <p>
     * If you are a {@link BuildStep}, most likely you should call {@link AbstractBuild#getBuildVariableResolver()}. 
     */
    public VariableResolver<String> createVariableResolver(AbstractBuild<?,?> build) {
        VariableResolver[] resolvers = new VariableResolver[parameters.size()+1];
        int i=0;
        for (ParameterValue p : parameters) {
            if (p == null) continue;
            resolvers[i++] = p.createVariableResolver(build);
        }
            
        resolvers[i] = build.getBuildVariableResolver();

        return new VariableResolver.Union<String>(resolvers);
    }
    
    public Iterator<ParameterValue> iterator() {
        return parameters.iterator();
    }

    @Exported(visibility=2)
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public ParameterValue getParameter(String name) {
        for (ParameterValue p : parameters) {
            if (p == null) continue;
            if (p.getName().equals(name))
                return p;
        }
        return null;
    }

    public Label getAssignedLabel(SubTask task) {
        for (ParameterValue p : parameters) {
            if (p == null) continue;
            Label l = p.getAssignedLabel(task);
            if (l!=null)    return l;
        }
        return null;
    }

    public String getDisplayName() {
        return Messages.ParameterAction_DisplayName();
    }

    public String getIconFileName() {
        return "document-properties.png";
    }

    public String getUrlName() {
        return "parameters";
    }

    /**
     * Allow an other build of the same project to be scheduled, if it has other parameters.
     */
    public boolean shouldSchedule(List<Action> actions) {
        List<ParametersAction> others = Util.filter(actions, ParametersAction.class);
        if (others.isEmpty()) {
            return !parameters.isEmpty();
        } else {
            // I don't think we need multiple ParametersActions, but let's be defensive
            Set<ParameterValue> params = new HashSet<ParameterValue>();
            for (ParametersAction other: others) {
                params.addAll(other.parameters);
            }
            return !params.equals(new HashSet<ParameterValue>(this.parameters));
        }
    }

    /**
     * Creates a new {@link ParametersAction} that contains all the parameters in this action
     * with the overrides / new values given as parameters.
     * @return New {@link ParametersAction}. The result may contain null {@link ParameterValue}s
     */
    @Nonnull
    public ParametersAction createUpdated(Collection<? extends ParameterValue> overrides) {
        if(overrides == null) {
            return new ParametersAction(parameters);
        }
        List<ParameterValue> combinedParameters = newArrayList(overrides);
        Set<String> names = newHashSet();

        for(ParameterValue v : overrides) {
            if (v == null) continue;
            names.add(v.getName());
        }

        for (ParameterValue v : parameters) {
            if (v == null) continue;
            if (!names.contains(v.getName())) {
                combinedParameters.add(v);
            }
        }

        return new ParametersAction(combinedParameters);
    }

    /*
     * Creates a new {@link ParametersAction} that contains all the parameters in this action
     * with the overrides / new values given as another {@link ParametersAction}.
     * @return New {@link ParametersAction}. The result may contain null {@link ParameterValue}s
     */
    @Nonnull
    public ParametersAction merge(@CheckForNull ParametersAction overrides) {
        if (overrides == null) {
            return new ParametersAction(parameters);
        }
        return createUpdated(overrides.getParameters());
    }

    private Object readResolve() {
        if (build != null)
            OldDataMonitor.report(build, "1.283");
        return this;
    }
}
