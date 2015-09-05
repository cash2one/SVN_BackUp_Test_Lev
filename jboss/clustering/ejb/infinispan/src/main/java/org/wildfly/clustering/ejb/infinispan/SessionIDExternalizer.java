/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.clustering.ejb.infinispan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class SessionIDExternalizer implements Externalizer<SessionID> {

    private final Class<? extends SessionID> targetClass;

    SessionIDExternalizer(Class<? extends SessionID> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public void writeObject(ObjectOutput output, SessionID id) throws IOException {
        byte[] encoded = id.getEncodedForm();
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    @Override
    public SessionID readObject(ObjectInput input) throws IOException {
        byte[] encoded = new byte[input.readInt()];
        input.readFully(encoded);
        return SessionID.createSessionID(encoded);
    }

    @Override
    public Class<? extends SessionID> getTargetClass() {
        return this.targetClass;
    }
}
