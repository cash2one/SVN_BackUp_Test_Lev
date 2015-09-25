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
package org.apache.cxf.rs.security.oauth2.provider;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public abstract class AbstractOAuthJoseJwtProducer extends AbstractJoseJwtProducer {
    private boolean encryptWithClientSecret;
    private boolean signWithClientSecret;
    
    protected String processJwt(JwtToken jwt, String clientSecret) {
        return processJwt(jwt, 
                         getInitializedEncryptionProvider(clientSecret),
                         getInitializedSignatureProvider(clientSecret));
    }
    
    protected JwsSignatureProvider getInitializedSignatureProvider(String clientSecret) {
        if (signWithClientSecret) {
            byte[] hmac = CryptoUtils.decodeSequence(clientSecret);
            return JwsUtils.getHmacSignatureProvider(hmac, SignatureAlgorithm.HS256);
        }
        return null;
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider(String clientSecret) {
        if (encryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(clientSecret);
            return JweUtils.getDirectKeyJweEncryption(key, ContentAlgorithm.A128GCM);
        }
        return null;
    }

    public void setEncryptWithClientSecret(boolean encryptWithClientSecret) {
        if (signWithClientSecret) {
            throw new SecurityException();
        }
        this.encryptWithClientSecret = encryptWithClientSecret;
    }
    public void setSignWithClientSecret(boolean signWithClientSecret) {
        if (encryptWithClientSecret) {
            throw new SecurityException();
        }
        this.signWithClientSecret = signWithClientSecret;
    }
    public boolean isSignWithClientSecret() {
        return signWithClientSecret;
    }
    public boolean isEncryptWithClientSecret() {
        return encryptWithClientSecret;
    }
}
