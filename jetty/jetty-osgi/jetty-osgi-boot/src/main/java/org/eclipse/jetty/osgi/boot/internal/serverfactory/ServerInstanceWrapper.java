//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.osgi.boot.internal.serverfactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.AppProvider;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.bindings.StandardStarter;
import org.eclipse.jetty.deploy.bindings.StandardStopper;
import org.eclipse.jetty.osgi.boot.BundleContextProvider;
import org.eclipse.jetty.osgi.boot.BundleWebAppProvider;
import org.eclipse.jetty.osgi.boot.JettyBootstrapActivator;
import org.eclipse.jetty.osgi.boot.OSGiDeployer;
import org.eclipse.jetty.osgi.boot.OSGiServerConstants;
import org.eclipse.jetty.osgi.boot.OSGiUndeployer;
import org.eclipse.jetty.osgi.boot.ServiceContextProvider;
import org.eclipse.jetty.osgi.boot.ServiceWebAppProvider;
import org.eclipse.jetty.osgi.boot.internal.webapp.LibExtClassLoaderHelper;
import org.eclipse.jetty.osgi.boot.utils.BundleFileLocatorHelperFactory;
import org.eclipse.jetty.osgi.boot.utils.FakeURLClassLoader;
import org.eclipse.jetty.osgi.boot.utils.TldBundleDiscoverer;
import org.eclipse.jetty.osgi.boot.utils.Util;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * ServerInstanceWrapper
 * 
 *  Configures and starts a jetty Server instance. 
 */
public class ServerInstanceWrapper
{

    /**
     * The value of this property points to the parent director of the jetty.xml
     * configuration file currently executed. Everything is passed as a URL to
     * support the case where the bundle is zipped.
     */
    public static final String PROPERTY_THIS_JETTY_XML_FOLDER_URL = "this.jetty.xml.parent.folder.url";
    
    
    private static Collection<TldBundleDiscoverer> __containerTldBundleDiscoverers = new ArrayList<TldBundleDiscoverer>();

    private static Logger LOG = Log.getLogger(ServerInstanceWrapper.class.getName());
    
 

    private final String _managedServerName;

    /**
     * The managed jetty server
     */
    private Server _server;

    private ContextHandlerCollection _ctxtCollection;

    /**
     * This is the class loader that should be the parent classloader of any
     * webapp classloader. It is in fact the _libExtClassLoader with a trick to
     * let the TldScanner find the jars where the tld files are.
     */
    private ClassLoader _commonParentClassLoaderForWebapps;

    private DeploymentManager _deploymentManager;
    
    
    
    /* ------------------------------------------------------------ */
    public static void addContainerTldBundleDiscoverer (TldBundleDiscoverer tldBundleDiscoverer)
    {
        __containerTldBundleDiscoverers.add(tldBundleDiscoverer);
    }
    
    /* ------------------------------------------------------------ */
    public static Collection<TldBundleDiscoverer> getContainerTldBundleDiscoverers()
    {
        return __containerTldBundleDiscoverers;
    }
    
 

    
    /* ------------------------------------------------------------ */
    public static Server configure(Server server, List<URL> jettyConfigurations, Dictionary props) throws Exception
    {
       
        if (jettyConfigurations == null || jettyConfigurations.isEmpty()) { return server; }
        
        Map<String, Object> id_map = new HashMap<String, Object>();
        if (server != null)
        {
            //Put in a mapping for the id "Server" and the name of the server as the instance being configured
            id_map.put("Server", server);
            id_map.put((String)props.get(OSGiServerConstants.MANAGED_JETTY_SERVER_NAME), server);
        }

        Map<String, String> properties = new HashMap<String, String>();
        if (props != null)
        {
            Enumeration<Object> en = props.keys();
            while (en.hasMoreElements())
            {
                Object key = en.nextElement();
                Object value = props.get(key);
                String keyStr = String.valueOf(key);
                String valStr = String.valueOf(value);
                properties.put(keyStr, valStr);
                if (server != null) server.setAttribute(keyStr, valStr);
            }
        }

        for (URL jettyConfiguration : jettyConfigurations)
        {
        	try(Resource r = Resource.newResource(jettyConfiguration))
        	{
        		// Execute a Jetty configuration file
        		if (!r.exists())
        		{
        			LOG.warn("File does not exist "+r);
        			throw new IllegalStateException("No such jetty server config file: "+r);
        		}

        		XmlConfiguration config = new XmlConfiguration(r.getURL());

        		config.getIdMap().putAll(id_map);
        		config.getProperties().putAll(properties);

        		// #334062 compute the URL of the folder that contains the
        		// conf file and set it as a property so we can compute relative paths
        		// from it.
        		String urlPath = jettyConfiguration.toString();
        		int lastSlash = urlPath.lastIndexOf('/');
        		if (lastSlash > 4)
        		{
        			urlPath = urlPath.substring(0, lastSlash);
        			config.getProperties().put(PROPERTY_THIS_JETTY_XML_FOLDER_URL, urlPath);
        		}

        		Object o = config.configure();
        		if (server == null)
        			server = (Server)o;

        		id_map = config.getIdMap();
        	}
        	catch (Exception e)
        	{
        		LOG.warn("Configuration error in " + jettyConfiguration);
        		throw e;
        	}
        }

        return server;
    }
    
    
    
    
    /* ------------------------------------------------------------ */
    public ServerInstanceWrapper(String managedServerName)
    {
        _managedServerName = managedServerName;
    }

