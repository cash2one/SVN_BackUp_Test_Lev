/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.common;

import java.security.Permission;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SecurePermTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEqualsObject() {
		

		Permission perm1 = new SecureServiceAccessPermission("a/b/c", "read,write");
		
		Permission perm2 = new SecureServiceAccessPermission("a/b/c/d", "read,write");
		Assert.assertFalse(perm1.equals(perm2));
		
		Permission p3 = null;
		Assert.assertFalse(perm1.equals(p3));
		

		Permission p5 = new SecureServiceAccessPermission("a/b/c");
		Assert.assertFalse(perm1.equals(p5));
	}

	@Test
	public void testEquals1() {
		Permission p1 = new SecureServiceAccessPermission("a/b/c", "read,write");
		Permission p2 = new SecureServiceAccessPermission("a/b/c/", "read,write");
		Assert.assertFalse(p1.equals(p2));
		Assert.assertFalse(p1.implies(p2));
		
	}
	
	@Test
	public void testImpliesPermission() {
		Permission p1 = new SecureServiceAccessPermission("a", "read");
		Permission p2 = new SecureServiceAccessPermission("b", "read");
		Assert.assertFalse(p1.implies(p2));
		
		Permission p3 = new SecureServiceAccessPermission("a", "read,write");
		Assert.assertTrue(p3.implies(p1));
	}

	@Test
	public void testImpliesWild() {
		Permission p1 = new SecureServiceAccessPermission("a/*", "read");
		
		Permission p2 = new SecureServiceAccessPermission("a/b", "read");
		Assert.assertTrue(p1.implies(p2));
		
		Permission p3 = new SecureServiceAccessPermission("a/b/c", "read");
		Assert.assertTrue(p1.implies(p3));
		
		
		Permission p11 = new SecureServiceAccessPermission("a/b/*", "read");
		Assert.assertTrue(p11.implies(p3));
		
		Assert.assertFalse(p11.implies(p1));
		Assert.assertFalse(p11.implies(p2));
		
		Assert.assertTrue(p11.implies(p11));
	}

   @Test
    public void testImpliesWild1() {
        Permission p1 = new SecureServiceAccessPermission("a/*", null);
        Permission p2 = new SecureServiceAccessPermission("a/default", null);
        Assert.assertTrue(p1.implies(p2));
   }
	
	@Test
	public void testImpliesActions() {
		Permission p1 = new SecureServiceAccessPermission("a", "read,write");
		Permission p2 = new SecureServiceAccessPermission("a", "read");
		Assert.assertTrue(p1.implies(p2));
		
		Permission p3 = new SecureServiceAccessPermission("a", "write");
		Assert.assertTrue(p1.implies(p3));
	}


}
