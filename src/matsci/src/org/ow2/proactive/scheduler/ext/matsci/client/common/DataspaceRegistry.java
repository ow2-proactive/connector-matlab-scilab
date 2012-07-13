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
package org.ow2.proactive.scheduler.ext.matsci.client.common;

import org.ow2.proactive.scheduler.ext.matsci.client.common.data.Pair;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.UnReifiable;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * DataspaceRegistry the interface to the middleman Dataspace registry (it creates dataspaces on given directories)
 *
 * @author The ProActive Team
 */
public interface DataspaceRegistry extends Remote {

    /**
     * Creates a dataspace for the given path
     * @param path path where to create the dataspace
     * @return
     * @throws java.rmi.RemoteException
     */
    public UnReifiable<Pair<String, String>> createDataSpace(String path) throws RemoteException;

    /**
     * Initializes the registry by specifying debug mode, input and output spaces base names
     * @param inbasename base name of each input space created
     * @param outbasename base name of each output space created
     * @param debug debug mode
     * @throws java.rmi.RemoteException
     */
    public void init(String inbasename, String outbasename, boolean debug) throws RemoteException;
}
