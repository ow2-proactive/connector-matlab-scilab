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
package org.ow2.proactive.scheduler.ext.matlab.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.objectweb.proactive.core.ProActiveException;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabGlobalConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabInitException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.UnreachableLicenseProxyException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.UnsufficientLicencesException;

import com.activeeon.proactive.license_saver.client.LicenseSaverClient;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProcessCreator;
import matlabcontrol.RemoteMatlabProxy;
import matlabcontrol.RemoteMatlabProxyFactory;


/**
 * This class uses the matlabcontrol API to establish a connection with MATLAB for
 * MATLAB tasks executions. There can be only one instance at a time.
 * Be careful this class is not thread safe.
 */
public class MatlabConnectionMCImpl implements MatlabConnection {

    /** The proxy to the remote MATLAB */
    private RemoteMatlabProxy proxy;

    /** The thread executed on shutdown that releases this connection */
    private Thread shutdownHook;

    private CustomMatlabProcessCreator processCreator;

    protected int TIMEOUT_START = 6000;

    protected File workingDirectory;

    private PASolveMatlabGlobalConfig paconfig;
    private PASolveMatlabTaskConfig tconfig;

    private LicenseSaverClient lclient;

    protected String[] startUpOptions;

    /** Targeted stream for the task output redirection (in particular to display the task output from the scheduler portal) */
    private final PrintStream taskOut;

    /** Pattern used to remove Matlab startup message from logs */
    private static final String startPattern = "---- MATLAB START ----";

    private static final String lcFailedPattern = "License checkout failed";

    private static final String outofmemoryPattern = "java.lang.OutOfMemoryError";
    private static Thread outputThread;

    public MatlabConnectionMCImpl(final PrintStream taskOut) {
        this.taskOut = taskOut;
    }

