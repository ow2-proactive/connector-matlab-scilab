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


import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;

import org.apache.commons.io.IOUtils;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository;
import org.ow2.proactive.scheduler.ext.matlab.middleman.AOMatlabEnvironment;

import functionaltests.utils.SchedulerFunctionalTest;
import functionaltests.utils.SchedulerTHelper;
import functionaltests2.SchedulerCommandLine;


/**
 * AbstractScilabTest
 *
 * @author The ProActive Team
 */
public class AbstractMatlabTest extends SchedulerFunctionalTest {

    String fs = System.getProperty("file.separator");

    File mat_tb_home;

    File test_home;

    File credFile;

    File logFile;

    URI schedURI;

    protected String adminName = "demo";
    protected String adminPwd = "demo";

    protected Credentials adminCredentials;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");

    protected void init() throws Exception {

        mat_tb_home = new File(System.getProperty("pa.matlab.home")).getCanonicalFile();

        test_home = resourceToFile("RunUnitTest.m").getParentFile();

        credFile = new File(test_home, "demo.cred");

        schedURI = new URI(SchedulerTHelper.getLocalUrl());

        // delete all db files
        File[] dbJobFiles = new File(TMPDIR).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(AOMatlabEnvironment.MIDDLEMAN_JOBS_FILE_NAME)) {
                    return true;
                } else if (name.startsWith(MatlabTaskRepository.MATLAB_EMBEDDED_JOBS_FILE_NAME)) {
                    return true;
                    // TODO : change below the value to JobDB.DEFAULT_STATUS_FILENAME
                } else if (name.startsWith("SmartProxy")) {
                    return true;
                }
                return false;
            }
        });

        for (File f : dbJobFiles) {
            try {
                System.out.println("Deleting " + f);
                f.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    protected File resourceToFile(String resourcePath) {
        try {

            return new File(getClass().getResource(resourcePath).toURI());
        } catch(URISyntaxException e) {
            return new File(getClass().getResource(resourcePath).getPath());
        }

    }


    protected void start() throws Exception {
        init();

        String schedSettings = System.getProperty(PASchedulerProperties.PA_SCHEDULER_PROPERTIES_FILEPATH);
        String rmSettings = System.getProperty(PAResourceManagerProperties.PA_RM_PROPERTIES_FILEPATH);
        schedulerHelper = new SchedulerTHelper(true, schedSettings, rmSettings, null);
        
        SchedulerAuthenticationInterface auth = schedulerHelper.getSchedulerAuth();

        PublicKey pubKey = auth.getPublicKey();
        Credentials cred;
        if (System.getProperty("proactive.test.login.user") != null) {
            cred = Credentials.createCredentials(new CredData(
                System.getProperty("proactive.test.login.user"), System
                        .getProperty("proactive.test.password.user")), pubKey);
        } else {
            cred = Credentials.createCredentials(new CredData(adminName, adminPwd), pubKey);
        }

        cred.writeToDisk(credFile.toString());
        adminCredentials = cred;
    }

    protected void end() throws Exception {
        schedulerHelper.killScheduler();
    }

    protected void startCmdLine(String uri, File proactiveConf) throws Exception {
        init();
        SchedulerCommandLine.startSchedulerCmdLine(false, proactiveConf);

        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin((uri != null) ? uri
                : schedURI.toString());

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
        adminCredentials = cred;
    }

    protected void restartCmdLine(String uri, File proactiveConf) throws Exception {
        SchedulerCommandLine.killSchedulerCmdLine();
        SchedulerCommandLine.startSchedulerCmdLine(true, proactiveConf);
        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin((uri != null) ? uri
                : schedURI.toString());
    }

    protected void killScheduler() {
        SchedulerCommandLine.killSchedulerCmdLine();
    }

    protected ProcessBuilder initCommand(String testName, int nb_iter) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(mat_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }

        logFile = new File(mat_tb_home, testName+".log");
        if (logFile.exists()) {
            logFile.delete();
        }
        // If no property specified for matlab exe suppose it's in the PATH
        String matlabExe = System.getProperty("matlab.bin.path", "matlab");
        // Build the matlab command that will run the test
        String matlabCmd = String.format("addpath('%s');", this.test_home);
        if (System.getProperty("disable.popup") != null) {
          matlabCmd += "PAoptions('EnableDisconnectedPopup', false);";
        }
        matlabCmd += getMatlabFunction(nb_iter,testName,runAsMe);
        return pb.command(matlabExe, "-nodesktop", "-nosplash", "-logfile", logFile.getAbsolutePath(), "-r", matlabCmd);
    }

    // sub-classes may override it
    protected String getMatlabFunction(int nb_iter, String testName, int runAsMe) {
        return String.format("RunUnitTest('%s', '%s', '%s', %d, '%s', %d);",
                this.schedURI, this.credFile, this.mat_tb_home, nb_iter, testName, runAsMe);
    }

    protected void runCommand(String testName, int nb_iter) throws Exception {

        ProcessBuilder pb = initCommand(testName, nb_iter);
        System.out.println("Running command : " + pb.command());

        File okFile = new File(mat_tb_home + fs + "ok.tst");
        File koFile = new File(mat_tb_home + fs + "ko.tst");
        File startFile = new File(mat_tb_home + fs + "start.tst");

        if (okFile.exists()) {
            okFile.delete();
        }

        if (koFile.exists()) {
            koFile.delete();
        }
        if (startFile.exists()) {
            startFile.delete();
        }

        Process p = pb.start();

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(), "[" + testName + "]", System.out);
        Thread t1 = new Thread(lt1, testName);
        t1.setDaemon(true);
        t1.start();


        p.waitFor();
        // sometimes a matlab laucher is used and it returns immediately, the waitFor will not be of use and we need to wait
        // for files to be created
        while (!startFile.exists()) {
            Thread.sleep(100);
        }

        while (!okFile.exists() && !koFile.exists()) {
            Thread.sleep(100);
        }

        if (logFile.exists()) {
            System.out.println(IOUtils.toString(logFile.toURI()));
        }

        assertTrue(testName + " passed", okFile.exists());
    }
}