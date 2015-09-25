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

import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;

public class AbstractJweDecryptingFilter {
    private JweDecryptionProvider decryption;
    private String defaultMediaType;
    protected JweDecryptionOutput decrypt(InputStream is) throws IOException {
        JweCompactConsumer jwe = new JweCompactConsumer(new String(IOUtils.readBytesFromStream(is), "UTF-8"));
        JweDecryptionProvider theDecryptor = getInitializedDecryptionProvider(jwe.getJweHeaders());
        JweDecryptionOutput out = new JweDecryptionOutput(jwe.getJweHeaders(), jwe.getDecryptedContent(theDecryptor));
        validateHeaders(out.getHeaders());
        return out;
    }

    protected void validateHeaders(JweHeaders headers) {
        // complete
    }
    public void setDecryptionProvider(JweDecryptionProvider decryptor) {
        this.decryption = decryptor;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider(JweHeaders headers) {
        if (decryption != null) {
            return decryption;    
        } 
        return JweUtils.loadDecryptionProvider(headers, true);
    }
    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

}
