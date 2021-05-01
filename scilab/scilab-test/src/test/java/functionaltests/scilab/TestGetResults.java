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

import org.junit.After;
import org.junit.Before;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;


/**
 * TestGetResults this test tests the PAgetResults function (getting results of previous scilab session)
 *
 * @author The ProActive Team
 */
public class TestGetResults extends AbstractScilabTest {

    static final int NB_ITER = 3;

    @Before
    public void before() throws Exception {
        start();
    }

    @org.junit.Test
    public void run() throws Throwable {
        super.run();
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
            pb.command(System.getProperty("scilab.bin.path"),
                       "-nw",
                       "-f",
                       (new File(test_home + fs + "RunTestDisconnected.sci")).getCanonicalPath(),
                       "-args",
                       schedURI.toString(),
                       credFile.toString(),
                       "" + nb_iter,
                       "" + index,
                       functionName,
                       "" + runAsMe);
        } else {
            pb.command("scilab",
                       "-nw",
                       "-f",
                       (new File(test_home + fs + "RunTestDisconnected.sci")).getCanonicalPath(),
                       "-args",
                       schedURI.toString(),
                       credFile.toString(),
                       "" + nb_iter,
                       "" + index,
                       functionName,
                       "" + runAsMe);
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

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(),
                                                              "[" + functionName + "_" + index + "]",
                                                              System.out);
        Thread t1 = new Thread(lt1, functionName + "_" + index);
        t1.setDaemon(true);
        t1.start();

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

    @After
    public void after() throws Exception {
        end();
    }
}
