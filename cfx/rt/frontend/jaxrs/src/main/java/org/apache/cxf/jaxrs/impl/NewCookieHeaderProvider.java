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

package org.apache.cxf.jaxrs.impl;

import java.util.Date;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;

public class NewCookieHeaderProvider implements HeaderDelegate<NewCookie> {

    private static final String VERSION = "Version";
    private static final String PATH = "Path";
    private static final String DOMAIN = "Domain";
    private static final String MAX_AGE = "Max-Age";
    private static final String COMMENT = "Comment";
    private static final String SECURE = "Secure";
    private static final String EXPIRES = "Expires";
    private static final String HTTP_ONLY = "HttpOnly";
        
    public NewCookie fromString(String c) {
        
        if (c == null) {
            throw new IllegalArgumentException("SetCookie value can not be null");
        }
        
        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        String comment = null;
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        boolean isSecure = false;
        Date expires = null;
        boolean httpOnly = false;
        int version = NewCookie.DEFAULT_VERSION;
        
        String[] tokens = StringUtils.split(c, ";");
        for (String token : tokens) {
            String theToken = token.trim();
            
            int sepIndex = theToken.indexOf('=');
            String paramName = sepIndex != -1 ? theToken.substring(0, sepIndex) : theToken;
            String paramValue = sepIndex == -1 || sepIndex == theToken.length() - 1 
                ? null : theToken.substring(sepIndex + 1);
            
            if (paramName.equalsIgnoreCase(MAX_AGE)) {
                maxAge = Integer.parseInt(paramValue);
            } else if (paramName.equalsIgnoreCase(PATH)) {
                path = paramValue;
            } else if (paramName.equalsIgnoreCase(DOMAIN)) {
                domain = paramValue;
            } else if (paramName.equalsIgnoreCase(COMMENT)) {
                comment = paramValue;
            } else if (paramName.equalsIgnoreCase(SECURE)) {
                isSecure = true;
            } else if (paramName.equalsIgnoreCase(EXPIRES)) {
                expires = HttpUtils.getHttpDate(paramValue);
            } else if (paramName.equalsIgnoreCase(HTTP_ONLY)) {
                httpOnly = true;
            } else if (paramName.equalsIgnoreCase(VERSION)) {
                version = Integer.parseInt(paramValue);
            } else if (paramValue != null) {
                name = paramName;
                value = paramValue;
            }
        }
        
        if (name == null || value == null) {
            throw new IllegalArgumentException("Set-Cookie is malformed : " + c);
        }
        
        return new NewCookie(name, value, path, domain, version, comment, maxAge, expires, isSecure, httpOnly);
    }

    public String toString(NewCookie value) {
        StringBuilder sb = new StringBuilder();
        sb.append(value.getName()).append('=').append(value.getValue());
        if (value.getComment() != null) {
            sb.append(';').append(COMMENT).append('=').append(value.getComment());
        }
        if (value.getDomain() != null) {
            sb.append(';').append(DOMAIN).append('=').append(value.getDomain());
        }
        if (value.getMaxAge() != -1) {
            sb.append(';').append(MAX_AGE).append('=').append(value.getMaxAge());
        }
        if (value.getPath() != null) {
            sb.append(';').append(PATH).append('=').append(value.getPath());
        }
        if (value.getExpiry() != null) {
            sb.append(';').append(EXPIRES).append('=').append(HttpUtils.toHttpDate(value.getExpiry()));
        }
        if (value.isSecure()) {
            sb.append(';').append(SECURE);
        }
        if (value.isHttpOnly()) {
            sb.append(';').append(HTTP_ONLY);
        }
        sb.append(';').append(VERSION).append('=').append(value.getVersion());
        return sb.toString();
    }

}
