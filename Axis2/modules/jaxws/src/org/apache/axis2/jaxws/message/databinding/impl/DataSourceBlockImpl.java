/*
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

package org.apache.axis2.jaxws.message.databinding.impl;

import org.apache.axiom.attachments.impl.BufferUtils;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.datasource.SourceDataSource;
import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.message.databinding.DataSourceBlock;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.impl.BlockImpl;
import org.apache.axis2.jaxws.message.util.Reader2Writer;
import org.apache.axis2.jaxws.utility.ConvertUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * SourceBlock
 * <p/>
 * Block containing a business object that is a javax.activation.DataSource
 * <p/>
 */
public class DataSourceBlockImpl extends BlockImpl implements DataSourceBlock {

    private static final Log log = LogFactory.getLog(DataSourceBlockImpl.class);

    /**
     * Constructor called from factory
     *
     * @param busObject
     * @param qName
     * @param factory
     */
    DataSourceBlockImpl(DataSource busObject, QName qName, BlockFactory factory)
            throws WebServiceException {
        super(busObject, null, qName, factory);
        // Check validity of DataSource
        if (!(busObject instanceof DataSource)) {
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("SourceNotSupported", busObject.getClass().getName()));
        }
    }


    /**
     * Constructor called from factory
     *
     * @param reader
     * @param qName
     * @param factory
     */
    public DataSourceBlockImpl(OMElement omElement, QName qName, BlockFactory factory) {
        super(omElement, null, qName, factory);
    }

    protected Object _getBOFromReader(XMLStreamReader reader, Object busContext) throws XMLStreamException, WebServiceException {
        Reader2Writer r2w = new Reader2Writer(reader);
        try {
            return new ByteArrayDataSource(r2w.getAsString(), "application/octet-stream");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }

    }

    public OMElement getOMElement() throws XMLStreamException, WebServiceException {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace("", "");
        return factory.createOMElement(this, "dummy", ns);
    }

    @Override
    protected Object _getBOFromOM(OMElement omElement, Object busContext)
        throws XMLStreamException, WebServiceException {
        Object busObject;
        
        // Shortcut to get business object from existing data source
        if (omElement instanceof OMSourcedElement) {
            OMDataSource ds = ((OMSourcedElement) omElement).getDataSource();
            if (ds instanceof SourceDataSource) {
                return ((SourceDataSource) ds).getObject();
            }
        }
        
        // If the message is a fault, there are some special gymnastics that we have to do
        // to get this working for all of the handler scenarios.  
        boolean hasFault = false;
        if ((parent != null && parent.isFault()) || 
            omElement.getQName().getLocalPart().equals(SOAP11Constants.SOAPFAULT_LOCAL_NAME)) {
            hasFault = true;
        }
        
        // Transform reader into business object
        if (!hasFault) {
            busObject = ((OMSourcedElement)omElement).getDataSource();
        }
        else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            omElement.serialize(baos);
            busObject = new ByteArrayDataSource(baos.toByteArray(), "UTF-8");
        }
        return busObject;
    }

    @Override
    protected XMLStreamReader _getReaderFromBO(Object busObj, Object busContext)
            throws XMLStreamException, WebServiceException {
        try {
            if (busObj instanceof DataSource) {
                XMLInputFactory f = StAXUtils.getXMLInputFactory();

                XMLStreamReader reader = f.createXMLStreamReader(((DataSource)busObj).getInputStream());
                StAXUtils.releaseXMLInputFactory(f);
                return reader;
            }
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("SourceNotSupported", busObject.getClass().getName()));
        } catch (Exception e) {
            String className = (busObj == null) ? "none" : busObj.getClass().getName();
            throw ExceptionFactory
                    .makeWebServiceException(Messages.getMessage("SourceReadErr", className), e);
        }
    }
    
    public void serialize(OutputStream output, OMOutputFormat format) throws XMLStreamException {
        try {
            BufferUtils.inputStream2OutputStream(((DataSource)busObject).getInputStream(), output);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    protected void _outputFromBO(Object busObject, Object busContext, XMLStreamWriter writer)
            throws XMLStreamException, WebServiceException {
        // There is no fast way to output the Source to a writer, so get the reader
        // and pass use the default reader->writer.
        if (log.isDebugEnabled()) {
            log.debug("Start _outputFromBO");
        }
        XMLStreamReader reader = _getReaderFromBO(busObject, busContext);
        if (log.isDebugEnabled()) {
            log.debug("Obtained reader=" + reader);
        }
        _outputFromReader(reader, writer);
        if (log.isDebugEnabled()) {
            log.debug("End _outputReaderFromBO");
        }
        // REVIEW Should we call close() on the Source ?
    }


    @Override
    protected Object _getBOFromBO(Object busObject, Object busContext, boolean consume) {
        if (consume) {
            return busObject;
        } else {
            // TODO Missing Impl
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("SourceMissingSupport", busObject.getClass().getName()));
        }
    }

    public boolean isElementData() {
        return false;  // The source could be a text or element etc.
    }

    /**
     * Return the class for this name
     * @return Class
     */
    private static Class forName(final String className) throws ClassNotFoundException {
        // NOTE: This method must remain private because it uses AccessController
        Class cl = null;
        try {
            cl = (Class)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                            return Class.forName(className);
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw (ClassNotFoundException)e.getException();
        }

        return cl;
    }
    
    
    public void close() {
        return; // Nothing to close
    }

    public InputStream getXMLInputStream(String encoding) throws UnsupportedEncodingException {
        try {
            byte[] bytes = (byte[]) 
                ConvertUtils.convert(getBusinessObject(false), byte[].class);
            return new ByteArrayInputStream(bytes);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Object getObject() {
        try {
            return getBusinessObject(false);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public boolean isDestructiveRead() {
        return true;
    }

    public boolean isDestructiveWrite() {
        return true;
    }


    public byte[] getXMLBytes(String encoding) throws UnsupportedEncodingException {
        if (log.isDebugEnabled()) {
            log.debug("Start getXMLBytes");
        }
        byte[] bytes = null;
        try {
            bytes = (byte[]) 
                ConvertUtils.convert(getBusinessObject(false), byte[].class);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
        if (log.isDebugEnabled()) {
            log.debug("End getXMLBytes");
        }
        return bytes;
    }
}