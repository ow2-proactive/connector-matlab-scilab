/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds 
 *
 * Copyright (C) 1997-2010 INRIA/University of 
 * 				Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 
 * or a different license than the GPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.ext.scilab.middleman;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.objectweb.proactive.core.body.exceptions.FutureMonitoringPingFailureException;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.UserException;
import org.ow2.proactive.scheduler.common.job.JobEnvironment;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.ForkEnvironment;
import org.ow2.proactive.scheduler.common.task.JavaTask;
import org.ow2.proactive.scheduler.common.task.ParallelEnvironment;
import org.ow2.proactive.scheduler.common.task.dataspaces.InputAccessMode;
import org.ow2.proactive.scheduler.common.util.SchedulerProxyUserInterface;
import org.ow2.proactive.scheduler.ext.common.util.BitMatrix;
import org.ow2.proactive.scheduler.ext.matsci.client.common.PASessionState;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.MatSciClientJobInfo;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.MatSciJobInfo;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.MatSciJobStatus;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.MatSciTaskStatus;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.TaskNameComparator;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASolveException;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveFile;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciGlobalConfig;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciTaskConfig;
import org.ow2.proactive.scheduler.ext.matsci.common.exception.MatSciTaskException;
import org.ow2.proactive.scheduler.ext.matsci.middleman.AOMatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.middleman.LogMode;
import org.ow2.proactive.scheduler.ext.scilab.client.common.data.ScilabResultsAndLogs;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabGlobalConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabTaskConfig;
import org.ow2.proactive.scheduler.ext.scilab.worker.ScilabExecutable;
import org.ow2.proactive.scripting.InvalidScriptException;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.topology.descriptor.ThresholdProximityDescriptor;
import org.ow2.proactive.topology.descriptor.TopologyDescriptor;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.Level;


/**
 * AOScilabEnvironment This active object handles the interaction between Scilab and the Scheduler it creates Scheduler jobs and receives results
 *
 * @author The ProActive Team
 */
public class AOScilabEnvironment extends AOMatSciEnvironment<Boolean, ScilabResultsAndLogs> {

    private static final long serialVersionUID = 61L;

    /**
     * Name of the middleman jobs backup
     */
    public static final String MIDDLEMAN_JOBS_FILE_NAME = "ScilabMiddlemanJobs";

    public AOScilabEnvironment() {
        super();
    }

    public AOScilabEnvironment(boolean debug) {
        super(debug);
    }

    @Override
    protected File getMidlemanJobsFile() {
        return new File(TMPDIR, MIDDLEMAN_JOBS_FILE_NAME);
    }

    /**
     * gets the result and logs of a given task
     * @param jid id of the job
     * @param tname name of the task
     * @return the result object associated with this task
     */
    protected ScilabResultsAndLogs getResultOfTask(Long jid, String tname) {
        ScilabResultsAndLogs answer = new ScilabResultsAndLogs();
        answer.setJobId("" + jid);
        answer.setTaskName(tname);
        MatSciJobInfo<Boolean> jinfo = getJobInfo(jid);

        if (debug) {

            printLog("Sending the results of task " + tname + " of job " + jid + " back...");

        }

        Throwable t = null;
        if (schedulerKilled) {
            answer.setStatus(MatSciTaskStatus.GLOBAL_ERROR);
            answer.setException(new PASolveException("The scheduler has been killed"));
        } else if (jinfo.getStatus() == MatSciJobStatus.KILLED) {
            // Job killed
            answer.setStatus(MatSciTaskStatus.GLOBAL_ERROR);
            answer.setException(new PASolveException("The job has been killed"));
        } else if ((t = jinfo.getException(tname)) != null) {
            // Error inside job
            if (t instanceof MatSciTaskException) {
                answer.setStatus(MatSciTaskStatus.MATSCI_ERROR);
                answer.setLogs(jinfo.getLogs(tname));
                //System.err.println(jinfo.getLogs(tid));
            } else {
                answer.setStatus(MatSciTaskStatus.RUNTIME_ERROR);
                answer.setException(new PASolveException(t));
                if (jinfo.getLogs(tname) != null) {
                    answer.setLogs(jinfo.getLogs(tname));
                }
            }
        } else {
            // Normal termination
            answer.setStatus(MatSciTaskStatus.OK);
            //System.out.println(jinfo.getLogs(tid));
            answer.setLogs(jinfo.getLogs(tname));
            answer.setResult(jinfo.getResult(tname));
        }

        jinfo.addServedTask(tname);

        commit();

        if (jinfo.allServed()) {
            currentJobs.remove(jid);
            finishedJobs.add(jid);
        }

        return answer;
    }

