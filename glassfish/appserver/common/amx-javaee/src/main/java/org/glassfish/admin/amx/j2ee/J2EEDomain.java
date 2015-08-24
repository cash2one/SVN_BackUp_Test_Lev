/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.admin.amx.j2ee;

import javax.management.ObjectName;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

import org.glassfish.admin.amx.annotation.ManagedAttribute;

/**
	The discovery and navigation of all managed objects in the J2EE
	management system begins with the J2EEDomain.

	@see J2EEServer
	@see J2EECluster
	@see J2EEApplication
	@see JVM
	@see AppClientModule
	@see EJBModule
	@see WebModule
	@see ResourceAdapterModule
	@see EntityBean
	@see StatefulSessionBean
	@see StatelessSessionBean
	@see MessageDrivenBean
	@see Servlet
	@see JavaMailResource
	@see JCAResource
	@see JCAConnectionFactory
	@see JCAManagedConnectionFactory
	@see JDBCResource
	@see JDBCDataSource
	@see JDBCDriver
	@see JMSResource
	@see JNDIResource
	@see JTAResource
	@see RMIIIOPResource
	@see URLResource
 */
@AMXMBeanMetadata(type=J2EETypes.J2EE_DOMAIN, singleton=true)
public interface J2EEDomain
	extends J2EEManagedObject
{
	/**
		Note that the Attribute name is case-sensitive
		"servers" as defined by JSR 77.
		
		@return the ObjectNames of the J2EEServers, as Strings
	 */
 	@ManagedAttribute
	public String[]	getservers();

 	@ManagedAttribute
    @Description( "Get the ObjectName of the corresponding config MBean, if any" )
    public ObjectName getCorrespondingConfig();
}









