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

import jdbm.PrimaryHashMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import org.apache.commons.vfs.*;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.EndActive;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.body.request.BlockingRequestQueueImpl;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.extensions.dataspaces.vfs.VFSFactory;
import org.objectweb.proactive.utils.NamedThreadFactory;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.exception.*;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.task.*;
import org.ow2.proactive.scheduler.common.task.dataspaces.InputSelector;
import org.ow2.proactive.scheduler.common.task.dataspaces.OutputSelector;
import org.ow2.proactive.scheduler.common.util.SchedulerProxyUserInterface;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;


/**
 * MatSciSchedulerProxy
 *
 * @author The ProActive Team
 */
public class MatSciSchedulerProxy extends SchedulerProxyUserInterface implements InitActive, EndActive,
        SchedulerEventListener {

    public static final String GENERIC_INFO_INPUT_FOLDER_PROPERTY_NAME = "client_input_data_folder";
    public static final String GENERIC_INFO_OUTPUT_FOLDER_PROPERTY_NAME = "client_output_data_folder";

    public static final String GENERIC_INFO_PUSH_URL_PROPERTY_NAME = "push_url";
    public static final String GENERIC_INFO_PULL_URL_PROPERTY_NAME = "pull_url";

    public static final int MAX_NB_OF_DATA_TRANSFER_THREADS = 20;

    private static final String TASKID_DIR_DEFAULT_NAME = "TASKID";

    private static final String CLZN = "[" + MatSciSchedulerProxy.class.getSimpleName() + "] ";

    /**
     * Thread factory for data transfer operations
     */
    protected transient ThreadFactory tf = new NamedThreadFactory("Data Transfer Thread");

    final protected transient ExecutorService tpe = Executors.newFixedThreadPool(
            MAX_NB_OF_DATA_TRANSFER_THREADS, tf);

    protected static final String TMPDIR = System.getProperty("java.io.tmpdir");

    /**
     * Default name of the file used to persist the list of jobs
     */
    protected static final String DEFAULT_STATUS_FILENAME = "SchedulerProxyUIWithDSupport";

    protected static String sessionName = DEFAULT_STATUS_FILENAME;

    /**
     * file which persists the list of AwaitedJob
     */
    public static File statusFile = new File(TMPDIR, sessionName);

    /**
     * Name of the jobs backup table being recorded
     */
    public static final String STATUS_RECORD_NAME = "AWAITED_JOBS";

    /**
     * Object handling the AwaitedJobsFile connection
     */
    protected RecordManager recMan;

    /**
     * last url used to connect to the scheduler
     */
    private String lastSchedUrl = null;

    /**
     * last creddata used to connect to the scheduler
     */
    private CredData lastCredData = null;

    /**
     * last credentials used to connect to the scheduler
     */
    private Credentials lastCredentials = null;

    /**
     * ProActive stup on this activeobject
     */
    private MatSciSchedulerProxy stubOnThis;

    /*
     * ProActive singleton pattern
     */
    private static MatSciSchedulerProxy activeInstance;

    /**
     * body of the instance
     */
    private Body bodyOnThis;

    /**
     * Thread of this active object's service
     */
    private Thread serviceThread;

    /*
     * Standard singleton pattern
     */
    private static MatSciSchedulerProxy stdInstance;

    /**
     * listeners registered to this proxy
     */
    protected Set<ISchedulerEventListenerExtended> eventListeners = Collections
            .synchronizedSet(new HashSet<ISchedulerEventListenerExtended>());

    /**
     * A map of jobs that have been launched and which results are awaited each
     * time a new job is sent to the scheduler for computation, it will be added
     * to this map, as an entry of (JobId, AwaitedJob), where JobId is given as
     * a string. When the output data related to this job has been transfered,
     * the corresponding awaited job will be removed from this map. This map is
     * persisted in the status file
     */
    protected PrimaryHashMap<String, AwaitedJob> awaitedJobs;

    // TODO: is the FileSystemManager threadSafe ? Do we need to create one
    // instance per thread ?
    // See https://issues.apache.org/jira/browse/VFS-98
    /**
     * The VFS {@link org.apache.commons.vfs.FileSystemManager} used for file transfer
     */
    transient protected FileSystemManager fsManager = null;

    {
        try {
            fsManager = VFSFactory.createDefaultFileSystemManager();
        } catch (FileSystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error(CLZN + "Could nnot create Default FileSystem Manager", e);
        }
    }

    /**
     * Returns a stub to the only active instance of the proxy (proactive singleton pattern)
     *
     * @return instance of the proxy
     * @throws org.objectweb.proactive.ActiveObjectCreationException
     *
     * @throws org.objectweb.proactive.core.node.NodeException
     *
     */
    public static synchronized MatSciSchedulerProxy getActiveInstance() throws ActiveObjectCreationException,
            NodeException {
        // we check if the activeInstance exists and is alive (otherwise we create a new one)
        if ((activeInstance != null) && (activeInstance.bodyOnThis != null) &&
            activeInstance.bodyOnThis.isActive())
            return activeInstance;
        stdInstance = getInstance();
        activeInstance = PAActiveObject.turnActive(stdInstance);

        return activeInstance;
    }

    /**
     * Returns the real singleton instance of the proxy
     *
     * @return instance of the proxy
     */
    public static synchronized MatSciSchedulerProxy getInstance() {

        if (stdInstance != null)
            return stdInstance;

        stdInstance = new MatSciSchedulerProxy();
        return stdInstance;
    }

    /**
     * This method forcefully terminates the activity of the proxy
     * This method should not be called via a proactive stub
     */
    public void terminateFast() {
        // if the service thread is locked on a user-level Thread.sleep() :
        serviceThread.interrupt();
        // destroy the request queue
        BlockingRequestQueueImpl rq = (BlockingRequestQueueImpl) bodyOnThis.getRequestQueue();
        rq.destroy();
        // kill the body
        try {
            bodyOnThis.terminate(false);
        } catch (Exception e) {

        }
        while (serviceThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (recMan != null) {
            try {
                recMan.close();
                recMan = null;
            } catch (IOException e) {

            }
        }
    }

    /**
     * Sets the name of this recording session. The name must be an unique word composed of alphanumerical charecter
     * The file used to persist awaited jobs will be named accordingly. If no session name is provided, a generic default name will be used.
     *
     * @param name alphanumerical word
     */
    public void setSessionName(String name) {
        if (awaitedJobs != null) {
            throw new IllegalStateException(
                "Session already started, try calling setSessionName before calling init");
        }
        if (name != null && !name.matches("\\w+")) {
            throw new IllegalArgumentException("Session Name must be an alphanumerical word.");
        }
        if (name == null) {
            sessionName = DEFAULT_STATUS_FILENAME;
        } else {
            sessionName = name;
        }
        statusFile = new File(TMPDIR, sessionName);
    }

    /*
     * (non-Javadoc)
     *
     * @see #init(String url, String user, String pwd)
     */
    @Override
    public void init(String url, String user, String pwd) throws SchedulerException, LoginException {
        CredData cred = new CredData(CredData.parseLogin(user), CredData.parseDomain(user), pwd);
        init(url, cred);
    }

    /*
     * (non-Javadoc)
     *
     * @see #init(String url, Credentials credentials)
     */
    @Override
    public void init(String url, Credentials credentials) throws SchedulerException, LoginException {
        // then we call super.init() which will create the connection to the
        // scheduler and subscribe as event listener
        super.init(url, credentials);
        this.lastSchedUrl = url;
        this.lastCredentials = credentials;
        this.lastCredData = null;
        // now we can can check if we need to transfer any data
        loadJobs();

        super.addEventListener(stubOnThis, true, SchedulerEvent.JOB_RUNNING_TO_FINISHED,
                SchedulerEvent.JOB_PENDING_TO_FINISHED, SchedulerEvent.KILLED, SchedulerEvent.SHUTDOWN,
                SchedulerEvent.SHUTTING_DOWN, SchedulerEvent.STOPPED, SchedulerEvent.RESUMED,
                SchedulerEvent.TASK_RUNNING_TO_FINISHED);
        syncAwaitedJobs();
    }

    /*
     * (non-Javadoc)
     *
     * @see #init(String url, Credentials credentials)
     */
    @Override
    public void init(String url, CredData credData) throws SchedulerException, LoginException {
        //  we call super.init() which will create the connection to the
        // scheduler and subscribe as event listener
        super.init(url, credData);
        this.lastSchedUrl = url;
        this.lastCredentials = null;
        this.lastCredData = credData;
        // now we can can check if we need to transfer any data
        loadJobs();
        super.addEventListener(stubOnThis, true, SchedulerEvent.JOB_RUNNING_TO_FINISHED,
                SchedulerEvent.JOB_PENDING_TO_FINISHED, SchedulerEvent.KILLED, SchedulerEvent.SHUTDOWN,
                SchedulerEvent.SHUTTING_DOWN, SchedulerEvent.STOPPED, SchedulerEvent.RESUMED,
                SchedulerEvent.TASK_RUNNING_TO_FINISHED);
        syncAwaitedJobs();
    }

    /**
     * load the awaited jobs from the status file
     */
    protected void loadJobs() {

        try {
            recMan = RecordManagerFactory.createRecordManager(statusFile.getCanonicalPath());
            awaitedJobs = recMan.hashMap(STATUS_RECORD_NAME);
            recMan.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initActivity(Body body) {
        stubOnThis = (MatSciSchedulerProxy) PAActiveObject.getStubOnThis();
        bodyOnThis = PAActiveObject.getBodyOnThis();
        serviceThread = Thread.currentThread();
    }

    @Override
    public void endActivity(Body body) {
        if (recMan != null) {
            try {
                recMan.close();
                recMan = null;
            } catch (Exception e) {

            }
        }
    }

    /**
     * Try to reconnect to a previously connected scheduler. The same url and credentials used during the previous connection will be reused.
     */
    public void reconnect() throws SchedulerException, LoginException {
        if (this.lastSchedUrl == null) {
            throw new IllegalStateException("No connection to the scheduler has been established yet.");
        }
        disconnect();
        if (this.lastCredentials == null) {
            super.init(lastSchedUrl, lastCredData);
        } else {
            super.init(lastSchedUrl, lastCredentials);
        }
        super.addEventListener(stubOnThis, true, SchedulerEvent.JOB_RUNNING_TO_FINISHED,
                SchedulerEvent.JOB_PENDING_TO_FINISHED, SchedulerEvent.KILLED, SchedulerEvent.SHUTDOWN,
                SchedulerEvent.SHUTTING_DOWN, SchedulerEvent.STOPPED, SchedulerEvent.RESUMED,
                SchedulerEvent.TASK_RUNNING_TO_FINISHED);
        loadJobs();
        syncAwaitedJobs();
    }

    /**
     * This call "reset" the current proxy by removing all knowledge of awaited jobs
     */
    public void discardAllJobs() {
        awaitedJobs.clear();
        logger.info(CLZN + "Proxy's database has been reseted.");
        try {
            recMan.commit();
        } catch (IOException e) {
            logger.error(CLZN + "Exception occured while closing connection to status file:", e);
        }
    }

    public void disconnect() throws PermissionException {
        try {
            super.disconnect();
        } catch (NotConnectedException e) {
            // we ignore this exception
        } catch (PermissionException e) {
            throw e;
        } catch (Exception e) {
            // we ignore any runtime exception
        }
        if (recMan != null) {
            try {
                recMan.close();
                recMan = null;
            } catch (Exception e) {

            }
        }
    }

    /**
     * Removes the given job of the awaited job list (should rarely be used)
     */
    public void discardJob(String jobID) {
        if (awaitedJobs.containsKey(jobID)) {
            awaitedJobs.remove(jobID);
            try {
                recMan.commit();
            } catch (IOException e) {
                logger.error(CLZN + "Exception occured while closing connection to status file:", e);
            }
        } else {
            logger.warn(CLZN + "Job " + jobID + " is not handled by the proxy.");
        }
    }

    @Override
    public JobId submit(Job job) throws NotConnectedException, PermissionException,
            SubmissionClosedException, JobCreationException {
        return super.submit(job);
    }

    /**
     * Does the following steps:
     * <ul>
     * <li>Prepares the temporary folders.
     * </li>
     * <p/>
     * <li>pushes all files from localInputFolderPath to the push_url location
     * <p/>
     * <li>submits the job to the scheduler</li>
     * <p/>
     * <li>adds the job to the awaited jobs list in order to download the output
     * data when the job is finished</li>
     * <p/>
     * </ul>
     * <p/>
     * Note: this method is synchronous. The caller will be blocked until the
     * the data is pushed and the job submitted.
     *
     * @param job                   job object to be submitted to the Scheduler Server for
     *                              execution
     * @param localInputFolderPath  path to the folder containing the input data for this job
     * @param push_url              the url where input data is to be pushed before the job
     *                              submission
     * @param localOutputFolderPath path to the folder where the output data produced by tasks in
     *                              this job should be copied
     * @param pull_url              the url where the data is to be retrieved after the job is
     *                              finished
     * @param isolateTaskOutputs    isolate the output produced by each task in a dedicated subfolder.
     *                              It will not be possible to reuse a file produced by a task as an input of a latter task,
     *                              but we guaranty this way that their will be no overlapping of files produced by parallel tasks.
     * @param automaticTransfer     when this is set to true, the transfer or files between the pull_url shared space to
     *                              the local machine will be done automatically by the proxy. Notifications according to
     *                              the ISchedulerEventListenerExtended interface will be sent to the listeners upon transfer
     *                              completion or failure.
     * @return id of the job created
     * @throws JobCreationException
     * @throws SubmissionClosedException
     * @throws PermissionException
     * @throws NotConnectedException
     * @throws FileSystemException
     */
    public JobId submit(TaskFlowJob job, String localInputFolderPath, String push_url,
            String localOutputFolderPath, String pull_url, boolean isolateTaskOutputs,
            boolean automaticTransfer) throws NotConnectedException, PermissionException,
            SubmissionClosedException, JobCreationException, FileSystemException {

        if (((push_url == null) || (push_url.equals(""))) && ((pull_url == null) || (pull_url.equals("")))) {
            logger
                    .warn(CLZN + "For the job " + job.getId() +
                        " no push or pull urls are defined. No data will be transfered for this job from the local machine ");
            return super.submit(job);
        }

        String newFolderName = createNewFolderName();
        String push_Url_update = prepareJobInput(job, localInputFolderPath, push_url, newFolderName);
        String pull_url_update = prepareJobOutput(job, localOutputFolderPath, pull_url, newFolderName,
                isolateTaskOutputs);

        pushData(job, localInputFolderPath);
        JobId id = null;
        try {
            id = super.submit(job);
        } catch (NotConnectedException e) {
            removeJobIO(job, push_url, pull_url, newFolderName);
            throw e;
        } catch (PermissionException e) {
            removeJobIO(job, push_url, pull_url, newFolderName);
            throw e;
        } catch (SubmissionClosedException e) {
            removeJobIO(job, push_url, pull_url, newFolderName);
            throw e;
        } catch (JobCreationException e) {
            removeJobIO(job, push_url, pull_url, newFolderName);
            throw e;
        } catch (RuntimeException e) {
            removeJobIO(job, push_url, pull_url, newFolderName);
            throw e;
        }

        HashMap<String, AwaitedTask> ats = new HashMap<String, AwaitedTask>();
        for (Task t : job.getTasks()) {
            ats.put(t.getName(), new AwaitedTask(t.getName(), t.getOutputFilesList()));
        }

        AwaitedJob aj = new AwaitedJob(id.toString(), localInputFolderPath, job.getInputSpace(),
            push_Url_update, localOutputFolderPath, job.getOutputSpace(), pull_url_update,
            isolateTaskOutputs, automaticTransfer, ats);

        addAwaitedJob(aj);
        return id;
    }

    /**
     * This method will create a remote folder for output of this
     * job and update the outputSpace job property. If the localOutputFolder
     * parameter is null, or pull_url no action will be performed concerning
     * this job's output.
     * <p/>
     * <p/>
     * We suppose there is file storage accessible by the client application as
     * well as the tasks on the computation nodes.
     * <p/>
     * This storage could be different for input and for output.
     * <p/>
     * <p/>
     * This output storage can be accessed, by the client application, using the
     * pull_url and by the tasks on the nodes using the job's output space url.
     * <p/>
     * <p/>
     * Prepare Output Data Transfer
     * <p/>
     * A folder will be created at pull_url/NewFolder/output (which, from the
     * nodes side, is job.OutputSpace/NewFolder/output).
     * <p/>
     * The OutputSpace property of the job will be changed to the new location.
     * job.OutputSpace = job.OutputSpace/NewFolder/output
     * <p/>
     * A generic information will be attached to the job containing the local
     * output folder path
     * If the option isolateTaskOutputs is set, a subfolder of "output" named "[TASKID]" will be created, it will behave as a tag to tell the
     * TaskLauncher to create a subfolder with the real taskid when the task is executed.
     *
     * @param job
     * @param localOutputFolder  path to the output folder on local machine if null, no actions
     *                           will be performed concerning the output data for this job
     * @param pull_url           - the url where the data is to be retrieved after the job is
     *                           finished
     * @param newFolderName      name of the folder to be used for pushing the output
     * @param isolateTaskOutputs task output isolation (see method submit)
     * @return a String representing the updated value of the pull_url
     * @throws FileSystemException
     */
    protected String prepareJobOutput(TaskFlowJob job, String localOutputFolder, String pull_url,
            String newFolderName, boolean isolateTaskOutputs) throws FileSystemException {
        // if the job defines an output space
        // and the localOutputFolder is not null
        // create a remote folder for the output data
        // and update the OutputSpace property of the job to reference that
        // folder
        String outputSpace_url = job.getOutputSpace();
        String pull_url_updated = "";

        // the output folder, on the remote output space, relative to the root
        // url
        String outputFolder = "";

        if ((localOutputFolder != null) && (outputSpace_url != null) && (!outputSpace_url.equals("")) &&
            (pull_url != null)) {
            if (isolateTaskOutputs) {
                // at the end we add the [TASKID] pattern without creating the folder
                outputFolder = newFolderName + "/output/" + TASKID_DIR_DEFAULT_NAME;
            } else {
                outputFolder = newFolderName + "/output";
            }

            pull_url_updated = pull_url + "/" + outputFolder;
            String outputSpace_url_updated = outputSpace_url + "/" + outputFolder;
            logger.debug(CLZN + "Output space of job " + job.getName() + " will be " +
                outputSpace_url_updated);

            createFolder(pull_url_updated);

            job.setOutputSpace(outputSpace_url_updated);
            job.addGenericInformation(GENERIC_INFO_OUTPUT_FOLDER_PROPERTY_NAME, new File(localOutputFolder)
                    .getAbsolutePath());
            job.addGenericInformation(GENERIC_INFO_PULL_URL_PROPERTY_NAME, pull_url_updated);
        }

        return pull_url_updated;
    }

    /**
     * This method will create a remote folder for the input data of this
     * job and update the inputSpace job property.
     * If the  localInputFolder parameter is null, or push_url is null, no action will
     * be performed concerning this job's input.
     * <p/>
     * <p/>
     * We suppose there is file storage accessible by the client application as
     * well as the tasks on the computation nodes.
     * <p/>
     * This storage could be different for input and for output.
     * <p/>
     * The input storage can be accessed, by the client application, using the
     * push_url and by the tasks on the nodes using the job's input space url.
     * <p/>
     * <p/>
     * Prepare Input Data Transfer
     * <p/>
     * A folder will be created at push_url/newFolderName/input (which, from the
     * nodes side, is the job.InputSpace/newFolderName/input) . The InputSpace
     * property of the job will be changed to the new location. job.InputSpace =
     * job.InputSpace/NewFolder/input
     * <p/>
     * <p/>
     * A generic information will be attached to the job containing the local
     * input folder path.
     *
     * @param job
     * @param localInputFolder path to the input folder on local machine if null, no actions
     *                         will be performed concerning the input data for this job
     * @param push_url         the url where input data is to be pushed before the job
     *                         submission
     * @param newFolderName    name of the new folder to be created
     * @return String representing the updated value of the push_url
     * @throws FileSystemException
     */
    protected String prepareJobInput(Job job, String localInputFolder, String push_url, String newFolderName)
            throws FileSystemException {
        // if the job defines an input space
        // and the localInputFolder is not null
        // create a remote folder for the input data
        // and update the InputSpace property of the job to reference that
        // folder

        String inputSpace_url = job.getInputSpace();
        String push_url_updated = "";

        // the input folder, on the remote input space, relative to the root url
        String inputFolder = "";
        if ((localInputFolder != null) && (inputSpace_url != null) && (!inputSpace_url.equals("")) &&
            (push_url != null)) {
            inputFolder = newFolderName + "/input";
            push_url_updated = push_url + "/" + inputFolder;
            String inputSpace_url_updated = inputSpace_url + "/" + inputFolder;
            createFolder(push_url_updated);
            job.setInputSpace(inputSpace_url_updated);
            job.addGenericInformation(GENERIC_INFO_INPUT_FOLDER_PROPERTY_NAME, new File(localInputFolder)
                    .getAbsolutePath());

            job.addGenericInformation(GENERIC_INFO_PUSH_URL_PROPERTY_NAME, push_url_updated);
        }

        // if the job defines an output space
        // and the localOutputFolder is not null
        // create a remote folder for the output data
        // and update the OutputSpace property of the job to reference that
        // folder

        return push_url_updated;
    }

    protected void removeJobIO(Job job, String push_url, String pull_url, String newFolderName)
            throws FileSystemException {
        String push_url_updated = push_url + "/" + newFolderName;
        FileObject fo = fsManager.resolveFile(push_url_updated);
        try {

            fo.delete(Selectors.SELECT_ALL);
            fo.delete();
        } catch (Exception e) {
            logger.debug(CLZN + "Error in removeJobIO push for job " + job.getName());
        }
        String pull_url_updated = pull_url + "/" + newFolderName;
        fo = fsManager.resolveFile(pull_url_updated);
        try {
            fo.delete(Selectors.SELECT_ALL);
            fo.delete();
        } catch (Exception e) {
            logger.debug(CLZN + "Error in removeJobIO pull for job " + job.getName());
        }
    }

    protected void createFolder(String fUri) throws FileSystemException {

        FileObject fo = fsManager.resolveFile(fUri);
        fo.createFolder();

        logger.debug(CLZN + "Created remote folder: " + fUri);
    }

    protected String createNewFolderName() {
        String user = System.getProperty("user.name");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);

        String newFolderName = user + "_" + strDate;
        return newFolderName;

    }

    // ******** Pushing and Pulling Data *********************** //

    /**
     * Push the input files of the given job from the local input folder to the push_url
     *
     * @param job                  job to push data for
     * @param localInputFolderPath local input folder
     * @return
     * @throws FileSystemException
     */
    protected boolean pushData(TaskFlowJob job, String localInputFolderPath) throws FileSystemException {

        String push_URL = job.getGenericInformations().get(GENERIC_INFO_PUSH_URL_PROPERTY_NAME);

        if ((push_URL == null) || (push_URL.trim().equals(""))) {
            return false;
        }// push inputData

        // TODO - if the copy fails, try to remove the files from the remote
        // folder before throwing an exception
        FileObject remoteFolder = fsManager.resolveFile(push_URL);
        FileObject localfolder = fsManager.resolveFile(localInputFolderPath);
        String jname = job.getName();
        logger
                .debug(CLZN + "Pushing files for job " + jname + " from " + localfolder + " to " +
                    remoteFolder);

        List<DataTransferProcessor> transferCallables = new ArrayList<DataTransferProcessor>();

        TaskFlowJob tfj = job;
        for (Task t : tfj.getTasks()) {
            logger.debug(CLZN + "Pushing files for task " + t.getName());
            List<InputSelector> inputFileSelectors = t.getInputFilesList();
            //create the selector
            MatSciDSFileSelector fileSelector = new MatSciDSFileSelector();
            for (InputSelector is : inputFileSelectors) {
                org.ow2.proactive.scheduler.common.task.dataspaces.FileSelector fs = is.getInputFiles();
                if (fs.getIncludes() != null)
                    fileSelector.addIncludes(Arrays.asList(fs.getIncludes()));

                if (fs.getExcludes() != null)
                    fileSelector.addExcludes(Arrays.asList(fs.getExcludes()));

                //We should check if a pattern exist in both includes and excludes. But that would be a user mistake.
            }
            DataTransferProcessor dtp = new DataTransferProcessor(localfolder, remoteFolder, tfj.getName(), t
                    .getName(), fileSelector);
            transferCallables.add(dtp);
        }

        List<Future<Boolean>> futures = null;
        try {
            futures = tpe.invokeAll(transferCallables);
        } catch (InterruptedException e) {
            logger.error(CLZN + "Interrupted while transferring files of job " + jname, e);
            throw new RuntimeException(e);
        }
        for (int i = 0; i < futures.size(); i++) {
            Future<Boolean> answer = futures.get(i);
            String tname = tfj.getTasks().get(i).getName();
            try {
                if (!answer.get()) {
                    throw new RuntimeException("Files of task " + tname + " for job " + jname +
                        " could not be transferred");
                }
            } catch (InterruptedException e) {
                logger.error(CLZN + "Interrupted while transferring files of task " + tname + " for job " +
                    jname, e);
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                logger.error(CLZN + "Exception occured while transferring files of task " + tname +
                    " for job " + jname, e);
                throw new RuntimeException(e);
            }
        }

        logger.debug(CLZN + "Finished push operation from " + localfolder + " to " + remoteFolder);
        return true;
    }

    /**
     * Pull the output files of the given task from the pull_url either to the localFolder defined for the job or to the localFolder specified as argument, if it is not null.
     * The call to the method pullData is triggered by the user. If the job is configured to be handled asynchronously, call to this method will trigger a RuntimeException.
     *
     * @param jobId       job to pull data for
     * @param t_name      name of the task
     * @param localFolder local output folder, if not null, it overrides the folder specified as output folder for the job
     * @return
     * @throws FileSystemException if there is a problem during the transfer
     */
    public void pullData(String jobId, String t_name, String localFolder) throws FileSystemException {

        AwaitedJob awaitedjob = awaitedJobs.get(jobId);
        if (awaitedjob == null) {
            throw new IllegalArgumentException("The job " + jobId + " is unknown ");
        }
        if (awaitedjob.isAutomaticTransfer()) {
            throw new UnsupportedOperationException("Transfer of input files with job " + jobId +
                " is handled automatically.");
        }

        String localOutFolderPath = null;
        if (localFolder == null) {
            localOutFolderPath = awaitedjob.getLocalOutputFolder();
        } else {
            localOutFolderPath = localFolder;
        }
        if (localOutFolderPath == null) {
            throw new IllegalArgumentException("The job " + awaitedjob.getJobId() +
                " does not define an output folder on local machine, please provide an outputFolder.");
        }
        pullDataInternal(awaitedjob, jobId, t_name, localOutFolderPath);
    }

    /**
     * Internal version of pullData, will use a separate Thread to transfer files if automaticTransfer is set to true
     *
     * @param awaitedjob  job to handle
     * @param jobId       job id
     * @param t_name      task name
     * @param localFolder local folder to copy files to or null if we use the job's local folder
     * @throws FileSystemException
     */
    public void pullDataInternal(AwaitedJob awaitedjob, String jobId, String t_name, String localFolder)
            throws FileSystemException {

        AwaitedTask atask = awaitedjob.getAwaitedTask(t_name);
        if (atask == null) {
            throw new IllegalArgumentException("The task " + t_name + " does not belong to job " + jobId);
        }
        String pull_URL = awaitedjob.getPullURL();

        if (awaitedjob.isIsolateTaskOutputs()) {
            pull_URL = pull_URL.replace(TASKID_DIR_DEFAULT_NAME, TASKID_DIR_DEFAULT_NAME + "/" +
                atask.getTaskId());
        }

        FileObject remotePullFolderFO = null;
        FileObject localfolderFO = null;

        try {
            remotePullFolderFO = fsManager.resolveFile(pull_URL);

            localfolderFO = fsManager.resolveFile(localFolder);
        } catch (FileSystemException e) {
            logger.error(CLZN + "Could not retrieve data for job " + jobId, e);
            throw new IllegalStateException("Could not retrieve data for job " + jobId, e);
        }

        String sourceUrl = remotePullFolderFO.getURL().toString();
        String destUrl = localfolderFO.getURL().toString();
        //create the selector
        MatSciDSFileSelector fileSelector = new MatSciDSFileSelector();

        List<OutputSelector> ouputFileSelectors = atask.getOutputSelectors();
        for (OutputSelector os : ouputFileSelectors) {
            org.ow2.proactive.scheduler.common.task.dataspaces.FileSelector fs = os.getOutputFiles();
            if (fs.getIncludes() != null)
                fileSelector.addIncludes(Arrays.asList(fs.getIncludes()));

            if (fs.getExcludes() != null)
                fileSelector.addExcludes(Arrays.asList(fs.getExcludes()));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(CLZN + "Looking at files in " + sourceUrl + " with " + fileSelector.getIncludes() +
                "-" + fileSelector.getExcludes());
            boolean goon = true;
            int cpt = 0;
            FileObject[] fos = null;
            while (goon) {
                fos = remotePullFolderFO.findFiles(fileSelector);
                goon = cpt < 50 && (fos == null || fos.length == 0);
                cpt++;
                if (goon) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                }
            }

            if (fos != null && fos.length > 0) {
                for (FileObject fo : fos) {
                    logger.debug(CLZN + "Found " + fo.getName());
                }
            } else {
                logger.warn(CLZN + "Couldn't find " + fileSelector.getIncludes() + "-" +
                    fileSelector.getExcludes() + " in " + sourceUrl);
            }
        }
        if (awaitedjob.isAutomaticTransfer()) {
            DataTransferProcessor dtp = new DataTransferProcessor(remotePullFolderFO, localfolderFO, jobId,
                t_name, fileSelector);
            tpe.submit((Runnable) dtp);
        } else {
            logger.debug(CLZN + "Copying files from " + sourceUrl + " to " + destUrl);
            localfolderFO.copyFrom(remotePullFolderFO, fileSelector);
            logger.debug(CLZN + "Finished copying files from " + sourceUrl + " to " + destUrl);
            // ok we can remove the task
            removeAwaitedTask(jobId, t_name);
        }
    }

    // ******** Scheduler Event Listener *********************** //

    /**
     * Subscribes a listener to the Scheduler
     */
    public void addEventListener(ISchedulerEventListenerExtended sel) throws NotConnectedException,
            PermissionException {
        eventListeners.add(sel);
    }

    public void removeEventListener(ISchedulerEventListenerExtended sel) {
        eventListeners.remove(sel);
    }

    // ***  Forward all events from the Scheduler to the local listeners *************** //

    /**
     * Invoked each time a scheduler event occurs.<br />
     * Scheduler events are stopped,started, paused, frozen, ...
     *
     * @param eventType the type of the event received.
     */
    public void schedulerStateUpdatedEvent(SchedulerEvent eventType) {
        for (SchedulerEventListener l : eventListeners) {
            l.schedulerStateUpdatedEvent(eventType);
        }

    }

    /**
     * Invoked each time a new job has been submitted to the Scheduler and
     * validated.
     *
     * @param job the newly submitted job.
     */
    public void jobSubmittedEvent(JobState job) {
        for (SchedulerEventListener l : eventListeners) {
            l.jobSubmittedEvent(job);
        }
    }

    /**
     * Invoked each time the state of a job has changed.<br>
     * If you want to maintain an up to date list of jobs, just use the
     * {@link org.ow2.proactive.scheduler.common.job.JobState#update(org.ow2.proactive.scheduler.common.job.JobInfo)}
     * method to update the content of your job.
     *
     * @param notification the data composed of the type of the event and the information
     *                     that have change in the job.
     */
    public void jobStateUpdatedEvent(NotificationData<JobInfo> notification) {
        updateJob(notification);
        for (SchedulerEventListener l : eventListeners) {
            l.jobStateUpdatedEvent(notification);
        }
    }

    /**
     * Invoked each time the state of a task has changed.<br>
     * In this case you can use the
     * {@link org.ow2.proactive.scheduler.common.job.JobState#update(org.ow2.proactive.scheduler.common.task.TaskInfo)}
     * method to update the content of the designated task inside your job.
     *
     * @param notification the data composed of the type of the event and the information
     *                     that have change in the task.
     */
    @Override
    public void taskStateUpdatedEvent(NotificationData<TaskInfo> notification) {
        updateTask(notification);
        for (SchedulerEventListener l : eventListeners) {
            l.taskStateUpdatedEvent(notification);
        }

    }

    /**
     * Invoked each time something change about users.
     *
     * @param notification the data composed of the type of the event and the data linked
     *                     to the change.
     */
    public void usersUpdatedEvent(NotificationData<UserIdentification> notification) {
        for (SchedulerEventListener l : eventListeners) {
            l.usersUpdatedEvent(notification);
        }
    }

    // ********* Awaited Jobs methods ******************************* //

    /**
     * @return a new HashSet with the awaited jobs. Modifying the result of this
     *         method will not affect the source HashSet (the awaited jobs)
     */
    protected HashSet<AwaitedJob> getAwaitedJobs() {
        return new HashSet<AwaitedJob>(awaitedJobs.values());
    }

    protected boolean isAwaitedJob(String id) {
        if (awaitedJobs.get(id) != null)
            return true;
        else
            return false;
    }

    protected void addAwaitedJob(AwaitedJob aj) {
        this.awaitedJobs.put(aj.getJobId(), aj);
        try {
            this.recMan.commit();
        } catch (IOException e) {
            logger.error(CLZN + "Could not save status file after adding job on awaited jobs list " +
                aj.getJobId(), e);
        }
    }

    /**
     * Removes from the proxy knowledge all info related with the given job.
     * This will also delete every folder created by the job in the shared input and output spaces
     *
     * @param id jobID
     */
    protected void removeAwaitedJob(String id) {

        AwaitedJob aj = awaitedJobs.get(id);
        if (aj == null) {
            throw new IllegalArgumentException("Job " + id + " not in the awaited list");
        }
        logger.debug(CLZN + "Removing knowledge of job " + id);

        String pull_URL = aj.getPullURL();

        String pushUrl = aj.getPushURL();

        FileObject remotePullFolder = null;
        FileObject remotePushFolder = null;

        try {
            remotePullFolder = fsManager.resolveFile(pull_URL);
            remotePushFolder = fsManager.resolveFile(pushUrl);
        } catch (Exception e) {
            logger.error(CLZN + "Could not remove data for job " + id, e);
            return;
        }
        if (aj.isIsolateTaskOutputs()) {
            try {
                remotePullFolder = remotePullFolder.getParent();
            } catch (FileSystemException e) {
                logger.error(CLZN + "Could not get the parent of folder " + remotePullFolder, e);
            }
        }

        Set<FileObject> foldersToDelete = new HashSet<FileObject>();
        try {
            foldersToDelete.add(remotePullFolder.getParent());
            if (!remotePullFolder.getParent().equals(remotePushFolder.getParent()))
                foldersToDelete.add(remotePushFolder.getParent());
        } catch (FileSystemException e) {
            logger.warn(CLZN + "Data in folders " + pull_URL + " and " + pushUrl +
                " cannot be deleted due to an unexpected error ", e);
            e.printStackTrace();
        }

        String url = "NOT YET DEFINED";
        for (FileObject fo : foldersToDelete) {
            try {
                url = fo.getURL().toString();

                if (!logger.isTraceEnabled()) {
                    logger.debug(CLZN + "Deleting directory " + url);
                    fo.delete(Selectors.SELECT_ALL);
                    fo.delete();
                }
            } catch (FileSystemException e) {
                logger.warn(CLZN + "Could not delete temporary files at location " + url + " .");
            }
        }

        this.awaitedJobs.remove(id);

        try {
            this.recMan.commit();
        } catch (IOException e) {
            logger.error(CLZN + "Could not save status file after removing job " + id, e);
        }
    }

    /**
     * Removes from the proxy knowledge all info related with the given task.
     * If all tasks of a job have been removed this way, the job itself will be removed.
     *
     * @param id    jobID
     * @param tname task name
     */
    protected void removeAwaitedTask(String id, String tname) {
        AwaitedJob aj = awaitedJobs.get(id);
        if (aj == null) {
            throw new IllegalArgumentException("Job " + id + " not in the awaited list");
        }
        AwaitedTask at = aj.getAwaitedTask(tname);
        if (at == null) {
            throw new IllegalArgumentException("Task " + tname + " from Job " + id +
                " not in the awaited list");
        }
        logger.debug(CLZN + "Removing knowledge of task " + tname + " from job " + id);
        if (aj.isIsolateTaskOutputs() && at.getTaskId() != null) {
            // If the output data as been isolated in a dedicated folder we can delete it.

            String pull_URL = aj.getPullURL();
            pull_URL = pull_URL.replace(TASKID_DIR_DEFAULT_NAME, TASKID_DIR_DEFAULT_NAME + "/" +
                at.getTaskId());

            FileObject remotePullFolder = null;

            try {
                remotePullFolder = fsManager.resolveFile(pull_URL);
                String url = remotePullFolder.getURL().toString();
                logger.debug(CLZN + "Deleting directory " + remotePullFolder);
                remotePullFolder.delete(Selectors.SELECT_ALL);
                remotePullFolder.delete();
            } catch (Exception e) {
                logger.warn(CLZN + "Could not remove data for task " + tname + " of job " + id, e);
            }

        }

        aj.removeAwaitedTask(tname);
        if (aj.getAwaitedTasks().isEmpty()) {
            removeAwaitedJob(id);
            return;
        } else {
            awaitedJobs.put(id, aj); // this is done to ensure persistance of the operation
        }

        try {
            this.recMan.commit();
        } catch (IOException e) {
            logger.error(CLZN + "Could not save status file after removing task Task " + tname + " from Job" +
                id, e);
        }

    }

    /**
     * This method will synchronize this proxy with a remote Scheduler, it will contact the scheduler and checks the current state of every job being handled.
     * It is called either during the proxy initialization, or after a manual reconnection.
     */
    protected void syncAwaitedJobs() {
        // we make a copy of the awaitedJobsIds set in order to iterate over it.
        Set<String> awaitedJobsIdsCopy = new HashSet<String>(awaitedJobs.keySet());
        for (String id : awaitedJobsIdsCopy) {
            syncAwaitedJob(id);
        }
    }

    /**
     * This method will synchronize this proxy with a remote Scheduler for the given job
     *
     * @param id jobId
     */
    protected void syncAwaitedJob(String id) {

        AwaitedJob awaitedJob = awaitedJobs.get(id);

        try {
            JobState js = uischeduler.getJobState(id);

            for (TaskState ts : js.getTasks()) {
                String tname = ts.getName();
                if (awaitedJob.getAwaitedTask(tname) != null) {
                    TaskResult tres = null;
                    try {
                        tres = uischeduler.getTaskResult(id, tname);
                        if (tres != null) {
                            taskStateUpdatedEvent(new NotificationData<TaskInfo>(
                                SchedulerEvent.TASK_RUNNING_TO_FINISHED, ts.getTaskInfo()));
                        }
                    } catch (NotConnectedException e) {
                        e.printStackTrace();
                    } catch (UnknownJobException e) {
                        logger.error(CLZN + "Could not retrieve output data for job " + id +
                            " because this job is not known by the Scheduler. \n ", e);
                    } catch (UnknownTaskException e) {
                        logger.error(CLZN + "Could not retrieve output data for task " + tname + " of job " +
                            id + " because this task is not known by the Scheduler. \n ", e);
                    } catch (Exception e) {
                        logger.error(CLZN + "Unexpected error while getting the output data for task " +
                            tname + " of job " + id, e);
                    }
                }
            }
            if (js.isFinished()) {
                jobStateUpdatedEvent(new NotificationData<JobInfo>(SchedulerEvent.JOB_RUNNING_TO_FINISHED, js
                        .getJobInfo()));
            }

        } catch (NotConnectedException e) {
            logger
                    .error(
                            CLZN +
                                "A connection error occured while trying to download output data of Job " +
                                id +
                                ". This job will remain in the list of awaited jobs. Another attempt to dowload the output data will be made next time the application is initialized. ",
                            e);
        } catch (UnknownJobException e) {
            logger.error(CLZN + "Could not retrieve output data for job " + id +
                " because this job is not known by the Scheduler. \n ", e);
            logger
                    .warn(CLZN +
                        "Job  " +
                        id +
                        " will be removed from the known job list. The system will not attempt again to retrieve data for this job. You could try to manually copy the data from the location  " +
                        awaitedJob.getPullURL());
            removeAwaitedJob(id);
        } catch (PermissionException e) {
            logger
                    .error(
                            CLZN +
                                "Could not retrieve output data for job " +
                                id +
                                " because you don't have permmission to access this job. You need to use the same connection credentials you used for submitting the job.  \n Another attempt to dowload the output data for this job will be made next time the application is initialized. ",
                            e);
        }

    }

    /**
     * Check if the job concerned by this notification is awaited. Retrieve
     * corresponding data if needed
     *
     * @param notification
     */
    protected void updateJob(NotificationData<?> notification) {

        // am I interested in this job?
        JobId id = ((NotificationData<JobInfo>) notification).getData().getJobId();

        AwaitedJob aj = awaitedJobs.get(id.toString());

        if (aj == null)
            return;

        JobStatus status = ((NotificationData<JobInfo>) notification).getData().getStatus();
        switch (status) {
            case KILLED: {
                logger.debug(CLZN + "The job " + id + "has been killed.");
                removeAwaitedJob(id.toString());
                break;
            }
            case FINISHED: {
                logger.debug(CLZN + "The job " + id + " is finished.");
                //removeAwaitedJob(id.toString());
                break;
            }
            case CANCELED: {
                logger.debug(CLZN + "The job " + id + " is canceled.");
                removeAwaitedJob(id.toString());
                break;
            }
            case FAILED: {
                logger.debug(CLZN + "The job " + id + " is failed.");
                //removeAwaitedJob(id.toString());
                break;
            }
        }
    }

    /**
     * Check if the task concerned by this notification is awaited. Retrieve
     * corresponding data if needed
     *
     * @param notification
     */
    protected void updateTask(NotificationData<?> notification) {

        // am I interested in this task?
        JobId id = ((NotificationData<TaskInfo>) notification).getData().getJobId();
        String tname = ((NotificationData<TaskInfo>) notification).getData().getName();
        TaskId tid = ((NotificationData<TaskInfo>) notification).getData().getTaskId();
        TaskStatus status = ((NotificationData<TaskInfo>) notification).getData().getStatus();

        AwaitedJob aj = awaitedJobs.get(id.toString());

        if (aj == null)
            return;

        AwaitedTask at = aj.getAwaitedTask(tname);

        if (at == null)
            return;

        at.setTaskId(tid.toString());
        awaitedJobs.put(id.toString(), aj);
        try {
            recMan.commit();
        } catch (IOException e) {
            logger.error(CLZN + "Could not save status file after updateTask notification for job " + id +
                " task " + tname);
        }

        switch (status) {
            case ABORTED:
            case NOT_RESTARTED:
            case NOT_STARTED:
            case SKIPPED: {
                logger.debug(CLZN + "The task " + tname + " from job " + id +
                    " couldn't start. No data will be transfered");
                removeAwaitedTask(id.toString(), tname);
                break;
            }
            case FINISHED: {
                logger.debug(CLZN + "The task " + tname + " from job " + id + " is finished.");
                if (aj.isAutomaticTransfer()) {
                    logger.debug(CLZN + "Transfering data for finished task " + tname + " from job " + id);
                    try {
                        pullDataInternal(aj, id.toString(), tname, aj.getLocalOutputFolder());
                    } catch (FileSystemException e) {
                        logger.error(CLZN + "Error while handling data for finished task " + tname +
                            " for job " + id + ", task will be removed");
                        removeAwaitedTask(id.toString(), tname);
                    }
                }
                break;
            }
            case FAULTY: {
                logger.debug(CLZN + "The task " + tname + " from job " + id + " is faulty.");
                if (aj.isAutomaticTransfer()) {
                    logger.debug(CLZN + "Transfering data for failed task " + tname + " from job " + id);
                    try {
                        pullDataInternal(aj, id.toString(), tname, aj.getLocalOutputFolder());
                    } catch (FileSystemException e) {
                        logger.error(CLZN + "Error while handling data for finished task " + tname +
                            " for job " + id + ", task will be removed");
                        removeAwaitedTask(id.toString(), tname);
                    }
                }
                break;
            }
        }
    }

    // ******** Scheduler Event Listener *********************** //

    /**
     * Handles the transfer of data asynchronously
     */
    private class DataTransferProcessor implements Runnable, Callable<Boolean> {
        private FileObject source;
        private FileObject dest;
        private String jobId;
        private String taskName;
        private FileSelector fileSelector;
        private String sourceUrl;
        private String destUrl;

        /**
         * @param source source folder
         * @param dest   dest folder
         * @param _jobId - only used for pull operations. For push operations, the
         *               jobId is null
         */
        public DataTransferProcessor(FileObject source, FileObject dest, String _jobId, String tname,
                FileSelector fileSelector) {
            this.source = source;
            this.dest = dest;
            this.jobId = _jobId;
            this.fileSelector = fileSelector;
            this.taskName = tname;
        }

        protected void transfer() throws Exception {
            sourceUrl = source.getURL().toString();
            destUrl = dest.getURL().toString();
            logger.debug(CLZN + "Copying files of task " + taskName + " of job " + jobId + " from " + source +
                " to " + dest);
            if (logger.isDebugEnabled()) {

                FileObject[] fos = source.findFiles(fileSelector);
                for (FileObject fo : fos) {
                    logger.debug(CLZN + "Found " + fo.getName());
                }

            }
            dest.copyFrom(source, fileSelector);
            logger.debug(CLZN + "Finished copying files of task " + taskName + " of job " + jobId + " from " +
                source + " to " + dest);
        }

        @Override
        public void run() {
            try {
                transfer();

            } catch (Exception e) {
                logger.error(CLZN + "An error occured while copying files of task " + taskName + " of job " +
                    jobId + " from " + source + " to " + dest, e);

                logger
                        .warn(CLZN +
                            "Task " +
                            taskName +
                            " of job " +
                            jobId +
                            " will be removed from the known task list. The system will not attempt again to retrieve data for this task. You could try to manually copy the data from the location  " +
                            sourceUrl);

                for (ISchedulerEventListenerExtended l : eventListeners) {
                    l.pullDataFailed(jobId, taskName, sourceUrl, e);
                }
                stubOnThis.removeAwaitedTask(jobId, taskName);
                return;

            }// catch

            stubOnThis.removeAwaitedTask(jobId, taskName);

            for (ISchedulerEventListenerExtended l : eventListeners) {
                l.pullDataFinished(jobId, taskName, destUrl);
            }
        }

        @Override
        public Boolean call() throws Exception {
            try {
                transfer();
            } catch (Exception e) {
                logger.error(CLZN + "An error occured while copying files of task " + taskName + " of job " +
                    jobId + " from " + source + " to " + dest, e);
                throw e;
            }// catch

            return true;
        }
    }
}
