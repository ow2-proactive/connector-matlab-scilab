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
package org.ow2.proactive.scheduler.ext.scilab.client.embedded.util;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import javax.swing.*;

import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.embedded.LoginFrame;
import org.ow2.proactive.scheduler.ext.matsci.client.embedded.util.MatSciJVMSpawnHelper;


/**
 * ScilabJVMSpawnHelper
 *
 * @author The ProActive Team
 */
public class ScilabJVMSpawnHelper extends MatSciJVMSpawnHelper {

    protected static ScilabJVMSpawnHelper instance = null;

    public static final String STUB_NAME = "org.ow2.proactive.scheduler.ext.scilab.middleman.AOScilabEnvironment";

    /**
     * Login Frame
     */
    LoginFrame lf;

    /**
     * Deployed MatlabEnvironment Interface
     */
    protected MatSciEnvironment scilab_environment;

    protected ScilabJVMSpawnHelper() {
        super();
        itfNames.add(STUB_NAME);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (jvmitf != null) {
                    try {
                        jvmitf.shutdown();
                    } catch (Exception e) {

                    }
                }
            }
        }));
    }

    public static ScilabJVMSpawnHelper getInstance() {
        if (instance == null) {
            instance = new ScilabJVMSpawnHelper();
        }
        return instance;
    }

    public MatSciEnvironment getScilabEnvironment() {
        return scilab_environment;
    }

    @Override
    protected void updateStubs(Registry registry) throws RemoteException, NotBoundException {
        super.updateStubs(registry);
        this.scilab_environment = (MatSciEnvironment) registry.lookup(STUB_NAME);
        stubs.put(STUB_NAME, this.scilab_environment);
    }

    /**
     * Starts the Login GUI
     */
    public void startLoginGUI() {
        if (scilab_environment == null) {
            throw new IllegalStateException("Environment not initialized");
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                lf = new LoginFrame(scilab_environment, true);
                lf.start();
            }
        });
    }

    /**
     * Returns the number of login attempts
     *
     * @return
     */
    public int getNbAttempts() {
        if (lf != null)
            return lf.getNbAttempts();
        return 0;
    }
}
