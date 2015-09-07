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

package org.eclipse.jetty.osgi.boot;

import org.eclipse.jetty.osgi.boot.internal.serverfactory.DefaultJettyAtJettyHomeHelper;
import org.eclipse.jetty.osgi.boot.internal.serverfactory.JettyServerServiceTracker;
import org.eclipse.jetty.osgi.boot.internal.webapp.BundleWatcher;
import org.eclipse.jetty.osgi.boot.internal.webapp.ServiceWatcher;
import org.eclipse.jetty.osgi.boot.utils.internal.PackageAdminServiceTracker;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

/**
 * JettyBootstrapActivator
 * <p>
 * Bootstrap jetty and publish a default Server instance as an OSGi service.
 * <p>
 * Listen for other Server instances to be published as services and support them as deployment targets.
 * <p>
 * Listen for Bundles to be activated, and deploy those that represent webapps/ContextHandlers to one of the known Server instances.
 */
public class JettyBootstrapActivator implements BundleActivator
{
    private static final Logger LOG = Log.getLogger(JettyBootstrapActivator.class);
    
    private static JettyBootstrapActivator INSTANCE = null;

    public static JettyBootstrapActivator getInstance()
    {
        return INSTANCE;
    }

    private ServiceRegistration _registeredServer;

    private ServiceTracker _contextHandlerTracker;
    
    private PackageAdminServiceTracker _packageAdminServiceTracker;

    private BundleTracker _webBundleTracker;

    private JettyServerServiceTracker _jettyServerServiceTracker;
    
    
    
    /* ------------------------------------------------------------ */
    /**
     * Setup a new jetty Server, registers it as a service. Setup the Service
     * tracker for the jetty ContextHandlers that are in charge of deploying the
     * webapps. Setup the BundleListener that supports the extender pattern for
     * the jetty ContextHandler.
     * 
     * @param context the bundle context
     */
    public void start(final BundleContext context) throws Exception
    {
        try {
        INSTANCE = this;

        // track other bundles and fragments attached to this bundle that we
        // should activate.
        _packageAdminServiceTracker = new PackageAdminServiceTracker(context);

        // track jetty Server instances that we should support as deployment targets
        _jettyServerServiceTracker = new JettyServerServiceTracker();
        context.addServiceListener(_jettyServerServiceTracker, "(objectclass=" + Server.class.getName() + ")");

        // Create a default jetty instance right now.
        Server defaultServer = DefaultJettyAtJettyHomeHelper.startJettyAtJettyHome(context);
        
        // track ContextHandler class instances and deploy them to one of the known Servers
        _contextHandlerTracker = new ServiceTracker(context, context.createFilter("(objectclass=" + ContextHandler.class.getName() + ")"), new ServiceWatcher());
        _contextHandlerTracker.open();

        //Create a bundle tracker to help deploy webapps and ContextHandlers
        BundleWatcher bundleTrackerCustomizer = new BundleWatcher();
        bundleTrackerCustomizer.setWaitForDefaultServer(defaultServer != null);
        _webBundleTracker =  new BundleTracker(context, Bundle.ACTIVE | Bundle.STOPPING, bundleTrackerCustomizer);
        bundleTrackerCustomizer.setBundleTracker(_webBundleTracker);
        bundleTrackerCustomizer.open();
        } catch (Exception e) { e.printStackTrace();}
    }
    
    
    
    /* ------------------------------------------------------------ */
    /**
     * Stop the activator.
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception
    {
        try
        {
            if (_webBundleTracker != null)
            {
                _webBundleTracker.close();
                _webBundleTracker = null;
            }
            if (_contextHandlerTracker != null)
            {
                _contextHandlerTracker.close();
                _contextHandlerTracker = null;
            }
            if (_jettyServerServiceTracker != null)
            {
                _jettyServerServiceTracker.stop();
                context.removeServiceListener(_jettyServerServiceTracker);
                _jettyServerServiceTracker = null;
            }
            if (_packageAdminServiceTracker != null)
            {
                _packageAdminServiceTracker.stop();
                context.removeServiceListener(_packageAdminServiceTracker);
                _packageAdminServiceTracker = null;
            }
            if (_registeredServer != null)
            {
                try
                {
                    _registeredServer.unregister();
                }
                catch (IllegalArgumentException ill)
                {
                    // already unregistered.
                }
                finally
                {
                    _registeredServer = null;
                }
            }
        }
        finally
        {
            INSTANCE = null;
        }
    }
}
