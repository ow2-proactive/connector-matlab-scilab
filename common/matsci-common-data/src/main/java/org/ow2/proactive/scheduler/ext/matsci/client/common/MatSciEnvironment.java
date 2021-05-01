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
package org.ow2.proactive.scheduler.ext.matsci.client.common;

import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.ow2.proactive.scheduler.ext.matsci.client.common.data.MatSciClientJobInfo;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.Pair;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.ResultsAndLogs;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.UnReifiable;
import org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciGlobalConfig;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciTaskConfig;


/**
 * MatSciEnvironment interface of the middleman environment which serves as an interface between matlab/scilab and the scheduler
 *
 * @author The ProActive Team
 */
public interface MatSciEnvironment extends Remote {

    /**
     * Tries to connect to the scheduler at the given url
     * @param url url of the scheduler
     * @return success
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while connecting
     * @throws java.rmi.RemoteException
     */
    public boolean join(String url) throws PASchedulerException, RemoteException;

    /**
     * Tries to log into the scheduler, using the provided user and password
     *
     * @param user   username
     * @param passwd password
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException
     *          if the login fails
     */
    public void login(String user, String passwd) throws PASchedulerException, RemoteException;

    /**
     * Tries to log into the scheduler, using the provided user and password, and an additional SSH key
     *
     * @param user   username
     * @param passwd password
     * @param keyFile path to SSH key File
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException
     *          if the login fails
     */
    public void login(String user, String passwd, String keyFile) throws PASchedulerException, RemoteException;

    /**
     * Tries to log into the scheduler, using the provided credential file
     *
     * @param credPath   path to the credentials file
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException
     *          if the login fails
     */
    public void login(String credPath) throws PASchedulerException, RemoteException;

    /**
     * Is the environment connected and logged into a scheduler ?
     * @return
     * @throws java.rmi.RemoteException
     */
    public boolean isLoggedIn() throws RemoteException;

    /**
     * Disconnects the environment from the scheduler
     * @return
     * @throws java.rmi.RemoteException
     */
    public boolean disconnect() throws RemoteException;

    /**
     * Returns true if the MiddleMan JVM has some user credentials stored in its database
     * @return tf
     * @throws RemoteException
     */
    public boolean hasCredentialsStored() throws RemoteException;

    /**
     * Is the environment actually connected to scheduler ? (the scheduler can choose to disconnect a logged-in client)
     * @return
     * @throws java.rmi.RemoteException
     */
    public boolean isConnected() throws RemoteException;

    /**
     * Has the environment successfully joined a scheduler before a login attempt ?
     * @return
     * @throws java.rmi.RemoteException
     */
    public boolean isJoined() throws RemoteException;

    /**
     * Returns the url of the scheduler currently connected to
     * @return scheduler url
     */
    public String getSchedulerURL() throws RemoteException;

    /**
     * Returns the path to the log file
     * @return path to log file
     */
    public String getLogFilePath() throws RemoteException;

    /**
     * Ensures that the current environment is connected to the scheduler, reconnect if not.
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an exception occurs during reconnection
     * @throws java.rmi.RemoteException
     */
    public boolean ensureConnection() throws PASchedulerException, RemoteException;

    /**
     * Terminates the current environment
     * @return
     * @throws java.rmi.RemoteException
     */
    public void terminate() throws RemoteException;

    /**
     * Starts Recording session or reload previous one
     * @throws RemoteException
     */
    public Pair<Boolean, String> beginSession() throws RemoteException;

    /**
     * Ends recording session
     * @throws RemoteException
     */
    public Pair<Boolean, String> endSession() throws RemoteException;

    /**
     * Asks the environment to submit a job to the scheduler
     * @param config the PAsolve job configuration
     * @param taskConfigs each individual task configuration
     * @return an object containing info about the job submitted
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException any exception occuring when submitting to the scheduler
     * @throws java.net.MalformedURLException if script urls are wrong
     * @throws java.rmi.RemoteException
     */
    public MatSciClientJobInfo solve(PASolveMatSciGlobalConfig config, PASolveMatSciTaskConfig[][] taskConfigs)
            throws PASchedulerException, MalformedURLException, RemoteException;

    /**
     * waits for the first computed task among a given list, with an optonal timeout
     * @param jid id of the job
     * @param tnames names of tasks to wait for
     * @param timeout timeout or -1 if none
     * @return a pair containing the index of the task received and the result
     * @throws java.rmi.RemoteException
     * @throws java.util.concurrent.TimeoutException if a timeout occurred
     */
    public UnReifiable<Pair<ResultsAndLogs, Integer>> waitAny(String jid, ArrayList<String> tnames, Integer timeout)
            throws RemoteException, Exception;

    /**
     * waits for all computed task among a given list, with an optonal timeout
     * @param jid id of the job
     * @param tnames names of tasks to wait for
     * @param timeout timeout or -1 if none
     * @return the list of results
     * @throws java.rmi.RemoteException
     * @throws java.util.concurrent.TimeoutException if a timeout occurred
     */
    public UnReifiable<ArrayList<ResultsAndLogs>> waitAll(String jid, ArrayList<String> tnames, Integer timeout)
            throws RemoteException, Exception;

    /**
     * tells if results among the given list are available or not
     * @param jid id of the job
     * @param tnames names of tasks
     * @return a list of answers
     * @throws java.rmi.RemoteException
     */
    public UnReifiable<ArrayList<Boolean>> areAwaited(String jid, ArrayList<String> tnames) throws RemoteException;

    /**
     * Current state of the scheduler
     * @return a string containing the current state
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String schedulerState() throws PASchedulerException, RemoteException;

    /**
     * Current state of the given job
     * @return a string containing the current state
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String jobState(String jid) throws PASchedulerException, RemoteException;

    /**
     * output of the given job
     * @return a string containing the job output
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String jobOutput(String jid) throws PASchedulerException, RemoteException;

    /**
     * textual result of the given job
     * @return a string containing the job result
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String jobResult(String jid) throws PASchedulerException, RemoteException;

    /**
     * removes the given job from the scheduler finished queue
     * @return a string containing the result of the action
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String jobRemove(String jid) throws PASchedulerException, RemoteException;

    /**
     * pauses the given job
     * @return a string containing the result of the action
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String pauseJob(String jid) throws PASchedulerException, RemoteException;

    /**
     * resumes the given job
     * @return a string containing the result of the action
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String resumeJob(String jid) throws PASchedulerException, RemoteException;

    /**
     * kills the given job
     * @return a string containing the result of the action
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String killJob(String jid) throws PASchedulerException, RemoteException;

    /**
     * output of the given task
     * @return a string containing the task output
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String taskOutput(String jid, String tname) throws PASchedulerException, RemoteException;

    /**
     * textual result of the given task
     * @return a string containing the task result
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String taskResult(String jid, String tname) throws PASchedulerException, RemoteException;

    /**
     * kills the given task
     * @return a string containing the result of the action
     * @throws org.ow2.proactive.scheduler.ext.matsci.client.common.exception.PASchedulerException if an error occurred while contacting the scheduler
     * @throws java.rmi.RemoteException
     */
    public String killTask(String jid, String tname) throws PASchedulerException, RemoteException;

}
