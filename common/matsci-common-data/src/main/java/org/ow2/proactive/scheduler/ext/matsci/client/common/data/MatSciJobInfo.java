/*
 * ################################################################
 *
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
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.matsci.client.common.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.ow2.proactive.scheduler.ext.common.util.BitMatrix;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciGlobalConfig;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciTaskConfig;


/**
 * MatSciJobInfo infos of a job seen by the middleman JVM
 *
 * @author The ProActive Team
 */
public class MatSciJobInfo<R> implements Serializable {

    /**
     * Id if the job
     */
    String jobId;

    /**
     * Names of all tasks
     */
    TreeSet<String> tnames;

    /**
     * Names of the final tasks (final in the chain)
     */
    TreeSet<String> finaltnames;

    /**
     * Number of parallel tasks in the job
     */
    int nbres;

    /**
     * Number of subsequent tasks in the job
     */
    int depth;

    /**
     * Configuration used for this job
     */
    PASolveMatSciGlobalConfig conf;

    /**
     * Tasks configuration
     */
    PASolveMatSciTaskConfig[][] taskConfigs;

    /**
     * Output variable file location of each final task
     */
    HashMap<String, String> outVariablesFiles;

    /**
     * Status of the job (status seen by the scheduler)
     */
    protected MatSciJobStatus status;

    /**
     * A boolean, true if the job is finished
     */
    protected boolean jobFinished;

    /**
     * results of the job
     */
    protected HashMap<String, R> results = new HashMap<String, R>();

    /**
     * Names of tasks already received by the middleman JVM
     */
    protected HashSet<String> receivedTasks = new HashSet<String>();

    /**
     * Names of tasks already given by the middleman JVM to the Matlab or Scilab client
     */
    protected HashSet<String> servedTasks = new HashSet<String>();

    /**
     * Exceptions which occurred in the job
     */
    protected HashMap<String, Throwable> exceptions = new HashMap<String, Throwable>();

    /**
     * logs of tasks in the job
     */
    protected HashMap<String, String> logs = new HashMap<String, String>();

    public MatSciJobInfo(String jobId, int nbres, int depth, PASolveMatSciGlobalConfig conf,
            PASolveMatSciTaskConfig[][] taskConfigs, TreeSet<String> tnames, TreeSet<String> finaltnames,
            HashMap<String, String> outVariablesFiles) {
        this.jobId = jobId;
        this.nbres = nbres;
        this.depth = depth;
        this.conf = conf;
        this.tnames = tnames;
        this.finaltnames = finaltnames;
        this.outVariablesFiles = outVariablesFiles;
        // convenience notation, the job is probably not running yet, we are only interested in the other states not pending/running
        this.status = MatSciJobStatus.RUNNING;
        this.jobFinished = false;
        this.taskConfigs = taskConfigs;
    }

    public MatSciClientJobInfo getClientJobInfo() {
        return new MatSciClientJobInfo(jobId, tnames, finaltnames, nbres, depth, conf.getDirToClean(),
            outVariablesFiles, conf, taskConfigs);
    }

    public String getJobId() {
        return jobId;
    }

    public int getNbres() {
        return nbres;
    }

    public int getDepth() {
        return depth;
    }

    public PASolveMatSciGlobalConfig getConf() {
        return conf;
    }

    public boolean isDebugCurrentJob() {
        return conf.isDebug();
    }

    public boolean isJobFinished() {
        return jobFinished;
    }

    public void setJobFinished(boolean jobFinished) {
        this.jobFinished = jobFinished;
    }

    public TreeSet<String> getTaskNames() {
        return tnames;
    }

    public TreeSet<String> getFinalTaskNames() {
        return finaltnames;
    }

    public ArrayList<String> getFinalTasksNamesAsList() {
        ArrayList<String> lst = new ArrayList<String>();
        lst.addAll(finaltnames);
        return lst;
    }

    public MatSciJobStatus getStatus() {
        return status;
    }

    public void setStatus(MatSciJobStatus status) {
        this.status = status;
    }

    public void setResult(String tname, R res) {
        this.results.put(tname, res);
    }

    public R getResult(String tname) {
        return this.results.get(tname);
    }

    public void addLogs(String tname, String log) {

        Pair<Integer, Integer> idpair = computeIdsFromTName(tname);
        int i = idpair.getX();
        int j = idpair.getY();
        String tname_t;
        // We add logs to each succeeding task (this way, we ensure that if a task fails, the logs from all previous tasks will appear).
        do {
            tname_t = "" + i + "_" + j;
            String logs = this.logs.get(tname_t);
            if (logs == null) {
                logs = "";
            }
            logs += log;
            this.logs.put(tname_t, logs);
            j = j + 1;
        } while (!finaltnames.contains(tname_t));
    }

    public String getLogs(String tname) {
        return this.logs.get(tname);
    }

    public Throwable getException(String tname) {
        return this.exceptions.get(tname);
    }

    public void setException(String tname, Throwable t) {
        this.exceptions.put(tname, t);
    }

    public int nbResults() {
        return this.nbres;
    }

    public void addReceivedTask(String tname) {
        this.receivedTasks.add(tname);
    }

    public boolean isReceivedTask(String tname) {
        return this.receivedTasks.contains(tname);
    }

    public void addServedTask(String tname) {
        this.servedTasks.add(tname);
    }

    public boolean isServedResult(String tname) {
        return this.servedTasks.contains(tname);
    }

    public boolean allServed() {
        return this.servedTasks.size() == nbResults();
    }

    public TreeSet<String> missingResults() {
        TreeSet<String> allTasks = (TreeSet<String>) finaltnames.clone();
        allTasks.removeAll(receivedTasks);
        return allTasks;
    }

    public static Pair<Integer, Integer> computeIdsFromTName(String tname) {
        String[] ids = tname.split("_");
        return new Pair<Integer, Integer>(Integer.parseInt(ids[0]), Integer.parseInt(ids[1]));
    }

    public static String computeTNameFromIds(int x, int y) {
        return "" + x + "_" + y;
    }

    public static List<Pair<Integer, Integer>> computeAllIdsFromTNames(List<String> tnames) {
        ArrayList<Pair<Integer, Integer>> ids = new ArrayList<Pair<Integer, Integer>>(tnames.size());
        for (String tname : tnames) {
            ids.add(computeIdsFromTName(tname));
        }
        return ids;
    }

    public static List<Integer> computeLinesFromTNames(List<String> tnames) {
        HashSet<Integer> ids = new HashSet<Integer>();
        for (String tname : tnames) {
            ids.add(computeIdsFromTName(tname).getX());
        }
        return new ArrayList(ids);
    }

    public BitMatrix getTaskReceptionMatrix() {
        int nblines = nbres;
        int nbcol = depth;
        BitMatrix answer = new BitMatrix(nblines, nbcol);
        for (String tname : tnames) {
            Pair<Integer, Integer> id = computeIdsFromTName(tname);
            answer.set(id.getX(), id.getY(), receivedTasks.contains(tname));
        }
        return answer;
    }

    public void resetServedTasks() {
        this.servedTasks.clear();
    }

}
