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
package org.apache.cxf.rs.security.oidc.rp;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.client.ClientCodeRequestFilter;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.IdToken;

public class OidcClientCodeRequestFilter extends ClientCodeRequestFilter {
    
    private static final String ACR_PARAMETER = "acr_values";
    private static final String PROMPT_PARAMETER = "prompt";
    private static final String MAX_AGE_PARAMETER = "max_age";
    private static final List<String> PROMPTS = Arrays.asList("none", "consent", "login", "select_account");
    private IdTokenReader idTokenReader;
    private UserInfoClient userInfoClient;
    private List<String> authenticationContextRef;
    private String promptLogin;
    private Long maxAgeOffset;
    private String claims;
    private String claimsLocales;
    
    public void setAuthenticationContextRef(String acr) {
        this.authenticationContextRef = Arrays.asList(StringUtils.split(acr, " "));
    }
    @Override
    protected ClientTokenContext createTokenContext(ContainerRequestContext rc, 
                                                    ClientAccessToken at,
                                                    MultivaluedMap<String, String> state) {
        if (rc.getSecurityContext() instanceof OidcSecurityContext) {
            return ((OidcSecurityContext)rc.getSecurityContext()).getOidcContext();
        }
        OidcClientTokenContextImpl ctx = new OidcClientTokenContextImpl();
        if (at != null) {
            IdToken idToken = idTokenReader.getIdToken(at, getConsumer());
            // Validate the properties set up at the redirection time.
            validateIdToken(idToken, state);
            
            ctx.setIdToken(idToken);
            if (userInfoClient != null) {
                ctx.setUserInfo(userInfoClient.getUserInfo(at, 
                                                           ctx.getIdToken(),
                                                           getConsumer()));
            }
            rc.setSecurityContext(new OidcSecurityContext(ctx));
        }
        
        return ctx;
    }
    @Override
    protected MultivaluedMap<String, String> toCodeRequestState(ContainerRequestContext rc, UriInfo ui) {
        MultivaluedMap<String, String> state = super.toCodeRequestState(rc, ui);
        if (maxAgeOffset != null) {
            state.putSingle(MAX_AGE_PARAMETER, Long.toString(System.currentTimeMillis() + maxAgeOffset));
        }
        return state;
    }
    private void validateIdToken(IdToken idToken, MultivaluedMap<String, String> state) {
        
        String nonce = state.getFirst("nonce");
        String tokenNonce = idToken.getNonce();
        if (nonce != null && (tokenNonce == null || !nonce.equals(tokenNonce))) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        if (maxAgeOffset != null) {
            Long authTime = Long.parseLong(state.getFirst(MAX_AGE_PARAMETER));
            Long tokenAuthTime = idToken.getAuthenticationTime();
            if (tokenAuthTime > authTime) {
                throw ExceptionUtils.toNotAuthorizedException(null, null);
            }
        }
        
        String acr = idToken.getAuthenticationContextRef();
        // Skip the check if the acr is not set given it is a voluntary claim
        if (acr != null && authenticationContextRef != null && !authenticationContextRef.contains(acr)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        
    }
    public void setIdTokenReader(IdTokenReader idTokenReader) {
        this.idTokenReader = idTokenReader;
    }
    public void setUserInfoClient(UserInfoClient userInfoClient) {
        this.userInfoClient = userInfoClient; 
    }
    
    @Override
    protected void checkSecurityContextStart(ContainerRequestContext rc) {
        SecurityContext sc = rc.getSecurityContext();
        if (!(sc instanceof OidcSecurityContext) && sc.getUserPrincipal() != null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }
    @Override
    protected void setAdditionalCodeRequestParams(UriBuilder ub, MultivaluedMap<String, String> redirectState) {
        if (claims != null) {
            ub.queryParam("claims", claims);
        }
        if (claimsLocales != null) {
            ub.queryParam("claims_locales", claimsLocales);
        }
        if (redirectState != null) {
            if (redirectState.getFirst(IdToken.NONCE_CLAIM) != null) {
                ub.queryParam(IdToken.NONCE_CLAIM, redirectState.getFirst(IdToken.NONCE_CLAIM));
            }
            if (redirectState.getFirst(MAX_AGE_PARAMETER) != null) {
                ub.queryParam(MAX_AGE_PARAMETER, redirectState.getFirst(MAX_AGE_PARAMETER));
            }
        }
        if (authenticationContextRef != null) {
            ub.queryParam(ACR_PARAMETER, authenticationContextRef);
        }
        if (promptLogin != null) {
            ub.queryParam(PROMPT_PARAMETER, promptLogin);
        }
    }
    
    public void setPromptLogin(String promptLogin) {
        if (PROMPTS.contains(promptLogin)) {
            this.promptLogin = promptLogin;
        } else {
            throw new IllegalArgumentException("Illegal prompt value");
        }
    }
    public void setMaxAgeOffset(Long maxAgeOffset) {
        this.maxAgeOffset = maxAgeOffset;
    }
    public void setClaims(String claims) {
        this.claims = claims;
    }
    public void setClaimsLocales(String claimsLocales) {
        this.claimsLocales = claimsLocales;
    }
}
