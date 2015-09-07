//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.start;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles basic license presentation and acknowledgement.
 */
public class Licensing
{
    private static final String PROP_ACK_LICENSES = "org.eclipse.jetty.start.ack.licenses";
    public Map<String, List<String>> licenseMap = new TreeMap<>(new NaturalSort.Strings());

    public void addModule(Module module)
    {
        if (!module.hasLicense())
        {
            // skip, no license
            return;
        }
        
        if (licenseMap.containsKey(module.getName()))
        {
            // skip, already being tracked
            return;
        }

        licenseMap.put(module.getName(),module.getLicense());
    }

    public boolean hasLicenses()
    {
        return !licenseMap.isEmpty();
    }

    public boolean acknowledgeLicenses() throws IOException
    {
        if (!hasLicenses())
        {
            return true;
        }
        
        System.err.printf("%nALERT: There are enabled module(s) with licenses.%n");
        System.err.printf("The following %d module(s):%n", licenseMap.size());
        System.err.printf(" + contains software not provided by the Eclipse Foundation!%n");
        System.err.printf(" + contains software not covered by the Eclipse Public License!%n");
        System.err.printf(" + has not been audited for compliance with its license%n");

        for (String key : licenseMap.keySet())
        {
            System.err.printf("%n Module: %s%n",key);
            for (String line : licenseMap.get(key))
            {
                System.err.printf("  + %s%n",line);
            }
        }

        boolean licenseAck = false;

        String propBasedAckValue = System.getProperty(PROP_ACK_LICENSES);
        if (propBasedAckValue != null)
        {
            StartLog.log("TESTING MODE","Programmatic ACK - %s=%s",PROP_ACK_LICENSES,propBasedAckValue);
            licenseAck = Boolean.parseBoolean(propBasedAckValue);
        }
        else
        {
            if (Boolean.getBoolean("org.eclipse.jetty.start.testing"))
            {
                throw new RuntimeException("Test Configuration Missing - Pre-specify answer to (" + PROP_ACK_LICENSES + ") in test case");
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            System.err.printf("%nProceed (y/N)? ");
            String response = input.readLine();

            licenseAck = (Utils.isNotBlank(response) && response.toLowerCase().startsWith("y"));
        }

        return licenseAck;
    }
}
