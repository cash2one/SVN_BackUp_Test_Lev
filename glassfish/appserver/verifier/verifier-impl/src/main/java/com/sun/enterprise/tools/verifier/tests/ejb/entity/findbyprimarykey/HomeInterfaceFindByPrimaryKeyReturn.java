/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.findbyprimarykey;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/**
 * Define findByPrimaryKey method which returns the primary key type test.  
 *
 *     Every entity enterprise Bean class must define the findByPrimaryKey 
 *     method. The return type for this method must be the primary key type.
 *     (i.e. the findByPrimaryKey method must be a single-object finder). 
 * 
 */
public class HomeInterfaceFindByPrimaryKeyReturn extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;

    /**
     * Define findByPrimaryKey method test.  
     *
     *     Every entity enterprise Bean class must define the findByPrimaryKey 
     *     method. The return type for this method must be the primary key type.
     *     (i.e. the findByPrimaryKey method must be a single-object finder). 
     *  
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();

	boolean oneFailed = false;
	if (descriptor instanceof EjbEntityDescriptor) {
	    if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName())&&
	       descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName()))
		oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(), descriptor.getRemoteClassName(),descriptor);
	    if(oneFailed == false) {
		if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName())&&
		   descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName()))
		    oneFailed = commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor.getLocalClassName(),descriptor);
	    }	   
	    if (oneFailed)  {
		result.setStatus(result.FAILED);
	    } else {
		result.setStatus(result.PASSED);
	    }
	    return result;

	} else {
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} bean, but called with {2} bean.",
				  new Object[] {getClass(),"Entity","Session"}));
	    return result;
	}
    }

    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home interface of the Ejb. 
     * @param remote for Remote/Local interface
     * @return boolean the results for this assertion i.e if a test has failed or not
     */


    private boolean commonToBothInterfaces(String home, String remote, EjbDescriptor descriptor) {
	boolean findByPrimaryKeyMethodFound = false;
	boolean oneFailed = false;
	boolean returnValueValid = false;
	try {
	    // retrieve the home interface methods
	    VerifierTestContext context = getVerifierContext();
	    ClassLoader jcl = context.getClassLoader();
	    Class homeInterfaceClass = Class.forName(home, false, getVerifierContext().getClassLoader());
	    Method [] ejbFinderMethods = homeInterfaceClass.getDeclaredMethods();
	    Class rc = Class.forName(remote, false, getVerifierContext().getClassLoader());
	    
	    for (int j = 0; j < ejbFinderMethods.length; ++j) {
		// reset all booleans for next method within the loop
		returnValueValid = false;
		
		if (ejbFinderMethods[j].getName().equals("findByPrimaryKey")) {
		    // Every entity enterprise Bean class must define the 
		    // findByPrimaryKey method. The return type for this method must 
		    // be the primary key type (i.e. the findByPrimaryKey method 
		    // must be a single-object finder). 
		    if (!findByPrimaryKeyMethodFound) {
			findByPrimaryKeyMethodFound = true;
		    }
		    Class returnByPrimaryKeyValue = ejbFinderMethods[j].getReturnType();
		    // as long as this returns a single object finder, then return type
		    // is valid
		    if (((returnByPrimaryKeyValue.getName().equals(rc.getName())) && 
			 (!((returnByPrimaryKeyValue.getName().equals("java.util.Collection")) || 
			    (returnByPrimaryKeyValue.getName().equals("java.util.Enumeration")))))) {
			returnValueValid = true;
		    }
		    
		    
		    // report for this particular findByPrimaryKey(...)
		    if (findByPrimaryKeyMethodFound && returnValueValid) {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString
					      (getClass().getName() + ".debug1",
					       "For Home interface [ {0} ] Finder Method [ {1} ]",
					       new Object[] {homeInterfaceClass.getName(),ejbFinderMethods[j].getName()}));
			result.addGoodDetails(smh.getLocalString
					      (getClass().getName() + ".passed",
					       "A findByPrimaryKey method was found with valid return type."));
		    } else if (findByPrimaryKeyMethodFound && !returnValueValid) {
			oneFailed = true;
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".debug1",
						"For Home interface [ {0} ] Finder Method [ {1} ]",
						new Object[] {homeInterfaceClass.getName(),ejbFinderMethods[j].getName()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed",
						"Error: A findByPrimaryKey method was found, but with invalid return type."));
		    }			 
		    // found findByPrimaryKey, so break out of loop
		    break;
		}
	    }
	    if (!findByPrimaryKeyMethodFound) {
		oneFailed = true;
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.addErrorDetails(smh.getLocalString
				       (getClass().getName() + ".debug3",
					"For Home interface [ {0} ] ",
					new Object[] {homeInterfaceClass.getName()}));
		result.addErrorDetails(smh.getLocalString
				       (getClass().getName() + ".failed1",
					"Error: No findByPrimaryKey method was found in home interface class [ {0} ].",
					new Object[] {homeInterfaceClass.getName()}));
	    }
	    return oneFailed;
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home interface [ {0} ] does not exist or is not loadable.",
			   new Object[] {home}));
	    oneFailed = true;
	    return oneFailed;
	}
	
    }
}
