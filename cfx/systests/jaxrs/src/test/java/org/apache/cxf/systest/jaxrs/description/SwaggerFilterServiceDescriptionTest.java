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
package org.apache.cxf.systest.jaxrs.description;

import org.junit.BeforeClass;
import org.junit.Ignore;

public class SwaggerFilterServiceDescriptionTest extends AbstractSwaggerServiceDescriptionTest {
    private static final String PORT = allocatePort(SwaggerFilterServiceDescriptionTest.class);
    
    @Ignore
    public static class SwaggerFilter extends Server {
        public SwaggerFilter() {
            super(PORT, true);
        }       
        
        public static void main(String[] args) {
            start(new SwaggerFilter());
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        startServers(SwaggerFilter.class);
    }
    
    @Override
    protected String getPort() {
        return PORT;
    }
}
