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
package org.ow2.proactive.scheduler.ext.matlab.client.embedded.util;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.embedded.util.MatSciJVMSpawnHelper;


/**
 * MatlabJVMSpawnHelper
 *
 * @author The ProActive Team
 */
public class MatlabJVMSpawnHelper extends MatSciJVMSpawnHelper {

    protected static MatlabJVMSpawnHelper instance = null;

    /**
     * Deployed MatlabEnvironment Interface
     */
    protected MatSciEnvironment matlab_environment;

    public static final String STUB_NAME = "org.ow2.proactive.scheduler.ext.matlab.middleman.AOMatlabEnvironment";

    protected MatlabJVMSpawnHelper() {
        super();
        itfNames.add(STUB_NAME);
    }

    public static MatlabJVMSpawnHelper getInstance() {
        if (instance == null) {
            instance = new MatlabJVMSpawnHelper();
        }
        return instance;
    }

    public MatSciEnvironment getMatlabEnvironment() {
        return matlab_environment;
    }

    @Override
    protected void updateStubs(Registry registry) throws RemoteException, NotBoundException {
        super.updateStubs(registry);
        this.matlab_environment = (MatSciEnvironment) registry.lookup(STUB_NAME);
        stubs.put(STUB_NAME, this.matlab_environment);
    }

}
