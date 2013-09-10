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
package functionaltests.scilab;

import java.io.File;

import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.junit.Ignore;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


/**
 * TestUltraConnected this test tests that a Scilab serie of jobs doesn't fail in case of scheduler restart. A scheduler
 * restart should be transparent to the scilab user
 *
 * @author The ProActive Team
 */
@Ignore // unstable
public class TestUltraConnected extends AbstractScilabTest {

    static final int NB_ITER = 2;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");

    @org.junit.Test
    public void run() throws Throwable {

        test_home = (new File(System.getProperty("pa.matsci.home") + fs + "classes" + fs + "matsciTests" +
            fs + "functionaltests" + fs + "scilab")).getCanonicalFile();
        // Start the scheduler
        File proactiveConf = new File(test_home, "ProActiveConfiguration.xml");
        startCmdLine("pnp://localhost:9999", proactiveConf);

        runCommand(NB_ITER, "TestBasic");

    }

    protected void runCommand(int nb_iter, String functionName) throws Exception {

        ProcessBuilder pb = new ProcessBuilder();
        File proactiveConf = new File(test_home, "ProActiveConfiguration.xml");
        pb.directory(sci_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }
        if (System.getProperty("scilab.bin.path") != null) {
            pb.command(System.getProperty("scilab.bin.path"), "-nw", "-f", (new File(test_home + fs +
                "RunUltraConnected.sci")).getCanonicalPath(), "-args", "pnp://localhost:9999", credFile
                    .toString(), "" + nb_iter, functionName, "" + runAsMe);
        } else {
            pb.command("scilab", "-nw", "-f", (new File(test_home + fs + "RunUltraConnected.sci"))
                    .getCanonicalPath(), "-args", "pnp://localhost:9999", credFile.toString(), "" + nb_iter,
                    functionName, "" + runAsMe);
        }
        System.out.println("Running command : " + pb.command());

        File connectFile = new File(sci_tb_home + fs + "connect.tst");
        File okFile = new File(sci_tb_home + fs + "ok.tst");
        File koFile = new File(sci_tb_home + fs + "ko.tst");
        File reFile = new File(sci_tb_home + fs + "re.tst");

        if (connectFile.exists()) {
            connectFile.delete();
        }
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

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[TestUltraConnected]", System.out,
            System.err);
        Thread t1 = new Thread(lt1, "TestUltraConnected");
        t1.setDaemon(true);
        t1.start();

        //ProcessResult pr = IOTools.blockingGetProcessResult(p, 580000);

        int code = -1;
        try {
            code = p.exitValue();
        } catch (Exception e) {

        }
        while (!connectFile.exists() && !koFile.exists() && !reFile.exists() && code == -1) {
            Thread.sleep(100);
            try {
                code = p.exitValue();
            } catch (Exception e) {
            }
        }
        if (connectFile.exists()) {
            System.out.println("Scilab session is connected.");
        }

        if (reFile.exists()) {
            // we restart in case of JIMS loading bug
            runCommand(nb_iter, functionName);
            return;
        }
        if (code != -1 || koFile.exists()) {
            assertFalse("Error during startup", code != -1 || koFile.exists());
        }
        // we are now connected to a scheduler, we will randomly restart the scheduler a nb_iter number of times

        for (int i = 0; i < nb_iter + 1; i++) {
            Thread.sleep((10 + Math.round(10 * Math.random())) * 1000);

            restartCmdLine("pnp://localhost:9999", proactiveConf);
        }

        p.waitFor();

        killScheduler();

        assertTrue(functionName + " passed", okFile.exists());

    }
}
