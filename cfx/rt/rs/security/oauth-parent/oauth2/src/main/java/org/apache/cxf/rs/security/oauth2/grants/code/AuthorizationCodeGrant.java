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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;



/**
 * Base Authorization Code Grant representation, captures the code 
 * and the redirect URI this code has been returned to, visible to the client
 */
public class AuthorizationCodeGrant implements AccessTokenGrant {
    private static final long serialVersionUID = -3738825769770411453L;
    private String code;
    private String redirectUri;
    
    public AuthorizationCodeGrant() {
        
    }
    
    public AuthorizationCodeGrant(String code) {
        this.code = code;
    }
    
    public AuthorizationCodeGrant(String code, URI uri) {
        this.code = code;
        redirectUri = uri.toString();
    }

    /**
     * Sets the redirect URI, if set then the client is expected to 
     * include the same URI during the access token request
     * @param redirectUri redirect URI
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Gets the redirect URI
     * @return the redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Gets the authorization code
     * @return the code
     */
    public String getCode() {
        return code;
    }
    
    public void setCode(String c) {
        this.code = c;
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return OAuthConstants.AUTHORIZATION_CODE_GRANT;
    }

    /**
     * {@inheritDoc}
     */
    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putSingle(OAuthConstants.GRANT_TYPE, OAuthConstants.AUTHORIZATION_CODE_GRANT);
        map.putSingle(OAuthConstants.AUTHORIZATION_CODE_VALUE, code);
        if (redirectUri != null) {
            map.putSingle(OAuthConstants.REDIRECT_URI, redirectUri);
        }
        return map;
    }

}
