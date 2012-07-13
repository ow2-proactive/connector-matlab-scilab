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
package functionaltests.matlab;

import functionaltests.SchedulerTStarter;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.tests.FunctionalTest;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.security.PublicKey;

import static junit.framework.Assert.assertTrue;


/**
 * AbstractScilabTest
 *
 * @author The ProActive Team
 */
public class AbstractMatlabTest extends FunctionalTest {

    String fs = System.getProperty("file.separator");

    File mat_tb_home;

    File test_home;

    File credFile;

    URI schedURI;

    String localhost;

    private String adminName = "demo";
    private String adminPwd = "demo";

    protected void init() throws Exception {

        mat_tb_home = new File(System.getProperty("pa.matlab.home")).getCanonicalFile();

        test_home = (new File(System.getProperty("pa.matsci.home") + fs + "classes" + fs + "matsciTests" +
            fs + "functionaltests" + fs + "matlab")).getCanonicalFile();

        credFile = new File(test_home, "demo.cred");

        localhost = InetAddress.getLocalHost().getHostName();

        schedURI = new URI("rmi://" + localhost + ":" + CentralPAPropertyRepository.PA_RMI_PORT.getValue() +
            "/");

    }

    protected void start() throws Exception {
        init();
        SchedulerTStarter.createRMAndScheduler(null);

        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(schedURI.toString());

        PublicKey pubKey = auth.getPublicKey();
        Credentials cred = null;
        if (System.getProperty("proactive.test.login.user") != null) {
            cred = Credentials.createCredentials(new CredData(
                System.getProperty("proactive.test.login.user"), System
                        .getProperty("proactive.test.password.user")), pubKey);
        } else {
            cred = Credentials.createCredentials(new CredData(adminName, adminPwd), pubKey);
        }

        cred.writeToDisk(credFile.toString());
    }

    protected void runCommand(String testName, int nb_iter) throws Exception {
        // Start the scheduler
        start();
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(mat_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }
        if (System.getProperty("matlab.bin.path") != null) {
            pb.command(System.getProperty("matlab.bin.path"), "-nodesktop", "-nosplash", "-r", "addpath('" +
                test_home + "');RunUnitTest('" + schedURI.toString() + "','" + credFile.toString() + "','" +
                mat_tb_home + "'," + nb_iter + ",'" + testName + "'," + runAsMe + ");");
        } else {
            pb.command("matlab", "-nodesktop", "-nosplash", "-r", "addpath('" + test_home +
                "');RunUnitTest('" + schedURI.toString() + "','" + credFile.toString() + "','" + mat_tb_home +
                "'," + nb_iter + ",'" + testName + "'," + runAsMe + ");");
        }
        System.out.println("Running command : " + pb.command());

        File okFile = new File(mat_tb_home + fs + "ok.tst");
        File koFile = new File(mat_tb_home + fs + "ko.tst");

        if (okFile.exists()) {
            okFile.delete();
        }

        if (koFile.exists()) {
            koFile.delete();
        }

        Process p = pb.start();

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[" + testName + "]", System.out, System.err);
        Thread t1 = new Thread(lt1, testName);
        t1.setDaemon(true);
        t1.start();

        //ProcessResult pr = IOTools.blockingGetProcessResult(p, 580000);

        p.waitFor();

        assertTrue(testName + " passed", okFile.exists());
    }
}
