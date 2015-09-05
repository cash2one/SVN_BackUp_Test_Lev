package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicHome extends EJBHome {

    public IIOPBasicRemote create() throws RemoteException;

}
