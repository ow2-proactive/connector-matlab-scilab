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
package org.ow2.proactive.scheduler.ext.matsci.client.embedded;

import jdbm.PrimaryHashMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import org.ow2.proactive.scheduler.ext.common.util.FileUtils;
import org.ow2.proactive.scheduler.ext.matsci.client.common.PASessionState;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.MatSciClientJobInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;


/**
 * MatSciTaskRepository
 *
 * @author The ProActive Team
 */
public abstract class MatSciTaskRepository {

    protected static MatSciTaskRepository instance;

    /**
     * Name of the jobs backup table
     */
    public static final String EMBEDDED_JOBS_RECORD_NAME = "EmbeddedJobs";

    /**
     * Name of the jobs which were submitted during the last Mat/Sci session
     */
    public static final String EMBEDDED_LASTJOBS_RECORD_NAME = "EmbeddedLastJobs";

    /**
     * Name of the jobs backup table being recorded for replay
     */
    public static final String EMBEDDED_RECORDEDJOBS_RECORD_NAME = "EmbeddedRecordedJobs";

    /**
     * Name of the jobs backup table being recorded for replay
     */
    public static final String EMBEDDED_SEQTOJOBID_RECORD_NAME = "EmbeddednSeq2JobId";

    /**
     * Object handling the middlemanJobsFile connection
     */
    protected RecordManager recMan;

    protected static final String TMPDIR = System.getProperty("java.io.tmpdir");

    /**
     * All jobs which have been submitted
     */
    protected PrimaryHashMap<Long, MatSciClientJobInfo> jobs;

    /**
     * All jobs which have been submitted
     */
    protected PrimaryHashMap<Long, MatSciClientJobInfo> recordedJobs;

    /**
     * ids of jobs from the last session
     */
    protected PrimaryHashMap<Long, Long> lastJobs;

    protected PrimaryHashMap<Integer, Long> mappingSeqToJobID;

    protected PASessionState state = PASessionState.NORMAL;

    protected int currentSequenceIndex = 1;

    protected MatSciTaskRepository() {

    }

    protected void init() {
        try {
            recMan = RecordManagerFactory.createRecordManager(getMatSciTaskRepFile().getCanonicalPath());
            // we load the list of standard jobs
            jobs = recMan.hashMap(EMBEDDED_JOBS_RECORD_NAME);

            // we load the list of recorded jobs
            recordedJobs = recMan.hashMap(EMBEDDED_RECORDEDJOBS_RECORD_NAME);
            mappingSeqToJobID = recMan.hashMap(EMBEDDED_SEQTOJOBID_RECORD_NAME);

            // we clear the jobs of the n-2 session, if any
            lastJobs = recMan.hashMap(EMBEDDED_LASTJOBS_RECORD_NAME);
            cleanOldJobs();
            // after this cleaning the jobs remaining are only the ones from last session, so we record their ids and replace
            // lastJobs accordingly
            lastJobs.clear();
            for (Long id : jobs.keySet()) {
                lastJobs.put(id, id);
            }

            recMan.commit();

        } catch (IOError e) {
            if (e.getCause() instanceof InvalidClassException) {
                try {
                    recMan.close();
                } catch (IOException e1) {
                    e.printStackTrace();
                }
                recMan = null;
                cleanDB();
                init();
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void cleanDB() {
        if (recMan != null) {
            throw new IllegalStateException("Connection to a DB is established, cannot clean it");
        }
        // delete all db files
        File[] dbJobFiles = new File(TMPDIR).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(getMatSciTaskRepFile().getName())) {
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

    abstract protected File getMatSciTaskRepFile();

    public MatSciClientJobInfo getNextJob() {
        Long jid = mappingSeqToJobID.get(currentSequenceIndex);

        if (state == PASessionState.REPLAYING && jid != null) {
            currentSequenceIndex++;
            return jobs.get(jid);
        }
        return null;
    }

    public void addJob(MatSciClientJobInfo jinfo) {
        Long jid = Long.parseLong(jinfo.getJobId());
        jobs.put(jid, jinfo);
        if (state == PASessionState.REPLAYING) {
            // we arrived at the end of the replaying session and we now switch to recording
            state = PASessionState.RECORDING;
        }
        if (state == PASessionState.RECORDING) {
            recordedJobs.put(jid, jinfo);
            mappingSeqToJobID.put(currentSequenceIndex, jid);
            currentSequenceIndex++;
        }
        try {
            recMan.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receivedTask(String jid, String tname) {
        Long ljid = Long.parseLong(jid);
        MatSciClientJobInfo jinfo = jobs.get(ljid);
        if (jinfo == null) {
            throw new IllegalArgumentException("Job " + jid + " is unknown.");
        }
        jinfo.receivedTask(tname);
        jobs.put(ljid, jinfo);
        try {
            recMan.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean allReceived(String jid) {
        Long ljid = Long.parseLong(jid);
        MatSciClientJobInfo jinfo = jobs.get(ljid);
        if (jinfo == null) {
            throw new IllegalArgumentException(jid + " is unknown.");
        }
        return jinfo.allReceived();
    }

    public ArrayList<String> notYetReceived() {
        ArrayList<String> notReceived = new ArrayList<String>();
        for (Long jid : jobs.keySet()) {
            if (!allReceived("" + jid)) {
                notReceived.add("" + jid);
            }
        }
        return notReceived;
    }

    public MatSciClientJobInfo getInfo(String jobid) {
        Long jid = Long.parseLong(jobid);
        if (!jobs.containsKey(jid)) {
            throw new IllegalArgumentException("Unknown job : " + jid);
        }
        return jobs.get(jid);
    }

    public void beginSession() {
        if (state != PASessionState.NORMAL) {
            System.err.println("Already in a session, please use endSession");
            return;
        }
        currentSequenceIndex = 1;

        if (mappingSeqToJobID.isEmpty()) {
            state = PASessionState.RECORDING;
        } else {
            jobs.putAll(recordedJobs);
            state = PASessionState.REPLAYING;
        }
        try {
            recMan.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void endSession() {
        if (state == PASessionState.NORMAL) {
            System.err.println("Recording Session not started, please use beginSession");
            return;
        }
        state = PASessionState.NORMAL;
        cleanRecordedJobs();
    }

    public boolean inSession() {
        return state != PASessionState.NORMAL;
    }

    /**
     * method used to clean the list of recorded jobs after a call to endSession :
     * after the end of a session, recorded jobs become handled as other normal jobs (they are not kept after two reboots)
     */
    protected void cleanRecordedJobs() {
        for (Map.Entry<Long, MatSciClientJobInfo> entry : recordedJobs.entrySet()) {
            MatSciClientJobInfo jinfo = entry.getValue();
            if (jinfo != null) {
                FileUtils.deleteDirectory(jinfo.getDirToClean());
            }
        }
        currentSequenceIndex = 1;
        recordedJobs.clear();
        mappingSeqToJobID.clear();
        try {
            recMan.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * method used to clean the jobs coming from the n-2 session, we remove all temporary directories and files
     * we avoid to remove the job data of a job inside the "recordedJobs" list
     */
    protected void cleanOldJobs() {
        for (Long key : lastJobs.keySet()) {
            if (!recordedJobs.containsKey(key)) {
                MatSciClientJobInfo jinfo = jobs.remove(key);
                if (jinfo != null) {
                    FileUtils.deleteDirectory(jinfo.getDirToClean());
                }
            }
        }
        try {
            recMan.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