    /** {@inheritDoc} */
    public MatSciClientJobInfo solve(PASolveMatSciGlobalConfig gconf, PASolveMatSciTaskConfig[][] tconf)
            throws PASchedulerException, MalformedURLException {

        if (state == PASessionState.REPLAYING) {
            Long jid = mappingSeqToJobID.get(currentSequenceIndex);
            if (jid != null) {
                MatSciJobInfo jinfo = recordedJobs.get(jid);
                commit();
                currentSequenceIndex++;
                return jinfo.getClientJobInfo();
            } else {
                // we reached the end of the list of the already submitted job, we switch to recording mode
                state = PASessionState.RECORDING;
                return solve(gconf, tconf);
            }
        } else {
            PASolveScilabGlobalConfig config = (PASolveScilabGlobalConfig) gconf;

            PASolveScilabTaskConfig[][] taskConfigs = (PASolveScilabTaskConfig[][]) tconf;

            HashMap<String, PASolveMatSciTaskConfig> tconf_maps = new HashMap<String, PASolveMatSciTaskConfig>();

            if (schedulerKilled) {
                printLog("the Scheduler is killed");
                throw new PASchedulerException("the Scheduler is killed");
            }
            ensureConnection();

            // We store the script selecting the nodes to use it later at termination.
            debug = config.isDebug();

            if (debug) {
                printLog("Submitting job of " + taskConfigs.length + " tasks...");
                // log all data transfer related events
                ProActiveLogger.getLogger(SchedulerProxyUserInterface.class).setLevel(Level.TRACE);
                // ProActiveLogger.getLogger(DSFileSelector.class).setLevel(Level.DEBUG);
            } else {
                // log all data transfer related events
                ProActiveLogger.getLogger(SchedulerProxyUserInterface.class).setLevel(Level.INFO);
            }
            //Thread t = java.lang.Thread.currentThread();
            //t.setContextClassLoader(this.getClass().getClassLoader());
            //System.out.println(this.getClass().getClassLoader());

            // Creating a task flow job
            TaskFlowJob job = new TaskFlowJob();
            job.setName(gconf.getJobName() + " " + lastGenJobId++);
            job.setPriority(JobPriority.findPriority(config.getPriority()));
            job.setCancelJobOnError(false);
            job.setDescription(gconf.getJobDescription());

            if (config.isUseJobClassPath()) {
                JobEnvironment je = new JobEnvironment();
                try {
                    ArrayList<String> workerJars = config.getWorkerJars();
                    if (config.isDebug()) {
                        printLog("Using jobClasspath : " + workerJars);
                    }
                    je.setJobClasspath(workerJars.toArray(new String[workerJars.size()]));
                    job.setEnvironment(je);
                } catch (IOException e) {
                    printLog(e);
                    throw new PASchedulerException(e);
                }
            }

            String pullUrl = config.getSharedPullPublicUrl();
            String pushUrl = config.getSharedPushPublicUrl();
            if ((pushUrl != null && pullUrl == null) || (pushUrl == null && pushUrl != null)) {
                IllegalStateException e = new IllegalStateException(
                    "Invalid shared urls, they must be both set or both null. PushUrl = " + pushUrl +
                        " , PullUrl=" + pullUrl);
                printLog(e);
                throw new PASchedulerException(e);
            }
            if (pushUrl != null) {
                job.setInputSpace(pushUrl);
                job.setOutputSpace(pullUrl);
            } else {
                job.setInputSpace(config.getInputSpaceURL());
                job.setOutputSpace(config.getOutputSpaceURL());
            }

            TreeSet<String> tnames = new TreeSet<String>(new TaskNameComparator());
            TreeSet<String> finaltnames = new TreeSet<String>(new TaskNameComparator());
            int nbResults = taskConfigs.length;
            int depth = taskConfigs[0].length;

            for (int i = 0; i < nbResults; i++) {
                JavaTask oldTask = null;
                for (int j = 0; j < depth; j++) {

                    JavaTask schedulerTask = new JavaTask();

                    if (config.isFork()) {
                        schedulerTask.setForkEnvironment(new ForkEnvironment());
                    }

                    schedulerTask.setMaxNumberOfExecution(gconf.getNbExecutions());

                    // Being fixed in the scheduler trunk
                    if (config.isRunAsMe()) {
                        schedulerTask.setRunAsMe(true);

                        // Fix for SCHEDULING-1308: With RunAsMe on windows the forked jvm can have a non-writable java.io.tmpdir
                        // With the following js script the forked jvm will inherit the scratchdir property or if undefined the java.io.tmpdir of the node jvm
                        final StringBuilder scriptsDir = new StringBuilder();
                        scriptsDir.append(config.getToolboxPath());
                        scriptsDir.append(File.separator);
                        scriptsDir.append("script");
                        scriptsDir.append(File.separator);

                        final ForkEnvironment fe = new ForkEnvironment();
                        final File forkenvFile = new File(scriptsDir + "forkenv.js");
                        SimpleScript sc = null;
                        try {
                            sc = new SimpleScript(forkenvFile, new String[0]);
                        } catch (InvalidScriptException e) {
                            printLog(e);
                            throw new PASchedulerException(e);
                        }
                        fe.setEnvScript(sc);
                        schedulerTask.setForkEnvironment(fe);
                    }

                    String tname = MatSciJobInfo.computeTNameFromIds(i, j);

                    tnames.add(tname);

                    if (j == depth - 1) {
                        finaltnames.add(tname);
                    }

                    schedulerTask.setName(tname);
                    if (j == depth - 1) {
                        schedulerTask.setPreciousResult(true);
                    }
                    schedulerTask.addArgument("input", taskConfigs[i][j].getInputScript());
                    schedulerTask.addArgument("script", taskConfigs[i][j].getMainScript());
                    schedulerTask.addArgument("functionName", taskConfigs[i][j].getFunctionName());
                    schedulerTask.addArgument("outputs", taskConfigs[i][j].getOutputs());
                    if (oldTask != null) {
                        schedulerTask.addDependence(oldTask);
                    }
                    oldTask = schedulerTask;
                    for (PASolveFile pafile : taskConfigs[i][j].getSourceFiles()) {
                        schedulerTask.addInputFiles(pafile.getPortablePathName(), getSourceAsMode(pafile
                                .getSource()));

                    }

                    if (config.isTransferEnv()) {
                        schedulerTask.addInputFiles(config.getEnvMatFile().getPortablePathName(),
                                getSourceAsMode(config.getEnvMatFile().getSource()));
                    }

                    schedulerTask.addInputFiles(taskConfigs[i][j].getInputVariablesFile()
                            .getPortablePathName(), getSourceAsMode(taskConfigs[i][j].getInputVariablesFile()
                            .getSource()));
                    if (taskConfigs[i][j].getComposedInputVariablesFile() != null) {
                        schedulerTask.addInputFiles(taskConfigs[i][j].getComposedInputVariablesFile()
                                .getPortablePathName(), InputAccessMode.TransferFromOutputSpace); // This should be automatically on the output space, as output of a previous task
                    }
                    schedulerTask.addOutputFiles(taskConfigs[i][j].getOutputVariablesFile()
                            .getPortablePathName(), getDestinationAsMode(taskConfigs[i][j]
                            .getOutputVariablesFile().getDestination()));

                    InputAccessMode iam = null;

                    for (PASolveFile inputFile : taskConfigs[i][j].getInputFiles()) {
                        schedulerTask.addInputFiles(inputFile.getPortablePathName(),
                                getSourceAsMode(inputFile.getSource()));
                    }

                    for (PASolveFile outputFile : taskConfigs[i][j].getOutputFiles()) {
                        schedulerTask.addOutputFiles(outputFile.getPortablePathName(),
                                getDestinationAsMode(outputFile.getDestination()));
                    }

                    if (taskConfigs[i][j].getDescription() != null) {
                        schedulerTask.setDescription(taskConfigs[i][j].getDescription());
                    } else {
                        schedulerTask.setDescription(taskConfigs[i][j].getMainScript());
                    }

                    // Topology
                    if (taskConfigs[i][j].getNbNodes() > 1) {
                        switch (taskConfigs[i][j].getTopology()) {
                            case ARBITRARY:
                                schedulerTask.setParallelEnvironment(new ParallelEnvironment(
                                    taskConfigs[i][j].getNbNodes(), TopologyDescriptor.ARBITRARY));
                                break;
                            case BEST_PROXIMITY:
                                schedulerTask.setParallelEnvironment(new ParallelEnvironment(
                                    taskConfigs[i][j].getNbNodes(), TopologyDescriptor.BEST_PROXIMITY));
                                break;
                            case SINGLE_HOST:
                                schedulerTask.setParallelEnvironment(new ParallelEnvironment(
                                    taskConfigs[i][j].getNbNodes(), TopologyDescriptor.SINGLE_HOST));
                                break;
                            case SINGLE_HOST_EXCLUSIVE:
                                schedulerTask
                                        .setParallelEnvironment(new ParallelEnvironment(taskConfigs[i][j]
                                                .getNbNodes(), TopologyDescriptor.SINGLE_HOST_EXCLUSIVE));
                                break;
                            case MULTIPLE_HOSTS_EXCLUSIVE:
                                schedulerTask.setParallelEnvironment(new ParallelEnvironment(
                                    taskConfigs[i][j].getNbNodes(),
                                    TopologyDescriptor.MULTIPLE_HOSTS_EXCLUSIVE));
                                break;
                            case DIFFERENT_HOSTS_EXCLUSIVE:
                                schedulerTask.setParallelEnvironment(new ParallelEnvironment(
                                    taskConfigs[i][j].getNbNodes(),
                                    TopologyDescriptor.DIFFERENT_HOSTS_EXCLUSIVE));
                                break;
                            case THRESHHOLD_PROXIMITY:
                                schedulerTask.setParallelEnvironment(new ParallelEnvironment(
                                    taskConfigs[i][j].getNbNodes(), new ThresholdProximityDescriptor(
                                        taskConfigs[i][j].getThresholdProximity())));
                                break;
                        }
                    }

                    schedulerTask.setExecutableClassName(ScilabExecutable.class.getName());

                    if (taskConfigs[i][j].getCustomScriptUrl() != null) {
                        URL url = new URL(taskConfigs[i][j].getCustomScriptUrl());

                        SelectionScript sscript = null;

                        String[] params;
                        if (taskConfigs[i][j].getCustomScriptParams() != null &&
                            taskConfigs[i][j].getCustomScriptParams().trim().length() > 0) {

                            params = taskConfigs[i][j].getCustomScriptParams().split("\\s");

                        } else {
                            params = new String[0];
                        }
                        try {

                            sscript = new SelectionScript(url, params, !taskConfigs[i][j]
                                    .isCustomScriptStatic());

                        } catch (InvalidScriptException e1) {
                            throw new PASchedulerException(e1);
                        }
                        schedulerTask.addSelectionScript(sscript);

                        printLog("Task " + tname + ":" + " using task custom script (" +
                            (taskConfigs[i][j].isCustomScriptStatic() ? "static" : "dynamic") + ") " + url +
                            " with params : " + params);
                    }

                    if (config.getCustomScriptUrl() != null) {
                        URL url = new URL(config.getCustomScriptUrl());

                        SelectionScript sscript = null;

                        String[] params;
                        if (config.getCustomScriptParams() != null &&
                            config.getCustomScriptParams().trim().length() > 0) {

                            params = config.getCustomScriptParams().split("\\s");

                        } else {
                            params = new String[0];
                        }

                        try {

                            sscript = new SelectionScript(url, params, !config.isCustomScriptStatic());

                        } catch (InvalidScriptException e1) {
                            printLog(e1);
                            throw new PASchedulerException(e1);
                        }
                        schedulerTask.addSelectionScript(sscript);
                        printLog("Task " + tname + ":" + " using global custom script (" +
                            (taskConfigs[i][j].isCustomScriptStatic() ? "static" : "dynamic") + ") " + url +
                            " with params : " + params);
                    }

                    URL url1 = new URL(config.getFindMatSciScriptUrl());

                    SelectionScript sscript = null;
                    try {
                        sscript = new SelectionScript(url1, new String[] { "forceSearch",
                                "" + config.isForceMatSciSearch(), "versionPref", config.getVersionPref(),
                                "versionRej", config.getVersionRejAsString(), "versionMin",
                                config.getVersionMin(), "versionMax", config.getVersionMax(), "versionArch",
                                config.getVersionArch() }, !config.isFindMatSciScriptStatic());
                    } catch (InvalidScriptException e1) {
                        throw new PASchedulerException(e1);
                    }
                    schedulerTask.addSelectionScript(sscript);

                    schedulerTask.addArgument("global_config", config);
                    schedulerTask.addArgument("task_config", taskConfigs[i][j]);

                    try {
                        job.addTask(schedulerTask);
                    } catch (UserException e) {
                        printLog(e);
                        throw new PASchedulerException(e);
                    }

                    tconf_maps.put(tname, taskConfigs[i][j]);
                }

            }
            MatSciJobInfo jinfo = null;
            JobId sjid = null;

            try {
                if (pushUrl != null) {
                    sjid = sched_proxy.submit(job, config.getJobDirectoryFullPath(), pushUrl, config
                            .getJobDirectoryFullPath(), pullUrl, false, config.isSharedAutomaticTransfer());
                } else {
                    sjid = sched_proxy.submit(job);
                }

            } catch (FutureMonitoringPingFailureException re) {
                printLog(re);
                return null;
            } catch (JobCreationException re) {
                printLog(re);
                throw new PASchedulerException(re);

            } catch (FileSystemException re) {
                // a file system exception can occur either because the shared dataspace is wrongly configured or we are not connected
                printLog(re);
                if (isConnected()) {
                    throw new PASchedulerException(re);
                }
                return null;
            } catch (Exception re) {
                if (isProActiveExeption(re)) {
                    printLog(re);
                    return null;
                } else {
                    printLog(re);
                    throw new PASchedulerException(re);
                }
            } catch (Error re) {
                printLog(re, LogMode.FILEANDOUTALWAYS);
                throw new PASchedulerException(re);
            }

            Long jid = Long.parseLong(sjid.value());
            if (debug) {
                printLog("Job " + jid + " submitted.");
            }
            HashMap<String, String> outVariablesFiles = new HashMap<String, String>();
            for (String ftname : finaltnames) {
                outVariablesFiles.put(ftname, tconf_maps.get(ftname).getOutputVariablesFile()
                        .getFullPathName());
            }
            jinfo = new MatSciJobInfo(sjid.value(), nbResults, depth, config, taskConfigs, tnames,
                finaltnames, outVariablesFiles);

            currentJobs.add(jid);
            tasksReceived.put(jid, new BitMatrix(nbResults, depth));
            putJobInfo(jid, jinfo);
            if (state == PASessionState.RECORDING) {
                mappingSeqToJobID.put(currentSequenceIndex, jid);
                currentSequenceIndex++;
            }
            commit();

            return jinfo.getClientJobInfo();
        }
    }

    public ScilabResultsAndLogs waitResult(String sjid, String tname) {
        Long jid = Long.parseLong(sjid);
        return getResultOfTask(jid, tname);
    }

}
