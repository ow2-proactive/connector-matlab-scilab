/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
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
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.matsci.middleman;

import org.objectweb.proactive.api.PAActiveObject;
import org.ow2.proactive.scheduler.ext.matlab.middleman.AOMatlabEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.common.DataspaceRegistry;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciJVMProcessInterface;
import org.ow2.proactive.scheduler.ext.scilab.middleman.AOScilabEnvironment;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


/**
 * MiddlemanDeployer a main class used to deploy the environment and dataspace registry in the middleman JVM
 *
 * @author The ProActive Team
 */
public class MiddlemanDeployer {

    private static AOScilabEnvironment paenv_scilab;
    private static AOMatlabEnvironment paenv_matlab;
    private static AODataspaceRegistry reg;
    private static MatSciJVMProcessInterfaceImpl itf;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("rmi.port"));

        paenv_scilab = (AOScilabEnvironment) PAActiveObject.newActive(AOScilabEnvironment.class.getName(),
                new Object[0]);
        paenv_matlab = (AOMatlabEnvironment) PAActiveObject.newActive(AOMatlabEnvironment.class.getName(),
                new Object[0]);

        reg = (AODataspaceRegistry) PAActiveObject.newActive(AODataspaceRegistry.class.getName(),
                new Object[0]);

        itf = (MatSciJVMProcessInterfaceImpl) PAActiveObject.newActive(MatSciJVMProcessInterfaceImpl.class
                .getName(), new Object[] { paenv_scilab, paenv_matlab });

        MatSciEnvironment stubenv_sci = (MatSciEnvironment) UnicastRemoteObject.exportObject(paenv_scilab);
        MatSciEnvironment stubenv_mat = (MatSciEnvironment) UnicastRemoteObject.exportObject(paenv_matlab);
        DataspaceRegistry stubreg = (DataspaceRegistry) UnicastRemoteObject.exportObject(reg);
        MatSciJVMProcessInterface stubjvm = (MatSciJVMProcessInterface) UnicastRemoteObject.exportObject(itf);
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(AOScilabEnvironment.class.getName(), stubenv_sci);
        registry.rebind(AOMatlabEnvironment.class.getName(), stubenv_mat);
        registry.rebind(DataspaceRegistry.class.getName(), stubreg);
        registry.rebind(MatSciJVMProcessInterface.class.getName(), stubjvm);
    }
}
