/*
 *  *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 *  * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.matsci.client.embedded.util;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.ow2.proactive.scheduler.ext.matsci.client.common.DataspaceRegistry;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciJVMProcessInterface;


/**
 * MatSciJVMSpawnHelper
 *
 * @author The ProActive Team
 */
public abstract class MatSciJVMSpawnHelper extends StandardJVMSpawnHelper {

    /**
     * Deployed DataspaceRegistry Interface
     */
    protected DataspaceRegistry dsregistry;

    /*
     * Deployed MatSciJVMProcessInterface Interface
     */
    protected MatSciJVMProcessInterface jvmitf;

    protected MatSciJVMSpawnHelper() {
        super();
        itfNames.add(DataspaceRegistry.class.getName());
        itfNames.add(MatSciJVMProcessInterface.class.getName());
    }

    public MatSciJVMProcessInterface getJvmInterface() {
        return jvmitf;
    }

    public DataspaceRegistry getDSRegistry() {
        return dsregistry;
    }

    public void shutdown() {
        try {
            this.jvmitf.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected void updateStubs(Registry registry) throws RemoteException, NotBoundException {
        this.dsregistry = (DataspaceRegistry) registry.lookup(DataspaceRegistry.class.getName());
        this.jvmitf = (MatSciJVMProcessInterface) registry.lookup(MatSciJVMProcessInterface.class.getName());
        stubs.put(MatSciJVMProcessInterface.class.getName(), this.dsregistry);
        stubs.put(DataspaceRegistry.class.getName(), jvmitf);
    }
}
