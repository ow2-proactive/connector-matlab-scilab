package org.ow2.proactive.scheduler.ext.matsci.client.common;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * JVMProcessInterface an interface to control the lifecycle of the middleman JVM
 *
 * @author The ProActive Team
 */
public interface MatSciJVMProcessInterface extends Remote {

    /**
     * Returns this JVM PID
     * @return
     * @throws java.rmi.RemoteException
     */
    public Integer getPID() throws RemoteException;

    /**
     * Shuts down this JVM
     * @return
     * @throws java.rmi.RemoteException
     */
    public void shutdown() throws RemoteException;

}
