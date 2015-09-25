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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.DiskStoreConfiguration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.EHCacheUtil;

public class DefaultEHCacheOAuthDataProvider extends AbstractOAuthDataProvider 
    implements ClientRegistrationProvider {
    public static final String CLIENT_CACHE_KEY = "cxf.oauth2.client.cache";
    public static final String ACCESS_TOKEN_CACHE_KEY = "cxf.oauth2.accesstoken.cache";
    public static final String REFRESH_TOKEN_CACHE_KEY = "cxf.oauth2.refreshtoken.cache";
    public static final String DEFAULT_CONFIG_URL = "cxf-oauth2-ehcache.xml";
    
    protected CacheManager cacheManager;
    private Ehcache clientCache;
    private Ehcache accessTokenCache;
    private Ehcache refreshTokenCache;
    
    public DefaultEHCacheOAuthDataProvider() {
        this(DEFAULT_CONFIG_URL, null);
    }
    
    public DefaultEHCacheOAuthDataProvider(String configFileURL, Bus bus) {
        this(configFileURL, bus, CLIENT_CACHE_KEY, ACCESS_TOKEN_CACHE_KEY, REFRESH_TOKEN_CACHE_KEY);
    }
    
    public DefaultEHCacheOAuthDataProvider(String configFileURL, 
                                               Bus bus,
                                               String clientCacheKey, 
                                               String accessTokenKey,
                                               String refreshTokenKey) {
        createCaches(configFileURL, bus, clientCacheKey, accessTokenKey, refreshTokenKey);
    }
    
    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        return getCacheValue(clientCache, clientId, Client.class);
    }
    
    @Override
    public void setClient(Client client) {
        putCacheValue(clientCache, client.getClientId(), client, 0);
        
    }

    @Override
    public Client removeClient(String clientId) {
        Client c = getClient(clientId);
        clientCache.remove(clientId);
        return c;
    }

    @Override
    public List<Client> getClients() {
        List<String> keys = CastUtils.cast(clientCache.getKeys());
        List<Client> clients = new ArrayList<Client>(keys.size());
        for (String key : keys) {
            clients.add(getClient(key));
        }
        return clients;
    }
    
    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        return getCacheValue(accessTokenCache, accessToken, ServerAccessToken.class);
    }

    @Override
    public void removeAccessToken(ServerAccessToken accessToken) throws OAuthServiceException {
        revokeAccessToken(accessToken.getTokenKey());
    }

    protected boolean revokeAccessToken(String accessTokenKey) {
        return accessTokenCache.remove(accessTokenKey);
    }
    
    protected RefreshToken revokeRefreshToken(Client client, String refreshTokenKey) { 
        RefreshToken refreshToken = getCacheValue(refreshTokenCache, refreshTokenKey, RefreshToken.class);
        if (refreshToken != null) {
            refreshTokenCache.remove(refreshTokenKey);
        }
        return refreshToken;
    }
    
    protected void saveAccessToken(ServerAccessToken serverToken) {
        putCacheValue(accessTokenCache, serverToken.getTokenKey(), serverToken, serverToken.getExpiresIn());
    }
    
    protected void saveRefreshToken(ServerAccessToken at, RefreshToken refreshToken) {
        putCacheValue(refreshTokenCache, refreshToken.getTokenKey(), refreshToken, refreshToken.getExpiresIn());
    }
    
    protected static <T> T getCacheValue(Ehcache cache, String key, Class<T> cls) {
        Element e = cache.get(key);
        if (e != null) {
            return cls.cast(e.getObjectValue());
        } else {
            return null;
        }
    }
    protected static void putCacheValue(Ehcache cache, String key, Object value, long ttl) {
        Element element = new Element(key, value);
        int parsedTTL = (int)ttl;
        if (ttl != (long)parsedTTL) {
            throw new OAuthServiceException("Requested time to live can not be supported");
        }
        element.setTimeToLive(parsedTTL);
        element.setTimeToIdle(parsedTTL);
        cache.put(element);
    }
    
    private static CacheManager createCacheManager(String configFile, Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus(true);
        }
        
        URL configFileURL = null;
        try {
            configFileURL = 
                ResourceUtils.getClasspathResourceURL(configFile, DefaultEHCacheOAuthDataProvider.class, bus);
        } catch (Exception ex) {
            // ignore
        }
        CacheManager cacheManager = null;
        if (configFileURL == null) {
            cacheManager = EHCacheUtil.createCacheManager();
        } else {
            Configuration conf = ConfigurationFactory.parseConfiguration(configFileURL);
            
            if (bus != null) {
                conf.setName(bus.getId());
                DiskStoreConfiguration dsc = conf.getDiskStoreConfiguration();
                if (dsc != null && "java.io.tmpdir".equals(dsc.getOriginalPath())) {
                    String path = conf.getDiskStoreConfiguration().getPath() + File.separator
                        + bus.getId();
                    conf.getDiskStoreConfiguration().setPath(path);
                }
            }
            
            cacheManager = EHCacheUtil.createCacheManager(conf);
        }
        return cacheManager;
    }
    
    protected static Ehcache createCache(CacheManager cacheManager, String cacheKey) { 
        CacheConfiguration clientCC = EHCacheUtil.getCacheConfiguration(cacheKey, cacheManager);
        return cacheManager.addCacheIfAbsent(new Cache(clientCC));
    }
    
    private void createCaches(String configFile, Bus bus, 
                              String clientCacheKey, String accessTokenKey, String refreshTokenKey) {
        cacheManager = createCacheManager(configFile, bus);
        
        clientCache = createCache(cacheManager, clientCacheKey);
        accessTokenCache = createCache(cacheManager, accessTokenKey);
        refreshTokenCache = createCache(cacheManager, refreshTokenKey);
    }

    
}
