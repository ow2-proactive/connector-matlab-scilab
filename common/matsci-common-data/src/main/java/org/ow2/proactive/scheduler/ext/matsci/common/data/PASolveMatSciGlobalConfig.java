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
package org.ow2.proactive.scheduler.ext.matsci.common.data;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;


/**
 * PASolveMatSciGlobalConfig global configuration of a PAsolve job
 *
 * @author The ProActive Team
 */
public class PASolveMatSciGlobalConfig implements Serializable {

    private static final long serialVersionUID = 62L;

    /**
     * Name of the Scheduler job
     */
    protected String jobName = null;

    /**
     * Description of the Scheduler job
     */
    protected String jobDescription = null;

    /**
     * Debug Mode
     **/
    protected boolean debug = false;

    /**
     * login of the user submitting the job
     **/
    protected String login;

    /**
     * The tasks are in a separate JVM process
     **/
    protected boolean fork = false;

    /**
     * The tasks are executed under the account of the current user
     **/
    protected boolean runAsMe = false;

    /**
     * The root directory of the toolbox
     */
    protected String toolboxPath = null;

    /**
     * Push URL of the Scheduler's shared space, seen by the Matlab/Scilab client
     */
    protected String sharedPushPublicUrl = null;

    /**
     * Pull URL of the Scheduler's shared space, seen by the Matlab/Scilab client
     */
    protected String sharedPullPublicUrl = null;

    /**
     * Push URL of the Scheduler's shared space, seen by the nodes
     */
    protected String sharedPushPrivateUrl = null;

    /**
     * Pull URL of the Scheduler's shared space, seen by the nodes
     */
    protected String sharedPullPrivateUrl = null;

    /**
     * Do we use automatic transfer in proxy ?
     */
    protected boolean sharedAutomaticTransfer = true;

    /**
     * Directory storing the files used by the job (used only with the scheduler proxy and the shared space)
     */
    protected String jobDirectoryFullPath = null;

    /**
     * Default number of task executions
     */
    private int nbExecutions = 2;

    /**
     * Preferred Version to use
     **/
    protected String versionPref = null;

    /**
     * Versions forbidden to use
     **/
    protected HashSet<String> versionRej = new HashSet<String>();

    /**
     * Minimum version to use
     **/
    protected String versionMin = null;

    /**
     * Maximum version to use
     */
    protected String versionMax = null;

    /**
     * Version architecture to use
     */
    protected String versionArch = null;

    /**
     * Do we force the automated research of Matlab/Scilab ?
     */
    protected boolean forceMatSciSearch = false;

    /**
     * Transfers source to the remote engine
     */
    protected boolean transferEnv = false;

    /**
     * url of the selection script used to check Matlab or Scilab installation
     */
    protected String FindMatSciScriptUrl = null;

    /**
     * url of a custom selection script, if any
     */
    protected String customScriptUrl = null;

    /**
     * url of a custom selection script, if any
     */
    protected boolean customScriptStatic = false;

    /**
     * Parameters of the custom script
     */
    private String customScriptParams = null;

    /**
     * uri of the local dataspace (available when executing the task)
     */
    protected URI localSpace;

    /**
     * matlab or scilab startup options on windows machines
     */
    private String[] windowsStartupOptions = null;

    /**
     * matlab or scilab startup options on linux machines
     */
    private String[] linuxStartupOptions = null;

    /**
     * environment mat file (TransferEnv)
     */
    protected PASolveEnvFile envMatFile = null;

    /**
     * name of the input space
     */
    protected String inputSpaceName = null;

    /**
     * name of the output space
     */
    protected String outputSpaceName = null;

    /**
     * priority of the job
     */
    protected String priority = null;

    /**
     * directory structure of the matsci temp directory (each element of the array is a subdirectory of the previous one)
     */
    protected String jobSubDirPath;

    /**
     * url of the input space
     */
    protected String inputSpaceURL = null;

    /**
     * url of the output space
     */
    protected String outputSpaceURL = null;

    /**
     * Is check MatSci Script static
     */
    protected boolean findMatSciScriptStatic = true;

    /**
     * timeout used to start Matlab/Scilab worker processes (*10ms)
     */
    protected int workerTimeoutStart = 6000;

    /**
     * Directory used in this job which needs to be cleaned
     */
    protected String dirToClean;

    /**
     * Do we use the job classpath for Matlab/Scilab jars ?
     */
    protected boolean useJobClassPath = false;

    protected ArrayList<String> workerJars = new ArrayList<String>();

