package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalHome extends EJBHome {

    public IIOPTransactionalRemote create() throws RemoteException;

}
