/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.scheduler.ext.matlab.middleman;

import java.rmi.server.UnicastRemoteObject;

import org.objectweb.proactive.api.PAActiveObject;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.middleman.MiddlemanDeployer;


/**
 * MiddlemanDeployer a main class used to deploy the environment and dataspace dsregistry in the middleman JVM
 *
 * @author The ProActive Team
 */
public class MatlabMiddlemanDeployer extends MiddlemanDeployer {

    /**
     * Standalone objects
     */
    protected AOMatlabEnvironment paenv_matlab;

    /**
     * ProActive Stubs
     */
    protected AOMatlabEnvironment pastub_paenv_matlab;

    /**
     * RMI stubs
     */
    protected MatSciEnvironment rmistub_paenv_matlab;

    public static MiddlemanDeployer getInstance() {
        if (instance == null) {
            instance = new MatlabMiddlemanDeployer();
        }
        if (!(instance instanceof MatlabMiddlemanDeployer)) {
            throw new IllegalStateException(instance.getClass() + " is not expected");
        }
        return instance;
    }

    protected void init() throws Exception {
        paenv_matlab = new AOMatlabEnvironment(debug);
        super.init(paenv_matlab);
    }

    protected void activate() throws Exception {
        super.activate();
        pastub_paenv_matlab = PAActiveObject.turnActive(paenv_matlab, mainNode);

    }

    protected void terminateAO() throws Exception {
        paenv_matlab.terminateFast();
        super.terminateAO();
    }

    @Override
    protected void unbindAll() throws Exception {
        registry.unbind(AOMatlabEnvironment.class.getName());
        super.unbindAll();
    }

    protected void unexportAll() throws Exception {
        UnicastRemoteObject.unexportObject(pastub_paenv_matlab, true);
        super.unexportAll();
    }

    protected void exportAll() throws Exception {
        rmistub_paenv_matlab = (MatSciEnvironment) UnicastRemoteObject.exportObject(pastub_paenv_matlab);
        super.exportAll();
    }

    protected void rebindAll() throws Exception {
        registry.rebind(AOMatlabEnvironment.class.getName(), rmistub_paenv_matlab);
        super.rebindAll();
    }

    public static void main(String[] args) throws Exception {
        MatlabMiddlemanDeployer dep = (MatlabMiddlemanDeployer) MatlabMiddlemanDeployer.getInstance();
        dep.setPort(Integer.parseInt(System.getProperty("rmi.port")));
        dep.setDebug(Boolean.parseBoolean(args[0]));

        dep.submitMain();
    }
}