    public PASolveMatSciGlobalConfig() {
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getInputSpaceURL() {
        return inputSpaceURL;
    }

    public void setInputSpaceURL(String inputSpaceURL) {
        this.inputSpaceURL = inputSpaceURL;
    }

    public String getOutputSpaceURL() {
        return outputSpaceURL;
    }

    public void setOutputSpaceURL(String outputSpaceURL) {
        this.outputSpaceURL = outputSpaceURL;
    }

    public String getFindMatSciScriptUrl() {
        return FindMatSciScriptUrl;
    }

    public void setFindMatSciScriptUrl(String FindMatSciScriptUrl) {
        this.FindMatSciScriptUrl = FindMatSciScriptUrl;
    }

    public String getCustomScriptUrl() {
        return customScriptUrl;
    }

    public void setCustomScriptUrl(String customScriptUrl) {
        this.customScriptUrl = customScriptUrl;
    }

    public boolean isCustomScriptStatic() {
        return customScriptStatic;
    }

    public void setCustomScriptStatic(boolean customScriptStatic) {
        this.customScriptStatic = customScriptStatic;
    }

    public String getCustomScriptParams() {
        return customScriptParams;
    }

    public void setCustomScriptParams(String customScriptParams) {
        this.customScriptParams = customScriptParams;
    }

    public String getVersionPref() {
        return versionPref;
    }

    public void setVersionPref(String versionPref) {
        this.versionPref = versionPref;
    }

    public HashSet<String> getVersionRej() {
        return versionRej;
    }

    public String getVersionRejAsString() {
        String answer = "";
        if (versionRej == null)
            return null;
        for (String v : versionRej) {
            answer += v + ",";
        }
        if (answer.length() > 0) {
            answer = answer.substring(0, answer.length());
        }
        return answer;
    }

    public void setVersionRejAsString(String vrej) {
        HashSet<String> vrejSet = new HashSet<String>();
        if ((vrej != null) && (vrej.length() > 0)) {
            vrej = vrej.trim();
            String[] vRejArr = vrej.split("[ ,;]+");

            for (String rej : vRejArr) {
                if (rej != null) {
                    vrejSet.add(rej);
                }
            }
        }
        versionRej = vrejSet;
    }

    public void setVersionRej(HashSet<String> versionRej) {
        this.versionRej = versionRej;
    }

    public String getVersionMin() {
        return versionMin;
    }

    public void setVersionMin(String versionMin) {
        this.versionMin = versionMin;
    }

    public String getVersionMax() {
        return versionMax;
    }

    public void setVersionMax(String versionMax) {
        this.versionMax = versionMax;
    }

    public String getVersionArch() {
        return versionArch;
    }

    public void setVersionArch(String versionArch) {
        this.versionArch = versionArch;
    }

    public boolean isForceMatSciSearch() {
        return forceMatSciSearch;
    }

    public void setForceMatSciSearch(boolean forceMatSciSearch) {
        this.forceMatSciSearch = forceMatSciSearch;
    }

    public boolean isUseJobClassPath() {
        return useJobClassPath;
    }

    public void setUseJobClassPath(boolean useJobClassPath) {
        this.useJobClassPath = useJobClassPath;
    }

    public ArrayList<String> getWorkerJars() {
        return workerJars;
    }

    public void setWorkerJars(ArrayList<String> workerJars) {
        this.workerJars = workerJars;
    }

    public void addWorkerJar(String jarname) {
        this.workerJars.add(jarname);
    }

    public boolean isFork() {
        return this.fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    public boolean isRunAsMe() {
        return this.runAsMe;
    }

    public void setRunAsMe(boolean runAsMe) {
        this.runAsMe = runAsMe;
    }

    public String getToolboxPath() {
        return toolboxPath;
    }

    public void setToolboxPath(String toolboxPath) {
        this.toolboxPath = toolboxPath;
    }

    public String getSharedPushPublicUrl() {
        return sharedPushPublicUrl;
    }

    public void setSharedPushPublicUrl(String sharedPushPublicUrl) {
        this.sharedPushPublicUrl = sharedPushPublicUrl;
    }

    public String getSharedPullPublicUrl() {
        return sharedPullPublicUrl;
    }

    public void setSharedPullPublicUrl(String sharedPullPublicUrl) {
        this.sharedPullPublicUrl = sharedPullPublicUrl;
    }

    public String getSharedPushPrivateUrl() {
        return sharedPushPrivateUrl;
    }

    public void setSharedPushPrivateUrl(String sharedPushPrivateUrl) {
        this.sharedPushPrivateUrl = sharedPushPrivateUrl;
    }

    public String getSharedPullPrivateUrl() {
        return sharedPullPrivateUrl;
    }

    public void setSharedPullPrivateUrl(String sharedPullPrivateUrl) {
        this.sharedPullPrivateUrl = sharedPullPrivateUrl;
    }

    public boolean isSharedAutomaticTransfer() {
        return sharedAutomaticTransfer;
    }

    public void setSharedAutomaticTransfer(boolean sharedAutomaticTransfer) {
        this.sharedAutomaticTransfer = sharedAutomaticTransfer;
    }

    public String getJobDirectoryFullPath() {
        return jobDirectoryFullPath;
    }

    public void setJobDirectoryFullPath(String jobDirectoryFullPath) {
        this.jobDirectoryFullPath = jobDirectoryFullPath;
    }

    public int getNbExecutions() {
        return nbExecutions;
    }

    public void setNbExecutions(int nbExecutions) {
        this.nbExecutions = nbExecutions;
    }

    public PASolveEnvFile getEnvMatFile() {
        return envMatFile;
    }

    public void setEnvMatFile(PASolveEnvFile envMatFile) {
        this.envMatFile = envMatFile;
    }

    public void setEnvMatFile(String pathname, String[] globalNames) {
        this.envMatFile = new PASolveEnvFile(pathname);
        this.envMatFile.setEnvGlobalNames(globalNames);
    }

    public void setEnvMatFile(String relPath, String name, String[] globalNames) {
        this.envMatFile = new PASolveEnvFile(relPath, name);
        this.envMatFile.setEnvGlobalNames(globalNames);
    }

    public String getInputSpaceName() {
        return inputSpaceName;
    }

    public void setInputSpaceName(String inputSpaceName) {
        this.inputSpaceName = inputSpaceName;
    }

    public String getOutputSpaceName() {
        return outputSpaceName;
    }

    public void setOutputSpaceName(String outputSpaceName) {
        this.outputSpaceName = outputSpaceName;
    }

    public void setLocalSpace(URI localSpaceURI) {
        this.localSpace = localSpaceURI;
    }

    public URI getLocalSpace() {
        return this.localSpace;
    }

    public boolean isTransferEnv() {
        return transferEnv;
    }

    public void setTransferEnv(boolean transferEnv) {
        this.transferEnv = transferEnv;
    }

    public String getJobSubDirPortablePath() {
        return jobSubDirPath;
    }

    public String getJobSubDirOSPath() {
        return jobSubDirPath.replace("/", File.separator);
    }

    public void setJobSubDirPath(String jobSurDir) {
        this.jobSubDirPath = jobSurDir.replace(File.separator, "/");
    }

    public String[] getStartupOptions() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return this.getWindowsStartupOptions();
        } else {
            return this.getLinuxStartupOptions();
        }
    }

