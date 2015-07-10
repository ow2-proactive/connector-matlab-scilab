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
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.matsci.middleman;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.ow2.proactive.db.SortOrder;
import org.ow2.proactive.db.SortParameter;
import org.ow2.proactive.scheduler.common.JobFilterCriteria;
import org.ow2.proactive.scheduler.common.JobSortParameter;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.UnknownTaskException;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.utils.ObjectArrayFormatter;
import org.ow2.proactive.utils.Tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;


public class SchedulerModel {

    private static final Logger logger = Logger.getLogger(SchedulerModel.class);

    protected static String newline = System.lineSeparator();

    protected Scheduler scheduler;

    static {
        TaskState.setSortingBy(TaskState.SORT_BY_ID);
        TaskState.setSortingOrder(TaskState.ASC_ORDER);
    }

    public void connectScheduler(Scheduler scheduler) {
        if (scheduler == null) {
            throw new NullPointerException("Given scheduler must not be null");
        }
        this.scheduler = scheduler;
    }

    @SuppressWarnings("unchecked")
    private static final List<SortParameter<JobSortParameter>> JOB_SORT_PARAMS = Arrays.asList(
            new SortParameter<JobSortParameter>(JobSortParameter.STATE, SortOrder.ASC),
            new SortParameter<JobSortParameter>(JobSortParameter.ID, SortOrder.DESC));

    public void handleExceptionDisplay(String msg, Throwable t) {
        if (t instanceof NotConnectedException) {
            String tmp = "Your session has expired, please try to reconnect server using reconnect() command !";
            System.err.println(tmp);
            logUserException(tmp, t);
        } else if (t instanceof PermissionException) {
            String tmp = msg + " : " + (t.getMessage() == null ? t : t.getMessage());
            System.err.println(tmp);
            logUserException(tmp, t);
        } else if (t instanceof ProActiveRuntimeException) {
            String tmp = msg + " : Scheduler server seems to be unreachable !";
            System.err.println(tmp);
            logUserException(tmp, t);
        } else {
            handleExceptionDisplay(msg, t);
        }
    }

    protected void logUserException(String msg, Throwable t) {
        //log the exception independently on the configuration
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        t.printStackTrace(printWriter);
        logger.warn("User exception occured. Msg:  " + msg + " stacktrace: " + result);
    }

