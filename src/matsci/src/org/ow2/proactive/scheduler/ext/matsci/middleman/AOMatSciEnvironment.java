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
package org.ow2.proactive.scheduler.ext.matsci.middleman;

import jdbm.PrimaryHashMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import org.apache.commons.vfs.FileSystemException;
import org.objectweb.proactive.*;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.objectweb.proactive.core.body.request.BlockingRequestQueue;
import org.objectweb.proactive.core.body.request.BlockingRequestQueueImpl;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.body.request.RequestFilter;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.scheduler.common.*;
import org.ow2.proactive.scheduler.common.exception.*;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.dataspaces.InputAccessMode;
import org.ow2.proactive.scheduler.common.task.dataspaces.OutputAccessMode;
import org.ow2.proactive.scheduler.ext.common.util.BitMatrix;
import org.ow2.proactive.scheduler.ext.common.util.FileUtils;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.common.PASessionState;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.*;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerExceptionType;
import org.ow2.proactive.scheduler.ext.matsci.common.data.DSSource;
import org.ow2.proactive.scheduler.ext.matsci.common.exception.MatSciTaskException;
import org.ow2.proactive.scheduler.ext.matsci.middleman.proxy.ISchedulerEventListenerExtended;
import org.ow2.proactive.scheduler.ext.matsci.middleman.proxy.MatSciSchedulerProxy;
import org.ow2.proactive.scheduler.util.console.SchedulerModel;
import org.ow2.proactive.utils.FileToBytesConverter;
import org.ow2.proactive.utils.console.StdOutConsole;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.security.KeyException;
import java.util.*;
import java.util.concurrent.TimeoutException;


/**
 * AOMatSciEnvironment
 *
 * @author The ProActive Team
 */
