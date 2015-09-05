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

package org.jboss.as.clustering.subsystem;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.Extension;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * Base class for clustering subsystem tests.
 * @author Paul Ferraro
 */
public abstract class ClusteringSubsystemTest extends AbstractSubsystemBaseTest {
    private final String path;

    protected ClusteringSubsystemTest(String name, Extension extension, String path) {
        super(name, extension);
        this.path = path;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(this.path);
    }

    /**
     * Need to override the XML comparison in the case where the input xsd and the output xsd differ.
     *
     * @param configId   the id of the xml configuration
     * @param original   the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     * @throws Exception
     */
    @Override
    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {

        final XMLStreamReader originalReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(original));
        final XMLStreamReader marshalledReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(marshalled));

        String originalNS = null;
        if (originalReader.next() == XMLStreamConstants.START_ELEMENT) {
            originalNS = originalReader.getNamespaceURI();
        }
        String marshalledNS = null;
        if (marshalledReader.next() == XMLStreamConstants.START_ELEMENT) {
            marshalledNS = marshalledReader.getNamespaceURI();
        }

        // only compare if namespace URIs are the same
        if (originalNS.equals(marshalledNS)) {
            compareXml(configId, original, marshalled, true);
        }
    }
}
