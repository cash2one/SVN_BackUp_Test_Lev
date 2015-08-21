/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.mongodb.mongo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.ServerAddress;

/**
 */
public class MongoUtils {
    /**
     * MongoDB default connection port
     */
    public static final int DEFAULT_PORT = 27017;

    public static List<ServerAddress> toServerAddresses(String connections) throws UnknownHostException {

        List<ServerAddress> addresses = new ArrayList<ServerAddress>();
        for(String connection : Arrays.asList(connections.split(","))) {
            int port = DEFAULT_PORT;
            String[] hostPort = connection.split(":");
            if(hostPort.length > 1 && !StringUtils.isEmpty(hostPort[1])) {
                port = Integer.parseInt(hostPort[1].trim());
            }
            addresses.add(new ServerAddress(hostPort[0], port));
        }
        return addresses;
    }
}
