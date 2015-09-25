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
package org.apache.cxf.jaxrs.swagger;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;

public abstract class AbstractSwaggerFeature extends AbstractFeature {
    protected boolean scan = true;
    protected boolean runAsFilter;
    private String resourcePackage;
    private String version = "1.0.0";
    // depending on swagger version basePath is set differently
    private String basePath;
    private String title = "Sample REST Application";
    private String description = "The Application";
    private String contact = "users@cxf.apache.org";
    private String license = "Apache 2.0 License";
    private String licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html";
    private String termsOfServiceUrl;
    private String filterClass;
    
    @Override
    public void initialize(Server server, Bus bus) {
        calculateDefaultResourcePackage(server);
        calculateDefaultBasePath(server);
        addSwaggerResource(server);
        
        initializeProvider(server.getEndpoint(), bus);
    }

    protected abstract void addSwaggerResource(Server server);

    protected abstract void setBasePathByAddress(String address);

    private void calculateDefaultResourcePackage(Server server) {
        JAXRSServiceFactoryBean serviceFactoryBean = 
            (JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
        AbstractResourceInfo resourceInfo = serviceFactoryBean.getClassResourceInfo().get(0);
        
        if ((resourceInfo != null) 
            && (getResourcePackage() == null || getResourcePackage().length() == 0)) {
            setResourcePackage(resourceInfo.getServiceClass().getPackage().getName());
        }
    }
    
    private void calculateDefaultBasePath(Server server) {
        if (getBasePath() == null || getBasePath().length() == 0) {
            String address = server.getEndpoint().getEndpointInfo().getAddress();
            setBasePathByAddress(address);
        }
    }

    public String getResourcePackage() {
        return resourcePackage;
    }
    public void setResourcePackage(String resourcePackage) {
        this.resourcePackage = resourcePackage;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public String getBasePath() {
        return basePath;
    }
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getContact() {
        return contact;
    }
    public void setContact(String contact) {
        this.contact = contact;
    }
    public String getLicense() {
        return license;
    }
    public void setLicense(String license) {
        this.license = license;
    }
    public String getLicenseUrl() {
        return licenseUrl;
    }
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
    public String getTermsOfServiceUrl() {
        return termsOfServiceUrl;
    }
    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        this.termsOfServiceUrl = termsOfServiceUrl;
    }
    public boolean isScan() {
        return scan;
    }
    public void setScan(boolean scan) {
        this.scan = scan;
    }
    public String getFilterClass() {
        return filterClass;
    }
    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }

    public boolean isRunAsFilter() {
        return runAsFilter;
    }
    public void setRunAsFilter(boolean runAsFilter) {
        this.runAsFilter = runAsFilter;
    }
}
