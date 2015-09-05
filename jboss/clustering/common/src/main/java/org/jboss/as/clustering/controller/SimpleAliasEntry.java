/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Simple alias entry that converts address using the alias and target addresses with which it was initialized/registered.
 * @author Paul Ferraro
 */
public class SimpleAliasEntry extends AliasEntry {

    public SimpleAliasEntry(ManagementResourceRegistration registration) {
        super(registration);
    }

    @Override
    public PathAddress convertToTargetAddress(PathAddress address) {
        PathAddress target = this.getTargetAddress();
        List<PathElement> result = new ArrayList<>(address.size());
        for (int i = 0; i < address.size(); ++i) {
            PathElement element = address.getElement(i);
            if (i < target.size()) {
                PathElement targetElement = target.getElement(i);
                result.add(targetElement.isWildcard() ? PathElement.pathElement(targetElement.getKey(), element.getValue()) : targetElement);
            } else {
                result.add(element);
            }
        }
        return PathAddress.pathAddress(result);
    }
}