    /* ------------------------------------------------------------ */ 
    public String getManagedServerName()
    {
        return _managedServerName;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * The classloader that should be the parent classloader for each webapp
     * deployed on this server.
     * 
     * @return the classloader
     */
    public ClassLoader getParentClassLoaderForWebapps()
    {
        return _commonParentClassLoaderForWebapps;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * @return The deployment manager registered on this server.
     */
    public DeploymentManager getDeploymentManager()
    {
        return _deploymentManager;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * @return The app provider registered on this server.
     */
    public Server getServer()
    {
        return _server;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return The collection of context handlers
     */
    public ContextHandlerCollection getContextHandlerCollection()
    {
        return _ctxtCollection;
    }
    
    /* ------------------------------------------------------------ */
    public void start(Server server, Dictionary props) throws Exception
    {
        _server = server;
        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        try
        {
            // passing this bundle's classloader as the context classloader
            // makes sure there is access to all the jetty's bundles
            ClassLoader libExtClassLoader = null;
            String sharedURLs = (String) props.get(OSGiServerConstants.MANAGED_JETTY_SHARED_LIB_FOLDER_URLS);

            List<File> shared = sharedURLs != null ? extractFiles(sharedURLs) : null;
            libExtClassLoader = LibExtClassLoaderHelper.createLibExtClassLoader(shared, null,JettyBootstrapActivator.class.getClassLoader());

            if (LOG.isDebugEnabled()) LOG.debug("LibExtClassLoader = "+libExtClassLoader);
            
            Thread.currentThread().setContextClassLoader(libExtClassLoader);

            String jettyConfigurationUrls = (String) props.get(OSGiServerConstants.MANAGED_JETTY_XML_CONFIG_URLS);
            List<URL> jettyConfigurations = jettyConfigurationUrls != null ? Util.fileNamesAsURLs(jettyConfigurationUrls, Util.DEFAULT_DELIMS) : null;
            
            _server = configure(server, jettyConfigurations, props);

            init();
            
            //if support for jsp is enabled, we need to convert locations of bundles that contain tlds into urls.
            //these are tlds that we want jasper to treat as if they are on the container's classpath. Web bundles
            //can use the Require-TldBundle MANIFEST header to name other tld-containing bundles that should be regarded
            //as on the webapp classpath.
            if (!__containerTldBundleDiscoverers.isEmpty())
            {
                Set<URL> urls = new HashSet<URL>();
                //discover bundles with tlds that need to be on the container's classpath as URLs
                for (TldBundleDiscoverer d:__containerTldBundleDiscoverers)
                {
                    URL[] list = d.getUrlsForBundlesWithTlds(_deploymentManager, BundleFileLocatorHelperFactory.getFactory().getHelper());
                    if (list != null)
                    {
                        for (URL u:list)
                            urls.add(u);
                    }
                }
                _commonParentClassLoaderForWebapps =  new FakeURLClassLoader(libExtClassLoader, urls.toArray(new URL[urls.size()]));
            }
            else
                _commonParentClassLoaderForWebapps = libExtClassLoader;

            
            if (LOG.isDebugEnabled()) LOG.debug("common classloader = "+_commonParentClassLoaderForWebapps);

            server.start();
        }
        catch (Exception e)
        {
            if (server != null)
            {
                try
                {
                    server.stop();
                }
                catch (Exception x)
                {
                    LOG.ignore(x);
                }
            }
            throw e;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(contextCl);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void stop()
    {
        try
        {
            if (_server.isRunning())
            {
                _server.stop();
            }
        }
        catch (Exception e)
        {
            LOG.warn(e);
        }
    }
    
    
   
    
    /* ------------------------------------------------------------ */
    /**
     * Must be called after the server is configured. 
     * 
     * It is assumed the server has already been configured with the ContextHandlerCollection structure.
     * 
     */
    private void init()
    {
        // Get the context handler
        _ctxtCollection = (ContextHandlerCollection) _server.getChildHandlerByClass(ContextHandlerCollection.class);

        if (_ctxtCollection == null) 
            throw new IllegalStateException("ERROR: No ContextHandlerCollection configured in Server");
        
        List<String> providerClassNames = new ArrayList<String>();
        
        // get a deployerManager and some providers
        Collection<DeploymentManager> deployers = _server.getBeans(DeploymentManager.class);
        if (deployers != null && !deployers.isEmpty())
        {
            _deploymentManager = deployers.iterator().next();
            
            for (AppProvider provider : _deploymentManager.getAppProviders())
            {
               providerClassNames.add(provider.getClass().getName());
            }
        }
        else
        {
            //add some kind of default
            _deploymentManager = new DeploymentManager();
            _deploymentManager.setContexts(_ctxtCollection);
            _server.addBean(_deploymentManager);
        }

        _deploymentManager.setUseStandardBindings(false);
        List<AppLifeCycle.Binding> deploymentLifeCycleBindings = new ArrayList<AppLifeCycle.Binding>();
        deploymentLifeCycleBindings.add(new OSGiDeployer(this));
        deploymentLifeCycleBindings.add(new StandardStarter());
        deploymentLifeCycleBindings.add(new StandardStopper());
        deploymentLifeCycleBindings.add(new OSGiUndeployer(this));
        _deploymentManager.setLifeCycleBindings(deploymentLifeCycleBindings);
        
        if (!providerClassNames.contains(BundleWebAppProvider.class.getName()))
        {
            // create it on the fly with reasonable default values.
            try
            {
                BundleWebAppProvider webAppProvider = new BundleWebAppProvider(this);
                _deploymentManager.addAppProvider(webAppProvider);
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }

        if (!providerClassNames.contains(ServiceWebAppProvider.class.getName()))
        {
            // create it on the fly with reasonable default values.
            try
            {
                ServiceWebAppProvider webAppProvider = new ServiceWebAppProvider(this);
                _deploymentManager.addAppProvider(webAppProvider);
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }

        if (!providerClassNames.contains(BundleContextProvider.class.getName()))
        {
            try
            {
                BundleContextProvider contextProvider = new BundleContextProvider(this);
                _deploymentManager.addAppProvider(contextProvider);
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }

        if (!providerClassNames.contains(ServiceContextProvider.class.getName()))
        {
            try
            {
                ServiceContextProvider contextProvider = new ServiceContextProvider(this);
                _deploymentManager.addAppProvider(contextProvider);
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }
    }
    

  
    
    
    /* ------------------------------------------------------------ */
    /**
     * Get the folders that might contain jars for the legacy J2EE shared
     * libraries
     */
    private List<File> extractFiles(String propertyValue)
    {
        StringTokenizer tokenizer = new StringTokenizer(propertyValue, ",;", false);
        List<File> files = new ArrayList<File>();
        while (tokenizer.hasMoreTokens())
        {
            String tok = tokenizer.nextToken();
            try
            {
                URL url = new URL(tok);
                url = BundleFileLocatorHelperFactory.getFactory().getHelper().getFileURL(url);
                if (url.getProtocol().equals("file"))
                {
                    Resource res = Resource.newResource(url);
                    File folder = res.getFile();
                    if (folder != null)
                    {
                        files.add(folder);
                    }
                }
            }
            catch (Throwable mfe)
            {
                LOG.warn(mfe);
            }
        }
        return files;
    }

}
