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

import org.apache.commons.io.FileUtils;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
import org.ow2.proactive.scheduler.ext.scilab.middleman.AOScilabEnvironment;

import java.io.File;

import static junit.framework.Assert.assertTrue;


/**
 * TestGetResults this test tests the PAgetResults function (getting results of previous scilab session)
 *
 * @author The ProActive Team
 */
public class TestGetResults extends AbstractScilabTest {

    static final int NB_ITER = 5;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");

    @org.junit.Test
    public void run() throws Throwable {
        // remove sched DBs to ensure that job index starts at 1
        File schedHome = new File(System.getProperty("pa.scheduler.home")).getCanonicalFile();
        FileUtils.deleteDirectory(new File(schedHome, "RM_DB"));
        FileUtils.deleteDirectory(new File(schedHome, "SCHEDULER_DB"));

        // Start the scheduler
        start();

        for (int i = 1; i <= NB_ITER; i++) {
            runCommand(NB_ITER, i, "TestGetResults");
        }
    }

    protected void runCommand(int nb_iter, int index, String functionName) throws Exception {

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(sci_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }
        if (System.getProperty("scilab.bin.path") != null) {
            pb.command(System.getProperty("scilab.bin.path"), "-nw", "-f", (new File(test_home + fs +
                "RunTestDisconnected.sci")).getCanonicalPath(), "-args", schedURI.toString(), credFile
                    .toString(), "" + nb_iter, "" + index, functionName, "" + runAsMe);
        } else {
            pb.command("scilab", "-nw", "-f", (new File(test_home + fs + "RunTestDisconnected.sci"))
                    .getCanonicalPath(), "-args", schedURI.toString(), credFile.toString(), "" + nb_iter, "" +
                index, functionName, "" + runAsMe);
        }
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

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[" + functionName + "_" + index + "]",
            System.out, System.err);
        Thread t1 = new Thread(lt1, functionName + "_" + index);
        t1.setDaemon(true);
        t1.start();

        //ProcessResult pr = IOTools.blockingGetProcessResult(p, 580000);

        int code = p.waitFor();
        if (reFile.exists()) {
            // we restart in case of JIMS loading bug
            runCommand(nb_iter, index, functionName);
            return;
        }

        if (index < nb_iter) {
            assertTrue(functionName + "_" + index + " passed", !koFile.exists());
            assertTrue(functionName + "_" + index + " passed", code == 0);
        } else {
            assertTrue(functionName + " passed", okFile.exists());
        }

    }
}
