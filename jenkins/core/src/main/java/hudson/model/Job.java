/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Martin Eigenbrodt, Matthew R. Harrah, Red Hat, Inc., Stephen Connolly, Tom Huybrechts, CloudBees, Inc.
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.infradna.tool.bridge_method_injector.WithBridgeMethods;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.PermalinkList;
import hudson.Util;
import hudson.cli.declarative.CLIResolver;
import hudson.model.Descriptor.FormException;
import hudson.model.Fingerprint.Range;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.listeners.ItemListener;
import hudson.search.QuickSilver;
import hudson.search.SearchIndex;
import hudson.search.SearchIndexBuilder;
import hudson.search.SearchItem;
import hudson.search.SearchItems;
import hudson.security.ACL;
import hudson.tasks.LogRotator;
import hudson.util.AlternativeUiTextProvider;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.CopyOnWriteList;
import hudson.util.DataSetBuilder;
import hudson.util.DescribableList;
import hudson.util.FormApply;
import hudson.util.Graph;
import hudson.util.ProcessTree;
import hudson.util.QuotedStringTokenizer;
import hudson.util.RunList;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;
import hudson.util.TextFile;
import hudson.widgets.HistoryWidget;
import hudson.widgets.HistoryWidget.Adapter;
import hudson.widgets.Widget;
import jenkins.model.BuildDiscarder;
import jenkins.model.DirectlyModifiableTopLevelItemGroup;
import jenkins.model.Jenkins;
import jenkins.model.ProjectNamingStrategy;
import jenkins.security.HexStringConfidentialKey;
import jenkins.util.io.OnMaster;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jvnet.localizer.Localizable;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.stapler.StaplerOverridable;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;

