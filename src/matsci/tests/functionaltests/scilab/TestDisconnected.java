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

import jdbm.PrimaryHashMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.matsci.client.embedded.MatSciTaskRepository;
import org.ow2.proactive.scheduler.ext.matsci.middleman.AOMatSciEnvironment;
import org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
import org.ow2.proactive.scheduler.ext.scilab.middleman.AOScilabEnvironment;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;


/**
 * TestDisconnected
 *
 * @author The ProActive Team
 */
public class TestDisconnected extends AbstractScilabTest {

    static final int NB_ITER = 5;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");

    File middlemanJobFile = new File(TMPDIR, AOScilabEnvironment.MIDDLEMAN_JOBS_FILE_NAME);

    File scilabJobFile = new File(TMPDIR, ScilabTaskRepository.SCILAB_EMBEDDED_JOBS_FILE_NAME);

    @org.junit.Test
    public void run() throws Throwable {
        // Start the scheduler
        start();

        // delete all middleman disconnected mode files

        RecordManager recMan = RecordManagerFactory.createRecordManager(middlemanJobFile.getCanonicalPath());

        PrimaryHashMap allJobs = recMan.hashMap(AOMatSciEnvironment.MIDDLEMAN_JOBS_RECORD_NAME);
        // we clear the standard jobs from last session if any
        allJobs.clear();
        PrimaryHashMap recordedJobs = recMan.hashMap(AOMatSciEnvironment.MIDDLEMAN_RECORDEDJOBS_RECORD_NAME);
        recordedJobs.clear();
        PrimaryHashMap mappingSeqToJobID = recMan
                .hashMap(AOMatSciEnvironment.MIDDLEMAN_SEQTOJOBID_RECORD_NAME);
        mappingSeqToJobID.clear();
        PrimaryHashMap sessions = recMan.hashMap(AOMatSciEnvironment.SESSION_RECORD_NAME);
        sessions.clear();

        recMan.commit();
        try {
            recMan.close();
        } catch (Throwable e) {
        }

        // delete all scilab jvm disconnected mode files

        recMan = RecordManagerFactory.createRecordManager(scilabJobFile.getCanonicalPath());

        allJobs = recMan.hashMap(MatSciTaskRepository.EMBEDDED_JOBS_RECORD_NAME);
        // we clear the standard jobs from last session if any
        allJobs.clear();
        recordedJobs = recMan.hashMap(MatSciTaskRepository.EMBEDDED_RECORDEDJOBS_RECORD_NAME);
        recordedJobs.clear();
        mappingSeqToJobID = recMan.hashMap(MatSciTaskRepository.EMBEDDED_SEQTOJOBID_RECORD_NAME);
        mappingSeqToJobID.clear();

        recMan.commit();
        try {
            recMan.close();
        } catch (Throwable e) {
        }

        for (int i = 1; i <= NB_ITER; i++) {
            runCommand(NB_ITER, i);
        }
    }

    protected void runCommand(int nb_iter, int index) throws Exception {

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
                    .toString(), "" + nb_iter, "" + index, "TestDisconnected", "" + runAsMe);
        } else {
            pb.command("scilab", "-nw", "-f", (new File(test_home + fs + "RunTestDisconnected.sci"))
                    .getCanonicalPath(), "-args", schedURI.toString(), credFile.toString(), "" + nb_iter, "" +
                index, "TestDisconnected", "" + runAsMe);
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

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p, "[TestDisconnected_" + index + "]",
            System.out, System.err);
        Thread t1 = new Thread(lt1, "TestDisconnected_" + index);
        t1.setDaemon(true);
        t1.start();

        //ProcessResult pr = IOTools.blockingGetProcessResult(p, 580000);

        int code = p.waitFor();
        if (reFile.exists()) {
            // we restart in case of JIMS loading bug
            runCommand(nb_iter, index);
            return;
        }

        if (index < nb_iter) {
            assertTrue("TestDisconnected_" + index + " passed", code == 0);
        } else {
            okFile.exists();
        }

    }
}
