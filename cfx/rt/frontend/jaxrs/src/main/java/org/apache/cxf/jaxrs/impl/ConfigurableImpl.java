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

package org.apache.cxf.jaxrs.impl;

import java.util.Map;

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public class ConfigurableImpl<C extends Configurable<C>> implements Configurable<C> {
    private ConfigurationImpl config;
    private C configurable;
    private Class<?>[] supportedProviderClasses;
    public ConfigurableImpl(C configurable, RuntimeType rt, Class<?>[] supportedProviderClasses) {
        this(configurable, supportedProviderClasses, new ConfigurationImpl(rt));
    }
    
    public ConfigurableImpl(C configurable, Class<?>[] supportedProviderClasses, Configuration config) {
        this(configurable, supportedProviderClasses);
        this.config = config instanceof ConfigurationImpl 
            ? (ConfigurationImpl)config : new ConfigurationImpl(config, supportedProviderClasses);
    }
    
    private ConfigurableImpl(C configurable, Class<?>[] supportedProviderClasses) {
        this.configurable = configurable;
        this.supportedProviderClasses = supportedProviderClasses;
    }
    
    protected C getConfigurable() {
        return configurable;
    }
    
    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public C property(String name, Object value) {
        config.setProperty(name, value);
        return configurable;
    }

    @Override
    public C register(Object provider) {
        return register(provider, AnnotationUtils.getBindingPriority(provider.getClass()));
    }

    @Override
    public C register(Object provider, int bindingPriority) {
        return doRegister(provider, bindingPriority, supportedProviderClasses);
    }
    
    @Override
    public C register(Object provider, Class<?>... contracts) {
        return doRegister(provider, Priorities.USER, contracts);
    }
    
    @Override
    public C register(Object provider, Map<Class<?>, Integer> contracts) {
        if (provider instanceof Feature) {
            Feature feature = (Feature)provider;
            boolean enabled = feature.configure(new FeatureContextImpl(this));
            config.setFeature(feature, enabled);
            
            return configurable;
        }
        config.register(provider, contracts);
        return configurable;
    }
    
    @Override
    public C register(Class<?> providerClass) {
        return register(providerClass, AnnotationUtils.getBindingPriority(providerClass));
    }

    @Override
    public C register(Class<?> providerClass, int bindingPriority) {
        return doRegister(ConfigurationImpl.createProvider(providerClass), 
                          bindingPriority, supportedProviderClasses);
    }

    @Override
    public C register(Class<?> providerClass, Class<?>... contracts) {
        return doRegister(providerClass, Priorities.USER, contracts);
    }

    @Override
    public C register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
        return register(ConfigurationImpl.createProvider(providerClass), contracts);
    }
    
    private C doRegister(Object provider, int bindingPriority, Class<?>... contracts) {
        return register(provider, ConfigurationImpl.initContractsMap(bindingPriority, contracts));
    }

    public static class FeatureContextImpl implements FeatureContext {
        private Configurable<?> cfg;
        public FeatureContextImpl(Configurable<?> cfg) {
            this.cfg = cfg;
        }
        
        @Override
        public Configuration getConfiguration() {
            return cfg.getConfiguration();
        }

        @Override
        public FeatureContext property(String name, Object value) {
            cfg.property(name, value);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls) {
            cfg.register(cls);
            return this;
        }

        @Override
        public FeatureContext register(Object obj) {
            cfg.register(obj);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls, int priority) {
            cfg.register(cls, priority);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls, Class<?>... contract) {
            cfg.register(cls, contract);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls, Map<Class<?>, Integer> map) {
            cfg.register(cls, map);
            return this;
        }

        @Override
        public FeatureContext register(Object obj, int priority) {
            cfg.register(obj, priority);
            return this;
        }

        @Override
        public FeatureContext register(Object obj, Class<?>... contract) {
            cfg.register(obj, contract);
            return this;
        }

        @Override
        public FeatureContext register(Object obj, Map<Class<?>, Integer> map) {
            cfg.register(obj, map);
            return this;
        } 
        
    }
}
