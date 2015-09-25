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
package org.apache.cxf.rs.security.oauth2.common;

import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * Server Access Token representation
 */
public abstract class ServerAccessToken extends AccessToken {
    private static final long serialVersionUID = 638776204861456064L;
    
    private String grantType;
    private Client client;
    private List<OAuthPermission> scopes = new LinkedList<OAuthPermission>();
    private UserSubject subject;
    private String audience;
    
    protected ServerAccessToken() {
        
    }
    
    protected ServerAccessToken(Client client, 
                                        String tokenType,
                                        String tokenKey,
                                        long expiresIn) {
        this(client, tokenType, tokenKey, expiresIn, OAuthUtils.getIssuedAt());
    }
    
    protected ServerAccessToken(Client client, 
                                String tokenType,
                                String tokenKey,
                                long expiresIn, 
                                long issuedAt) {
        super(tokenType, tokenKey, expiresIn, issuedAt);
        this.client = client;
    }
    
    protected ServerAccessToken(ServerAccessToken token, String key) {    
        super(token.getTokenType(), 
             key, 
             token.getExpiresIn(), 
             token.getIssuedAt(),
             token.getRefreshToken(),
             token.getParameters());
        this.client = token.getClient();
        this.grantType = token.getGrantType();
        this.scopes = token.getScopes();
        this.audience = token.getAudience();
        this.subject = token.getSubject();
    }

    /**
     * Returns the Client associated with this token
     * @return the client
     */
    public Client getClient() {
        return client;
    }

    public void setClient(Client c) {
        this.client = c;
    }
    
    /**
     * Returns a list of opaque permissions/scopes
     * @return the scopes
     */
    public List<OAuthPermission> getScopes() {
        return scopes;
    }

    /**
     * Sets a list of opaque permissions/scopes
     * @param scopes the scopes
     */
    public void setScopes(List<OAuthPermission> scopes) {
        this.scopes = scopes;
    }
    
    /**
     * Sets a subject capturing the login name 
     * the end user used to login to the resource server
     * when authorizing a given client request
     * @param subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Returns a subject capturing the login name 
     * the end user used to login to the resource server
     * when authorizing a given client request
     * @return UserSubject
     */
    public UserSubject getSubject() {
        return subject;
    }

    /**
     * Sets the grant type which was used to obtain the access token
     * @param grantType the grant type
     */
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    /**
     * Returns the grant type which was used to obtain the access token
     * @return the grant type
     */
    public String getGrantType() {
        return grantType;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    protected static ServerAccessToken validateTokenType(ServerAccessToken token, String expectedType) {
        if (!token.getTokenType().equals(expectedType)) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
        }
        return token;
    }
}