    public boolean pause_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.pauseJob(jobId);
        } catch (Exception e) {
            handleExceptionDisplay("Error while pausing job " + jobId, e);
            return false;
        }
        if (success) {
            print("Job " + jobId + " paused.");
        } else {
            print("Pause job " + jobId + " is not possible !!");
        }
        return success;
    }

    public boolean resume_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.resumeJob(jobId);
        } catch (Exception e) {
            handleExceptionDisplay("Error while resuming job  " + jobId, e);
            return false;
        }
        if (success) {
            print("Job " + jobId + " resumed.");
        } else {
            print("Resume job " + jobId + " is not possible !!");
        }
        return success;
    }

    public boolean kill_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.killJob(jobId);
        } catch (Exception e) {
            handleExceptionDisplay("Error while killing job  " + jobId, e);
            return false;
        }
        if (success) {
            print("Job " + jobId + " killed.");
        } else {
            print("kill job " + jobId + " is not possible !!");
        }
        return success;
    }

    public boolean remove_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.removeJob(jobId);
        } catch (Exception e) {
            handleExceptionDisplay("Error while removing job  " + jobId, e);
            return false;
        }
        if (success) {
            print("Job " + jobId + " removed.");
        } else {
            print("Remove job " + jobId + " is not possible !!");
        }
        return success;
    }

    public JobResult result_(String jobId) {
        try {
            JobResult result = scheduler.getJobResult(jobId);

            if (result != null) {
                print("Job " + jobId + " result => " + newline);

                for (Entry<String, TaskResult> e : result.getAllResults().entrySet()) {
                    TaskResult tRes = e.getValue();

                    try {
                        print("\t " + e.getKey() + " : " + tRes.value());
                    } catch (Throwable e1) {
                        handleExceptionDisplay("\t " + e.getKey(), e1);
                    }
                }
            } else {
                print("Job " + jobId + " is not finished !");
                return null;
            }
            return result;
        } catch (Exception e) {
            handleExceptionDisplay("Error on job " + jobId, e);
            return null;
        }
    }

    public TaskResult tresult_(String jobId, String taskName, String inc) {
        int rInc;
        try {
            rInc = Integer.parseInt(inc);
        } catch (NumberFormatException nfe) {
            handleExceptionDisplay("Bad value for 3rd argument : 'incarnation' ", nfe);
            return null;
        }
        try {
            TaskResult result = scheduler.getTaskResultFromIncarnation(jobId, taskName, rInc);

            if (result != null) {
                print("Task " + taskName + " result => " + newline);

                try {
                    print("\t " + result.value());
                } catch (Throwable e1) {
                    handleExceptionDisplay("\t " + result.getException().getMessage(), e1);
                }
            } else {
                print("Task '" + taskName + "' is not finished !");
            }
            return result;
        } catch (Exception e) {
            handleExceptionDisplay("Error on task " + taskName, e);
            return null;
        }
    }

    public boolean killt_(String jobId, String taskName) {
        try {
            boolean result = scheduler.killTask(jobId, taskName);

            if (result) {
                print("Task " + taskName + " has been killed." + newline);
            } else {
                print("Task '" + taskName + "' cannot be killed, it is probably not running.");
            }
            return result;
        } catch (Exception e) {
            handleExceptionDisplay("Error on task " + taskName, e);
            return false;
        }
    }

    public void output_(String jobId) {
        output_(jobId, "" + TaskState.SORT_BY_ID);
    }

    public void output_(String jobId, String tSort) {
        int sort = TaskState.SORT_BY_ID;
        try {
            sort = Integer.parseInt(tSort);
        } catch (NumberFormatException nfe) {
            handleExceptionDisplay("Bad value for 2rd argument : 'sort' ", nfe);
        }

        try {
            JobResult result = scheduler.getJobResult(jobId);

            if (result != null) {
                print("Job " + jobId + " output => '" + tSort + "' " + newline);

                JobState js = scheduler.getJobState(jobId);
                List<TaskState> tasks = js.getTasks();
                TaskState.setSortingBy(sort);
                Collections.sort(tasks);

                for (TaskState ts : tasks) {
                    TaskResult tRes = null;
                    try {
                        tRes = result.getResult(ts.getName());
                        print(ts.getName() + " : " + newline + tRes.getOutput().getAllLogs(false));
                    } catch (UnknownTaskException e) {
                        print(ts.getName() + " : " + newline + "No output available !");
                    } catch (Exception e1) {
                        if (tRes == null) {
                            throw e1;
                        }
                        System.err.println(tRes.getException().toString());
                    }
                }
            } else {
                print("Job " + jobId + " is not finished !");
            }
        } catch (Exception e) {
            handleExceptionDisplay("Error on job " + jobId, e);
        }
    }

    public void toutput_(String jobId, String taskName) {
        try {
            TaskResult result = null;

            result = scheduler.getTaskResult(jobId, taskName);
            if (result != null) {
                try {
                    print(taskName + " output :");
                    print(result.getOutput().getAllLogs(false));
                } catch (Throwable e1) {
                    handleExceptionDisplay("\t " + result.getException().getMessage(), e1);
                }
            } else {
                print("Task '" + taskName + "' is not finished !");
            }

        } catch (Exception e) {
            handleExceptionDisplay("Error on task " + taskName, e);
        }
    }

    public JobState jobState_(String jobId) {
        return jobState_(jobId, "" + TaskState.SORT_BY_ID);
    }

    public JobState jobState_(String jobId, String tSort) {
        int sort = TaskState.SORT_BY_ID;
        try {
            sort = Integer.parseInt(tSort);
        } catch (NumberFormatException nfe) {
            handleExceptionDisplay("Bad value for 2rd argument : 'sort' ", nfe);
        }

        List<String> list;
        try {
            JobState js = scheduler.getJobState(jobId);

            JobInfo ji = js.getJobInfo();

            String state = newline + "   Job '" + ji.getJobId() + "'    name:" +
                    ji.getJobId().getReadableName() + "'    project:" + js.getProjectName() + "    owner:" +
                    js.getOwner() + "    status:" + ji.getStatus() + "    #tasks:" + ji.getTotalNumberOfTasks() +
                    newline + newline;

            //create formatter
            ObjectArrayFormatter oaf = new ObjectArrayFormatter();
            oaf.setMaxColumnLength(30);
            //space between column
            oaf.setSpace(2);
            //title line
            list = new ArrayList<String>();
            list.add("ID");
            list.add("NAME");
            list.add("ITER");
            list.add("DUP");
            list.add("STATUS");
            list.add("HOSTNAME");
            list.add("EXEC DURATION");
            list.add("TOT DURATION");
            list.add("#NODES USED");
            list.add("#EXECUTIONS");
            list.add("#NODES KILLED");
            oaf.setTitle(list);
            //separator
            oaf.addEmptyLine();
            //add each lines
            List<TaskState> tasks = js.getTasks();
            TaskState.setSortingBy(sort);
            Collections.sort(tasks);
            for (TaskState ts : tasks) {
                list = new ArrayList<String>();
                list.add(ts.getId().toString());
                list.add(ts.getName());
                list.add((ts.getIterationIndex() > 0) ? "" + ts.getIterationIndex() : "");
                list.add((ts.getReplicationIndex() > 0) ? "" + ts.getReplicationIndex() : "");
                list.add(ts.getStatus().toString());
                list.add((ts.getExecutionHostName() == null) ? "unknown" : ts.getExecutionHostName());
                list.add(Tools.getFormattedDuration(0, ts.getExecutionDuration()));
                list.add(Tools.getFormattedDuration(ts.getFinishedTime(), ts.getStartTime()));
                list.add("" + ts.getNumberOfNodesNeeded());
                if (ts.getMaxNumberOfExecution() - ts.getNumberOfExecutionLeft() < ts
                        .getMaxNumberOfExecution()) {
                    list.add((ts.getMaxNumberOfExecution() - ts.getNumberOfExecutionLeft() + 1) + "/" +
                            ts.getMaxNumberOfExecution());
                } else {
                    list.add((ts.getMaxNumberOfExecution() - ts.getNumberOfExecutionLeft()) + "/" +
                            ts.getMaxNumberOfExecution());
                }
                list.add((ts.getMaxNumberOfExecutionOnFailure() - ts.getNumberOfExecutionOnFailureLeft()) +
                        "/" + ts.getMaxNumberOfExecutionOnFailure());
                oaf.addLine(list);
            }
            //print formatter
            print(state + Tools.getStringAsArray(oaf));

            return js;
        } catch (Exception e) {
            handleExceptionDisplay("Error on job " + jobId, e);
            return null;
        }
    }

    public void listjobs_() {
        List<String> list;
        try {
            List<JobInfo> jobs = scheduler.getJobs(0, -1, new JobFilterCriteria(false, true, true, true),
                    JOB_SORT_PARAMS);

            if (jobs.size() == 0) {
                print("\n\tThere are no jobs handled by the Scheduler");
                return;
            }
            //create formatter
            ObjectArrayFormatter oaf = new ObjectArrayFormatter();
            oaf.setMaxColumnLength(30);
            //space between column
            oaf.setSpace(4);
            //title line
            list = new ArrayList<String>();
            list.add("ID");
            list.add("NAME");
            list.add("OWNER");
            list.add("PRIORITY");
            list.add("STATUS");
            list.add("START AT");
            list.add("DURATION");
            oaf.setTitle(list);

            JobStatus lastStatus = null;
            for (JobInfo jobInfo : jobs) {
                if (changedOfStateGroup(lastStatus, jobInfo.getStatus())) {
                    oaf.addEmptyLine();
                }
                oaf.addLine(makeList(jobInfo));
                lastStatus = jobInfo.getStatus();
            }

            //print formatter
            print(Tools.getStringAsArray(oaf));
        } catch (Exception e) {
            handleExceptionDisplay("Error while getting list of jobs", e);
        }
    }

    // jobs are grouped in 3 groups (by state)
    private boolean changedOfStateGroup(JobStatus lastStatus, JobStatus newStatus) {
        if (isFirstGroup(lastStatus) && !isFirstGroup(newStatus)) {
            return true;
        } else if (isSecondGroup(lastStatus) && !isSecondGroup(newStatus)) {
            return true;
        } else if (isFirstGroup(lastStatus) && isThirdGroup(newStatus)) {
            return true;
        }
        return false;
    }

    private boolean isFirstGroup(JobStatus status) {
        return JobStatus.PENDING.equals(status);
    }

    private boolean isSecondGroup(JobStatus status) {
        return JobStatus.RUNNING.equals(status) || JobStatus.STALLED.equals(status) ||
                JobStatus.PAUSED.equals(status);
    }

    private boolean isThirdGroup(JobStatus status) {
        return !isFirstGroup(status) && !isSecondGroup(status);
    }

    private List<String> makeList(JobInfo jobInfo) {
        List<String> list = new ArrayList<String>();
        list.add(jobInfo.getJobId().value());
        list.add(jobInfo.getJobId().getReadableName());
        list.add(jobInfo.getJobOwner());
        list.add(jobInfo.getPriority().toString());
        list.add(jobInfo.getStatus().toString());
        long startTime = jobInfo.getStartTime();
        String date = Tools.getFormattedDate(startTime);
        if (startTime != -1) {
            date += " (" + Tools.getElapsedTime(startTime) + ")";
        }
        list.add(date);
        list.add(Tools.getFormattedDuration(startTime, jobInfo.getFinishedTime()));
        return list;
    }

    /**
     * Get the scheduler
     *
     * @return the scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    private void print(String message) {
        System.out.println(message);
    }

}