    public String[] getWindowsStartupOptions() {
        return windowsStartupOptions;
    }

    public void setWindowsStartupOptions(String[] windowsStartupOptions) {
        this.windowsStartupOptions = windowsStartupOptions;
    }

    public String[] getLinuxStartupOptions() {
        return linuxStartupOptions;
    }

    public void setLinuxStartupOptions(String[] linuxStartupOptions) {
        this.linuxStartupOptions = linuxStartupOptions;
    }

    public void setLinuxStartupOptionsAsString(String options) {
        if ((options != null) && (options.length() > 0)) {
            options = options.trim();
            linuxStartupOptions = options.split("[ ,;]+");

        }
    }

    public void setWindowsStartupOptionsAsString(String options) {
        if ((options != null) && (options.length() > 0)) {
            options = options.trim();
            windowsStartupOptions = options.split("[ ,;]+");

        }
    }

    public int getWorkerTimeoutStart() {
        return workerTimeoutStart;
    }

    public void setWorkerTimeoutStart(int workerTimeoutStart) {
        this.workerTimeoutStart = workerTimeoutStart;
    }

    public boolean isFindMatSciScriptStatic() {
        return findMatSciScriptStatic;
    }

    public void setFindMatSciScriptStatic(boolean findMatSciScriptStatic) {
        this.findMatSciScriptStatic = findMatSciScriptStatic;
    }

    public String getDirToClean() {
        return dirToClean;
    }

    public void setDirToClean(String dirToClean) {
        this.dirToClean = dirToClean;
    }

}
