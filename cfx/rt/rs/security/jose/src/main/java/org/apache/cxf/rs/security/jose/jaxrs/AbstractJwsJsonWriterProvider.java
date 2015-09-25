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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public class AbstractJwsJsonWriterProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJwsJsonWriterProvider.class);
    private static final String RSSEC_SIGNATURE_OUT_LIST_PROPS = "rs.security.signature.out.list.properties";
    private static final String RSSEC_SIGNATURE_LIST_PROPS = "rs.security.signature.list.properties";
    
    private List<JwsSignatureProvider> sigProviders;
    
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        setSignatureProviders(Collections.singletonList(signatureProvider));
    }
    public void setSignatureProviders(List<JwsSignatureProvider> signatureProviders) {
        this.sigProviders = signatureProviders;
    }
    
    protected List<JwsSignatureProvider> getInitializedSigProviders() {
        if (sigProviders != null) {
            return sigProviders;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        Object propLocsProp = 
            MessageUtils.getContextualProperty(m, RSSEC_SIGNATURE_OUT_LIST_PROPS, RSSEC_SIGNATURE_LIST_PROPS);
        if (propLocsProp == null) {
            LOG.warning("JWS JSON init properties resource is not identified");
            throw new JwsException(JwsException.Error.NO_INIT_PROPERTIES);
        }
        List<String> propLocs = null;
        if (propLocsProp instanceof String) {
            String[] props = ((String)propLocsProp).split(",");
            propLocs = Arrays.asList(props);
        } else {
            propLocs = CastUtils.cast((List<?>)propLocsProp);
        }
        List<JwsSignatureProvider> theSigProviders = new LinkedList<JwsSignatureProvider>();
        for (String propLoc : propLocs) {
            theSigProviders.addAll(JwsUtils.loadSignatureProviders(propLoc, m));
        }
        return theSigProviders;
    }
    protected void writeJws(JwsJsonProducer p, OutputStream os) 
        throws IOException {
        byte[] bytes = StringUtils.toBytesUTF8(p.getJwsJsonSignedDocument());
        IOUtils.copy(new ByteArrayInputStream(bytes), os);
    }
    
}
