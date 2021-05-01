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
package org.ow2.proactive.scheduler.ext.matsci.client.common.data;

import java.util.ArrayList;
import java.util.TreeSet;

import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciGlobalConfig;


/**
 * MatSciJobPermanentInfo job info necessary to be stored in a file when using disconnected mode
 *
 * @author The ProActive Team
 */
public class MatSciJobPermanentInfo implements java.io.Serializable, Cloneable {

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

    public MatSciJobPermanentInfo(String jobId, int nbres, int depth, PASolveMatSciGlobalConfig conf,
            TreeSet<String> tnames, TreeSet<String> finaltnames) {
        this.jobId = jobId;
        this.nbres = nbres;
        this.depth = depth;
        this.conf = conf;
        this.tnames = tnames;
        this.finaltnames = finaltnames;

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

    @Override
    public Object clone() throws CloneNotSupportedException {
        MatSciJobPermanentInfo newinfo = (MatSciJobPermanentInfo) super.clone();
        newinfo.tnames = (TreeSet<String>) tnames.clone();
        newinfo.finaltnames = (TreeSet<String>) finaltnames.clone();
        return newinfo;
    }
}
