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
package functionaltests.scilab;

import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;

import org.apache.commons.io.FileUtils;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
import org.ow2.proactive.scheduler.ext.scilab.middleman.AOScilabEnvironment;

import functionaltests.utils.SchedulerFunctionalTest;
import functionaltests.utils.SchedulerTHelper;
import functionaltests2.SchedulerCommandLine;


/**
 * AbstractScilabTest
 *
 * @author The ProActive Team
 */
public class AbstractScilabTest extends SchedulerFunctionalTest {

    String fs = System.getProperty("file.separator");

    File sci_tb_home;

    File test_home;

    File credFile;

    URI schedURI;

    String localhost;

    int testLeak = 0;

    File leakFile = new File(".");

    protected String adminName = "demo";

    protected String adminPwd = "demo";

    protected Credentials adminCredentials;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");

    protected void init() throws Exception {

        sci_tb_home = new File(System.getProperty("pa.scilab.home")).getCanonicalFile();

        test_home = resourceToFile("PrepareTest.sci").getParentFile();

        credFile = new File(test_home, "demo.cred");

        localhost = InetAddress.getLocalHost().getHostName();

        schedURI = new URI(SchedulerTHelper.getLocalUrl());

        // delete all db files
        File[] dbJobFiles = new File(TMPDIR).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(AOScilabEnvironment.MIDDLEMAN_JOBS_FILE_NAME)) {
                    return true;
                } else if (name.startsWith(ScilabTaskRepository.SCILAB_EMBEDDED_JOBS_FILE_NAME)) {
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
        } catch (URISyntaxException e) {
            return new File(getClass().getResource(resourcePath).getPath());
        }

    }

    protected void start() throws Exception {
        init();

        // remove sched DBs to ensure that job index starts at 1
        File schedHome = new File(System.getProperty("pa.scheduler.home")).getCanonicalFile();
        FileUtils.deleteDirectory(new File(schedHome, "db"));

        schedulerHelper = new SchedulerTHelper(true, getSchedulerPropertiesPath(), getRMPropertiesFilePath(), null);

        SchedulerAuthenticationInterface auth = schedulerHelper.getSchedulerAuth();

        PublicKey pubKey = auth.getPublicKey();
        Credentials cred = null;
        if (System.getProperty("proactive.test.login.user") != null) {
            cred = Credentials.createCredentials(new CredData(System.getProperty("proactive.test.login.user"),
                                                              System.getProperty("proactive.test.password.user")),
                                                 pubKey);
        } else {
            cred = Credentials.createCredentials(new CredData(adminName, adminPwd), pubKey);
        }

        cred.writeToDisk(credFile.toString());

        adminCredentials = cred;
    }

    private String getRMPropertiesFilePath() {
        String property = System.getProperty(PAResourceManagerProperties.PA_RM_PROPERTIES_FILEPATH);
        if (property == null || property.isEmpty()) {
            return "./build/scheduler/config/scheduler/settings.ini";
        }
        return property;
    }

    private String getSchedulerPropertiesPath() {
        String property = System.getProperty(PASchedulerProperties.PA_SCHEDULER_PROPERTIES_FILEPATH);
        if (property == null || property.isEmpty()) {
            return "./build/scheduler/config/scheduler/settings.ini";
        }
        return property;
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
            cred = Credentials.createCredentials(new CredData(System.getProperty("proactive.test.login.user"),
                                                              System.getProperty("proactive.test.password.user")),
                                                 pubKey);
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

    // install JIMS and build the toolbox
    public void run() throws Throwable {
        init();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(sci_tb_home);
        pb.redirectErrorStream(true);
        if (System.getProperty("scilab.bin.path") != null) {
            pb.command(System.getProperty("scilab.bin.path"),
                       "-nw",
                       "-f",
                       (new File(test_home + fs + "PrepareTest.sci")).getCanonicalPath());
        } else {
            pb.command("scilab", "-nw", "-f", (new File(test_home + fs + "PrepareTest.sci")).getCanonicalPath());
        }
        System.out.println("Running command : " + pb.command());

        File okFile = new File(sci_tb_home + fs + "ok.tst");
        File koFile = new File(sci_tb_home + fs + "ko.tst");

        if (okFile.exists()) {
            okFile.delete();
        }

        if (koFile.exists()) {
            koFile.delete();
        }

        Process p = pb.start();

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(), "[AbstractScilabTest]", System.out);
        Thread t1 = new Thread(lt1, "AbstractScilabTest");
        t1.setDaemon(true);
        t1.start();

        p.waitFor();

        assertTrue("Prepare Scilab Test passed", okFile.exists());

    }

    protected ProcessBuilder initCommand(String testName, int nb_iter) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(sci_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }
        if (System.getProperty("scilab.test.leaks") != null) {
            testLeak = 1;
            leakFile = new File(test_home + fs + "JIMS.log");
            if (leakFile.exists()) {
                leakFile.delete();
            }
        }
        if (System.getProperty("scilab.bin.path") != null) {
            pb.command(System.getProperty("scilab.bin.path"),
                       "-nw",
                       "-f",
                       (new File(test_home + fs + "RunUnitTest.sci")).getCanonicalPath(),
                       "-args",
                       schedURI.toString(),
                       credFile.toString(),
                       "" + nb_iter,
                       testName,
                       "" + runAsMe,
                       "" + testLeak,
                       "" + leakFile);
        } else {
            pb.command("scilab",
                       "-nw",
                       "-f",
                       (new File(test_home + fs + "RunUnitTest.sci")).getCanonicalPath(),
                       "-args",
                       schedURI.toString(),
                       credFile.toString(),
                       "" + nb_iter,
                       testName,
                       "" + runAsMe,
                       "" + testLeak,
                       "" + leakFile);
        }
        return pb;
    }

    protected void runCommand(String testName, int nb_iter) throws Exception {

        ProcessBuilder pb = initCommand(testName, nb_iter);
        System.out.println("Running command : " + pb.command());

        File okFile = new File(sci_tb_home + fs + "ok.tst");
        File koFile = new File(sci_tb_home + fs + "ko.tst");
        File reFile = new File(sci_tb_home + fs + "re.tst");

        if (okFile.exists()) {
            okFile.delete();
        }

        if (koFile.exists()) {
            koFile.delete();
        }
        if (reFile.exists()) {
            reFile.delete();
        }

        Process p = pb.start();

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(), "[" + testName + "]", System.out);
        Thread t1 = new Thread(lt1, testName);
        t1.setDaemon(true);
        t1.start();

        p.waitFor();
        if (reFile.exists()) {
            // we restart in case of JIMS loading bug
            runCommand(testName, nb_iter);
            return;
        }

        assertTrue(testName + " passed", okFile.exists());

        if (testLeak == 1) {
            File outFile = new File(test_home + fs + "JIMS.out");
            JIMSLogsParser parser = new JIMSLogsParser(leakFile, outFile);
            assertTrue("No leak found in " + outFile + " and " + leakFile, parser.testok());
        }
    }
}
