/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.server.provider;

import org.wildfly.clustering.server.CacheServiceNameProvider;
import org.wildfly.clustering.spi.CacheGroupServiceName;

/**
 * Provides the service name of a {@link org.wildfly.clustering.provider.ServiceProviderRegistrationFactory}.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistryServiceNameProvider extends CacheServiceNameProvider {

    /**
     * @param containerName
     * @param cacheName
     */
    public ServiceProviderRegistryServiceNameProvider(String containerName, String cacheName) {
        super(CacheGroupServiceName.SERVICE_PROVIDER_REGISTRY, containerName, cacheName);
    }
}
