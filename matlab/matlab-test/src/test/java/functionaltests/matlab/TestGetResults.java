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
package functionaltests.matlab;

import static junit.framework.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;


/**
 * TestDisconnected
 *
 * @author The ProActive Team
 */
@Ignore // Unstable
public class TestGetResults extends AbstractMatlabTest {
    static final int NB_ITER = 3;

    private int index;

    @Before
    public void before() throws Exception {
        start();
    }

    @org.junit.Test
    public void run() throws Throwable {
        for (this.index = 1; this.index <= NB_ITER; this.index++) {
            runCommand("TestGetResults", NB_ITER);
        }
    }

    @Override
    protected String getMatlabFunction(int nb_iter, String testName, int runAsMe) {
        return String.format("RunTestDisconnected('%s', '%s', '%s', %d, %d, '%s', %d);",
                             super.schedURI,
                             super.credFile,
                             super.mat_tb_home,
                             nb_iter,
                             this.index,
                             testName,
                             runAsMe);
    }

    protected void runCommand(String testName, int nb_iter, int index) throws Exception {

        ProcessBuilder pb = initCommand(testName, nb_iter);
        System.out.println("Running command : " + pb.command());

        File okFile = new File(mat_tb_home + fs + "ok.tst");
        File koFile = new File(mat_tb_home + fs + "ko.tst");
        File reFile = new File(mat_tb_home + fs + "re.tst");
        File startFile = new File(mat_tb_home + fs + "start.tst");

        if (okFile.exists()) {
            okFile.delete();
        }

        if (koFile.exists()) {
            koFile.delete();
        }
        if (reFile.exists()) {
            reFile.delete();
        }
        if (startFile.exists()) {
            startFile.delete();
        }

        Process p = pb.start();

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(),
                                                              "[" + testName + "_" + index + "]",
                                                              System.out);
        Thread t1 = new Thread(lt1, testName + "_" + index);
        t1.setDaemon(true);
        t1.start();

        int code = p.waitFor();
        while (!startFile.exists()) {
            Thread.sleep(100);
        }

        while (!okFile.exists() && !koFile.exists() && !reFile.exists()) {
            Thread.sleep(100);
        }

        if (logFile.exists()) {
            System.out.println(IOUtils.toString(logFile.toURI()));
        }

        if (index < nb_iter) {
            assertTrue(testName + "_" + index + " passed", !koFile.exists());
        } else {
            assertTrue(testName + " passed", okFile.exists());
        }
    }

    @After
    public void after() throws Exception {
        end();
    }
}
