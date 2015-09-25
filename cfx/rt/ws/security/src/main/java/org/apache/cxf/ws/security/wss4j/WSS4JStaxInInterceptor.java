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
package org.apache.cxf.ws.security.wss4j;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSSPolicyException;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ConfigurationConverter;
import org.apache.wss4j.stax.WSSec;
import org.apache.wss4j.stax.ext.InboundWSSec;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.validate.Validator;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.AbstractSecuredElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;

public class WSS4JStaxInInterceptor extends AbstractWSS4JStaxInterceptor {
    
    public static final String SECURITY_PROCESSED = WSS4JStaxInInterceptor.class.getName() + ".DONE";
    
    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JStaxInInterceptor.class);
    
    public WSS4JStaxInInterceptor(WSSSecurityProperties securityProperties) {
        super(securityProperties);
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
    }
    
    public WSS4JStaxInInterceptor(Map<String, Object> props) {
        super(props);
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
    }
    
    public WSS4JStaxInInterceptor() {
        super();
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
    }

    public final boolean isGET(SoapMessage message) {
        String method = (String)message.get(SoapMessage.HTTP_REQUEST_METHOD);
        return "GET".equals(method) && message.getContent(XMLStreamReader.class) == null;
    }
    
    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        
        if (soapMessage.containsKey(SECURITY_PROCESSED) || isGET(soapMessage)) {
            return;
        }

        XMLStreamReader originalXmlStreamReader = soapMessage.getContent(XMLStreamReader.class);
        XMLStreamReader newXmlStreamReader;

        soapMessage.getInterceptorChain().add(new StaxSecurityContextInInterceptor());
        
        try {
            @SuppressWarnings("unchecked")
            List<SecurityEvent> requestSecurityEvents = 
                (List<SecurityEvent>) soapMessage.getExchange().get(SecurityEvent.class.getName() + ".out");
            
            WSSSecurityProperties secProps = createSecurityProperties();
            translateProperties(soapMessage, secProps);
            configureCallbackHandler(soapMessage, secProps);
            configureProperties(soapMessage, secProps);
            
            if (secProps.getActions() != null && secProps.getActions().size() > 0) {
                soapMessage.getInterceptorChain().add(new StaxActionInInterceptor(secProps.getActions()));
            }
            
            if (secProps.getAttachmentCallbackHandler() == null) {
                secProps.setAttachmentCallbackHandler(new AttachmentCallbackHandler(soapMessage));
            }
            
            final TokenStoreCallbackHandler callbackHandler = 
                new TokenStoreCallbackHandler(
                    secProps.getCallbackHandler(), TokenStoreUtils.getTokenStore(soapMessage)
                );
            secProps.setCallbackHandler(callbackHandler);

            setTokenValidators(secProps, soapMessage);
            secProps.setMsgContext(soapMessage);
            
            final List<SecurityEventListener> securityEventListeners = 
                configureSecurityEventListeners(soapMessage, secProps);
            
            boolean returnSecurityError = 
                MessageUtils.getContextualBoolean(soapMessage, SecurityConstants.RETURN_SECURITY_ERROR, false);
            
            final InboundWSSec inboundWSSec = 
                WSSec.getInboundWSSec(secProps, MessageUtils.isRequestor(soapMessage), returnSecurityError);
            
            newXmlStreamReader = 
                inboundWSSec.processInMessage(originalXmlStreamReader, requestSecurityEvents, securityEventListeners);
            final Object provider = soapMessage.getExchange().get(Provider.class);
            if (provider != null && ThreadLocalSecurityProvider.isInstalled()) {
                newXmlStreamReader = new StreamReaderDelegate(newXmlStreamReader) {
                    @Override
                    public int next() throws XMLStreamException {
                        try {
                            ThreadLocalSecurityProvider.setProvider((Provider)provider);
                            return super.next();
                        } finally {
                            ThreadLocalSecurityProvider.unsetProvider();
                        }
                    }
                };
            }
            soapMessage.setContent(XMLStreamReader.class, newXmlStreamReader);

            // Warning: The exceptions which can occur here are not security relevant exceptions
            // but configuration-errors. To catch security relevant exceptions you have to catch 
            // them e.g.in the FaultOutInterceptor. Why? Because we do streaming security. This 
            // interceptor doesn't handle the ws-security stuff but just setup the relevant stuff
            // for it. Exceptions will be thrown as a wrapped XMLStreamException during further
            // processing in the WS-Stack.
            soapMessage.put(SECURITY_PROCESSED, Boolean.TRUE);
        } catch (WSSecurityException e) {
            throw WSS4JUtils.createSoapFault(soapMessage, soapMessage.getVersion(), e);
        } catch (XMLSecurityException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, soapMessage.getVersion().getSender());
        } catch (WSSPolicyException e) {
            throw new SoapFault(e.getMessage(), e, soapMessage.getVersion().getSender());
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, soapMessage.getVersion().getSender());
        }
    }
    
    protected List<SecurityEventListener> configureSecurityEventListeners(
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws WSSPolicyException {
        final List<SecurityEvent> incomingSecurityEventList = new LinkedList<>();
        msg.getExchange().put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);
        msg.put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);
        
        final SecurityEventListener securityEventListener = new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
                if (securityEvent.getSecurityEventType() == WSSecurityEventConstants.Timestamp
                    || securityEvent.getSecurityEventType() == WSSecurityEventConstants.SignatureValue
                    || securityEvent instanceof TokenSecurityEvent
                    || securityEvent instanceof AbstractSecuredElementSecurityEvent) {
                    // Store events required for the security context setup, or the crypto coverage checker
                    incomingSecurityEventList.add(securityEvent);
                }
            }
        };
        
        return Collections.singletonList(securityEventListener);
    }
    
    protected void configureProperties(
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws XMLSecurityException {
        
        // Configure replay caching
        ReplayCache nonceCache = null;
        if (isNonceCacheRequired(msg, securityProperties)) {
            nonceCache = WSS4JUtils.getReplayCache(
                msg, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
            );
        }
        if (nonceCache == null) {
            securityProperties.setEnableNonceReplayCache(false);
            securityProperties.setNonceReplayCache(null);
        } else {
            securityProperties.setEnableNonceReplayCache(true);
            securityProperties.setNonceReplayCache(nonceCache);
        }
        
        ReplayCache timestampCache = null;
        if (isTimestampCacheRequired(msg, securityProperties)) {
            timestampCache = WSS4JUtils.getReplayCache(
                msg, SecurityConstants.ENABLE_TIMESTAMP_CACHE, SecurityConstants.TIMESTAMP_CACHE_INSTANCE
            );
        }
        if (timestampCache == null) {
            securityProperties.setEnableTimestampReplayCache(false);
            securityProperties.setTimestampReplayCache(null);
        } else {
            securityProperties.setEnableTimestampReplayCache(true);
            securityProperties.setTimestampReplayCache(timestampCache);
        }
        
        ReplayCache samlCache = null;
        if (isSamlCacheRequired(msg, securityProperties)) {
            samlCache = WSS4JUtils.getReplayCache(
                msg, SecurityConstants.ENABLE_SAML_ONE_TIME_USE_CACHE, 
                SecurityConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE
            );
        }
        if (samlCache == null) {
            securityProperties.setEnableSamlOneTimeUseReplayCache(false);
            securityProperties.setSamlOneTimeUseReplayCache(null);
        } else {
            securityProperties.setEnableSamlOneTimeUseReplayCache(true);
            securityProperties.setSamlOneTimeUseReplayCache(samlCache);
        }
        
        boolean enableRevocation = 
            MessageUtils.isTrue(SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENABLE_REVOCATION, msg));
        securityProperties.setEnableRevocation(enableRevocation);
        
        // Crypto loading only applies for Map
        Map<String, Object> config = getProperties();
        if (config != null && !config.isEmpty()) {
            Crypto sigVerCrypto = 
                loadCrypto(
                    msg,
                    ConfigurationConstants.SIG_VER_PROP_FILE,
                    ConfigurationConstants.SIG_VER_PROP_REF_ID,
                    securityProperties
                );
            if (sigVerCrypto == null) {
                // Fall back to using the Signature properties for verification
                sigVerCrypto = 
                    loadCrypto(
                        msg,
                        ConfigurationConstants.SIG_PROP_FILE,
                        ConfigurationConstants.SIG_PROP_REF_ID,
                        securityProperties
                    );
            }
            if (sigVerCrypto != null) {
                config.put(ConfigurationConstants.SIG_VER_PROP_REF_ID, "RefId-" + sigVerCrypto.hashCode());
                config.put("RefId-" + sigVerCrypto.hashCode(), sigVerCrypto);
            }
            
            Crypto decCrypto = 
                loadCrypto(
                    msg,
                    ConfigurationConstants.DEC_PROP_FILE,
                    ConfigurationConstants.DEC_PROP_REF_ID,
                    securityProperties
                );
            if (decCrypto != null) {
                config.put(ConfigurationConstants.DEC_PROP_REF_ID, "RefId-" + decCrypto.hashCode());
                config.put("RefId-" + decCrypto.hashCode(), decCrypto);
            }
            ConfigurationConverter.parseCrypto(config, securityProperties);
        }
        
        // Add Audience Restrictions for SAML
        configureAudienceRestriction(msg, securityProperties);
    }
    
    private void configureAudienceRestriction(SoapMessage msg, WSSSecurityProperties securityProperties) {
        // Add Audience Restrictions for SAML
        boolean enableAudienceRestriction = true;
        String audRestrStr = 
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION, msg);
        if (audRestrStr != null) {
            enableAudienceRestriction = Boolean.parseBoolean(audRestrStr);
        }
        if (enableAudienceRestriction) {
            List<String> audiences = new ArrayList<String>();
            if (msg.getContextualProperty(org.apache.cxf.message.Message.REQUEST_URL) != null) {
                audiences.add((String)msg.getContextualProperty(org.apache.cxf.message.Message.REQUEST_URL));
            }
            if (msg.getContextualProperty("javax.xml.ws.wsdl.service") != null) {
                audiences.add(msg.getContextualProperty("javax.xml.ws.wsdl.service").toString());
            }
            securityProperties.setAudienceRestrictions(audiences);
        }
    }
    
    /**
     * Is a Nonce Cache required, i.e. are we expecting a UsernameToken 
     */
    protected boolean isNonceCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        
        if (securityProperties != null && securityProperties.getActions() != null) {
            for (WSSConstants.Action action : securityProperties.getActions()) {
                if (action == WSSConstants.USERNAMETOKEN) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Is a Timestamp cache required, i.e. are we expecting a Timestamp 
     */
    protected boolean isTimestampCacheRequired(
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) {
        
        if (securityProperties != null && securityProperties.getActions() != null) {
            for (WSSConstants.Action action : securityProperties.getActions()) {
                if (action == WSSConstants.TIMESTAMP) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Is a SAML Cache required, i.e. are we expecting a SAML Token 
     */
    protected boolean isSamlCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        
        if (securityProperties != null && securityProperties.getActions() != null) {
            for (WSSConstants.Action action : securityProperties.getActions()) {
                if (action == WSSConstants.SAML_TOKEN_UNSIGNED 
                    || action == WSSConstants.SAML_TOKEN_SIGNED) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void setTokenValidators(
        WSSSecurityProperties properties, SoapMessage message
    ) throws WSSecurityException {
        Validator validator = loadValidator(SecurityConstants.SAML1_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_saml_Assertion, validator);
        }
        validator = loadValidator(SecurityConstants.SAML2_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_saml2_Assertion, validator);
        }
        validator = loadValidator(SecurityConstants.USERNAME_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_wsse_UsernameToken, validator);
        }
        validator = loadValidator(SecurityConstants.SIGNATURE_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_dsig_Signature, validator);
        }
        validator = loadValidator(SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_wsu_Timestamp, validator);
        }
        validator = loadValidator(SecurityConstants.BST_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_wsse_BinarySecurityToken, validator);
        }
        validator = loadValidator(SecurityConstants.SCT_TOKEN_VALIDATOR, message);
        if (validator != null) {
            properties.addValidator(WSSConstants.TAG_wsc0502_SecurityContextToken, validator);
            properties.addValidator(WSSConstants.TAG_wsc0512_SecurityContextToken, validator);
        }
    }
    
    private Validator loadValidator(String validatorKey, SoapMessage message) throws WSSecurityException {
        Object o = message.getContextualProperty(validatorKey);
        if (o == null) {
            return null;
        }
        try {
            if (o instanceof Validator) {
                return (Validator)o;
            } else if (o instanceof Class) {
                return (Validator)((Class<?>)o).newInstance();
            } else if (o instanceof String) {
                return (Validator)ClassLoaderUtils.loadClass(o.toString(),
                                                             WSS4JStaxInInterceptor.class)
                                                             .newInstance();
            } else {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, 
                                                  "Cannot load Validator: " + o);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
    }

}
