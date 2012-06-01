package functionaltests.scilab;

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
public class AbstractScilabTest extends FunctionalTest {

    String fs = System.getProperty("file.separator");

    File sci_tb_home;

    File test_home;

    File credFile;

    URI schedURI;

    String localhost;

    int default_nb_iter = 10;

    private String adminName = "demo";
    private String adminPwd = "demo";

    protected void init() throws Exception {

        sci_tb_home = (new File(System.getProperty("pa.matsci.home") + fs + "scilab" + fs + "PAscheduler"))
                .getCanonicalFile();

        test_home = (new File(System.getProperty("pa.matsci.home") + fs + "classes" + fs + "matsciTests" +
            fs + "functionaltests" + fs + "scilab")).getCanonicalFile();

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

    @org.junit.Test
    public void run() throws Throwable {
        init();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(sci_tb_home);
        pb.redirectErrorStream(true);
        if (System.getProperty("scilab.bin.path") != null) {
            pb.command(System.getProperty("scilab.bin.path"), "-nw", "-f", (new File(test_home + fs +
                "PrepareTest.sci")).getCanonicalPath());
        } else {
            pb.command("scilab", "-nw", "-f", (new File(test_home + fs + "PrepareTest.sci"))
                    .getCanonicalPath());
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

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[AbstractScilabTest]", System.out,
            System.err);
        Thread t1 = new Thread(lt1, "AbstractScilabTest");
        t1.setDaemon(true);
        t1.start();

        //ProcessResult pr = IOTools.blockingGetProcessResult(p, 580000);

        p.waitFor();

        assertTrue("Prepare Scilab Test passed", okFile.exists());

    }

    protected void runCommand(String testName) throws Exception {
        // Start the scheduler
        start();
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(sci_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }
        if (System.getProperty("scilab.bin.path") != null) {
            pb.command(System.getProperty("scilab.bin.path"), "-nw", "-f", (new File(test_home + fs +
                "RunUnitTest.sci")).getCanonicalPath(), "-args", schedURI.toString(), credFile.toString(),
                    "" + default_nb_iter, testName, "" + runAsMe);
        } else {
            pb.command("scilab", "-nw", "-f", (new File(test_home + fs + "RunUnitTest.sci"))
                    .getCanonicalPath(), "-args", schedURI.toString(), credFile.toString(), "" +
                default_nb_iter, testName, "" + runAsMe);
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

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[" + testName + "]", System.out, System.err);
        Thread t1 = new Thread(lt1, testName);
        t1.setDaemon(true);
        t1.start();

        //ProcessResult pr = IOTools.blockingGetProcessResult(p, 580000);

        p.waitFor();

        assertTrue(testName + " passed", okFile.exists());
    }
}
