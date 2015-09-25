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
package org.apache.cxf.xkms.x509.repo.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.cxf.xkms.exception.XKMSConfigurationException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCertificateRepo implements CertificateRepo {
    private static final Logger LOG = LoggerFactory.getLogger(FileCertificateRepo.class);
    private static final String CN_PREFIX = "cn=";
    private static final String TRUSTED_CAS_PATH = "trusted_cas";
    private static final String CRLS_PATH = "crls";
    private static final String CAS_PATH = "cas";
    private final File storageDir;
    private final CertificateFactory certFactory;

    public FileCertificateRepo(String path) {
        storageDir = new File(path);
        try {
            this.certFactory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void saveCertificate(X509Certificate cert, UseKeyWithType id) {
        saveCategorizedCertificate(cert, id, false, false);
    }

    public void saveTrustedCACertificate(X509Certificate cert, UseKeyWithType id) {
        saveCategorizedCertificate(cert, id, true, false);
    }

    public void saveCACertificate(X509Certificate cert, UseKeyWithType id) {
        saveCategorizedCertificate(cert, id, false, true);
    }
    
    public void saveCRL(X509CRL crl, UseKeyWithType id) {
        String name = crl.getIssuerX500Principal().getName();
        try {
            String path = convertIdForFileSystem(name) + ".cer";
            Pattern p = Pattern.compile("[a-zA-Z_0-9-_]");
            if (!p.matcher(path).find()) {
                throw new URISyntaxException(path, "Input did not match [a-zA-Z_0-9-_].");
            }
            
            File certFile = new File(storageDir + "/" + CRLS_PATH, path);
            certFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(certFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(crl.getEncoded());
            bos.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Error saving CRL " + name + ": " + e.getMessage(), e);
        }
    }

    private boolean saveCategorizedCertificate(X509Certificate cert, UseKeyWithType id, boolean isTrustedCA,
                                               boolean isCA) {
        String category = "";
        if (isTrustedCA) {
            category = TRUSTED_CAS_PATH;
        }
        if (isCA) {
            category = CAS_PATH;
        }
        try {
            File certFile = new File(storageDir + "/" + category,
                                     getCertPath(cert, id));
            certFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(certFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(cert.getEncoded());
            bos.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Error saving certificate " + cert.getSubjectDN() + ": " + e.getMessage(), e);
        }
        return true;
    }

    public String convertIdForFileSystem(String dn) {
        String result = dn.replace("=", "-");
        result = result.replace(", ", "_");
        result = result.replace(",", "_");
        result = result.replace("/", "_");
        result = result.replace("\\", "_");
        result = result.replace("{", "_");
        result = result.replace("}", "_");
        result = result.replace(":", "_");
        return result;
    }

    public String getCertPath(X509Certificate cert, UseKeyWithType id)
        throws URISyntaxException {
        Applications application = null;
        String path = null;
        if (id != null) {
            application = Applications.fromUri(id.getApplication());
        }
        if (application == Applications.SERVICE_ENDPOINT) {
            path = id.getIdentifier();
        } else {
            path = cert.getSubjectDN().getName();
        }
        path = convertIdForFileSystem(path) + ".cer";
        validateCertificatePath(path);
        return path;
    }

    private void validateCertificatePath(String path) throws URISyntaxException {
        Pattern p = Pattern.compile("[a-zA-Z_0-9-_]");
        if (!p.matcher(path).find()) {
            throw new URISyntaxException(path, "Input did not match [a-zA-Z_0-9-_].");
        }
    }

    private File[] getX509Files() {
        List<File> certificateFiles = new ArrayList<>();
        try {
            certificateFiles.addAll(Arrays.asList(storageDir.listFiles()));
            certificateFiles.addAll(Arrays.asList(new File(storageDir + "/" + TRUSTED_CAS_PATH).listFiles()));
            certificateFiles.addAll(Arrays.asList(new File(storageDir + "/" + CAS_PATH).listFiles()));
            certificateFiles.addAll(Arrays.asList(new File(storageDir + "/" + CRLS_PATH).listFiles()));
        } catch (NullPointerException e) {
            //
        }
        if (certificateFiles.isEmpty()) {
            throw new XKMSConfigurationException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                                 ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE,
                                                 "File base persistence storage is not found: "
                                                     + storageDir.getPath());
        }
        return certificateFiles.toArray(new File[certificateFiles.size()]);
    }

    public X509Certificate readCertificate(File certFile) throws CertificateException, FileNotFoundException {
        FileInputStream fis = new FileInputStream(certFile);
        return (X509Certificate)certFactory.generateCertificate(fis);
    }
    
    public X509CRL readCRL(File crlFile) throws FileNotFoundException, CRLException {
        FileInputStream fis = new FileInputStream(crlFile);
        return (X509CRL)certFactory.generateCRL(fis);
    }

    @Override
    public List<X509Certificate> getTrustedCaCerts() {
        List<X509Certificate> results = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                if (certFile.getParent().endsWith(TRUSTED_CAS_PATH)) {
                    X509Certificate cert = readCertificate(certFile);
                    results.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        return results;
    }

    @Override
    public List<X509Certificate> getCaCerts() {
        List<X509Certificate> results = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                if (certFile.getParent().endsWith(CAS_PATH)) {
                    X509Certificate cert = readCertificate(certFile);
                    results.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        return results;
    }
    
    @Override
    public List<X509CRL> getCRLs() {
        List<X509CRL> results = new ArrayList<>();
        File[] list = getX509Files();
        for (File crlFile : list) {
            try {
                if (crlFile.isDirectory()) {
                    continue;
                }
                if (crlFile.getParent().endsWith(CRLS_PATH)) {
                    X509CRL crl = readCRL(crlFile);
                    results.add(crl);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load CRL from file: %s. Error: %s", crlFile,
                                       e.getMessage()));
            }

        }
        
        return results;
    }

    @Override
    public X509Certificate findByServiceName(String serviceName) {
        return findBySubjectDn(CN_PREFIX + serviceName);
    }

    @Override
    public X509Certificate findByEndpoint(String endpoint) {
        try {
            String path = convertIdForFileSystem(endpoint) + ".cer";
            validateCertificatePath(path);
            File certFile = new File(storageDir.getAbsolutePath() + "/" + path);
            if (!certFile.exists()) {
                LOG.warn(String.format("Certificate not found for endpoint %s, path %s", endpoint,
                                       certFile.getAbsolutePath()));
                return null;
            }
            return (X509Certificate)certFactory.generateCertificate(new FileInputStream(certFile));
        } catch (Exception e) {
            LOG.warn(String.format("Cannot load certificate by endpoint: %s. Error: %s", endpoint,
                                   e.getMessage()), e);
            return null;
        }
    }

    @Override
    public X509Certificate findBySubjectDn(String subjectDn) {
        List<X509Certificate> result = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                X509Certificate cert = readCertificate(certFile);
                LOG.debug("Searching for " + subjectDn + ". Checking cert " 
                    + cert.getSubjectDN().getName() + ", " + cert.getSubjectX500Principal().getName());
                if (subjectDn.equalsIgnoreCase(cert.getSubjectDN().getName())
                    || subjectDn.equalsIgnoreCase(cert.getSubjectX500Principal().getName())) {
                    result.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    @Override
    public X509Certificate findByIssuerSerial(String issuer, String serial) {
        List<X509Certificate> result = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                X509Certificate cert = readCertificate(certFile);
                BigInteger cs = cert.getSerialNumber();
                BigInteger ss = new BigInteger(serial, 16);
                if (issuer.equalsIgnoreCase(cert.getIssuerX500Principal().getName()) && cs.equals(ss)) {
                    result.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

}