import java.awt.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static javax.servlet.http.HttpServletResponse.*;
import jenkins.model.ModelObjectWithChildren;
import jenkins.model.RunIdMigrator;
import jenkins.model.lazy.LazyBuildMixIn;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * A job is an runnable entity under the monitoring of Hudson.
 * 
 * <p>
 * Every time it "runs", it will be recorded as a {@link Run} object.
 *
 * <p>
 * To create a custom job type, extend {@link TopLevelItemDescriptor} and put {@link Extension} on it.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Job<JobT extends Job<JobT, RunT>, RunT extends Run<JobT, RunT>>
        extends AbstractItem implements ExtensionPoint, StaplerOverridable, ModelObjectWithChildren, OnMaster {

    /**
     * Next build number. Kept in a separate file because this is the only
     * information that gets updated often. This allows the rest of the
     * configuration to be in the VCS.
     * <p>
     * In 1.28 and earlier, this field was stored in the project configuration
     * file, so even though this is marked as transient, don't move it around.
     */
    protected transient volatile int nextBuildNumber = 1;

    /**
     * Newly copied jobs get this flag set, so that Hudson doesn't try to run the job until its configuration
     * is saved once.
     */
    private transient volatile boolean holdOffBuildUntilSave;

    /**
     * {@link ItemListener}s can, and do, modify the job with a corresponding save which will clear
     * {@link #holdOffBuildUntilSave} prematurely. The {@link LastItemListener} is responsible for
     * clearing this flag as the last item listener.
     */
    private transient volatile boolean holdOffBuildUntilUserSave;

    private volatile BuildDiscarder logRotator;

    /**
     * Not all plugins are good at calculating their health report quickly.
     * These fields are used to cache the health reports to speed up rendering
     * the main page.
     */
    private transient Integer cachedBuildHealthReportsBuildNumber = null;
    private transient List<HealthReport> cachedBuildHealthReports = null;

    boolean keepDependencies;

    /**
     * List of properties configured for this project.
     */
    // this should have been DescribableList but now it's too late
    protected CopyOnWriteList<JobProperty<? super JobT>> properties = new CopyOnWriteList<JobProperty<? super JobT>>();

    @Restricted(NoExternalUse.class)
    public transient RunIdMigrator runIdMigrator;

    protected Job(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public synchronized void save() throws IOException {
        super.save();
        holdOffBuildUntilSave = holdOffBuildUntilUserSave;
    }

    @Override public void onCreatedFromScratch() {
        super.onCreatedFromScratch();
        runIdMigrator = new RunIdMigrator();
        runIdMigrator.created(getBuildDir());
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name)
            throws IOException {
        super.onLoad(parent, name);

        File buildDir = getBuildDir();
        runIdMigrator = new RunIdMigrator();
        runIdMigrator.migrate(buildDir, Jenkins.getInstance().getRootDir());

        TextFile f = getNextBuildNumberFile();
        if (f.exists()) {
            // starting 1.28, we store nextBuildNumber in a separate file.
            // but old Hudson didn't do it, so if the file doesn't exist,
            // assume that nextBuildNumber was read from config.xml
            try {
                synchronized (this) {
                    this.nextBuildNumber = Integer.parseInt(f.readTrim());
                }
            } catch (NumberFormatException e) {
                // try to infer the value of the next build number from the existing build records. See JENKINS-11563
                File[] folders = buildDir.listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        return file.isDirectory() && file.getName().matches("[0-9]+");
                    }
                });

                if (folders == null || folders.length == 0) {
                    this.nextBuildNumber = 1;
                } else {
                    Collection<Integer> foldersInt = Collections2.transform(Arrays.asList(folders), new Function<File, Integer>() {
                        public Integer apply(File file) {
                            return Integer.parseInt(file.getName());
                        }
                    });
                    this.nextBuildNumber = Collections.max(foldersInt) + 1;
                }
                saveNextBuildNumber();
            }
        } else {
            // From the old Hudson, or doCreateItem. Create this file now.
            saveNextBuildNumber();
        }

        if (properties == null) // didn't exist < 1.72
            properties = new CopyOnWriteList<JobProperty<? super JobT>>();

        for (JobProperty p : properties)
            p.setOwner(this);
    }

    @Override
    public void onCopiedFrom(Item src) {
        super.onCopiedFrom(src);
        synchronized (this) {
            this.nextBuildNumber = 1; // reset the next build number
            this.holdOffBuildUntilUserSave = true;
            this.holdOffBuildUntilSave = this.holdOffBuildUntilUserSave;
        }
    }

    @Extension(ordinal = -Double.MAX_VALUE)
    public static class LastItemListener extends ItemListener {

        @Override
        public void onCopied(Item src, Item item) {
            // If any of the other ItemListeners modify the job, they effect
            // a save, which will clear the holdOffBuildUntilUserSave and
            // causing a regression of JENKINS-2494
            if (item instanceof Job) {
                Job job = (Job) item;
                synchronized (job) {
                    job.holdOffBuildUntilUserSave = false;
                }
            }
        }
    }

    @Override
    protected void performDelete() throws IOException, InterruptedException {
        // if a build is in progress. Cancel it.
        RunT lb = getLastBuild();
        if (lb != null) {
            Executor e = lb.getExecutor();
            if (e != null) {
                e.interrupt();
                // should we block until the build is cancelled?
            }
        }
        super.performDelete();
    }

    /*package*/ TextFile getNextBuildNumberFile() {
        return new TextFile(new File(this.getRootDir(), "nextBuildNumber"));
    }

    public synchronized boolean isHoldOffBuildUntilSave() {
        return holdOffBuildUntilSave;
    }

    protected synchronized void saveNextBuildNumber() throws IOException {
        if (nextBuildNumber == 0) { // #3361
            nextBuildNumber = 1;
        }
        getNextBuildNumberFile().write(String.valueOf(nextBuildNumber) + '\n');
    }

    @Exported
    public boolean isInQueue() {
        return false;
    }

    /**
     * If this job is in the build queue, return its item.
     */
    @Exported
    public Queue.Item getQueueItem() {
        return null;
    }

    /**
     * Returns true if a build of this project is in progress.
     */
    public boolean isBuilding() {
        RunT b = getLastBuild();
        return b!=null && b.isBuilding();
    }
    
    /**
     * Returns true if the log file is still being updated.
     */
    public boolean isLogUpdated() {
        RunT b = getLastBuild();
        return b!=null && b.isLogUpdated();
    }    

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, Messages.Job_Pronoun());
    }

    /**
     * Returns whether the name of this job can be changed by user.
     */
    public boolean isNameEditable() {
        return true;
    }

    /**
     * If true, it will keep all the build logs of dependency components.
     * (This really only makes sense in {@link AbstractProject} but historically it was defined here.)
     */
    @Exported
    public boolean isKeepDependencies() {
        return keepDependencies;
    }

    /**
     * Allocates a new buildCommand number.
     */
    public synchronized int assignBuildNumber() throws IOException {
        int r = nextBuildNumber++;
        saveNextBuildNumber();
        return r;
    }

    /**
     * Peeks the next build number.
     */
    @Exported
    public int getNextBuildNumber() {
        return nextBuildNumber;
    }

    /**
     * Builds up the environment variable map that's sufficient to identify a process
     * as ours. This is used to kill run-away processes via {@link ProcessTree#killAll(Map)}.
     */
    public EnvVars getCharacteristicEnvVars() {
        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE",SERVER_COOKIE.get());
        env.put("HUDSON_SERVER_COOKIE",SERVER_COOKIE.get()); // Legacy compatibility
        env.put("JOB_NAME",getFullName());
        return env;
    }

    /**
     * Creates an environment variable override for launching processes for this project.
     *
     * <p>
     * This is for process launching outside the build execution (such as polling, tagging, deployment, etc.)
     * that happens in a context of a specific job.
     *
     * @param node
     *      Node to eventually run a process on. The implementation must cope with this parameter being null
     *      (in which case none of the node specific properties would be reflected in the resulting override.)
     */
    public @Nonnull EnvVars getEnvironment(@CheckForNull Node node, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        EnvVars env;

        if (node!=null) {
            final Computer computer = node.toComputer();
            env = (computer != null) ? computer.buildEnvironment(listener) : new EnvVars();                
        } else {
            env = new EnvVars();
        }

        env.putAll(getCharacteristicEnvVars());

        // servlet container may have set CLASSPATH in its launch script,
        // so don't let that inherit to the new child process.
        // see http://www.nabble.com/Run-Job-with-JDK-1.4.2-tf4468601.html
        env.put("CLASSPATH","");

        // apply them in a reverse order so that higher ordinal ones can modify values added by lower ordinal ones
        for (EnvironmentContributor ec : EnvironmentContributor.all().reverseView())
            ec.buildEnvironmentFor(this,env,listener);


        return env;
    }

    /**
     * Programatically updates the next build number.
     * 
     * <p>
     * Much of Hudson assumes that the build number is unique and monotonic, so
     * this method can only accept a new value that's bigger than
     * {@link #getLastBuild()} returns. Otherwise it'll be no-op.
     * 
     * @since 1.199 (before that, this method was package private.)
     */
    public synchronized void updateNextBuildNumber(int next) throws IOException {
        RunT lb = getLastBuild();
        if (lb!=null ?  next>lb.getNumber() : next>0) {
            this.nextBuildNumber = next;
            saveNextBuildNumber();
        }
    }

    /**
     * Returns the configured build discarder for this job, or null if none.
     */
    public BuildDiscarder getBuildDiscarder() {
        return logRotator;
    }

    public void setBuildDiscarder(BuildDiscarder bd) throws IOException {
        this.logRotator = bd;
        save();
    }

    /**
     * Left for backward compatibility. Returns non-null if and only
     * if {@link LogRotator} is configured as {@link BuildDiscarder}.
     *
     * @deprecated as of 1.503
     *      Use {@link #getBuildDiscarder()}.
     */
    @Deprecated
    public LogRotator getLogRotator() {
        if (logRotator instanceof LogRotator)
            return (LogRotator) logRotator;
        return null;
    }

    /**
     * @deprecated as of 1.503
     *      Use {@link #setBuildDiscarder(BuildDiscarder)}
     */
    @Deprecated
    public void setLogRotator(LogRotator logRotator) throws IOException {
        setBuildDiscarder(logRotator);
    }

    /**
     * Perform log rotation.
     */
    public void logRotate() throws IOException, InterruptedException {
        BuildDiscarder bd = getBuildDiscarder();
        if (bd != null)
            bd.perform(this);
    }

    /**
     * True if this instance supports log rotation configuration.
     */
    public boolean supportsLogRotator() {
        return true;
    }

    @Override
    protected SearchIndexBuilder makeSearchIndex() {
        return super.makeSearchIndex().add(new SearchIndex() {
            public void find(String token, List<SearchItem> result) {
                try {
                    if (token.startsWith("#"))
                        token = token.substring(1); // ignore leading '#'
                    int n = Integer.parseInt(token);
                    Run b = getBuildByNumber(n);
                    if (b == null)
                        return; // no such build
                    result.add(SearchItems.create("#" + n, "" + n, b));
                } catch (NumberFormatException e) {
                    // not a number.
                }
            }

            public void suggest(String token, List<SearchItem> result) {
                find(token, result);
            }
        }).add("configure", "config", "configure");
    }

    public Collection<? extends Job> getAllJobs() {
        return Collections.<Job> singleton(this);
    }

    /**
     * Adds {@link JobProperty}.
     * 
     * @since 1.188
     */
    public void addProperty(JobProperty<? super JobT> jobProp) throws IOException {
        ((JobProperty)jobProp).setOwner(this);
        properties.add(jobProp);
        save();
    }

    /**
     * Removes {@link JobProperty}
     *
     * @since 1.279
     */
    public void removeProperty(JobProperty<? super JobT> jobProp) throws IOException {
        properties.remove(jobProp);
        save();
    }

    /**
     * Removes the property of the given type.
     *
     * @return
     *      The property that was just removed.
     * @since 1.279
     */
    public <T extends JobProperty> T removeProperty(Class<T> clazz) throws IOException {
        for (JobProperty<? super JobT> p : properties) {
            if (clazz.isInstance(p)) {
                removeProperty(p);
                return clazz.cast(p);
            }
        }
        return null;
    }

    /**
     * Gets all the job properties configured for this job.
     */
    @SuppressWarnings("unchecked")
    public Map<JobPropertyDescriptor, JobProperty<? super JobT>> getProperties() {
        return Descriptor.toMap((Iterable) properties);
    }

    /**
     * List of all {@link JobProperty} exposed primarily for the remoting API.
     * @since 1.282
     */
    @Exported(name="property",inline=true)
    public List<JobProperty<? super JobT>> getAllProperties() {
        return properties.getView();
    }

    /**
     * Gets the specific property, or null if the propert is not configured for
     * this job.
     */
    public <T extends JobProperty> T getProperty(Class<T> clazz) {
        for (JobProperty p : properties) {
            if (clazz.isInstance(p))
                return clazz.cast(p);
        }
        return null;
    }

    /**
     * Bind {@link JobProperty}s to URL spaces.
     *
     * @since 1.403
     */
    public JobProperty getProperty(String className) {
        for (JobProperty p : properties)
            if (p.getClass().getName().equals(className))
                return p;
        return null;
    }

    /**
     * Overrides from job properties.
     * @see JobProperty#getJobOverrides
     */
    public Collection<?> getOverrides() {
        List<Object> r = new ArrayList<Object>();
        for (JobProperty<? super JobT> p : properties)
            r.addAll(p.getJobOverrides());
        return r;
    }

    public List<Widget> getWidgets() {
        ArrayList<Widget> r = new ArrayList<Widget>();
        r.add(createHistoryWidget());
        return r;
    }

    /**
     * @see LazyBuildMixIn#createHistoryWidget
     */
    protected HistoryWidget createHistoryWidget() {
        return new HistoryWidget<Job, RunT>(this, getBuilds(), HISTORY_ADAPTER);
    }

    public static final HistoryWidget.Adapter<Run> HISTORY_ADAPTER = new Adapter<Run>() {
        public int compare(Run record, String key) {
            try {
                int k = Integer.parseInt(key);
                return record.getNumber() - k;
            } catch (NumberFormatException nfe) {
                return String.valueOf(record.getNumber()).compareTo(key);
            }
        }

        public String getKey(Run record) {
            return String.valueOf(record.getNumber());
        }

        public boolean isBuilding(Run record) {
            return record.isBuilding();
        }

        public String getNextKey(String key) {
            try {
                int k = Integer.parseInt(key);
                return String.valueOf(k + 1);
            } catch (NumberFormatException nfe) {
                return "-unable to determine next key-";
            }
        }
    };

    /**
     * Renames a job.
     */
    @Override
    public void renameTo(String newName) throws IOException {
        File oldBuildDir = getBuildDir();
        super.renameTo(newName);
        File newBuildDir = getBuildDir();
        if (oldBuildDir.isDirectory() && !newBuildDir.isDirectory()) {
            if (!newBuildDir.getParentFile().isDirectory()) {
                newBuildDir.getParentFile().mkdirs();
            }
            if (!oldBuildDir.renameTo(newBuildDir)) {
                throw new IOException("failed to rename " + oldBuildDir + " to " + newBuildDir);
            }
        }
    }

    @Override
    public void movedTo(DirectlyModifiableTopLevelItemGroup destination, AbstractItem newItem, File destDir) throws IOException {
        Job newJob = (Job) newItem; // Missing covariant parameters type here.
        File oldBuildDir = getBuildDir();
        super.movedTo(destination, newItem, destDir);
        File newBuildDir = getBuildDir();
        if (oldBuildDir.isDirectory()) {
            FileUtils.moveDirectory(oldBuildDir, newBuildDir);
        }
    }

    @Override public void delete() throws IOException, InterruptedException {
        super.delete();
        Util.deleteRecursive(getBuildDir());
    }

    /**
     * Returns true if we should display "build now" icon
     */
    @Exported
    public abstract boolean isBuildable();

    /**
     * Gets the read-only view of all the builds.
     * 
     * @return never null. The first entry is the latest build.
     */
    @Exported(name="allBuilds",visibility=-2)
    @WithBridgeMethods(List.class)
    public RunList<RunT> getBuilds() {
        return RunList.fromRuns(_getRuns().values());
    }

    /**
     * Gets the read-only view of the recent builds.
     *
     * @since 1.485
     */
    @Exported(name="builds")
    public RunList<RunT> getNewBuilds() {
        return getBuilds().limit(100);
    }

    /**
     * Obtains all the {@link Run}s whose build numbers matches the given {@link RangeSet}.
     */
    public synchronized List<RunT> getBuilds(RangeSet rs) {
        List<RunT> builds = new LinkedList<RunT>();

        for (Range r : rs.getRanges()) {
            for (RunT b = getNearestBuild(r.start); b!=null && b.getNumber()<r.end; b=b.getNextBuild()) {
                builds.add(b);
            }
        }

        return builds;
    }

    /**
     * Gets all the builds in a map.
     */
    public SortedMap<Integer, RunT> getBuildsAsMap() {
        return Collections.unmodifiableSortedMap(_getRuns());
    }

    /**
     * Looks up a build by its ID.
     * @see LazyBuildMixIn#getBuild
     */
    public RunT getBuild(String id) {
        for (RunT r : _getRuns().values()) {
            if (r.getId().equals(id))
                return r;
        }
        return null;
    }

    /**
     * @param n
     *            The build number.
     * @return null if no such build exists.
     * @see Run#getNumber()
     * @see LazyBuildMixIn#getBuildByNumber
     */
    public RunT getBuildByNumber(int n) {
        return _getRuns().get(n);
    }

    /**
     * Obtains a list of builds, in the descending order, that are within the specified time range [start,end).
     *
     * @return can be empty but never null.
     * @deprecated
     *      as of 1.372. Should just do {@code getBuilds().byTimestamp(s,e)} to avoid code bloat in {@link Job}.
     */
    @WithBridgeMethods(List.class)
    @Deprecated
    public RunList<RunT> getBuildsByTimestamp(long start, long end) {
        return getBuilds().byTimestamp(start,end);
    }

    @CLIResolver
    public RunT getBuildForCLI(@Argument(required=true,metaVar="BUILD#",usage="Build number") String id) throws CmdLineException {
        try {
            int n = Integer.parseInt(id);
            RunT r = getBuildByNumber(n);
            if (r==null)
                throw new CmdLineException(null, "No such build '#"+n+"' exists");
            return r;
        } catch (NumberFormatException e) {
            throw new CmdLineException(null, id+ "is not a number");
        }
    }

    /**
     * Gets the youngest build #m that satisfies <tt>n&lt;=m</tt>.
     * 
     * This is useful when you'd like to fetch a build but the exact build might
     * be already gone (deleted, rotated, etc.)
     * @see LazyBuildMixIn#getNearestBuild
     */
    public RunT getNearestBuild(int n) {
        SortedMap<Integer, ? extends RunT> m = _getRuns().headMap(n - 1); // the map should
                                                                          // include n, so n-1
        if (m.isEmpty())
            return null;
        return m.get(m.lastKey());
    }

    /**
     * Gets the latest build #m that satisfies <tt>m&lt;=n</tt>.
     * 
     * This is useful when you'd like to fetch a build but the exact build might
     * be already gone (deleted, rotated, etc.)
     * @see LazyBuildMixIn#getNearestOldBuild
     */
    public RunT getNearestOldBuild(int n) {
        SortedMap<Integer, ? extends RunT> m = _getRuns().tailMap(n);
        if (m.isEmpty())
            return null;
        return m.get(m.firstKey());
    }

    @Override
    public Object getDynamic(String token, StaplerRequest req,
            StaplerResponse rsp) {
        try {
            // try to interpret the token as build number
            return getBuildByNumber(Integer.valueOf(token));
        } catch (NumberFormatException e) {
            // try to map that to widgets
            for (Widget w : getWidgets()) {
                if (w.getUrlName().equals(token))
                    return w;
            }

            // is this a permalink?
            for (Permalink p : getPermalinks()) {
                if(p.getId().equals(token))
                    return p.resolve(this);
            }

            return super.getDynamic(token, req, rsp);
        }
    }

    /**
     * Directory for storing {@link Run} records.
     * <p>
     * Some {@link Job}s may not have backing data store for {@link Run}s, but
     * those {@link Job}s that use file system for storing data should use this
     * directory for consistency.
     * 
     * @see RunMap
     */
    public File getBuildDir() {
        Jenkins j = Jenkins.getInstance();
        if (j == null) {
            return new File(getRootDir(), "builds");
        }
        return j.getBuildDirFor(this);
    }

    /**
     * Gets all the runs.
     * 
     * The resulting map must be treated immutable (by employing copy-on-write
     * semantics.) The map is descending order, with newest builds at the top.
     * @see LazyBuildMixIn#_getRuns
     */
    protected abstract SortedMap<Integer, ? extends RunT> _getRuns();

    /**
     * Called from {@link Run} to remove it from this job.
     * 
     * The files are deleted already. So all the callee needs to do is to remove
     * a reference from this {@link Job}.
     * @see LazyBuildMixIn#removeRun
     */
    protected abstract void removeRun(RunT run);

    /**
     * Returns the last build.
     * @see LazyBuildMixIn#getLastBuild
     */
    @Exported
    @QuickSilver
    public RunT getLastBuild() {
        SortedMap<Integer, ? extends RunT> runs = _getRuns();

        if (runs.isEmpty())
            return null;
        return runs.get(runs.firstKey());
    }

    /**
     * Returns the oldest build in the record.
     * @see LazyBuildMixIn#getFirstBuild
     */
    @Exported
    @QuickSilver
    public RunT getFirstBuild() {
        SortedMap<Integer, ? extends RunT> runs = _getRuns();

        if (runs.isEmpty())
            return null;
        return runs.get(runs.lastKey());
    }

    /**
     * Returns the last successful build, if any. Otherwise null. A successful build
     * would include either {@link Result#SUCCESS} or {@link Result#UNSTABLE}.
     * 
     * @see #getLastStableBuild()
     */
    @Exported
    @QuickSilver
    public RunT getLastSuccessfulBuild() {
        return (RunT)Permalink.LAST_SUCCESSFUL_BUILD.resolve(this);
    }

    /**
     * Returns the last build that was anything but stable, if any. Otherwise null.
     * @see #getLastSuccessfulBuild
     */
    @Exported
    @QuickSilver
    public RunT getLastUnsuccessfulBuild() {
        return (RunT)Permalink.LAST_UNSUCCESSFUL_BUILD.resolve(this);
    }

    /**
     * Returns the last unstable build, if any. Otherwise null.
     * @see #getLastSuccessfulBuild
     */
    @Exported
    @QuickSilver
    public RunT getLastUnstableBuild() {
        return (RunT)Permalink.LAST_UNSTABLE_BUILD.resolve(this);
    }

    /**
     * Returns the last stable build, if any. Otherwise null.
     * @see #getLastSuccessfulBuild
     */
    @Exported
    @QuickSilver
    public RunT getLastStableBuild() {
        return (RunT)Permalink.LAST_STABLE_BUILD.resolve(this);
    }

    /**
     * Returns the last failed build, if any. Otherwise null.
     */
    @Exported
    @QuickSilver
    public RunT getLastFailedBuild() {
        return (RunT)Permalink.LAST_FAILED_BUILD.resolve(this);
    }

    /**
     * Returns the last completed build, if any. Otherwise null.
     */
    @Exported
    @QuickSilver
    public RunT getLastCompletedBuild() {
        RunT r = getLastBuild();
        while (r != null && r.isBuilding())
            r = r.getPreviousBuild();
        return r;
    }
    
    /**
     * Returns the last 'numberOfBuilds' builds with a build result >= 'threshold'
     * 
     * @return a list with the builds. May be smaller than 'numberOfBuilds' or even empty
     *   if not enough builds satisfying the threshold have been found. Never null.
     */
    public List<RunT> getLastBuildsOverThreshold(int numberOfBuilds, Result threshold) {
        
        List<RunT> result = new ArrayList<RunT>(numberOfBuilds);
        
        RunT r = getLastBuild();
        while (r != null && result.size() < numberOfBuilds) {
            if (!r.isBuilding() && 
                 (r.getResult() != null && r.getResult().isBetterOrEqualTo(threshold))) {
                result.add(r);
            }
            r = r.getPreviousBuild();
        }
        
        return result;
    }
    
    /**
     * Returns candidate build for calculating the estimated duration of the current run.
     * 
     * Returns the 3 last successful (stable or unstable) builds, if there are any.
     * Failing to find 3 of those, it will return up to 3 last unsuccessful builds.
     * 
     * In any case it will not go more than 6 builds into the past to avoid costly build loading.
     */
    @SuppressWarnings("unchecked")
    protected List<RunT> getEstimatedDurationCandidates() {
        List<RunT> candidates = new ArrayList<RunT>(3);
        RunT lastSuccessful = getLastSuccessfulBuild();
        int lastSuccessfulNumber = -1;
        if (lastSuccessful != null) {
            candidates.add(lastSuccessful);
            lastSuccessfulNumber = lastSuccessful.getNumber();
        }

        int i = 0;
        RunT r = getLastBuild();
        List<RunT> fallbackCandidates = new ArrayList<RunT>(3);
        while (r != null && candidates.size() < 3 && i < 6) {
            if (!r.isBuilding() && r.getResult() != null && r.getNumber() != lastSuccessfulNumber) {
                Result result = r.getResult();
                if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
                    candidates.add(r);
                } else if (result.isCompleteBuild()) {
                    fallbackCandidates.add(r);
                }
            }
            i++;
            r = r.getPreviousBuild();
        }
        
        while (candidates.size() < 3) {
            if (fallbackCandidates.isEmpty())
                break;
            RunT run = fallbackCandidates.remove(0);
            candidates.add(run);
        }
        
        return candidates;
    }
    
    public long getEstimatedDuration() {
        List<RunT> builds = getEstimatedDurationCandidates();
        
        if(builds.isEmpty())     return -1;

        long totalDuration = 0;
        for (RunT b : builds) {
            totalDuration += b.getDuration();
        }
        if(totalDuration==0) return -1;

        return Math.round((double)totalDuration / builds.size());
    }

    /**
     * Gets all the {@link Permalink}s defined for this job.
     *
     * @return never null
     */
    public PermalinkList getPermalinks() {
        // TODO: shall we cache this?
        PermalinkList permalinks = new PermalinkList(Permalink.BUILTIN);
        for (PermalinkProjectAction ppa : getActions(PermalinkProjectAction.class)) {
            permalinks.addAll(ppa.getPermalinks());
        }
        return permalinks;
    }
    
    @Override public ContextMenu doChildrenContextMenu(StaplerRequest request, StaplerResponse response) throws Exception {
        // not sure what would be really useful here. This needs more thoughts.
        // for the time being, I'm starting with permalinks
        ContextMenu menu = new ContextMenu();
        for (Permalink p : getPermalinks()) {
            if (p.resolve(this) != null) {
                menu.add(p.getId(), p.getDisplayName());
            }
        }
        return menu;
    }

    /**
     * Used as the color of the status ball for the project.
     */
    @Exported(visibility = 2, name = "color")
    public BallColor getIconColor() {
        RunT lastBuild = getLastBuild();
        while (lastBuild != null && lastBuild.hasntStartedYet())
            lastBuild = lastBuild.getPreviousBuild();

        if (lastBuild != null)
            return lastBuild.getIconColor();
        else
            return BallColor.NOTBUILT;
    }

    /**
     * Get the current health report for a job.
     * 
     * @return the health report. Never returns null
     */
    public HealthReport getBuildHealth() {
        List<HealthReport> reports = getBuildHealthReports();
        return reports.isEmpty() ? new HealthReport() : reports.get(0);
    }

    @Exported(name = "healthReport")
    public List<HealthReport> getBuildHealthReports() {
        List<HealthReport> reports = new ArrayList<HealthReport>();
        RunT lastBuild = getLastBuild();

        if (lastBuild != null && lastBuild.isBuilding()) {
            // show the previous build's report until the current one is
            // finished building.
            lastBuild = lastBuild.getPreviousBuild();
        }

        // check the cache
        if (cachedBuildHealthReportsBuildNumber != null
                && cachedBuildHealthReports != null
                && lastBuild != null
                && cachedBuildHealthReportsBuildNumber.intValue() == lastBuild
                        .getNumber()) {
            reports.addAll(cachedBuildHealthReports);
        } else if (lastBuild != null) {
            for (HealthReportingAction healthReportingAction : lastBuild
                    .getActions(HealthReportingAction.class)) {
                final HealthReport report = healthReportingAction
                        .getBuildHealth();
                if (report != null) {
                    if (report.isAggregateReport()) {
                        reports.addAll(report.getAggregatedReports());
                    } else {
                        reports.add(report);
                    }
                }
            }
            final HealthReport report = getBuildStabilityHealthReport();
            if (report != null) {
                if (report.isAggregateReport()) {
                    reports.addAll(report.getAggregatedReports());
                } else {
                    reports.add(report);
                }
            }

            Collections.sort(reports);

            // store the cache
            cachedBuildHealthReportsBuildNumber = lastBuild.getNumber();
            cachedBuildHealthReports = new ArrayList<HealthReport>(reports);
        }

        return reports;
    }

    private HealthReport getBuildStabilityHealthReport() {
        // we can give a simple view of build health from the last five builds
        int failCount = 0;
        int totalCount = 0;
        RunT i = getLastBuild();
        while (totalCount < 5 && i != null) {
            switch (i.getIconColor()) {
            case BLUE:
            case YELLOW:
                // failCount stays the same
                totalCount++;
                break;
            case RED:
                failCount++;
                totalCount++;
                break;

            default:
                // do nothing as these are inconclusive statuses
                break;
            }
            i = i.getPreviousBuild();
        }
        if (totalCount > 0) {
            int score = (int) ((100.0 * (totalCount - failCount)) / totalCount);

            Localizable description;
            if (failCount == 0) {
                description = Messages._Job_NoRecentBuildFailed();
            } else if (totalCount == failCount) {
                // this should catch the case where totalCount == 1
                // as failCount must be between 0 and totalCount
                // and we can't get here if failCount == 0
                description = Messages._Job_AllRecentBuildFailed();
            } else {
                description = Messages._Job_NOfMFailed(failCount, totalCount);
            }
            return new HealthReport(score, Messages._Job_BuildStability(description));
        }
        return null;
    }

    //
    //
    // actions
    //
    //
    /**
     * Accepts submission from the configuration page.
     */
    @RequirePOST
    public synchronized void doConfigSubmit(StaplerRequest req,
            StaplerResponse rsp) throws IOException, ServletException, FormException {
        checkPermission(CONFIGURE);

        description = req.getParameter("description");

        JSONObject json = req.getSubmittedForm();

        try {
            setDisplayName(json.optString("displayNameOrNull"));

            if (json.optBoolean("logrotate"))
                logRotator = req.bindJSON(BuildDiscarder.class, json.optJSONObject("buildDiscarder"));
            else
                logRotator = null;

            DescribableList<JobProperty<?>, JobPropertyDescriptor> t = new DescribableList<JobProperty<?>, JobPropertyDescriptor>(NOOP,getAllProperties());
            JSONObject jsonProperties = json.optJSONObject("properties");
            if (jsonProperties != null) {
              t.rebuild(req,jsonProperties,JobPropertyDescriptor.getPropertyDescriptors(Job.this.getClass()));
            } else {
              t.clear();
            }
            properties.clear();
            for (JobProperty p : t) {
                p.setOwner(this);
                properties.add(p);
            }

            submit(req, rsp);

            save();
            ItemListener.fireOnUpdated(this);

            String newName = req.getParameter("name");
            final ProjectNamingStrategy namingStrategy = Jenkins.getInstance().getProjectNamingStrategy();
            if (newName != null && !newName.equals(name)) {
                // check this error early to avoid HTTP response splitting.
                Jenkins.checkGoodName(newName);
                namingStrategy.checkName(newName);
                if (FormApply.isApply(req)) {
                    FormApply.applyResponse("notificationBar.show(" + QuotedStringTokenizer.quote(Messages.Job_you_must_use_the_save_button_if_you_wish()) + ",notificationBar.WARNING)").generateResponse(req, rsp, null);
                } else {
                    rsp.sendRedirect("rename?newName=" + URLEncoder.encode(newName, "UTF-8"));
                }
            } else {
                if(namingStrategy.isForceExistingJobs()){
                    namingStrategy.checkName(name);
                }
                FormApply.success(".").generateResponse(req, rsp, null);
            }
        } catch (JSONException e) {
            Logger.getLogger(Job.class.getName()).log(Level.WARNING, "failed to parse " + json, e);
            sendError(e, req, rsp);
        }
    }

    /**
     * Derived class can override this to perform additional config submission
     * work.
     */
    protected void submit(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException, FormException {
    }

    /**
     * Accepts and serves the job description
     */
    public void doDescription(StaplerRequest req, StaplerResponse rsp)
            throws IOException {
        if (req.getMethod().equals("GET")) {
            //read
            rsp.setContentType("text/plain;charset=UTF-8");
            rsp.getWriter().write(Util.fixNull(this.getDescription()));
            return;
        }
        if (req.getMethod().equals("POST")) {
            checkPermission(CONFIGURE);

            // submission
            if (req.getParameter("description") != null) {
                this.setDescription(req.getParameter("description"));
                rsp.sendError(SC_NO_CONTENT);
                return;
            }
        }

        // huh?
        rsp.sendError(SC_BAD_REQUEST);
    }

    /**
     * Returns the image that shows the current buildCommand status.
     */
    public void doBuildStatus(StaplerRequest req, StaplerResponse rsp)
            throws IOException {
        rsp.sendRedirect2(req.getContextPath() + "/images/48x48/" + getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return getIconColor().getImage();
    }

    public String getBuildStatusIconClassName() {
        return getIconColor().getIconClassName();
    }

    public Graph getBuildTimeGraph() {
        return new Graph(getLastBuildTime(),500,400) {
            @Override
            protected JFreeChart createGraph() {
                class ChartLabel implements Comparable<ChartLabel> {
                    final Run run;

                    public ChartLabel(Run r) {
                        this.run = r;
                    }

                    public int compareTo(ChartLabel that) {
                        return this.run.number - that.run.number;
                    }

                    @Override
                    public boolean equals(Object o) {
                        // HUDSON-2682 workaround for Eclipse compilation bug
                        // on (c instanceof ChartLabel)
                        if (o == null || !ChartLabel.class.isAssignableFrom( o.getClass() ))  {
                            return false;
                        }
                        ChartLabel that = (ChartLabel) o;
                        return run == that.run;
                    }

                    public Color getColor() {
                        // TODO: consider gradation. See
                        // http://www.javadrive.jp/java2d/shape/index9.html
                        Result r = run.getResult();
                        if (r == Result.FAILURE)
                            return ColorPalette.RED;
                        else if (r == Result.UNSTABLE)
                            return ColorPalette.YELLOW;
                        else if (r == Result.ABORTED || r == Result.NOT_BUILT)
                            return ColorPalette.GREY;
                        else
                            return ColorPalette.BLUE;
                    }

                    @Override
                    public int hashCode() {
                        return run.hashCode();
                    }

                    @Override
                    public String toString() {
                        String l = run.getDisplayName();
                        if (run instanceof Build) {
                            String s = ((Build) run).getBuiltOnStr();
                            if (s != null)
                                l += ' ' + s;
                        }
                        return l;
                    }

                }

                DataSetBuilder<String, ChartLabel> data = new DataSetBuilder<String, ChartLabel>();
                for (Run r : getNewBuilds()) {
                    if (r.isBuilding())
                        continue;
                    data.add(((double) r.getDuration()) / (1000 * 60), "min",
                            new ChartLabel(r));
                }

                final CategoryDataset dataset = data.build();

                final JFreeChart chart = ChartFactory.createStackedAreaChart(null, // chart
                                                                                    // title
                        null, // unused
                        Messages.Job_minutes(), // range axis label
                        dataset, // data
                        PlotOrientation.VERTICAL, // orientation
                        false, // include legend
                        true, // tooltips
                        false // urls
                        );

                chart.setBackgroundPaint(Color.white);

                final CategoryPlot plot = chart.getCategoryPlot();

                // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
                plot.setBackgroundPaint(Color.WHITE);
                plot.setOutlinePaint(null);
                plot.setForegroundAlpha(0.8f);
                // plot.setDomainGridlinesVisible(true);
                // plot.setDomainGridlinePaint(Color.white);
                plot.setRangeGridlinesVisible(true);
                plot.setRangeGridlinePaint(Color.black);

                CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
                plot.setDomainAxis(domainAxis);
                domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
                domainAxis.setLowerMargin(0.0);
                domainAxis.setUpperMargin(0.0);
                domainAxis.setCategoryMargin(0.0);

                final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                ChartUtil.adjustChebyshev(dataset, rangeAxis);
                rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

                StackedAreaRenderer ar = new StackedAreaRenderer2() {
                    @Override
                    public Paint getItemPaint(int row, int column) {
                        ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
                        return key.getColor();
                    }

                    @Override
                    public String generateURL(CategoryDataset dataset, int row,
                            int column) {
                        ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                        return String.valueOf(label.run.number);
                    }

                    @Override
                    public String generateToolTip(CategoryDataset dataset, int row,
                            int column) {
                        ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                        return label.run.getDisplayName() + " : "
                                + label.run.getDurationString();
                    }
                };
                plot.setRenderer(ar);

                // crop extra space around the graph
                plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

                return chart;
            }
        };
    }

    private Calendar getLastBuildTime() {
        final RunT lastBuild = getLastBuild();
        if (lastBuild ==null) {
            final GregorianCalendar neverBuiltCalendar = new GregorianCalendar();
            neverBuiltCalendar.setTimeInMillis(0);
            return neverBuiltCalendar;
        }
        return lastBuild.getTimestamp();
    }

    /**
     * Renames this job.
     */
    @RequirePOST
    public/* not synchronized. see renameTo() */void doDoRename(
            StaplerRequest req, StaplerResponse rsp) throws IOException,
            ServletException {

        if (!hasPermission(CONFIGURE)) {
            // rename is essentially delete followed by a create
            checkPermission(CREATE);
            checkPermission(DELETE);
        }

        String newName = req.getParameter("newName");
        Jenkins.checkGoodName(newName);

        if (isBuilding()) {
            // redirect to page explaining that we can't rename now
            rsp.sendRedirect("rename?newName=" + URLEncoder.encode(newName, "UTF-8"));
            return;
        }

        renameTo(newName);
        // send to the new job page
        // note we can't use getUrl() because that would pick up old name in the
        // Ancestor.getUrl()
        rsp.sendRedirect2("../" + newName);
    }

    public void doRssAll(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        rss(req, rsp, " all builds", getBuilds());
    }

    public void doRssFailed(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        rss(req, rsp, " failed builds", getBuilds().failureOnly());
    }

    private void rss(StaplerRequest req, StaplerResponse rsp, String suffix,
            RunList runs) throws IOException, ServletException {
        RSS.forwardToRss(getDisplayName() + suffix, getUrl(), runs.newBuilds(),
                Run.FEED_ADAPTER, req, rsp);
    }

    /**
     * Returns the {@link ACL} for this object.
     * We need to override the identical method in AbstractItem because we won't
     * call getACL(Job) otherwise (single dispatch)
     */
    @Override
    public ACL getACL() {
        return Jenkins.getInstance().getAuthorizationStrategy().getACL(this);
    }

    public BuildTimelineWidget getTimeline() {
        return new BuildTimelineWidget(getBuilds());
    }

    private final static HexStringConfidentialKey SERVER_COOKIE = new HexStringConfidentialKey(Job.class,"serverCookie",16);
}
