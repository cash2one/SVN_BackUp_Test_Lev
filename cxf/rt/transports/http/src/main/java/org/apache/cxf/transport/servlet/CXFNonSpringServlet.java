/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;

public class CXFNonSpringServlet extends AbstractHTTPServlet {
    private static final long serialVersionUID = -2437897227486327166L;
   
    protected Bus bus;
    private DestinationRegistry destinationRegistry;
    private boolean globalRegistry;
    private ServletController controller;
    private ClassLoader loader;
    private boolean loadBus = true;
    
    public CXFNonSpringServlet() {
    }

    public CXFNonSpringServlet(DestinationRegistry destinationRegistry) {
        this(destinationRegistry, true);
    }
    public CXFNonSpringServlet(DestinationRegistry destinationRegistry,
                               boolean loadBus) {
        this.destinationRegistry = destinationRegistry;
        this.globalRegistry = destinationRegistry != null;
        this.loadBus = loadBus;
    }
    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        if (this.bus == null && loadBus) {
            loadBus(sc);
        }
        if (this.bus != null) {
            loader = initClassLoader();
            ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
            resourceManager.addResourceResolver(new ServletContextResourceResolver(sc.getServletContext()));
            if (destinationRegistry == null) {
                this.destinationRegistry = getDestinationRegistryFromBus();
            }
        }

        this.controller = createServletController(sc);
        finalizeServletInit(sc);
    }

    protected ClassLoader initClassLoader() {
        return bus.getExtension(ClassLoader.class);
    }
    
    protected DestinationRegistry getDestinationRegistryFromBus() {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            DestinationFactory df = dfm
                .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            if (df instanceof HTTPTransportFactory) {
                HTTPTransportFactory transportFactory = (HTTPTransportFactory)df;
                return transportFactory.getRegistry();
            }
        } catch (BusException e) {
            // why are we throwing a busexception if the DF isn't found?
        }
        return null;
    }

    protected void loadBus(ServletConfig sc) {
        this.bus = BusFactory.newInstance().createBus();
    }
    
    private ServletController createServletController(ServletConfig servletConfig) {
        HttpServlet serviceListGeneratorServlet = 
            new ServiceListGeneratorServlet(destinationRegistry, bus);
        ServletController newController =
            new ServletController(destinationRegistry,
                                  servletConfig,
                                  serviceListGeneratorServlet);        
        return newController;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            try {
                if (loader != null) {
                    origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
                }
                if (bus != null) {
                    origBus = BusFactory.getAndSetThreadDefaultBus(bus);
                }
                HttpServletRequest httpRequest = (HttpServletRequest)request;
                if (controller.filter(new HttpServletRequestFilter(httpRequest,
                                                                   super.getServletName()),
                                      (HttpServletResponse)response)) {
                    return;
                }
            } finally {
                if (origBus != bus) {
                    BusFactory.setThreadDefaultBus(origBus);
                }
                if (origLoader != null) {
                    origLoader.reset();
                }
            }
        }
        chain.doFilter(request, response);
    }
    @Override
    protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            if (bus != null) {
                origBus = BusFactory.getAndSetThreadDefaultBus(bus);
            }
            controller.invoke(request, response);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    public void destroy() {
        if (!globalRegistry) {
            for (String path : destinationRegistry.getDestinationsPaths()) {
                // clean up the destination in case the destination itself can 
                // no longer access the registry later
                AbstractHTTPDestination dest = destinationRegistry.getDestinationForPath(path);
                synchronized (dest) {
                    destinationRegistry.removeDestination(path);
                    dest.releaseRegistry();
                }
            }
            destinationRegistry = null;
        }
        destroyBus();
    }
    
    public void destroyBus() {
        if (bus != null) {
            bus.shutdown(true);
            bus = null;
        }
    }
    
    private static class HttpServletRequestFilter extends HttpServletRequestWrapper {
        private String filterName;
        private String servletPath;
        private String pathInfo;
        public HttpServletRequestFilter(HttpServletRequest request, String filterName) {
            super(request);
            this.filterName = filterName;
        }
        
        @Override
        public String getServletPath() {
            if (servletPath == null) {
                try {
                    Method m = ServletContext.class.getMethod("getFilterRegistration", new Class[] {String.class});
                    Object registration = m.invoke(super.getServletContext(), new Object[]{filterName});
                    if (registration != null) {
                        m = registration.getClass().getMethod("getUrlPatternMappings", new Class[] {});
                        Collection<String> mappings = 
                            CastUtils.cast((Collection<?>)m.invoke(registration, new Object[]{}));
                        if (!mappings.isEmpty()) {
                            String mapping = mappings.iterator().next();
                            if (mapping.endsWith("/*")) {
                                servletPath = mapping.substring(0, mapping.length() - 2);
                            }
                        }
                    }
                } catch (Throwable ex) {
                    // ignore
                }
                if (servletPath == null) {
                    servletPath = "";
                }
            }
            
            return servletPath;
        }
        
        @Override
        public String getPathInfo() {
            if (pathInfo == null) {
                pathInfo = super.getPathInfo();
                if (pathInfo == null) {
                    pathInfo = getRequestURI();
                }
                String prefix = super.getContextPath() + this.getServletPath();
                if (pathInfo.startsWith(prefix)) {
                    pathInfo = pathInfo.substring(prefix.length());
                }
            }
            return pathInfo;
        }
    }
}
