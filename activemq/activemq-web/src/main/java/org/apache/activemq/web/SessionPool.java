/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.web;

import java.util.LinkedList;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple pool of JMS Session objects intended for use by Queue browsers.
 */
public class SessionPool {

    private static final Logger LOG = LoggerFactory.getLogger(SessionPool.class);

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private final LinkedList<Session> sessions = new LinkedList<Session>();

    public Connection getConnection() throws JMSException {
        if (checkConnection()) {
            return connection;
        }

        synchronized (this) {
            try {
                connection = getConnectionFactory().createConnection();
                connection.start();
                return connection;
            } catch (JMSException jmsEx) {
                LOG.debug("Caught exception while attempting to get a new Connection.", jmsEx);
                connection.close();
                connection = null;
                throw jmsEx;
            }
        }
    }

    private boolean checkConnection() {
        if (connection == null) {
            return false;
        }

        try {
            connection.getMetaData();
            return true;
        } catch (JMSException e) {
            return false;
        }
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            throw new IllegalStateException("No ConnectionFactory has been set for the session pool");
        }
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Session borrowSession() throws JMSException {
        Session answer = null;
        synchronized (sessions) {
            if (sessions.isEmpty()) {
                LOG.trace("Creating a new session.");
                answer = createSession();
            } else {
                LOG.trace("Serving session from the pool.");
                answer = sessions.removeLast();
            }
        }
        return answer;
    }

    public void returnSession(Session session) {
        if (session != null && !((ActiveMQSession) session).isClosed()) {
            synchronized (sessions) {
                LOG.trace("Returning session to the pool.");
                sessions.add(session);
            }
        } else {
            LOG.debug("Session closed or null, not returning to the pool.");
        }
    }

    protected Session createSession() throws JMSException {
        return getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
}