public abstract class AOMatSciEnvironment<R, RL> implements MatSciEnvironment, Serializable,
        ISchedulerEventListenerExtended, InitActive, RunActive {

    private static final long serialVersionUID = 11;

    /**
     * Connection to the scheduler
     */
    protected Scheduler scheduler;

    /**
     * ProActive stub of the proxy to the scheduler with disconnected mode support
     */
    protected MatSciSchedulerProxy sched_proxy;

    /**
     * real object of the proxy
     */
    protected MatSciSchedulerProxy sched_proxy_root;

    /**
     * Connection to the scheduler console
     */
    protected SchedulerModel model;

    /**
     * Internal job id for job description only
     */
    protected long lastGenJobId = 0;

    /**
     * Id of this active object
     */
    protected String aoid;

    /**
     * Name of the jobs backup table
     */
    public static final String MIDDLEMAN_JOBS_RECORD_NAME = "MiddlemanJobs";

    /**
     * Name of the jobs backup table being recorded for replay
     */
    public static final String MIDDLEMAN_LASTJOBS_RECORD_NAME = "MiddlemanLastJobs";

    /**
     * Name of the jobs backup table being recorded for replay
     */
    public static final String MIDDLEMAN_RECORDEDJOBS_RECORD_NAME = "MiddlemanRecordedJobs";

    /**
     * Name of the jobs backup table being recorded for replay
     */
    public static final String MIDDLEMAN_SEQTOJOBID_RECORD_NAME = "MiddlemanSeq2JobId";

    /**
     * Name of the jobs backup table being recorded for replay
     */
    public static final String SESSION_RECORD_NAME = "MiddlemanSession";

    protected static final int MIN_RECONNECTION_SLEEP = 4;

    protected static final int MAX_RECONNECTION_SLEEP = 128;

    /**
     * Object handling the middlemanJobsFile connection
     */
    protected RecordManager recMan;

    protected static final String TMPDIR = System.getProperty("java.io.tmpdir");

    /**
     * All jobs which have been submitted
     */
    protected PrimaryHashMap<Long, MatSciJobInfo<R>> allJobs;

    /**
     * jobs being recorded during a session
     */
    protected PrimaryHashMap<Long, MatSciJobInfo<R>> recordedJobs;

    /**
     * ids of jobs from the last session
     */
    protected PrimaryHashMap<Long, Long> lastJobs;

    protected PrimaryHashMap<Integer, Long> mappingSeqToJobID;

    protected PrimaryHashMap<Integer, Date> sessions;

    protected PASessionState state = PASessionState.NORMAL;

    protected Date sessionStart = null;

    protected int currentSequenceIndex = 1;

    protected HashMap<Long, BitMatrix> tasksReceived = new HashMap<Long, BitMatrix>();

    /**
     * Ids of current jobs
     */
    protected HashSet<Long> currentJobs = new HashSet<Long>();

    /**
     * Ids of finished jobs
     */
    protected HashSet<Long> finishedJobs = new HashSet<Long>();

    /**
     * Map storing the pending request for a given job
     */
    protected HashMap<Long, Request> pendingRequests = new HashMap<Long, Request>();

    /**
     * Index used when executing PAwaitAny (index of the result received)
     */
    protected int lastPAWaitAnyIndex = -1;

    /**
     * timeout given when calling waitAny or waitAll
     */
    protected HashMap<Long, Integer> timeouts = new HashMap<Long, Integer>();

    /**
     * current time when the waitAny or waitAll requests are received
     */
    protected HashMap<Long, Long> beginTimes = new HashMap<Long, Long>();

    /**
     * If a timeout occured before serving waitAny or waitAll
     */
    protected HashMap<Long, Boolean> timeoutOccured = new HashMap<Long, Boolean>();

    /**
     * Debug mode
     */
    protected boolean debug;

    /**
     * tells if the hook used for PAMR reconnection has been deployed or not
     */
    private static boolean pamrHookSet = false;

    /**
     * Credentials used last
     */
    private Credentials oldCred;

    /**
     * Number of seconds slept before two reconnection attempts
     */
    protected int reconnectionSleep = MIN_RECONNECTION_SLEEP;

    /**
     * Proactive stub on this AO
     */
    protected AOMatSciEnvironment<R, RL> stubOnThis;

    protected Body bodyOnThis;

    /**
     * Is the AO terminated ?
     */
    protected boolean terminated;

    /**
     * Thread used for the service of this active object
     */
    protected Thread serviceThread;

    protected Thread shutDownHook;

    /**
     * Thread used to ping the scheduler
     */
    Thread pingerThread;

    /**
     * The scheduler has been killed
     */
    protected boolean schedulerKilled = false;

    /**
     * Is the current session logged into the scheduler
     */
    protected boolean loggedin;

    /**
     * Has the current session successfully joined a scheduler
     */
    protected boolean joined;

    /**
     * joined interface to the scheduler, before authentication
     */
    protected SchedulerAuthenticationInterface auth;

    /**
     * URL of the scheduler
     */
    protected String schedulerURL = null;

    /**
     * *******************************************************************************************************
     * ************************************* LOGIN AND CONNECTION ********************************************
     */

    public AOMatSciEnvironment() {

    }

    public AOMatSciEnvironment(boolean debug) {
        this.debug = debug;
        loadDB();
    }

    protected void loadDB() {
        try {
            recMan = RecordManagerFactory.createRecordManager(getMidlemanJobsFile().getCanonicalPath());
            allJobs = recMan.hashMap(MIDDLEMAN_JOBS_RECORD_NAME);

            lastJobs = recMan.hashMap(MIDDLEMAN_LASTJOBS_RECORD_NAME);
            recordedJobs = recMan.hashMap(MIDDLEMAN_RECORDEDJOBS_RECORD_NAME);
            mappingSeqToJobID = recMan.hashMap(MIDDLEMAN_SEQTOJOBID_RECORD_NAME);
            sessions = recMan.hashMap(SESSION_RECORD_NAME);

            // we clear the jobs of the n-2 session, if any
            lastJobs = recMan.hashMap(MIDDLEMAN_LASTJOBS_RECORD_NAME);
            cleanOldJobs();
            // after this cleaning the jobs remaining are only the ones from last session, so we record their ids and replace
            // lastJobs accordingly
            lastJobs.clear();
            for (Long id : allJobs.keySet()) {
                lastJobs.put(id, id);
            }

            // updating results matrix
            for (Map.Entry<Long, MatSciJobInfo<R>> entry : allJobs.entrySet()) {
                Long jid = entry.getKey();
                MatSciJobInfo jinfo = entry.getValue();
                jinfo.resetServedTasks();
                BitMatrix matrix = jinfo.getTaskReceptionMatrix();
                //System.out.println("Job "+jid+" : "+matrix);
                tasksReceived.put(jid, matrix);
                if (matrix.isTrue()) {
                    finishedJobs.add(jid);
                } else {
                    currentJobs.add(jid);
                }
            }

            try {
                recMan.commit();
            } catch (IOException e) {
                printLog(e, true, true);
            }
        } catch (IOError e) {
            // we track invalid class exceptions
            if (e.getCause() instanceof InvalidClassException) {
                try {
                    recMan.close();
                } catch (IOException e1) {
                    printLog(e, true, true);
                }
                recMan = null;
                cleanDataBase();
                loadDB();
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void login(String user, String passwd) throws PASchedulerException {
        login(user, passwd, null);
    }

    /**
     * {@inheritDoc}
     */
    public void login(String user, String passwd, String keyfile) throws PASchedulerException {

        if (scheduler != null) {
            try {
                scheduler.disconnect();
                sched_proxy.disconnect();
            } catch (Exception e) {

            }
            scheduler = null;
        }

        //System.out.println("Trying to connect with "+user+" " +passwd);
        Credentials creds = null;
        try {
            if (keyfile != null && keyfile.length() > 0) {
                byte[] keyfileContent = FileToBytesConverter.convertFileToByteArray(new File(keyfile));
                CredData cd = new CredData(CredData.parseLogin(user), CredData.parseDomain(user), passwd,
                    keyfileContent);
                creds = Credentials.createCredentials(cd, auth.getPublicKey());
            } else {
                creds = Credentials.createCredentials(new CredData(CredData.parseLogin(user), CredData
                        .parseDomain(user), passwd), auth.getPublicKey());
            }
        } catch (IOException e) {
            throw new PASchedulerException(e);
        } catch (KeyException e) {
            throw new PASchedulerException(e, PASchedulerExceptionType.KeyException);
        } catch (LoginException e) {
            throw new PASchedulerException(new LoginException(
                "Could not retrieve public key, contact the Scheduler admininistrator\n" + e),
                PASchedulerExceptionType.LoginException);
        } catch (Exception e) {
            throw new PASchedulerException(e, PASchedulerExceptionType.OtherException);
        }
        initLogin(creds);
    }

    /**
     * Internal method for login
     *
     * @param creds
     * @throws PASchedulerException
     */
    protected void initLogin(Credentials creds) throws PASchedulerException {
        SchedulerStatus status = null;
        try {
            try {
                this.scheduler = auth.login(creds);
                this.sched_proxy.init(schedulerURL, creds);

                loggedin = true;
                this.oldCred = creds;

                this.sched_proxy.addEventListener(stubOnThis);

                status = scheduler.getStatus();
            } catch (NotConnectedException e) {
                throw new PASchedulerException(e, PASchedulerExceptionType.NotConnectedException);
            } catch (PermissionException e) {
                throw new PASchedulerException(e, PASchedulerExceptionType.PermissionException);
            } catch (AlreadyConnectedException e) {
                // This very nasty error occur when trying to reconnect to the scheduler, in that case, we have no other
                // choice than to restart all proactive on this machine
                MiddlemanDeployer.getInstance().restartAll();
                throw new PASchedulerException(e, PASchedulerExceptionType.AlreadyConnectedException);
            } catch (LoginException e) {
                throw new PASchedulerException(e, PASchedulerExceptionType.LoginException);
            }
            schedulerKilled = (status == SchedulerStatus.KILLED);

            this.model = SchedulerModel.getNewModel(false);
            model.connectConsole(new StdOutConsole());
            model.connectScheduler(this.scheduler);
            if (shutDownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutDownHook);
            }

            shutDownHook = new Thread(new Runnable() {

                public void run() {
                    try {
                        if (scheduler != null) {
                            scheduler.disconnect();
                        }
                    } catch (Exception e) {
                    }
                }
            });

            Runtime.getRuntime().addShutdownHook(shutDownHook);

        } catch (PASchedulerException e) {
            throw e;
        } catch (Exception e) {
            throw new PASchedulerException(e, PASchedulerExceptionType.OtherException);
        }
        // We start a thread that will keep the session alive
        pingerThread = new Thread(new SchedulerPinger());
        pingerThread.setDaemon(true);
        pingerThread.start();
        if (!currentJobs.isEmpty()) {
            syncAll();
        }

    }

    /**
     * {@inheritDoc}
     */
    public void login(String credPath) throws PASchedulerException {

        if (scheduler != null) {
            try {
                scheduler.disconnect();
            } catch (Exception e) {

            }
            scheduler = null;
        }

        //System.out.println("Trying to connect with "+user+" " +passwd);
        Credentials creds = null;
        try {
            creds = Credentials.getCredentials(credPath);
        } catch (KeyException e) {
            throw new PASchedulerException(e, PASchedulerExceptionType.KeyException);
        }
        initLogin(creds);

    }

    /**
     * {@inheritDoc}
     */
    public boolean isLoggedIn() {

        return loggedin && isConnected();
    }

    /**
     * {@inheritDoc}
     */
    public boolean disconnect() {
        if (scheduler != null) {
            try {
                this.scheduler.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.scheduler = null;
            this.model = null;
            this.auth = null;
            loggedin = false;
            joined = false;
            schedulerKilled = false;
        }
        return true;
    }

    /**
     * This method tries forcefully to reconnect to a lost scheduler, it will not stop until it could reconnect
     */
    protected void reconnect() {
        boolean joined = false;
        printLog("Connection to " + schedulerURL + " lost, trying to reconnect.", true, true);

        // if we are connected, we will try to disconnect, we use a timeout as it can happen that this disconnect
        // call blocks forever

        long old_timeout = CentralPAPropertyRepository.PA_FUTURE_SYNCHREQUEST_TIMEOUT.getValue();

        // 5 sec is an acceptable timeout
        CentralPAPropertyRepository.PA_FUTURE_SYNCHREQUEST_TIMEOUT.setValue(5000);

        try {
            sched_proxy.disconnect();
        } catch (Exception e) {
            // this should never occur
            printLog(e);
        }
        try {
            scheduler.disconnect();
        } catch (Exception e) {
            // we ignore any exception
        }
        CentralPAPropertyRepository.PA_FUTURE_SYNCHREQUEST_TIMEOUT.setValue(old_timeout);

        while (!joined) {
            joined = this.join(schedulerURL);
            try {
                Thread.sleep(reconnectionSleep);
                reconnectionSleep = reconnectionSleep * 2;
                if (reconnectionSleep > MAX_RECONNECTION_SLEEP) {
                    reconnectionSleep = MAX_RECONNECTION_SLEEP;
                }
            } catch (InterruptedException e) {
                printLog(e);
                return;
            }
        }

        initLogin(oldCred);
        printLog("Reconnected to " + schedulerURL + " synchronizing jobs...", true, true);
        syncAll();
        printLog("jobs synchronized...", true, true);

    }

    /**
     * {@inheritDoc}
     */
    public boolean isConnected() {
        try {
            return this.scheduler.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ensureConnection() {
        if (!isLoggedIn()) {
            try {
                this.scheduler.renewSession();
            } catch (Exception e) {
                printLog(e);
                reconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getLogFilePath() {
        return MatSciJVMProcessInterfaceImpl.getLogFilePath();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
     */
    @SuppressWarnings("unchecked")
    public void initActivity(Body body) {
        stubOnThis = (AOMatSciEnvironment) PAActiveObject.getStubOnThis();
        aoid = PAActiveObject.getBodyOnThis().getID().getVMID().toString();
        terminated = false;
        serviceThread = Thread.currentThread();
        bodyOnThis = PAActiveObject.getBodyOnThis();

        sched_proxy_root = MatSciSchedulerProxy.getInstance();
        try {
            sched_proxy = PAActiveObject.turnActive(sched_proxy_root, PAActiveObject.getNode());
        } catch (ActiveObjectCreationException e) {
            throw new RuntimeException(e);
        } catch (NodeException e) {
            throw new RuntimeException(e);
        }

    }

    protected abstract File getMidlemanJobsFile();

    protected InputAccessMode getSourceAsMode(DSSource dss) {
        InputAccessMode iam = null;
        switch (dss) {
            case INPUT:
                iam = InputAccessMode.TransferFromInputSpace;
                break;
            case OUTPUT:
                iam = InputAccessMode.TransferFromOutputSpace;
                break;
            case GLOBAL:
                iam = InputAccessMode.TransferFromGlobalSpace;
                break;
            case AUTOMATIC:
                iam = InputAccessMode.TransferFromInputSpace;
                break;
        }
        return iam;
    }

    protected OutputAccessMode getDestinationAsMode(DSSource dss) {
        OutputAccessMode iam = null;
        switch (dss) {
            case OUTPUT:
                iam = OutputAccessMode.TransferToOutputSpace;
                break;
            case GLOBAL:
                iam = OutputAccessMode.TransferToGlobalSpace;
                break;
            case AUTOMATIC:
                iam = OutputAccessMode.TransferToOutputSpace;
                break;
        }
        return iam;
    }

    /**
     * {@inheritDoc}
     */
    public boolean join(String url) throws PASchedulerException {
        try {
            auth = SchedulerConnection.join(url);
        } catch (Exception e) {
            printLog(e, false, true);
            return false;
        }
        schedulerURL = url;
        this.loggedin = false;
        joined = true;
        return true;
    }

    public String getSchedulerURL() {
        return schedulerURL;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isJoined() {
        return joined;
    }

    /**
     * {@inheritDoc}
     */
    public void terminate() {
        this.terminated = true;
        try {
            if (scheduler != null) {
                scheduler.disconnect();
            }
        } catch (Throwable e) {

        }
        try {
            recMan.close();
        } catch (Throwable e) {
        }
    }

    protected void cleanDataBase() {
        if (recMan != null) {
            throw new IllegalStateException("Connection to a DB is established, cannot clean it");
        }
        // delete all db files
        File[] dbJobFiles = new File(TMPDIR).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(getMidlemanJobsFile().getName())) {
                    return true;
                } else if (name.startsWith(MatSciSchedulerProxy.DEFAULT_STATUS_FILENAME)) {
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

    /**
     * method used to clean the list of recorded jobs after a call to endSession :
     * after the end of a session, recorded jobs become handled as other normal jobs (they are not kept after two reboots)
     */
    protected void cleanRecordedJobs() {
        currentSequenceIndex = 1;
        recordedJobs.clear();
        mappingSeqToJobID.clear();
        try {
            recMan.commit();
        } catch (IOException e) {
            printLog(e, true, true);
        }
    }

    /**
     * method used to clean the jobs coming from the n-2 session, we remove all temporary directories and files
     * we avoid to remove the job data of a job inside the "recordedJobs" list
     */
    protected void cleanOldJobs() {
        for (Long key : lastJobs.keySet()) {
            if (!recordedJobs.containsKey(key)) {
                allJobs.remove(key);
            }
        }
        try {
            recMan.commit();
        } catch (IOException e) {
            printLog(e, true, true);
        }
    }

    /**
     * This method tries to terminate the active object with
     */
    public void terminateFast() {
        sched_proxy_root.terminateFast();
        // if the service thread is locked on a user-level Thread.sleep() :
        serviceThread.interrupt();
        // interrupt the pinger thread
        pingerThread.interrupt();
        // terminates this runactivity
        this.terminated = true;
        // destroy the request queue
        BlockingRequestQueueImpl rq = (BlockingRequestQueueImpl) bodyOnThis.getRequestQueue();
        rq.destroy();
        // kill the body
        try {
            bodyOnThis.terminate(false);
        } catch (Exception e) {

        }
        stubOnThis = null;
        while (serviceThread.isAlive()) {
            try {
                Thread.sleep(10);
                // if the service thread is locked on a user-level Thread.sleep() :
                serviceThread.interrupt();
            } catch (InterruptedException e) {

                throw new RuntimeException(e);
            }
        }
        this.pendingRequests.clear();
        this.timeouts.clear();
        this.timeoutOccured.clear();
    }

    @Override
    public Pair<Boolean, String> beginSession() {
        currentSequenceIndex = 1;
        if (debug) {
            printLog("Start recording session");
        }
        if (mappingSeqToJobID.isEmpty()) {
            state = PASessionState.RECORDING;
            sessionStart = new Date();
            sessions.put(0, sessionStart);
            return new Pair<Boolean, String>(true, "Started Recording Session at " + sessionStart);
        } else {
            Long jid;
            int index = 1;
            state = PASessionState.REPLAYING;
            while ((jid = mappingSeqToJobID.get(index)) != null) {

                MatSciJobInfo jinfo = recordedJobs.get(jid);
                jinfo.resetServedTasks();
                BitMatrix matrix = jinfo.getTaskReceptionMatrix();
                //System.out.println("Job "+jid+" : "+matrix);
                tasksReceived.put(jid, matrix);
                currentJobs.add(jid);
                if (!matrix.isTrue()) {
                    // we get the current state of the job if it's not finished yet
                    syncRetrieve(jinfo);
                }
                index++;
            }
            try {
                recMan.commit();
            } catch (IOException e) {
                printLog(e, true, true);
            }
            if (sessionStart == null) {
                sessionStart = sessions.get(0);
            }
            return new Pair<Boolean, String>(true, "Recalled Recorded Session at " + sessionStart);
        }

    }

    @Override
    public Pair<Boolean, String> endSession() {
        if (state == PASessionState.NORMAL) {
            return new Pair<Boolean, String>(false, "Recording Session not started, please use beginSession");
        }
        if (debug) {
            printLog("End recording session");
        }
        state = PASessionState.NORMAL;
        cleanRecordedJobs();

        return new Pair<Boolean, String>(true, "Ended Recording Session started at " + sessionStart);
    }

    protected void printLog(final String message) {
        printLog(message, false, false);
    }

    protected void printLog(final String message, boolean forceOut, boolean forceFile) {
        if (!debug && !forceOut && !forceFile) {
            return;
        }
        if (debug) {
            MatSciJVMProcessInterfaceImpl.printLog(this, message, true, true);
        } else {
            MatSciJVMProcessInterfaceImpl.printLog(this, message, forceOut, forceFile);
        }
    }

    protected void printLog(final Throwable ex) {
        printLog(ex, false, false);
    }

    protected void printLog(final Throwable ex, boolean forceOut, boolean forceFile) {
        if (!debug && !forceOut && !forceFile) {
            return;
        }
        if (debug) {
            MatSciJVMProcessInterfaceImpl.printLog(this, ex, true, true);
        } else {
            MatSciJVMProcessInterfaceImpl.printLog(this, ex, forceOut, forceFile);
        }

    }

    protected boolean isProActiveExeption(Exception e) {
        Throwable re = e;
        do {
            if (re instanceof ProActiveRuntimeException) {
                return true;
            } else if (re instanceof ProActiveException) {
                return true;
            }
            re = re.getCause();
        } while (re != null);
        return false;
    }

    /**
     * *******************************************************************************************************
     * ************************************ JOB STATES AND COMMANDS ******************************************
     */

    private ByteArrayOutputStream redirectStreams() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);
        return baos;
    }

    private void resetStreams() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
    }

    /**
     * {@inheritDoc}
     */
    public String schedulerState() throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.schedulerState_();
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String jobState(String jid) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.jobState_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String jobOutput(String jid) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.output_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String jobResult(String jid) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.result_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String jobRemove(String jid) throws PASchedulerException {
        if (state == PASessionState.REPLAYING) {
            return "Currently in Replaying mode, skipped removal of job " + jid;
        }
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.remove_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String pauseJob(String jid) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.pause_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String resumeJob(String jid) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.resume_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String killJob(String jid) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.kill_(jid);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String taskOutput(String jid, String tname) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.toutput_(jid, tname);
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String taskResult(String jid, String tname) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.tresult_(jid, tname, "0");
        resetStreams();
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String killTask(String jid, String tname) throws PASchedulerException {
        ensureConnection();
        ByteArrayOutputStream baos = redirectStreams();
        model.killt_(jid, tname);
        resetStreams();
        return baos.toString();
    }

    /**********************************************************************************************************/
    /**
     * ******************************************** TASKS  *************************************************
     */

    protected MatSciJobInfo getJobInfo(Long jid) {
        if (allJobs.containsKey(jid)) {
            return allJobs.get(jid);
        } else {
            return recordedJobs.get(jid);
        }
    }

    protected void putJobInfo(Long jid, MatSciJobInfo jinfo) {
        if (state == PASessionState.NORMAL) {
            allJobs.put(jid, jinfo);
        } else {
            recordedJobs.put(jid, jinfo);
        }
    }

    protected void syncAll() throws PASchedulerException {
        for (Long jid : currentJobs) {
            MatSciJobInfo jinfo = allJobs.get(jid);
            syncRetrieve(jinfo);
        }
    }

    /**
     * Updates synchronously the list of results from the given job
     */
    protected void syncRetrieve(MatSciJobInfo jinfo) throws PASchedulerException {
        ensureConnection();
        Long jid = Long.parseLong(jinfo.getJobId());
        if (debug) {
            printLog("Sync updating results of job " + jid);
        }
        //currentJobs.put(jid, jinfo);
        JobResult jResult = null;
        try {
            boolean doThing = true;
            while (doThing) {
                try {
                    jResult = scheduler.getJobResult("" + jid);
                    doThing = false;
                } catch (ProActiveRuntimeException re) {
                    stubOnThis.ensureConnection();
                    return;
                } catch (UnknownJobException uje) {
                    printLog("[WARNING] : job " + jid + " is unknown, maybe it has been removed?", true, true);
                    return;
                }
            }
        } catch (SchedulerException e) {
            printLog(e, true, true);
        }
        if (jResult != null) {
            // full update if the job is finished
            updateJobResult(jid, jResult, jResult.getJobInfo().getStatus());
        }
        // partial update otherwise
        TreeSet<String> tnames = jinfo.getTaskNames();
        for (String tname : tnames) {
            TaskResult tres = null;
            try {
                boolean doThing = true;
                while (doThing) {
                    try {
                        tres = scheduler.getTaskResult("" + jid, tname);
                        doThing = false;
                    } catch (ProActiveRuntimeException re) {
                        stubOnThis.ensureConnection();
                        return;
                    }
                }
            } catch (NotConnectedException e) {
                printLog(e);
                e.printStackTrace();
            } catch (UnknownJobException e) {
                printLog(e);
                e.printStackTrace();
                // if this happens it means that the scheduler has been restarted clean
            } catch (UnknownTaskException e) {
                printLog(e);
                // if this happens it means that the scheduler has been restarted clean
            } catch (PermissionException e) {
                printLog(e);
                e.printStackTrace();
            }
            if (tres != null) {

                updateTaskResult(null, tres, jid, tname);
            }
        }

    }

    /**
     * updates the result of the given job, using the information received from the scheduler
     *
     * @param jid
     * @param jResult
     * @param status
     */
    protected void updateJobResult(Long jid, JobResult jResult, JobStatus status) {
        // Getting the Job result from the Scheduler
        MatSciJobInfo jinfo = getJobInfo(jid);
        if (debug) {
            printLog("Updating results of job " + jid + " : " + status);
        }
        jinfo.setStatus(MatSciJobStatus.getJobStatus(status.toString()));
        jinfo.setJobFinished(true);

        Throwable mainException = null;
        if (schedulerKilled) {
            mainException = new IllegalStateException("The Scheduler has been killed.");
        } else if (status == JobStatus.KILLED) {
            mainException = new IllegalStateException("The Job " + jid + " has been killed.");
        }

        if (schedulerKilled || status == JobStatus.KILLED || status == JobStatus.CANCELED) {

            int depth = jinfo.getDepth();
            // Getting the task results from the job result
            TreeMap<String, TaskResult> task_results = null;

            if (jResult != null) {
                task_results = new TreeMap<String, TaskResult>(new TaskNameComparator());
                task_results.putAll(jResult.getAllResults());

                if (debug) {
                    printLog("Updating job " + jResult.getName() + "(" + jid + ") tasks ");
                }
                BitMatrix received = tasksReceived.get(jid);

                // Iterating over the task results
                for (String tname : task_results.keySet()) {
                    Pair<Integer, Integer> ids = MatSciJobInfo.computeIdsFromTName(tname);
                    if (!received.get(ids.getX(), ids.getY())) {
                        TaskResult res = task_results.get(tname);
                        if (debug) {
                            printLog("Looking for result of task: " + tname);
                        }

                        updateTaskResult(mainException, res, jid, tname);
                    }

                }
            }
            TreeSet<String> missinglist = jinfo.missingResults();
            for (String missing : missinglist) {
                updateTaskResult(null, null, jid, missing);
            }
        }
    }

    /**
     * Updates the result of a task, using the information retrieved from the scheduler
     *
     * @param mainException if an exception occurred globally, such as a job killed
     * @param res           result object received from the scheduler
     * @param jid           job id
     * @param tname         name of the task
     */
    protected void updateTaskResult(Throwable mainException, TaskResult res, Long jid, String tname) {
        MatSciJobInfo jinfo = getJobInfo(jid);

        boolean intermediate = !jinfo.getFinalTaskNames().contains(tname);
        if (debug) {
            if (intermediate) {
                printLog("Looking for result of intermediate task " + tname + " for job " + jid);
            } else {
                printLog("Looking for result of task " + tname + " for job " + jid);
            }
        }
        String logs = null;
        if (res != null) {

            logs = res.getOutput().getAllLogs(false);

            jinfo.addLogs(tname, logs);
        }
        Throwable ex = mainException;
        if (res == null && mainException == null) {
            ex = new RuntimeException("Task id = " + tname + " was not returned by the scheduler");
        } else if (res != null && res.hadException()) {
            if (res.getException() instanceof MatSciTaskException) {
                ex = res.getException();
            } else {
                ex = new PASchedulerException(res.getException());
            }
        }
        if (ex != null) {
            if (debug) {
                if (intermediate) {
                    printLog("Intermediate task " + tname + " for job " + jid + " threw an exception : " +
                        ex.getClass() + " " + ex.getMessage());
                } else {
                    printLog("Task " + tname + " for job " + jid + " threw an exception : " + ex.getClass() +
                        " " + ex.getMessage());
                }
            }
            if (ex instanceof MatSciTaskException) {
                jinfo.setException(tname, ex);
            } else {
                jinfo.setException(tname, new PASchedulerException(ex));
            }
        } else {
            if (!intermediate) {
                // Normal success
                R computedResult = null;
                try {
                    computedResult = (R) res.value();
                    jinfo.setResult(tname, computedResult);
                    //results.add(computedResult);
                    // We print the logs of the job, if any
                    //if (logs.length() > 0) {
                    //   System.out.println(logs);
                    //}
                } catch (Throwable e2) {
                    // should never occur
                    jinfo.setException(tname, new PASchedulerException(e2));
                    //jobDidNotSucceed(jid, e2, true, logs);
                }
            }
        }
        if (jinfo.getConf().getSharedPullPublicUrl() != null) {
            if (jinfo.getConf().isSharedAutomaticTransfer()) {
                // do nothing this will be handled by the callback
                return;
            }
        }
        jinfo.addReceivedTask(tname);
        Pair<Integer, Integer> ids = MatSciJobInfo.computeIdsFromTName(tname);
        tasksReceived.get(jid).set(ids.getX(), ids.getY(), true);
        putJobInfo(jid, jinfo);
        //System.out.println("Job "+jid+" : "+tasksReceived.get(jid));
        try {
            recMan.commit();
        } catch (IOException e) {
            printLog(e, true, true);
        }
    }

    /**
     * gets the result of the given task (the result is already received)
     * Converts exceptions if any occurred
     *
     * @param jid   id of the job
     * @param tname name of the task
     * @return an object containing the result, exception and logs
     */
    protected abstract ResultsAndLogs getResultOfTask(Long jid, String tname);

    /**
     * {@inheritDoc}
     */
    public UnReifiable<Pair<ResultsAndLogs, Integer>> waitAny(String sjid, ArrayList<String> tnames,
            Integer timeout) throws Exception {
        try {
            Long jid = Long.parseLong(sjid);
            Boolean tout = timeoutOccured.get(jid);
            if (tout != null && tout) {
                timeoutOccured.put(jid, false);
                throw new TimeoutException("Timeout occured while executing PAwaitAny for job " + jid);
            }
            return new UnReifiable<Pair<ResultsAndLogs, Integer>>(new Pair<ResultsAndLogs, Integer>(
                getResultOfTask(jid, tnames.get(lastPAWaitAnyIndex)), lastPAWaitAnyIndex));
        } catch (Exception e) {
            printLog(e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public UnReifiable<ArrayList<ResultsAndLogs>> waitAll(String sjid, ArrayList<String> tnames,
            Integer timeout) throws Exception {
        try {
            Long jid = Long.parseLong(sjid);
            Boolean tout = timeoutOccured.get(jid);
            if (tout != null && tout) {
                timeoutOccured.put(jid, false);
                throw new TimeoutException("Timeout occured while executing PAwaitAll for job " + jid);
            }
            ArrayList<ResultsAndLogs> answers = new ArrayList<ResultsAndLogs>();
            for (String tname : tnames) {
                answers.add(getResultOfTask(jid, tname));
            }
            return new UnReifiable<ArrayList<ResultsAndLogs>>(answers);
        } catch (Exception e) {
            printLog(e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public UnReifiable<ArrayList<Boolean>> areAwaited(String sjid, ArrayList<String> tnames) {
        try {
            Long jid = Long.parseLong(sjid);
            if (currentJobs.contains(jid)) {
                return new UnReifiable<ArrayList<Boolean>>(tasksReceived.get(jid).not(
                        MatSciJobInfo.computeLinesFromTNames(tnames)));
            } else if (finishedJobs.contains(jid)) {
                ArrayList<Boolean> answer = new ArrayList<Boolean>(tnames.size());
                for (int i = 0; i < tnames.size(); i++) {
                    answer.add(new Boolean(false));
                }
                return new UnReifiable<ArrayList<Boolean>>(answer);
            } else {
                throw new IllegalArgumentException("Unknown job " + jid);
            }
        } catch (RuntimeException e) {
            printLog(e);
            throw e;
        }
    }

    /**********************************************************************************************************/
    /*********************************************** EVENTS  **************************************************/

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#schedulerStateUpdatedEvent(org.ow2.proactive.scheduler.common.SchedulerEvent)
     */
    public void schedulerStateUpdatedEvent(SchedulerEvent eventType) {
        // we don't react to these events any more, if the scheduler is stopped, we wait that it resumes
        switch (eventType) {
            case KILLED:
            case SHUTDOWN:
            case SHUTTING_DOWN:
                if (debug) {
                    printLog("Received " + eventType.toString() + " event");
                }
                schedulerKilled = true;

                break;
            case STOPPED:
                if (debug) {
                    printLog("Received " + eventType.toString() + " event");
                }
                break;
            case RESUMED:
                if (debug) {
                    printLog("Received " + eventType.toString() + " event");
                }
                break;
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#jobStateUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void jobStateUpdatedEvent(NotificationData<JobInfo> notification) {
        JobInfo info = notification.getData();
        Long jid = Long.parseLong(info.getJobId().value());
        switch (notification.getEventType()) {

            case JOB_PENDING_TO_FINISHED:
                if (info.getStatus() == JobStatus.KILLED) {
                    if (debug) {
                        printLog("Received job " + jid + " killed event...");
                    }

                    // Filtering the right job
                    if (!currentJobs.contains(jid)) {
                        return;
                    }

                    updateJobResult(jid, null, JobStatus.KILLED);
                }

            case JOB_RUNNING_TO_FINISHED:

                if (info.getStatus() == JobStatus.KILLED) {
                    if (debug) {
                        printLog("Received job " + jid + " killed event...");
                    }

                    // Filtering the right job
                    if (!currentJobs.contains(jid)) {
                        return;
                    }

                    JobResult jResult = null;
                    try {
                        boolean doThing = true;
                        while (doThing) {
                            try {
                                jResult = scheduler.getJobResult("" + jid);
                                doThing = false;
                            } catch (ProActiveRuntimeException re) {
                                stubOnThis.ensureConnection();
                                return;
                            }
                        }

                    } catch (SchedulerException e) {
                        e.printStackTrace();
                    }
                    updateJobResult(jid, jResult, JobStatus.KILLED);
                } else {
                    if (debug) {
                        printLog("Received job " + jid + " finished event...");
                    }

                    if (info == null) {
                        return;
                    }

                    // Filtering the right job
                    if (!currentJobs.contains(jid)) {
                        return;
                    }
                    JobResult jResult = null;
                    try {
                        boolean doThing = true;
                        while (doThing) {
                            try {
                                jResult = scheduler.getJobResult("" + jid);
                                doThing = false;
                            } catch (ProActiveRuntimeException re) {
                                stubOnThis.ensureConnection();
                                return;
                            }
                        }
                    } catch (SchedulerException e) {
                        e.printStackTrace();
                    }
                    updateJobResult(jid, jResult, info.getStatus());
                }
                break;
        }

    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#jobSubmittedEvent(org.ow2.proactive.scheduler.common.job.JobState)
     */
    public void jobSubmittedEvent(JobState job) {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#taskStateUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void taskStateUpdatedEvent(NotificationData<TaskInfo> notification) {
        switch (notification.getEventType()) {
            case TASK_RUNNING_TO_FINISHED:
                TaskInfo info = notification.getData();
                String tnm = info.getName();
                Long jid = Long.parseLong(info.getJobId().value());
                if (debug) {
                    printLog("Received task " + tnm + " of Job " + jid + " finished event...");
                }
                // Filtering the right job
                if (!currentJobs.contains(jid)) {
                    return;
                }
                TaskResult tres = null;

                try {
                    tres = scheduler.getTaskResult("" + jid, tnm);
                } catch (ProActiveRuntimeException re) {
                    // we have been disconnected from the scheduler
                    stubOnThis.ensureConnection();
                    return;
                } catch (NotConnectedException e) {
                    // If this happens, it means either a bug, or that the scheduler has been restarted clean, between
                    // the moment that the notification sent and it has been handled
                    e.printStackTrace();
                    return;
                } catch (UnknownJobException e) {
                    // If this happens, it means either a bug, or that the scheduler has been restarted clean, between
                    // the moment that the notification sent and it has been handled
                    e.printStackTrace();
                    return;
                } catch (UnknownTaskException e) {
                    // If this happens, it means either a bug, or that the scheduler has been restarted clean, between
                    // the moment that the notification sent and it has been handled
                    e.printStackTrace();
                    return;
                } catch (PermissionException e) {
                    // If this happens, it means either a bug, or that the scheduler has been restarted clean, between
                    // the moment that the notification sent and it has been handled
                    e.printStackTrace();
                    return;
                }
                updateTaskResult(null, tres, jid, tnm);
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#usersUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void usersUpdatedEvent(NotificationData<UserIdentification> notification) {
        // TODO Auto-generated method stub
    }

    @Override
    public void pullDataFinished(String jobId, String taskName, String localFolderPath) {
        Long jid = Long.parseLong(jobId);
        MatSciJobInfo jinfo = getJobInfo(jid);
        jinfo.addReceivedTask(taskName);
        Pair<Integer, Integer> ids = MatSciJobInfo.computeIdsFromTName(taskName);
        tasksReceived.get(jid).set(ids.getX(), ids.getY(), true);
        putJobInfo(jid, jinfo);
        //System.out.println("Job "+jid+" : "+tasksReceived.get(jid));
        try {
            recMan.commit();
        } catch (IOException e) {
            printLog(e, true, true);
        }
    }

    @Override
    public void pullDataFailed(String jobId, String taskName, String remoteFolder_URL, Throwable t) {
        Long jid = Long.parseLong(jobId);
        MatSciJobInfo jinfo = getJobInfo(jid);
        jinfo.setException(taskName, t);
        jinfo.addReceivedTask(taskName);
        Pair<Integer, Integer> ids = MatSciJobInfo.computeIdsFromTName(taskName);
        tasksReceived.get(jid).set(ids.getX(), ids.getY(), true);
        putJobInfo(jid, jinfo);
        //System.out.println("Job "+jid+" : "+tasksReceived.get(jid));
        try {
            recMan.commit();
        } catch (IOException e) {
            printLog(e, true, true);
        }
    }

    /**********************************************************************************************************/
    /*********************************************** ACTIVITY  ************************************************/

    /**
     * {@inheritDoc}
     */
    public void runActivity(final Body body) {
        Service service = new Service(body);

        while (!terminated) {
            try {
                long in = System.currentTimeMillis();
                int tout = computeTimeoutServing();

                if (tout == Integer.MAX_VALUE) {
                    if (debug) {
                        printLog("waiting for request with no timeout");

                    }
                    service.waitForRequest();
                } else {
                    if (debug) {
                        printLog("waiting for request with timeout = " + tout);

                    }
                    BlockingRequestQueue queue = body.getRequestQueue();
                    queue.waitForRequest(tout);
                }
                int time = (int) (System.currentTimeMillis() - in);
                substractTimeout(time);

                if (service.hasRequestToServe()) {

                    Request randomRequest = service.getOldest();
                    if (debug) {
                        printLog("Request received : " + randomRequest.getMethodName());
                    }
                    if (randomRequest.getMethodName().equals("waitAll")) {
                        String sjid = (String) randomRequest.getParameter(0);
                        ArrayList<String> tnames = (ArrayList<String>) randomRequest.getParameter(1);
                        Long jid = Long.parseLong(sjid);
                        tout = (Integer) randomRequest.getParameter(2);
                        if (tout > 0) {
                            timeouts.put(jid, tout);
                            beginTimes.put(jid, System.currentTimeMillis());
                        } else {
                            timeouts.put(jid, null);
                        }

                        pendingRequests.put(jid, randomRequest);
                        if (debug) {
                            printLog("Removed " + randomRequest.getMethodName() + "(" + tnames +
                                ") for job=" + jid + " request from the queue");
                        }
                        service.blockingRemoveOldest("waitAll");
                    } else if (randomRequest.getMethodName().equals("waitAny")) {
                        String sjid = (String) randomRequest.getParameter(0);
                        Long jid = Long.parseLong(sjid);
                        tout = (Integer) randomRequest.getParameter(2);
                        if (tout > 0) {
                            timeouts.put(jid, tout);
                            beginTimes.put(jid, System.currentTimeMillis());
                        } else {
                            timeouts.put(jid, null);
                        }
                        pendingRequests.put(jid, randomRequest);
                        if (debug) {
                            printLog("Removed " + randomRequest.getMethodName() + " for job=" + jid +
                                " request from the queue");
                        }
                        service.blockingRemoveOldest("waitAny");
                    }
                    if (service.hasRequestToServe()) {
                        String mname = service.getOldest().getMethodName();
                        // TODO : do we need to make sure that only one method "ensureConnection" is in the queue at the same time ?
                        if (debug) {
                            printLog("Serving " + mname);
                        }
                        service.serveOldest();
                    }
                }
                // we maybe serve the pending waitXXX method if there is one and if the necessary results are collected
                maybeServePending(service);
            } catch (Throwable ex) {
                printLog(ex);
            }
        }

        // we clear the service to avoid dirty pending requests
        service.flushAll();

        // we finally terminate the master
        body.terminate();
    }

    /**
     * computes the minimum timeout required by active timed-out requests
     *
     * @return
     */
    private int computeTimeoutServing() {
        int min_timeout = Integer.MAX_VALUE;
        for (Long jid : timeouts.keySet()) {
            Integer tout = timeouts.get(jid);
            if ((tout != null) && (tout < min_timeout)) {
                min_timeout = tout;
            }
        }
        return min_timeout;
    }

    /**
     * substracts the amount of time actually waited to each stored requests timeouts
     *
     * @param time time waited
     */
    private void substractTimeout(int time) {
        for (Long jid : timeouts.keySet()) {
            Integer tout = timeouts.get(jid);
            if (tout != null) {
                timeouts.put(jid, tout - time);
                beginTimes.put(jid, beginTimes.get(jid) + time);
            }
        }
    }

    /**
     * If there is a pending waitXXX method, we serve it if the necessary results are collected
     *
     * @param service
     */
    protected void maybeServePending(Service service) {
        boolean noneServed = true;
        for (Map.Entry<Long, Request> entry : ((HashMap<Long, Request>) pendingRequests.clone()).entrySet()) {
            Long jid = entry.getKey();
            Request req = entry.getValue();
            if (req.getMethodName().equals("waitAll")) {
                ArrayList<String> tnames = (ArrayList<String>) req.getParameter(1);
                Integer tout = timeouts.get(jid);
                boolean to = (tout != null) && (System.currentTimeMillis() - beginTimes.get(jid) >= tout);
                List<Integer> lines = MatSciJobInfo.computeLinesFromTNames(tnames);
                //printLog("lines :"+lines);
                boolean ok = tasksReceived.get(jid).areLinesTrue(lines);
                if (debug) {
                    printLog("TaskReceived:" + tasksReceived.get(jid));
                    printLog("ok:" + ok);
                }
                //printLog("bitset:"+tasksReceived.get(jid));
                //printLog("ok :"+ok);
                if (to || ok) {
                    beginTimes.remove(jid);
                    timeouts.remove(jid);
                    timeoutOccured.put(jid, to);
                    pendingRequests.remove(jid);
                    if (debug) {
                        printLog("serving " + req.getMethodName() + "(" + tnames + ") for job " + jid);
                    }
                    noneServed = false;
                    service.serve(req);
                }
            } else if (req.getMethodName().equals("waitAny")) {
                ArrayList<String> tnames = (ArrayList<String>) req.getParameter(1);
                Integer tout = timeouts.get(jid);
                boolean to = (tout != null) && (System.currentTimeMillis() - beginTimes.get(jid) >= tout);
                int any = tasksReceived.get(jid).isAnyLineTrue(MatSciJobInfo.computeLinesFromTNames(tnames));
                if (to || any >= 0) {
                    beginTimes.remove(jid);
                    timeouts.remove(jid);
                    timeoutOccured.put(jid, to);
                    pendingRequests.remove(jid);
                    if (debug) {
                        printLog("serving " + req.getMethodName() + "(" + any + ") for job " + jid);
                    }
                    lastPAWaitAnyIndex = any;
                    noneServed = false;
                    service.serve(req);
                }
            }
        }
        if (noneServed) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * @author The ProActive Team
     *         Internal class for filtering requests in the queue
     */
    private class FindNotWaitFilter implements RequestFilter {

    private static final long serialVersionUID = 11;

        /**
         * Creates the filter
         */
        public FindNotWaitFilter() {
        }

        /**
         * {@inheritDoc}
         */
        public boolean acceptRequest(final Request request) {
            // We find all the requests which can't be served yet
            String name = request.getMethodName();
            if (name.equals("waitAny")) {
                return false;
            } else if (name.equals("waitAll")) {
                return false;
            }
            return true;
        }
    }

    protected class SchedulerPinger implements Runnable {

        private static final int KEEP_ALIVE_TIME = 20000;

        /**
         * we use a private Schuduler connection because two different thread cannot use the same scheduler connection
         */
        private Scheduler scheduler_itf_for_pinger;

        @Override
        public void run() {
            try {
                scheduler_itf_for_pinger = auth.login(oldCred);
            } catch (LoginException e) {
                // should never occur
                printLog(e, true, true);
                return;
            } catch (AlreadyConnectedException e) {
                // should never occur
                printLog(e, true, true);
                return;
            }
            while (true) {
                try {
                    synchronized (scheduler) {
                        scheduler_itf_for_pinger.renewSession();
                    }
                } catch (RuntimeException e) {
                    // scheduler connection lost
                    printLog(e);
                    // if we lost connection to the scheduler we call the ensureConnection method
                    // this method will reconnect only if necessary (i.e. if it is not has been reconnected by another call)
                    synchronized (stubOnThis) {
                        stubOnThis.ensureConnection();
                    }
                    return;
                } catch (NotConnectedException e) {
                    printLog(e);
                    synchronized (stubOnThis) {
                        stubOnThis.ensureConnection();
                    }
                    return;
                }
                try {
                    Thread.sleep(KEEP_ALIVE_TIME);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

}
