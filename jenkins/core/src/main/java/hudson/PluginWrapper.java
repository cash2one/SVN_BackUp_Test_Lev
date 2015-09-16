/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Yahoo! Inc., Erik Ramfelt, Tom Huybrechts
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
package hudson;

import hudson.PluginManager.PluginInstanceStore;
import hudson.model.Api;
import hudson.model.ModelObject;
import jenkins.YesNoMaybe;
import jenkins.model.Jenkins;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.util.VersionNumber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import static java.util.logging.Level.WARNING;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/**
 * Represents a Jenkins plug-in and associated control information
 * for Jenkins to control {@link Plugin}.
 *
 * <p>
 * A plug-in is packaged into a jar file whose extension is <tt>".jpi"</tt> (or <tt>".hpi"</tt> for backward compatibility),
 * A plugin needs to have a special manifest entry to identify what it is.
 *
 * <p>
 * At the runtime, a plugin has two distinct state axis.
 * <ol>
 *  <li>Enabled/Disabled. If enabled, Jenkins is going to use it
 *      next time Jenkins runs. Otherwise the next run will ignore it.
 *  <li>Activated/Deactivated. If activated, that means Jenkins is using
 *      the plugin in this session. Otherwise it's not.
 * </ol>
 * <p>
 * For example, an activated but disabled plugin is still running but the next
 * time it won't.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class PluginWrapper implements Comparable<PluginWrapper>, ModelObject {
    /**
     * {@link PluginManager} to which this belongs to.
     */
    public final PluginManager parent;

    /**
     * Plugin manifest.
     * Contains description of the plugin.
     */
    private final Manifest manifest;

    /**
     * {@link ClassLoader} for loading classes from this plugin.
     * Null if disabled.
     */
    public final ClassLoader classLoader;

    /**
     * Base URL for loading static resources from this plugin.
     * Null if disabled. The static resources are mapped under
     * <tt>CONTEXTPATH/plugin/SHORTNAME/</tt>.
     */
    public final URL baseResourceURL;

    /**
     * Used to control enable/disable setting of the plugin.
     * If this file exists, plugin will be disabled.
     */
    private final File disableFile;

    /**
     * Used to control the unpacking of the bundled plugin.
     * If a pin file exists, Jenkins assumes that the user wants to pin down a particular version
     * of a plugin, and will not try to overwrite it. Otherwise, it'll be overwritten
     * by a bundled copy, to ensure consistency across upgrade/downgrade.
     * @since 1.325
     */
    private final File pinFile;

    /**
     * A .jpi file, an exploded plugin directory, or a .jpl file.
     */
    private final File archive;

    /**
     * Short name of the plugin. The artifact Id of the plugin.
     * This is also used in the URL within Jenkins, so it needs
     * to remain stable even when the *.jpi file name is changed
     * (like Maven does.)
     */
    private final String shortName;

    /**
     * True if this plugin is activated for this session.
     * The snapshot of <tt>disableFile.exists()</tt> as of the start up.
     */
    private final boolean active;
    
    private boolean hasCycleDependency = false;

    private final List<Dependency> dependencies;
    private final List<Dependency> optionalDependencies;

    /**
     * Is this plugin bundled in jenkins.war?
     */
    /*package*/ boolean isBundled;

    @ExportedBean
    public static final class Dependency {
        @Exported
        public final String shortName;
        @Exported
        public final String version;
        @Exported
        public final boolean optional;

        public Dependency(String s) {
            int idx = s.indexOf(':');
            if(idx==-1)
                throw new IllegalArgumentException("Illegal dependency specifier "+s);
            this.shortName = s.substring(0,idx);
            this.version = s.substring(idx+1);
            
            boolean isOptional = false;
            String[] osgiProperties = s.split(";");
            for (int i = 1; i < osgiProperties.length; i++) {
                String osgiProperty = osgiProperties[i].trim();
                if (osgiProperty.equalsIgnoreCase("resolution:=optional")) {
                    isOptional = true;
                }
            }
            this.optional = isOptional;
        }

        @Override
        public String toString() {
            return shortName + " (" + version + ")";
        }        
    }

    /**
     * @param archive
     *      A .jpi archive file jar file, or a .jpl linked plugin.
     *  @param manifest
     *  	The manifest for the plugin
     *  @param baseResourceURL
     *  	A URL pointing to the resources for this plugin
     *  @param classLoader
     *  	a classloader that loads classes from this plugin and its dependencies
     *  @param disableFile
     *  	if this file exists on startup, the plugin will not be activated
     *  @param dependencies a list of mandatory dependencies
     *  @param optionalDependencies a list of optional dependencies
     */
    public PluginWrapper(PluginManager parent, File archive, Manifest manifest, URL baseResourceURL, 
			ClassLoader classLoader, File disableFile, 
			List<Dependency> dependencies, List<Dependency> optionalDependencies) {
        this.parent = parent;
		this.manifest = manifest;
		this.shortName = computeShortName(manifest, archive.getName());
		this.baseResourceURL = baseResourceURL;
		this.classLoader = classLoader;
		this.disableFile = disableFile;
        this.pinFile = new File(archive.getPath() + ".pinned");
		this.active = !disableFile.exists();
		this.dependencies = dependencies;
		this.optionalDependencies = optionalDependencies;
        this.archive = archive;
    }

    public String getDisplayName() {
        return StringUtils.removeStart(getLongName(), "Jenkins ");
    }

    public Api getApi() {
        return new Api(this);
    }

    /**
     * Returns the URL of the index page jelly script.
     */
    public URL getIndexPage() {
        // In the current impl dependencies are checked first, so the plugin itself
        // will add the last entry in the getResources result.
        URL idx = null;
        try {
            Enumeration<URL> en = classLoader.getResources("index.jelly");
            while (en.hasMoreElements())
                idx = en.nextElement();
        } catch (IOException ignore) { }
        // In case plugin has dependencies but is missing its own index.jelly,
        // check that result has this plugin's artifactId in it:
        return idx != null && idx.toString().contains(shortName) ? idx : null;
    }

    static String computeShortName(Manifest manifest, String fileName) {
        // use the name captured in the manifest, as often plugins
        // depend on the specific short name in its URLs.
        String n = manifest.getMainAttributes().getValue("Short-Name");
        if(n!=null)     return n;

        // maven seems to put this automatically, so good fallback to check.
        n = manifest.getMainAttributes().getValue("Extension-Name");
        if(n!=null)     return n;

        // otherwise infer from the file name, since older plugins don't have
        // this entry.
        return getBaseName(fileName);
    }

    @Exported
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<Dependency> getOptionalDependencies() {
        return optionalDependencies;
    }


    /**
     * Returns the short name suitable for URL.
     */
    @Exported
    public String getShortName() {
        return shortName;
    }

    /**
     * Gets the instance of {@link Plugin} contributed by this plugin.
     */
    public @CheckForNull Plugin getPlugin() {
        PluginInstanceStore pis = Jenkins.lookup(PluginInstanceStore.class);
        return pis != null ? pis.store.get(this) : null;
    }

    /**
     * Gets the URL that shows more information about this plugin.
     * @return
     *      null if this information is unavailable.
     * @since 1.283
     */
    @Exported
    public String getUrl() {
        // first look for the manifest entry. This is new in maven-hpi-plugin 1.30
        String url = manifest.getMainAttributes().getValue("Url");
        if(url!=null)      return url;

        // fallback to update center metadata
        UpdateSite.Plugin ui = getInfo();
        if(ui!=null)    return ui.wiki;

        return null;
    }
    
    

    @Override
    public String toString() {
        return "Plugin:" + getShortName();
    }

    /**
     * Returns a one-line descriptive name of this plugin.
     */
    @Exported
    public String getLongName() {
        String name = manifest.getMainAttributes().getValue("Long-Name");
        if(name!=null)      return name;
        return shortName;
    }

    /**
     * Does this plugin supports dynamic loading?
     */
    @Exported
    public YesNoMaybe supportsDynamicLoad() {
        String v = manifest.getMainAttributes().getValue("Support-Dynamic-Loading");
        if (v==null) return YesNoMaybe.MAYBE;
        return Boolean.parseBoolean(v) ? YesNoMaybe.YES : YesNoMaybe.NO;
    }

    /**
     * Returns the version number of this plugin
     */
    @Exported
    public String getVersion() {
        return getVersionOf(manifest);
    }

    private String getVersionOf(Manifest manifest) {
        String v = manifest.getMainAttributes().getValue("Plugin-Version");
        if(v!=null)      return v;

        // plugins generated before maven-hpi-plugin 1.3 should still have this attribute
        v = manifest.getMainAttributes().getValue("Implementation-Version");
        if(v!=null)      return v;

        return "???";
    }

    /**
     * Returns the version number of this plugin
     */
    public VersionNumber getVersionNumber() {
        return new VersionNumber(getVersion());
    }

    /**
     * Returns true if the version of this plugin is older than the given version.
     */
    public boolean isOlderThan(VersionNumber v) {
        try {
            return getVersionNumber().compareTo(v) < 0;
        } catch (IllegalArgumentException e) {
            // if we can't figure out our current version, it probably means it's very old,
            // since the version information is missing only from the very old plugins 
            return true;
        }
    }

    /**
     * Terminates the plugin.
     */
    public void stop() {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            try {
                LOGGER.log(Level.FINE, "Stopping {0}", shortName);
                plugin.stop();
            } catch (Throwable t) {
                LOGGER.log(WARNING, "Failed to shut down " + shortName, t);
            }
        } else {
            LOGGER.log(Level.FINE, "Could not find Plugin instance to stop for {0}", shortName);
        }
        // Work around a bug in commons-logging.
        // See http://www.szegedi.org/articles/memleak.html
        LogFactory.release(classLoader);
    }

    public void releaseClassLoader() {
        if (classLoader instanceof Closeable)
            try {
                ((Closeable) classLoader).close();
            } catch (IOException e) {
                LOGGER.log(WARNING, "Failed to shut down classloader",e);
            }
    }

    /**
     * Enables this plugin next time Jenkins runs.
     */
    public void enable() throws IOException {
        if(!disableFile.delete())
            throw new IOException("Failed to delete "+disableFile);
    }

    /**
     * Disables this plugin next time Jenkins runs.
     */
    public void disable() throws IOException {
        // creates an empty file
        OutputStream os = new FileOutputStream(disableFile);
        os.close();
    }

    /**
     * Returns true if this plugin is enabled for this session.
     */
    @Exported
    public boolean isActive() {
        return active && !hasCycleDependency();
    }
    
    public boolean hasCycleDependency(){
        return hasCycleDependency;
    }

    public void setHasCycleDependency(boolean hasCycle){
        hasCycleDependency = hasCycle;
    }
    
    @Exported
    public boolean isBundled() {
        return isBundled;
    }

    /**
     * If true, the plugin is going to be activated next time
     * Jenkins runs.
     */
    @Exported
    public boolean isEnabled() {
        return !disableFile.exists();
    }

    public Manifest getManifest() {
        return manifest;
    }

    public void setPlugin(Plugin plugin) {
        Jenkins.lookup(PluginInstanceStore.class).store.put(this,plugin);
        plugin.wrapper = this;
    }

    public String getPluginClass() {
        return manifest.getMainAttributes().getValue("Plugin-Class");
    }

    public boolean hasLicensesXml() {
        try {
            new URL(baseResourceURL,"WEB-INF/licenses.xml").openStream().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Makes sure that all the dependencies exist, and then accept optional dependencies
     * as real dependencies.
     *
     * @throws IOException
     *             thrown if one or several mandatory dependencies doesn't exists.
     */
    /*package*/ void resolvePluginDependencies() throws IOException {
        List<String> missingDependencies = new ArrayList<String>();
        // make sure dependencies exist
        for (Dependency d : dependencies) {
            if (parent.getPlugin(d.shortName) == null)
                missingDependencies.add(d.toString());
        }
        if (!missingDependencies.isEmpty())
            throw new IOException("Dependency "+Util.join(missingDependencies, ", ")+" doesn't exist");

        // add the optional dependencies that exists
        for (Dependency d : optionalDependencies) {
            if (parent.getPlugin(d.shortName) != null)
                dependencies.add(d);
        }
    }

    /**
     * If the plugin has {@link #getUpdateInfo() an update},
     * returns the {@link hudson.model.UpdateSite.Plugin} object.
     *
     * @return
     *      This method may return null &mdash; for example,
     *      the user may have installed a plugin locally developed.
     */
    public UpdateSite.Plugin getUpdateInfo() {
        UpdateCenter uc = Jenkins.getInstance().getUpdateCenter();
        UpdateSite.Plugin p = uc.getPlugin(getShortName());
        if(p!=null && p.isNewerThan(getVersion())) return p;
        return null;
    }
    
    /**
     * returns the {@link hudson.model.UpdateSite.Plugin} object, or null.
     */
    public UpdateSite.Plugin getInfo() {
        UpdateCenter uc = Jenkins.getInstance().getUpdateCenter();
        return uc.getPlugin(getShortName());
    }

    /**
     * Returns true if this plugin has update in the update center.
     *
     * <p>
     * This method is conservative in the sense that if the version number is incomprehensible,
     * it always returns false.
     */
    @Exported
    public boolean hasUpdate() {
        return getUpdateInfo()!=null;
    }
    
    @Exported
    public boolean isPinned() {
        return pinFile.exists();
    }

    /**
     * Returns true if this plugin is deleted.
     *
     * The plugin continues to function in this session, but in the next session it'll disappear.
     */
    @Exported
    public boolean isDeleted() {
        return !archive.exists();
    }

    /**
     * Sort by short name.
     */
    public int compareTo(PluginWrapper pw) {
        return shortName.compareToIgnoreCase(pw.shortName);
    }

    /**
     * returns true if backup of previous version of plugin exists
     */
    @Exported
    public boolean isDowngradable() {
        return getBackupFile().exists();
    }

    /**
     * Where is the backup file?
     */
    public File getBackupFile() {
        return new File(Jenkins.getInstance().getRootDir(),"plugins/"+getShortName() + ".bak");
    }

    /**
     * returns the version of the backed up plugin,
     * or null if there's no back up.
     */
    @Exported
    public String getBackupVersion() {
        File backup = getBackupFile();
        if (backup.exists()) {
            try {
                JarFile backupPlugin = new JarFile(backup);
                try {
                    return backupPlugin.getManifest().getMainAttributes().getValue("Plugin-Version");
                } finally {
                    backupPlugin.close();
                }
            } catch (IOException e) {
                LOGGER.log(WARNING, "Failed to get backup version from " + backup, e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Checks if this plugin is pinned and that's forcing us to use an older version than the bundled one.
     */
    public boolean isPinningForcingOldVersion() {
        if (!isPinned())    return false;

        Manifest bundled = Jenkins.getInstance().pluginManager.getBundledPluginManifest(getShortName());
        if (bundled==null)  return false;

        VersionNumber you = new VersionNumber(getVersionOf(bundled));
        VersionNumber me = getVersionNumber();

        return me.isOlderThan(you);
    }

//
//
// Action methods
//
//
    @RequirePOST
    public HttpResponse doMakeEnabled() throws IOException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        enable();
        return HttpResponses.ok();
    }

    @RequirePOST
    public HttpResponse doMakeDisabled() throws IOException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        disable();
        return HttpResponses.ok();
    }

    @RequirePOST
    public HttpResponse doPin() throws IOException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        new FileOutputStream(pinFile).close();
        return HttpResponses.ok();
    }

    @RequirePOST
    public HttpResponse doUnpin() throws IOException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        pinFile.delete();
        return HttpResponses.ok();
    }

    @RequirePOST
    public HttpResponse doDoUninstall() throws IOException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        archive.delete();
        return HttpResponses.redirectViaContextPath("/pluginManager/installed");   // send back to plugin manager
    }


    private static final Logger LOGGER = Logger.getLogger(PluginWrapper.class.getName());

    /**
     * Name of the plugin manifest file (to help find where we parse them.)
     */
    public static final String MANIFEST_FILENAME = "META-INF/MANIFEST.MF";
}