    /**
     * Each time this method is called creates a new MATLAB process using
     * the matlabcontrol API.
     *
     * @param matlabExecutablePath The full path to the MATLAB executable
     * @param workingDir the directory where to start MATLAB
     * @param paconfig configuration of a Matlab PAsolve Job
     * @param tconfig configuration of a Matlab Task
     * @param jobId current job id
     * @param taskId current task id
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabInitException if MATLAB could not be initialized
     */
    public void acquire(String matlabExecutablePath, File workingDir, PASolveMatlabGlobalConfig paconfig,
                        PASolveMatlabTaskConfig tconfig, final String jobId, final String taskId) throws MatlabInitException {
        RemoteMatlabProxyFactory proxyFactory;
        this.paconfig = paconfig;
        this.tconfig = tconfig;
        this.workingDirectory = workingDir;
        this.TIMEOUT_START = paconfig.getWorkerTimeoutStart();

        if (paconfig.getLicenseSaverURL() != null) {
            try {
                this.lclient = new LicenseSaverClient(paconfig.getLicenseSaverURL());
            } catch (ProActiveException e) {
                throw new MatlabInitException(new UnreachableLicenseProxyException(
                        "License Proxy Server at url " + paconfig.getLicenseSaverURL() +
                                " could not be contacted.", e));
            }
        }

        this.startUpOptions = paconfig.getStartupOptions();

        // If a user is specified create the proxy factory with a specific
        // MATLAB process as user creator
        try {

            processCreator = new CustomMatlabProcessCreator(matlabExecutablePath, workingDir,
                    this.startUpOptions, paconfig.isDebug(), this.taskOut, jobId, taskId);

            proxyFactory = new RemoteMatlabProxyFactory(processCreator);
        } catch (MatlabConnectionException e) {
            // Possible cause: dsregistry problem or receiver is not bind
            e.printStackTrace();

            // Nothing can be done maybe a retry ... check this later
            MatlabInitException me = new MatlabInitException(
                    "Unable to create the MATLAB proxy factory. Possible causes: dsregistry cannot be created or the receiver cannot be bind");
            me.initCause(e);

            try {
                sendAck(false);
            } catch (Exception e1) {
                // We print the exception though ignore it (general failure case)
                e1.printStackTrace();
            }

            throw me;
        }

        // This will start a MATLAB process, wait until the JVM inside MATLAB
        try {
            proxy = proxyFactory.getProxy();
        } catch (MatlabConnectionException e) {
            // Possible cause: timeout
            e.printStackTrace();

            // Nothing can be done maybe a retry ... check this later
            MatlabInitException me = new MatlabInitException(
                    "Unable to create the MATLAB proxy factory. Possible causes: dsregistry cannot be created or the receiver cannot be bind");
            me.initCause(e);

            // clean factory
            proxyFactory.clean();

            // destroy process that can be spawned
            // even if we didn't managed to get the proxy
            processCreator.killProcess();

            try {
                sendAck(false);
            } catch (Exception e1) {
                // We print the exception though ignore it (general failure case)
                e1.printStackTrace();
            }

            throw me;
        }

        // Return a new MATLAB connection
        // Add shutdown hook to release the connection on jvm exit
        shutdownHook = new Thread(new Runnable() {
            public final void run() {
                release();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

    }

    public void init() {
    }

    /**
     * Releases the connection, after a call to this method
     * the connection becomes unusable !
     */
    public void release() {
        if (this.proxy == null) {
            return;
        }
        // Stop MATLAB use true for immediate
        try {
            this.proxy.exit(true);
        } catch (Exception e) {
            // Here maybe we should kill the process it self ... need more tests
        }

        // Clean threads used by the proxy
        this.proxy.clean();

        // Kill the MATLAB process
        this.processCreator.killProcess();

        if( outputThread != null) {
            outputThread.interrupt();
            try {
                outputThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.proxy = null;
        // Remove the shutdown hook
        try {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        } catch (Exception e) {
        }
        System.gc();
    }

    @Override
    public boolean isMatlabRunViaAStarter() {
        return false;
    }

    /**
     * Evaluate the given string in the workspace.
     *
     * @param command the command to evaluate
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException If unable to evaluate the command
     */
    public void evalString(final String command) throws MatlabTaskException {
        try {
            String out = this.proxy.eval(command);
            taskOut.println(out);
        } catch (MatlabInvocationException e) {
            throw new MatlabTaskException("Unable to eval command " + command, e);
        }
    }

    /**
     * Extract a variable from the workspace.
     *
     * @param variableName name of the variable
     * @return value of the variable
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException if unable to get the variable
     */
    public Object get(String variableName) throws MatlabTaskException {
        try {
            return this.proxy.getVariable(variableName);
        } catch (MatlabInvocationException e) {
            throw new MatlabTaskException("Unable to get get the variable " + variableName, e);
        }
    }

    /**
     * Push a variable in to the workspace.
     *
     * @param variableName name of the variable
     * @param value the value of the variable
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException if unable to set a variable
     */
    public void put(final String variableName, final Object value) throws MatlabTaskException {
        try {
            this.proxy.setVariable(variableName, value);
        } catch (MatlabInvocationException e) {
            throw new MatlabTaskException("Unable to set the variable " + variableName, e);
        }
    }

    public void beforeLaunch() {

    }

    public void launch() {

    }

    public void execCheckToolboxes(String command) throws Exception {
        evalString(command);

        // wait for ack or nack files to make sure all toolbox licences are available
        File ackFile = new File(workingDirectory, "matlab.ack");
        File nackFile = new File(workingDirectory, "matlab.nack");
        int cpt = 0;
        try {

            while (!ackFile.exists() && !nackFile.exists() && (cpt < TIMEOUT_START) &&
                    !CustomMatlabProcessCreator.outputThreadDefinition.patternFound(lcFailedPattern) &&
                    !CustomMatlabProcessCreator.outputThreadDefinition.patternFound(outofmemoryPattern)) {
                Thread.sleep(10);
                cpt++;
            }
        } catch (InterruptedException e) {
            release();
            throw new MatlabInitException(e);
        }
        if (ackFile.exists()) {
            ackFile.delete();
            sendAck(true);
        }

        if (nackFile.exists()) {
            nackFile.delete();
            sendAck(false);
            release();
            throw new UnsufficientLicencesException();
        }
        if (CustomMatlabProcessCreator.outputThreadDefinition.patternFound(lcFailedPattern)) {
            sendAck(false);
            release();
            throw new UnsufficientLicencesException();
        }
        if (CustomMatlabProcessCreator.outputThreadDefinition.patternFound(outofmemoryPattern)) {
            sendAck(false);
            release();
            throw new RuntimeException("Out of memory error in Matlab process");
        }
        if (cpt >= TIMEOUT_START) {
            sendAck(false);
            release();
            throw new MatlabInitException("Timeout occured while waiting for ack file");
        }

    }

    /*********** PRIVATE INTERNAL CLASS ***********/

    /**
     * Send Ack to the LicenseSaverClient
     * @param ack
     * @throws Exception
     */
    protected void sendAck(boolean ack) throws Exception {
        if (lclient != null) {
            try {
                lclient.notifyLicenseStatus(tconfig.getRid(), ack);
            } catch (Exception e) {
                throw new UnreachableLicenseProxyException(
                        "Error while sending ack to License Proxy Server at url " + paconfig.getLicenseSaverURL(),
                        e);
            }
        }
    }

    /**
     * This class is used to create a MATLAB process under a specific user
     */
    private static class CustomMatlabProcessCreator implements MatlabProcessCreator {

        protected String[] startUpOptions;
        protected final String matlabLocation;
        protected final File workingDirectory;

        protected File taskOutputFile;
        private final PrintStream taskOut;

        protected boolean debug;

        private Process process;

        static IOTools.LoggingThread outputThreadDefinition;

        public CustomMatlabProcessCreator(final String matlabLocation, final File workingDirectory,
                                          String[] startUpOptions, boolean debug, final PrintStream taskOut, final String jobId, final String taskId) {
            this.matlabLocation = matlabLocation;
            this.workingDirectory = workingDirectory;
            this.taskOutputFile = new File(this.workingDirectory, "MatlabStart_" + jobId + "_" + taskId + ".log");
            if (!this.taskOutputFile.exists()) {
                try {
                    this.taskOutputFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.startUpOptions = startUpOptions;
            this.debug = debug;
            this.taskOut = taskOut;
        }

        public Process createMatlabProcess(String runArg) throws Exception {
            // Attempt to run MATLAB
            final ArrayList<String> commandList = new ArrayList<String>();
            commandList.add(this.matlabLocation);
            commandList.addAll(Arrays.asList(this.startUpOptions));
            commandList.add("-logfile");
            commandList.add(this.taskOutputFile.toString());
            commandList.add("-r");
            commandList.add(runArg);

            String[] command = (String[]) commandList.toArray(new String[commandList.size()]);

            ProcessBuilder b = new ProcessBuilder();
            b.directory(this.workingDirectory);
            b.command(command);

            process = b.start();

            // Logging thread creation & start
            outputThreadDefinition = new IOTools.LoggingThread(new FileInputStream(taskOutputFile), "[MATLAB]", taskOut, debug ? null : startPattern, null, new String[] { lcFailedPattern, outofmemoryPattern });
            outputThread = new Thread(outputThreadDefinition, "OUT MATLAB");
            outputThread.setDaemon(true);
            outputThread.start();

            return process;
        }

        public File getLogFile() {
            return this.taskOutputFile;
        }

        public boolean isDebug() {
            return debug;
        }

        public void killProcess() {
            try {
                process.destroy();
            } catch (Exception e) {
            }
        }
    }
}