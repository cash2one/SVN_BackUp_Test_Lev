package org.jboss.as.test.integration.jaxrs.integration.ejb;

import javax.transaction.SystemException;

/**
 * @author Stuart Douglas
 */
public interface EjbInterface {
    public String getMessage() throws SystemException;
}
