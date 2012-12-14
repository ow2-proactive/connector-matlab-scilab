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
package org.ow2.proactive.scheduler.ext.matsci.middleman.proxy;

import java.io.Serializable;
import java.util.HashMap;


/**
 * AwaitedJob
 *
 * @author The ProActive Team
 */
public class AwaitedJob implements Serializable {

    private String jobId;

    // input data
    private String localInputFolder;
    private String inputSpaceURL;
    private String pushURL;
    private HashMap<String, AwaitedTask> awaitedTasks;
    private boolean isolateTaskOutputs;
    private boolean automaticTransfer;

    public boolean isIsolateTaskOutputs() {
        return isolateTaskOutputs;
    }

    public void setIsolateTaskOutputs(boolean isolateTaskOutputs) {
        this.isolateTaskOutputs = isolateTaskOutputs;
    }

    public void setPushURL(String pushURL) {
        this.pushURL = pushURL;
    }

    public void setPullURL(String pullURL) {
        this.pullURL = pullURL;
    }

    // output data
    private String localOutputFolder;
    private String outputSpaceURL;
    private String pullURL;

    public String getPushURL() {
        return pushURL;
    }

    public String getPullURL() {
        return pullURL;
    }

    public AwaitedJob() {
    }

    public AwaitedJob(String jobId, String localInputFolder, String inputSpaceURL, String push_url,
            String localOutputFolder, String outputSpaceURL, String pull_url, boolean isolatetaskOutputs,
            boolean automaticTransfer, HashMap<String, AwaitedTask> awaitedTasks) {
        super();
        this.jobId = jobId;
        this.localInputFolder = localInputFolder;
        this.inputSpaceURL = inputSpaceURL;
        this.localOutputFolder = localOutputFolder;
        this.outputSpaceURL = outputSpaceURL;
        this.pushURL = push_url;
        this.pullURL = pull_url;
        this.awaitedTasks = awaitedTasks;
        this.isolateTaskOutputs = isolatetaskOutputs;
        this.automaticTransfer = automaticTransfer;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getLocalInputFolder() {
        return localInputFolder;
    }

    public void setLocalInputFolder(String localInputFolder) {
        this.localInputFolder = localInputFolder;
    }

    public String getInputSpaceURL() {
        return inputSpaceURL;
    }

    public void setInputSpaceURL(String inputSpaceURL) {
        this.inputSpaceURL = inputSpaceURL;
    }

    public String getLocalOutputFolder() {
        return localOutputFolder;
    }

    public void setLocalOutputFolder(String localOutputFolder) {
        this.localOutputFolder = localOutputFolder;
    }

    public String getOutputSpaceURL() {
        return outputSpaceURL;
    }

    public void setOutputSpaceURL(String outputSpaceURL) {
        this.outputSpaceURL = outputSpaceURL;
    }

    public HashMap<String, AwaitedTask> getAwaitedTasks() {
        return awaitedTasks;
    }

    public AwaitedTask getAwaitedTask(String tname) {
        return awaitedTasks.get(tname);
    }

    public void removeAwaitedTask(String tname) {
        this.awaitedTasks.remove(tname);
    }

    public void setAwaitedTasks(HashMap<String, AwaitedTask> awaitedTasks) {
        this.awaitedTasks = awaitedTasks;
    }

    public boolean isAutomaticTransfer() {
        return automaticTransfer;
    }

    public void setAutomaticTransfer(boolean automaticTransfer) {
        this.automaticTransfer = automaticTransfer;
    }

    @Override
    public String toString() {
        return "AwaitedJob{" + "jobId='" + jobId + '\'' + ", localInputFolder='" + localInputFolder + '\'' +
            ", inputSpaceURL='" + inputSpaceURL + '\'' + ", pushURL='" + pushURL + '\'' +
            ", isolateTaskOutputs=" + isolateTaskOutputs + ", automaticTransfer=" + automaticTransfer +
            ", localOutputFolder='" + localOutputFolder + '\'' + ", outputSpaceURL='" + outputSpaceURL +
            '\'' + ", pullURL='" + pullURL + '\'' + '}';
    }
}
