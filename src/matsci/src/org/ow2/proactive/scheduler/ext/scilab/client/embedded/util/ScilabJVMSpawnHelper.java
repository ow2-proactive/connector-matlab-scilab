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
package org.ow2.proactive.scheduler.ext.scilab.client.embedded.util;

import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.embedded.LoginFrame;
import org.ow2.proactive.scheduler.ext.matsci.client.embedded.util.MatSciJVMSpawnHelper;

import javax.swing.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;


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
