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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectweb.proactive.*;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.objectweb.proactive.core.ProActiveTimeoutException;
import org.objectweb.proactive.core.body.request.BlockingRequestQueue;
import org.objectweb.proactive.core.body.request.BlockingRequestQueueImpl;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.body.request.RequestFilter;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
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
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.common.PASessionState;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.*;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerExceptionType;
import org.ow2.proactive.scheduler.ext.matsci.common.data.DSSource;
import org.ow2.proactive.scheduler.ext.matsci.common.exception.MatSciTaskException;
import org.ow2.proactive.scheduler.smartproxy.SmartProxyImpl;
import org.ow2.proactive.scheduler.smartproxy.common.SchedulerEventListenerExtended;
import org.ow2.proactive.utils.FileToBytesConverter;

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
        SchedulerEventListenerExtended, InitActive, RunActive {

    /**
     * Connection to the scheduler
     */
    protected Scheduler scheduler;

    /**
     * ProActive stub of the proxy to the scheduler with disconnected mode support
     */
    protected SmartProxyImpl sched_proxy;

    /**
     * real object of the proxy
     */
    protected SmartProxyImpl sched_proxy_root;

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
     * Loggers of Remote Object
     */
    static final Logger LOGGER_RO = ProActiveLogger.getLogger(Loggers.REMOTEOBJECT);
    static final Level RO_LEVEL = LOGGER_RO.getLevel();

    /**
     * Table name of the jobs backup
     */
    public static final String MIDDLEMAN_JOBS_RECORD_NAME = "MiddlemanJobs";

    /**
     * Table name of the jobs from last session
     */
    public static final String MIDDLEMAN_LASTJOBS_RECORD_NAME = "MiddlemanLastJobs";

    /**
     * Table name of the jobs being recorded for replay
     */
    public static final String MIDDLEMAN_RECORDEDJOBS_RECORD_NAME = "MiddlemanRecordedJobs";

    /**
     * Table name of the matching between the sequence and the job
     */
    public static final String MIDDLEMAN_SEQTOJOBID_RECORD_NAME = "MiddlemanSeq2JobId";

    /**
     * Table name of the last recording session
     */
    public static final String SESSION_RECORD_NAME = "MiddlemanSession";

    /**
     * Table name of the last credentials
     */
    public static final String CONNECTIONDATA_RECORD_NAME = "MiddlemanConnectionData";

    protected static final int MIN_RECONNECTION_SLEEP = 4;

    protected static final int MAX_RECONNECTION_SLEEP = 128;

    protected static String HOSTNAME = null;

    static {
        try {
            HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();

        } catch (Exception e) {
        }
    }

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

    /**
     * Mapping of sequence index with jobs (in PAsessions)
     */
    protected PrimaryHashMap<Integer, Long> mappingSeqToJobID;

    /**
     * Date of last session
     */
    protected PrimaryHashMap<Integer, Date> sessions;

    /**
     * last info of connection
     */
    protected PrimaryHashMap<Integer, ConnectionData> lastConnectionData;

    /**
     * state of the recording session
     */
    protected PASessionState state = PASessionState.NORMAL;

    /**
     * last login used
     */
    protected String lastLogin;

    /**
     * Last credentials used
     */
    protected Credentials lastCred;

    /**
     * Last Scheduler url used
     */
    protected String lastSchedulerURL;

    /**
     * Last key file used
     */
    protected String lastKeyFile;

    /**
     * start time of current session
     */
    protected Date sessionStart = null;

    /**
     * current sequence index in the PAsession
     */
    protected int currentSequenceIndex = 1;

    /**
     * Matrix of task received for each job
     */
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
            lastConnectionData = recMan.hashMap(CONNECTIONDATA_RECORD_NAME);

            // by this vanilla call we trigger serial version UIDs which could possibly appear
            ConnectionData cData = lastConnectionData.get(0);

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

            commit();
        } catch (IOError e) {
            // we track invalid class exceptions
            if (e.getCause() instanceof InvalidClassException) {
                try {
                    recMan.close();
                } catch (IOException e1) {
                    printLog(e, LogMode.FILEANDOUTALWAYS);
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
            LOGGER_RO.setLevel(Level.FATAL);
            try {
                scheduler.disconnect();
                sched_proxy.disconnect();
            } catch (Exception e) {

            }
            LOGGER_RO.setLevel(RO_LEVEL);
            scheduler = null;
        }

        //System.out.println("Trying to connect with "+user+" " +passwd);
        Credentials creds = null;
        if (user == null) {
            if (lastConnectionData.containsKey(0) && lastConnectionData.get(0) != null) {
                creds = lastConnectionData.get(0).getCredentials();
                lastKeyFile = lastConnectionData.get(0).getKeyFile();
                lastLogin = lastConnectionData.get(0).getLogin();
                lastSchedulerURL = lastConnectionData.get(0).getUrl();
                lastCred = creds;
            } else {
                throw new IllegalStateException();
            }

        }
        if (creds == null) {
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
                lastKeyFile = keyfile;
                lastLogin = user;
                lastCred = creds;
            } catch (IOException e) {
                printLog(e, LogMode.FILEANDOUTALWAYS);
                throw new PASchedulerException(e);
            } catch (KeyException e) {
                printLog(e, LogMode.FILEANDOUTALWAYS);
                throw new PASchedulerException(e, PASchedulerExceptionType.KeyException);
            } catch (LoginException e) {
                printLog(e, LogMode.FILEANDOUTALWAYS);
                throw new PASchedulerException(new LoginException(
                    "Could not retrieve public key, contact the Scheduler admininistrator\n" + e),
                    PASchedulerExceptionType.LoginException);
            } catch (Exception e) {
                printLog(e, LogMode.FILEANDOUTALWAYS);
                throw new PASchedulerException(e, PASchedulerExceptionType.OtherException);
            }
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
                this.sched_proxy.init(lastSchedulerURL, creds);

                loggedin = true;
                this.lastCred = creds;

                this.sched_proxy.addEventListener(stubOnThis);

                status = scheduler.getStatus();
            } catch (NotConnectedException e) {
                printLog(e, LogMode.FILEALWAYSNEVEROUT);
                throw new PASchedulerException(e, PASchedulerExceptionType.NotConnectedException);
            } catch (PermissionException e) {
                printLog(e, LogMode.FILEALWAYSNEVEROUT);
                throw new PASchedulerException(e, PASchedulerExceptionType.PermissionException);
            } catch (AlreadyConnectedException e) {
                // This very nasty error occur when trying to reconnect to the scheduler, in that case, we have no other
                // choice than to restart all proactive on this machine
                printLog(e, LogMode.FILEALWAYSNEVEROUT);
                MiddlemanDeployer.getInstance().restartAll();
                throw new PASchedulerException(e, PASchedulerExceptionType.AlreadyConnectedException);
            } catch (LoginException e) {
                printLog(e, LogMode.FILEALWAYSNEVEROUT);
                throw new PASchedulerException(e, PASchedulerExceptionType.LoginException);
            }
            schedulerKilled = (status == SchedulerStatus.KILLED);

            this.model = new SchedulerModel();
            model.connectScheduler(this.scheduler);
            if (shutDownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutDownHook);
            }

            shutDownHook = new Thread(new Runnable() {

                public void run() {
                    try {
                        if (scheduler != null) {
                            scheduler.disconnect();
                            sched_proxy.disconnect();
                        }
                    } catch (Exception e) {
                    }
                }
            });

            Runtime.getRuntime().addShutdownHook(shutDownHook);

        } catch (PASchedulerException e) {
            printLog(e, LogMode.FILEALWAYSNEVEROUT);
            throw e;
        } catch (ProActiveRuntimeException e) {
            throw e;
        } catch (Exception e) {
            printLog(e, LogMode.FILEALWAYSNEVEROUT);
            throw new PASchedulerException(e, PASchedulerExceptionType.OtherException);
        }
        // We start a thread that will keep the session alive
        pingerThread = new Thread(new SchedulerPinger());
        pingerThread.setDaemon(true);
        pingerThread.start();
        if (!currentJobs.isEmpty()) {
            syncAll();
        }
        lastConnectionData.put(0, new ConnectionData(lastSchedulerURL, lastLogin, lastCred, lastKeyFile));
        commit();
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

    public boolean hasCredentialsStored() {
        return lastConnectionData.containsKey(0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean disconnect() {
        if (scheduler != null) {
            try {
                this.scheduler.disconnect();
            } catch (Exception e) {
                printLog(e);
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
     * Many different scenarios can lead to the execution of this method, this is why it is complex to understand
     */
    protected void reconnect() {
        boolean joined = false;
        printLog("Connection to " + lastSchedulerURL + " lost, trying to reconnect.",
                LogMode.FILEANDOUTALWAYS);

        // we set a timeout for all ProActive synchronous calls. The reason behind this is that we can be in a situation
        // where the PAMR router failed and when this happens all ProActive calls block forever. By putting a timeout we
        // ensure that we will not be waiting forever and can react

        long old_timeout = CentralPAPropertyRepository.PA_FUTURE_SYNCHREQUEST_TIMEOUT.getValue();

        // 10 sec is an acceptable timeout
        CentralPAPropertyRepository.PA_FUTURE_SYNCHREQUEST_TIMEOUT.setValue(10000);
        try {
            LOGGER_RO.setLevel(Level.FATAL);
            try {

                sched_proxy.disconnect();
            } catch (ProActiveTimeoutException e) {
                // if a proactive timeout occurs, either it is a normal timeout (in that case we ignore it ) or it's a timeout
                // due to a PAMR router failure. In the latter case, we must do a Thread.sleep in order to allow Thread.interrupt
                // coming from the terminateFast method to occur
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // If we are interrupted by the terminateFast method, we exit the reconnection loop with an Exception
                    // clients of the middleman JVM will this way be notified that the ActiveObjects have been restarted
                    // and that they need to call reconnect again
                    printLog(e1);
                    throw new RuntimeException(e1);
                }
            } catch (Exception e) {
                // we ignore any random exception
                printLog(e, LogMode.FILEONLY);
            }
            try {
                scheduler.disconnect();
            } catch (ProActiveTimeoutException e) {
                // see above comment
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    printLog(e1, LogMode.FILEONLY);
                    throw new RuntimeException(e1);
                }

            } catch (Exception e) {
                // we ignore any random exception
            }

            while (!joined) {
                try {
                    joined = this.join(lastSchedulerURL);
                } catch (ProActiveTimeoutException e) {
                    // see above comment
                }
                try {
                    Thread.sleep(reconnectionSleep * 1000);
                    reconnectionSleep = reconnectionSleep * 2;
                    if (reconnectionSleep > MAX_RECONNECTION_SLEEP) {
                        reconnectionSleep = MAX_RECONNECTION_SLEEP;
                    }
                } catch (InterruptedException e) {
                    printLog(e, LogMode.FILEONLY);
                    throw new RuntimeException(e);
                }
            }
            try {
                initLogin(lastCred);

                printLog("Reconnected to " + lastSchedulerURL + " synchronizing jobs...",
                        LogMode.FILEANDOUTALWAYS);
                syncAll();
                printLog("jobs synchronized...", LogMode.FILEANDOUTALWAYS);

            } catch (ProActiveTimeoutException e) {
                // see above comment
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    printLog(e1, LogMode.FILEONLY);
                    throw new RuntimeException(e1);
                }

            }

        } finally {
            LOGGER_RO.setLevel(RO_LEVEL);
            CentralPAPropertyRepository.PA_FUTURE_SYNCHREQUEST_TIMEOUT.setValue(old_timeout);
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean isConnected() {
        boolean answer = false;
        LOGGER_RO.setLevel(Level.FATAL);
        try {
            answer = this.scheduler.isConnected();
        } catch (Throwable e) {
        }
        LOGGER_RO.setLevel(RO_LEVEL);
        return answer;
    }

    /**
     * {@inheritDoc}
     */
    public boolean ensureConnection() {
        if (!isLoggedIn()) {
            LOGGER_RO.setLevel(Level.FATAL);
            try {
                this.scheduler.renewSession();
            } catch (Exception e) {
                printLog(e);
                reconnect();
            }
            LOGGER_RO.setLevel(RO_LEVEL);
        }
        return true;
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

        sched_proxy_root = SmartProxyImpl.getInstance();
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
            case USER:
                iam = InputAccessMode.TransferFromUserSpace;
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
            case USER:
                iam = OutputAccessMode.TransferToUserSpace;
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
        if ((url == null) && (lastConnectionData.containsKey(0))) {
            url = lastConnectionData.get(0).getUrl();
        } else if (url == null) {
            url = "rmi://" + HOSTNAME + ":1099";
        }
        try {
            auth = SchedulerConnection.join(url);
        } catch (Exception e) {
            printLog(e, LogMode.FILEALWAYS);
            return false;
        }
        lastSchedulerURL = url;
        this.loggedin = false;
        joined = true;
        return true;
    }

    public String getSchedulerURL() {
        return lastSchedulerURL;
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

    protected void commit() {
        try {
            recMan.commit();
        } catch (IOException e) {
            printLog(e, LogMode.FILEANDOUTALWAYS);
            throw new PASchedulerException(e);
        }
    }

    /**
     * method used to clean the list of recorded jobs after a call to endSession :
     * after the end of a session, recorded jobs become handled as other normal jobs (they are not kept after two reboots)
     */
    protected void cleanRecordedJobs() {
        currentSequenceIndex = 1;
        printAllStackTraces();
        // for an obsure reason the clear method of jdbm maps does not work, removal is done here differently :
        ArrayList<Long> keys = new ArrayList<Long>(recordedJobs.keySet());
        for (Long key : keys) {
            recordedJobs.remove(key);
        }
        ArrayList<Integer> keys2 = new ArrayList<Integer>(mappingSeqToJobID.keySet());
        for (Integer key : keys2) {
            mappingSeqToJobID.remove(key);
        }
        commit();
    }

    private void printAllStackTraces() {
        Map liveThreads = Thread.getAllStackTraces();
        for (Iterator i = liveThreads.keySet().iterator(); i.hasNext(); ) {
            Thread key = (Thread)i.next();
            printLog("Thread " + key.getName());
            StackTraceElement[] trace = (StackTraceElement[])liveThreads.get(key);
            for (int j = 0; j < trace.length; j++) {
                printLog("\tat " + trace[j]);
            }
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
                currentJobs.remove(key);
                finishedJobs.remove(key);
                tasksReceived.remove(key);
            }
        }
        commit();
    }

    /**
     * This method tries to terminate the active object forcefully
     */
    public void terminateFast() {

        // fix for MSC-207 : BodyTerminatedException occurs when reconnecting to a PAMR router
        // We remove this AO from the list of listeners of the proxy
        sched_proxy_root.removeEventListener(stubOnThis);
        // we kill the proxy
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

        printLog("Start recording session");

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

                if (!matrix.isTrue()) {
                    // we get the current state of the job if it's not finished yet
                    currentJobs.add(jid);
                    syncRetrieve(jinfo);
                } else {
                    finishedJobs.add(jid);
                }

                index++;
            }
            commit();
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

        printLog("End recording session");

        state = PASessionState.NORMAL;
        cleanRecordedJobs();

        return new Pair<Boolean, String>(true, "Ended Recording Session started at " + sessionStart);
    }

    protected void printLog(final String message) {
        printLog(message, LogMode.STD);
    }

    protected void printLog(final String message, LogMode mode) {
        MatSciJVMProcessInterfaceImpl.printLog(this, message, mode, debug);
    }

    protected void printLog(final Throwable ex) {
        printLog(ex, LogMode.STD);
    }

    protected void printLog(final Throwable ex, LogMode mode) {
        MatSciJVMProcessInterfaceImpl.printLog(this, ex, mode, debug);
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
        model.listjobs_();
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
        Long id = Long.parseLong(jid);
        if (recordedJobs.containsKey(id)) {
            return "[WARNING] job " +
                jid +
                " is being recorded and cannot be removed, set PAoption RemoveJobAfterRetrieve to false in order to disable automatic removal.";
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
        try {
            if (allJobs.containsKey(jid)) {
                return allJobs.get(jid);
            } else {
                return recordedJobs.get(jid);
            }
        } finally {
            commit();
        }
    }

    protected void putJobInfo(Long jid, MatSciJobInfo jinfo) {
        if (state == PASessionState.NORMAL) {
            allJobs.put(jid, jinfo);
        } else {
            recordedJobs.put(jid, jinfo);
        }
        commit();
    }

    protected void syncAll() throws PASchedulerException {
        Iterator<Long> it = currentJobs.iterator();

        while (it.hasNext()) {
            Long jid = it.next();
            if (allJobs.containsKey(jid)) {
                MatSciJobInfo jinfo = allJobs.get(jid);
                syncRetrieve(jinfo);
            } else if (recordedJobs.containsKey(jid)) {
                MatSciJobInfo jinfo = recordedJobs.get(jid);
                printLog("" + jid, LogMode.FILEANDOUTALWAYS);
                printLog("" + jinfo, LogMode.FILEANDOUTALWAYS);
                syncRetrieve(jinfo);
            } else {
                printLog("Warning : unknown current job " + jid + " removing it", LogMode.FILEANDOUTALWAYS);
                it.remove();
            }
        }

        commit();
    }

    /**
     * Updates synchronously the list of results from the given job
     */
    protected void syncRetrieve(MatSciJobInfo jinfo) throws PASchedulerException {
        ensureConnection();
        Long jid = Long.parseLong(jinfo.getJobId());

        printLog("Sync updating results of job " + jid);

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
                    if (recordedJobs.containsKey(jid)) {
                        printLog(
                                "[SEVERE] job " + jid +
                                    " which is recorded by the MatSciConnector is unknown by the scheduler, maybe it has been removed?",
                                LogMode.FILEANDOUTALWAYS);
                    } else {
                        printLog("[WARNING] : job " + jid + " is unknown, maybe it has been removed?",
                                LogMode.FILEANDOUTALWAYS);
                    }
                    // we update the job result only if the job is among our currentJobs list (i.e. waited by the user), otherwise it can be that the user simply removed a finished job
                    if (currentJobs.contains(jid)) {
                        updateJobResult(jid, null, MatSciJobStatus.UNKNOWN);
                    }
                    return;
                }
            }
        } catch (SchedulerException e) {
            printLog(e, LogMode.FILEANDOUTALWAYS);
        }
        if (jResult != null) {
            // full update if the job is finished
            updateJobResult(jid, jResult, MatSciJobStatus.getJobStatus(jResult.getJobInfo().getStatus()
                    .toString()));
            return;
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
     * this method triggers the update of tasks only if the job was failed
     *
     * @param jid
     * @param jResult
     * @param status
     */
    protected void updateJobResult(Long jid, JobResult jResult, MatSciJobStatus status) {
        // Getting the Job result from the Scheduler
        MatSciJobInfo jinfo = getJobInfo(jid);

        printLog("Updating results of job " + jid + " : " + status);

        jinfo.setStatus(status);
        jinfo.setJobFinished(true);

        Throwable mainException = null;
        if (schedulerKilled) {
            mainException = new IllegalStateException("The Scheduler has been killed.");
        } else if (status == MatSciJobStatus.KILLED) {
            mainException = new IllegalStateException("The Job " + jid + " has been killed.");
        } else if (status == MatSciJobStatus.UNKNOWN) {
            mainException = new IllegalStateException("The Job " + jid + " is unknown by the scheduler.");
        }

        if (schedulerKilled || status == MatSciJobStatus.KILLED || status == MatSciJobStatus.CANCELED ||
            status == MatSciJobStatus.UNKNOWN) {

            int depth = jinfo.getDepth();
            // Getting the task results from the job result
            TreeMap<String, TaskResult> task_results = null;

            if (jResult != null) {
                // if we received a valid jresult we update each individual task result
                task_results = new TreeMap<String, TaskResult>(new TaskNameComparator());
                task_results.putAll(jResult.getAllResults());

                printLog("Updating job " + jResult.getName() + "(" + jid + ") tasks ");

                BitMatrix received = tasksReceived.get(jid);

                // Iterating over the task results
                for (String tname : task_results.keySet()) {
                    Pair<Integer, Integer> ids = MatSciJobInfo.computeIdsFromTName(tname);
                    if (!received.get(ids.getX(), ids.getY())) {
                        TaskResult res = task_results.get(tname);

                        printLog("Looking for result of task: " + tname);

                        updateTaskResult(mainException, res, jid, tname);
                    }

                }
            }
            // This updates every task missing from the previous treatment (for example when the job was cancelled or killed)
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

                    updateJobResult(jid, null, MatSciJobStatus.KILLED);
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
                    updateJobResult(jid, jResult, MatSciJobStatus.KILLED);
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
                    updateJobResult(jid, jResult, MatSciJobStatus.getJobStatus(info.getStatus().toString()));
                }
                break;
        }

    }

      /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#jobSubmittedEvent(org.ow2.proactive.scheduler.common.job.JobState)
     */
    public void jobSubmittedEvent(JobState job) {
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#jobUpdatedFullDataEvent(org.ow2.proactive.scheduler.common.job.JobState)
     */
    public void jobUpdatedFullDataEvent(JobState job) {
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#taskStateUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void taskStateUpdatedEvent(NotificationData<TaskInfo> notification) {
        switch (notification.getEventType()) {
            case TASK_RUNNING_TO_FINISHED:
            case TASK_SKIPPED:
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
                break;
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
            } catch (InterruptedException ex) {
                printLog(ex, LogMode.FILEONLY);
                throw new RuntimeException(ex);
            } catch (Throwable ex) {
                printLog(ex, LogMode.FILEONLY);
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
         * we use a private Scheduler connection because two different thread cannot use the same scheduler connection
         */
        private Scheduler scheduler_itf_for_pinger;

        @Override
        public void run() {
            try {
                scheduler_itf_for_pinger = auth.login(lastCred);
            } catch (LoginException e) {
                // should never occur
                printLog(e, LogMode.FILEANDOUTALWAYS);
                return;
            } catch (AlreadyConnectedException e) {
                // should never occur
                printLog(e, LogMode.FILEANDOUTALWAYS);
                return;
            }
            while (true) {
                try {
                    synchronized (scheduler) {
                        LOGGER_RO.setLevel(Level.FATAL);
                        scheduler_itf_for_pinger.renewSession();
                        LOGGER_RO.setLevel(RO_LEVEL);
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
