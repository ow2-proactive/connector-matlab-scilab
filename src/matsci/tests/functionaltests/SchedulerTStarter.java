/*
 * ################################################################
 *
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
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package functionaltests;

import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.resourcemanager.RMFactory;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.scheduler.SchedulerFactory;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.tests.ProActiveSetup2;

import java.io.File;
import java.io.Serializable;
import java.net.URI;


/**
 * Starts Scheduler and Resource Manager.
 * It is used to start scheduler in a separate JVM than the Test itself.
 *
 * @author The ProActive Team
 */
public class SchedulerTStarter implements Serializable {

    protected String rmUsername = "demo";
    protected String rmPassword = "demo";

    protected static String schedulerDefaultURL = "//Localhost/";

    private static final int DEFAULT_NUMBER_OF_NODES = 4;
    private static final int DEFAULT_NODE_TIMEOUT = 30 * 1000;
    private static int nodeTimeout = DEFAULT_NODE_TIMEOUT;

    private static Process p;

    static final String fs = System.getProperty("file.separator");

    /**
     * Start a Scheduler and Resource Manager.
     */
    public static void startSchedulerCmdLine() throws Exception {

        File schedHome = new File(System.getProperty("pa.scheduler.home")).getCanonicalFile();
        File rmHome = new File(System.getProperty("pa.rm.home")).getCanonicalFile();

        System.out.println(schedHome);

        p = null;
        if (OperatingSystem.getOperatingSystem().equals(OperatingSystem.unix)) {
            p = Runtime.getRuntime().exec(
                    new String[] { "/bin/bash",
                            schedHome + fs + "bin" + fs + "unix" + fs + "scheduler-start-clean" }, null,
                    new File(schedHome + fs + "bin" + fs + "unix"));
        } else {
            p = Runtime.getRuntime().exec(
                    new String[] { "cmd.exe", "/c",
                            schedHome + fs + "bin" + fs + "windows" + fs + "scheduler-start-clean.bat" },
                    null, new File(schedHome + fs + "bin" + fs + "unix"));
        }

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[SchedulerTStarter]", System.out,
            System.err);
        Thread t1 = new Thread(lt1, "SchedulerTStarter");
        t1.setDaemon(true);
        t1.start();

        // waiting the initialization
        RMAuthentication rmAuth = RMConnection.waitAndJoin("rmi://localhost:" +
            CentralPAPropertyRepository.PA_RMI_PORT.getValue() + "/");

        System.out.println("RM successfully joined.");

        SchedulerConnection.waitAndJoin("rmi://localhost:" +
            CentralPAPropertyRepository.PA_RMI_PORT.getValue() + "/");
        System.out.println("Scheduler successfully joined.");

    }

    public static void killSchedulerCmdLine() {
        p.destroy();
    }

    /**
     * Start a Scheduler and Resource Manager. Must be called with following
     * arguments:
     * <ul>
     * <li>first argument: true if the RM started with the scheduler has to start some nodes
     * <li>second argument: path to a scheduler Properties file
     * <li>third argument: path to a RM Properties file
     * </ul>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                "SchedulerTStarter must be started with 3 parameters: localhodes schedPropPath rmPropPath");
        }

        if (args.length == 4) {
            createWithExistingRM(args);
        } else {
            createRMAndScheduler(args);
        }
        System.out.println("Scheduler successfully created !");
    }

    public static void createRMAndScheduler(String[] args) throws Exception {
        ProActiveSetup2 setup = new ProActiveSetup2();
        boolean localnodes;
        String schedPropPath;
        String RMPropPath;
        if (args != null && args.length == 3) {
            localnodes = Boolean.valueOf(args[0]);
            schedPropPath = args[1];
            RMPropPath = args[2];
        } else {
            localnodes = true;
            schedPropPath = System.getProperty(PASchedulerProperties.PA_SCHEDULER_PROPERTIES_FILEPATH);
            RMPropPath = System.getProperty(PAResourceManagerProperties.PA_RM_PROPERTIES_FILEPATH);
        }

        PAResourceManagerProperties.updateProperties(RMPropPath);
        PASchedulerProperties.updateProperties(schedPropPath);

        //Starting a local RM
        RMFactory.setOsJavaProperty();
        RMFactory.startLocal();

        // waiting the initialization
        RMAuthentication rmAuth = RMConnection.waitAndJoin(null);

        SchedulerFactory.createScheduler(new URI("rmi://localhost:" +
            CentralPAPropertyRepository.PA_RMI_PORT.getValue() + "/"),
                PASchedulerProperties.SCHEDULER_DEFAULT_POLICY.getValueAsString());

        SchedulerConnection.waitAndJoin(schedulerDefaultURL);
        System.out.println("Scheduler successfully created at " + "rmi://localhost:" +
            CentralPAPropertyRepository.PA_RMI_PORT.getValue() + "/");
        if (localnodes) {
            RMTHelper.getDefaultInstance().createLocalNodeSource();
        }
    }

    public static void createWithExistingRM(String[] args) throws Exception {
        String schedPropPath = args[1];
        String rmUrl = args[3];

        System.out.println("Creating with existing " + rmUrl);

        PASchedulerProperties.updateProperties(schedPropPath);

        SchedulerFactory.createScheduler(new URI(rmUrl), PASchedulerProperties.SCHEDULER_DEFAULT_POLICY
                .getValueAsString());

        SchedulerConnection.waitAndJoin(schedulerDefaultURL);

    }
}
