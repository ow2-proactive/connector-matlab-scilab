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
package org.ow2.proactive.scheduler.ext.matsci.client.common.data;

import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciGlobalConfig;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciTaskConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;


/**
 * MatSciClientJobInfo
 *
 * JobInfo used by the Matlab and Scilab JVM
 *
 * @author The ProActive Team
 */
public class MatSciClientJobInfo implements Serializable {

    private static final long serialVersionUID = 11;
    /**
     * Id if the job
     */
    String jobId;

    /**
     * Names of all tasks
     */
    TreeSet<String> taskNames;

    /**
     * Names of the final tasks (final in the chain)
     */
    TreeSet<String> finalTaskNames;

    /**
     * Files storing the output variable for each file
     */
    HashMap<String, String> outVariablesFiles;

    /**
     * Number of parallel tasks in the job
     */
    int nbres;

    /**
     * Number of subsequent tasks in the job
     */
    int depth;

    /**
     * Directory to clean when the job is finished
     */
    String dirToClean;

    /**
     * Tasks which still need to be received
     */
    HashSet<String> toReceive;

    /**
     * Global configuration
     */
    PASolveMatSciGlobalConfig globalConfig;

    /**
     * Tasks configuration
     */
    PASolveMatSciTaskConfig[][] taskConfigs;

    public PASolveMatSciGlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public PASolveMatSciTaskConfig[][] getTaskConfigs() {
        return taskConfigs;
    }

    /**
     *
     * @param jobId Id if the job
     * @param tnames Names of all tasks
     * @param finaltnames Names of the final tasks (final in the chain)
     * @param nbres Number of parallel tasks in the job
     * @param depth Number of subsequent tasks in the job
     * @param dirToClean directory to clean at the end of the job
     */
    public MatSciClientJobInfo(String jobId, TreeSet<String> tnames, TreeSet<String> finaltnames, int nbres,
            int depth, String dirToClean, HashMap<String, String> outVariablesFiles,
            PASolveMatSciGlobalConfig conf, PASolveMatSciTaskConfig[][] tconfs) {

        this.jobId = jobId;
        this.taskNames = tnames;
        this.finalTaskNames = finaltnames;
        this.nbres = nbres;
        this.depth = depth;
        this.dirToClean = dirToClean;
        this.outVariablesFiles = outVariablesFiles;
        this.toReceive = new HashSet<String>(finalTaskNames);
        this.globalConfig = conf;
        this.taskConfigs = tconfs;

    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public TreeSet<String> getTaskNames() {
        return taskNames;
    }

    public void setTaskNames(TreeSet<String> tnames) {
        this.taskNames = tnames;
    }

    public TreeSet<String> getFinalTaskNames() {
        return finalTaskNames;
    }

    public void setFinalTaskNames(TreeSet<String> finaltnames) {
        this.finalTaskNames = finaltnames;
    }

    public ArrayList<String> getFinalTasksNamesAsList() {
        ArrayList<String> lst = new ArrayList<String>();
        lst.addAll(finalTaskNames);
        return lst;
    }

    public int getNbres() {
        return nbres;
    }

    public void setNbres(int nbres) {
        this.nbres = nbres;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getDirToClean() {
        return dirToClean;
    }

    public void setDirToClean(String dirToClean) {
        this.dirToClean = dirToClean;
    }

    public String getOutputVariablePathWithIndex(int index) {
        return outVariablesFiles.get("" + index + "_" + (depth - 1));
    }

    public String getOutputVariablePath(String tname) {
        return outVariablesFiles.get(tname);
    }

    public void receivedTask(String tname) {
        if (toReceive.contains(tname)) {
            toReceive.remove(tname);
        }
    }

    public boolean allReceived() {
        return toReceive.isEmpty();
    }

}
