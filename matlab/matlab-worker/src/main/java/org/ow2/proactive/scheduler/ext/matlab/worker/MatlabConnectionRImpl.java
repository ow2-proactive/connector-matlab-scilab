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
package org.ow2.proactive.scheduler.ext.matlab.worker;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.objectweb.proactive.core.ProActiveException;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabGlobalConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabInitException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.UnreachableLicenseProxyException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.UnsufficientLicencesException;

import com.activeeon.proactive.license_saver.client.LicenseSaverClient;


/**
 * MatlabConnectionRImpl this class
 *
 * @author The ProActive Team
 */
public class MatlabConnectionRImpl implements MatlabConnection {

    /**
     * The name of the property that defines tmp directory
     */
    public static final String MATLAB_TASK_TMPDIR = "matlab.task.tmpdir";

    /**
     * The name of the property that defines MATLAB preferences directory
     */
    public static final String MATLAB_PREFDIR = "matlab.prefdir";

    /**
     * System-dependent line separator
     */
    public static final String nl = System.lineSeparator();

    /**
     * Pattern used to remove Matlab startup message from logs
     */
    private static final String startPattern = "---- MATLAB START ----";

    private static final String lcFailedPattern = "License checkout failed";

    private static final String outofmemoryPattern = "java.lang.OutOfMemoryError";

    /**
     * Startup Options of the Matlab process
     */
    protected String[] startUpOptions;

    /**
     * Location of the Matlab process
     */
    protected String matlabLocation;

    /**
     * Full Matlab code which will be executed by the Matlab process
     */
    protected StringBuilder fullcommand = new StringBuilder();

    /**
     * File used to store MATLAB code to be executed
     */
    protected File mainFuncFile;

    /**
     * Directory where the MATLAB process should start (Localspace)
     */
    protected File workingDirectory;

    /**
     * File used to capture MATLAB process output (in addition to Threads)
     */
    private File taskOutputFile;

    /** Targeted stream for the task output redirection (in particular to display the task output from the scheduler portal) */
    private final PrintStream taskOut;

    /**
     * Timeout for the matlab process startup x 10 ms
     */
    protected int TIMEOUT_START;

    /**
     * Debug mode
     */
    protected boolean debug;

    /**
     * MATLAB Process
     */
    protected Process process;

    /**
     * Lock used to prevent process destroy while starting up
     */
    protected Boolean running = false;

    protected boolean isMatlabUsingAStarter = false;

    /**
     * Licensing Proxy Server Client
     */
    private LicenseSaverClient lclient;

    /**
     * Matlab configuration of the current job
     */
    PASolveMatlabGlobalConfig paconfig;

    /**
     * Matlab configuration of the current task
     */
    PASolveMatlabTaskConfig tconfig;

    /**
     * Logging thread used *
     */
    IOTools.LoggingThread outputThreadDefinition;

    private Thread outputThread;

    public MatlabConnectionRImpl(final PrintStream taskOut) {
        this.taskOut = taskOut;
    }

    public void acquire(String matlabExecutablePath, File workingDir, PASolveMatlabGlobalConfig paconfig,
            PASolveMatlabTaskConfig tconfig, final String jobId, final String taskId) throws MatlabInitException {
        this.matlabLocation = matlabExecutablePath;
        this.workingDirectory = workingDir;
        this.debug = paconfig.isDebug();
        this.paconfig = paconfig;
        this.tconfig = tconfig;
        this.startUpOptions = paconfig.getStartupOptions();
        this.TIMEOUT_START = paconfig.getWorkerTimeoutStart();

        this.taskOutputFile = new File(this.workingDirectory, "MatlabStart_" + jobId + "_" + taskId + ".log");
        if (!this.taskOutputFile.exists()) {
            try {
                this.taskOutputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.mainFuncFile = new File(this.workingDirectory, "PAMain.m");
        if (!this.mainFuncFile.exists()) {
            try {
                this.mainFuncFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (paconfig.getLicenseSaverURL() != null) {
            try {
                this.lclient = new LicenseSaverClient(paconfig.getLicenseSaverURL());
            } catch (ProActiveException e) {
                throw new MatlabInitException(new UnreachableLicenseProxyException("License Proxy Server at url " +
                                                                                   paconfig.getLicenseSaverURL() +
                                                                                   " could not be contacted.", e));
            }
        }

    }

    public void init() {
        fullcommand.append("function PAmain()" + nl);
        fullcommand.append("disp('" + startPattern + "');" + nl);
        fullcommand.append("try" + nl);
    }

    public void release() {
        synchronized (running) {
            if (process != null) {
                try {
                    process.destroy();
                    process = null;
                } catch (Exception e) {

                }
            }
            if (outputThread != null) {
                outputThread.interrupt();
                try {
                    outputThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            running = false;
        }
    }

    public void execCheckToolboxes(String command) {

        fullcommand.append(command);
    }

    @Override
    public boolean isMatlabRunViaAStarter() {
        return isMatlabUsingAStarter;
    }

    public void evalString(String command) throws MatlabTaskException {
        fullcommand.append(command + nl);
    }

    public Object get(String variableName) throws MatlabTaskException {
        throw new UnsupportedOperationException();
    }

    public void put(String variableName, Object value) throws MatlabTaskException {
        throw new UnsupportedOperationException();
    }

    public void beforeLaunch() {
        fullcommand.append("catch ME" + nl + "disp('Error occurred in .');" + nl + "disp(getReport(ME));" + nl + "end" +
                           nl);
        // we remove all file handles possibily kept by matlab
        fullcommand.append("fclose('all');restoredefaultpath();");
        // we create a marker file to signify the end, but keep a handle on it. By doing that we can synchronize the java
        // process with the real matlab termination (it will liberate the handle when it will really exit)
        fullcommand.append("    fend = fullfile('" + workingDirectory + "','matlab.end');" + nl +

                           "fid = fopen(fend,'w');" + nl);
        fullcommand.append("exit();");
    }

    public void launch() throws Exception {

        PrintStream out = null;

        try {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(mainFuncFile)));
        } catch (FileNotFoundException e) {
            sendAck(false);
            throw e;
        }

        out.println(fullcommand);
        out.flush();
        out.close();

        synchronized (running) {
            process = createMatlabProcess("cd('" + this.workingDirectory + "');PAMain();");
            running = true;
        }

        // Logging thread creation & start
        outputThreadDefinition = new IOTools.LoggingThread(new FileInputStream(taskOutputFile),
                                                           "[MATLAB]",
                                                           taskOut,
                                                           debug ? null : startPattern,
                                                           null,
                                                           new String[] { lcFailedPattern, outofmemoryPattern });
        outputThread = new Thread(outputThreadDefinition, "OUT MATLAB");
        outputThread.setDaemon(true);
        outputThread.start();

        File ackFile = new File(workingDirectory, "matlab.ack");
        File nackFile = new File(workingDirectory, "matlab.nack");
        File endFile = new File(workingDirectory, "matlab.end");

        try {

            int cpt = 0;
            while (!ackFile.exists() && !nackFile.exists() && (cpt < TIMEOUT_START) &&
                   !outputThreadDefinition.patternFound(lcFailedPattern) &&
                   !outputThreadDefinition.patternFound(outofmemoryPattern) && running) {
                try {
                    // WARNING : on windows platform, matlab is initialized by a startup program which exits immediately, we cannot take decisions based on exit status.
                    int exitValue = process.exitValue();
                    if (exitValue != 0) {
                        sendAck(false);
                        // outputThreadDefinition.goon = false; unnecessary as matlab process exited
                        throw new MatlabInitException("Matlab process exited with code : " + exitValue);
                    } else {
                        // matlab uses a startup program, unfortunately, we won't be able to receive logs from the standard process
                        isMatlabUsingAStarter = true;

                    }
                    // maybe the matlab launcher exited
                } catch (IllegalThreadStateException e) {
                    // ok process still exists
                }
                Thread.sleep(10);
                cpt++;
            }

            if (nackFile.exists()) {
                sendAck(false);
                throw new UnsufficientLicencesException();
            }
            if (outputThreadDefinition.patternFound(lcFailedPattern)) {
                process.destroy();
                process = null;
                sendAck(false);
                throw new UnsufficientLicencesException();
            }
            if (outputThreadDefinition.patternFound(outofmemoryPattern)) {
                process.destroy();
                process = null;
                sendAck(false);
                throw new RuntimeException("Out of memory error in Matlab process");
            }
            if (cpt >= TIMEOUT_START) {
                process.destroy();
                process = null;
                sendAck(false);
                String output = FileUtils.readFileToString(this.taskOutputFile, "UTF-8");
                throw new MatlabInitException("Timeout occured while starting Matlab, with following output (" +
                                              this.taskOutputFile + "):" + nl + output);
            }
            if (!running) {
                sendAck(false);
                throw new MatlabInitException("Task killed while initialization");
            }
            sendAck(true);

            int exitValue = process.waitFor();
            if (exitValue != 0) {
                String output = FileUtils.readFileToString(this.taskOutputFile, "UTF-8");
                throw new MatlabInitException("Matlab process exited with code : " + exitValue +
                                              " after task started. With following output (" + this.taskOutputFile +
                                              "):" + nl + output);
            }
            // on windows the matlab initialization process can terminate while Matlab still exists in the background
            // we use then the end file to synchronize
            while (!endFile.exists()) {
                Thread.sleep(10);
            }

            // now we wait that matlab exists completely and remove its grasp on the file
            boolean isDeleted = false;
            while (!isDeleted) {
                try {
                    isDeleted = endFile.delete();
                } catch (Exception e) {

                }
                if (!isDeleted) {
                    Thread.sleep(10);
                }
            }
        } finally {
            if (ackFile.exists()) {
                ackFile.delete();
            }
            if (nackFile.exists()) {
                nackFile.delete();
            }
            if (endFile.exists()) {
                endFile.delete();
            }
        }

    }

    protected void sendAck(boolean ack) throws Exception {
        if (lclient != null) {
            try {
                lclient.notifyLicenseStatus(tconfig.getRid(), ack);
            } catch (Exception e) {
                throw new UnreachableLicenseProxyException("Error while sending ack to License Proxy Server at url " +
                                                           paconfig.getLicenseSaverURL(), e);
            }
        }
    }

    protected Process createMatlabProcess(String runArg) throws Exception {
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
        // invalid on windows, it affects the starter only
        b.directory(this.workingDirectory);
        b.command(command);

        // Fix for SCHEDULING-1309: If MATLAB client uses RunAsMe option the MATLAB
        // worker jvm can crash if the client user has never started any MATLAB
        // session on the worker host

        // Since the user profile can be missing on Windows with RunAsMe, by setting
        // the MATLAB_PREFDIR variable to a writable dir (can be non-writable on Windows with RunAsMe)
        // the MATLAB doesn't crash no more

        Map<String, String> env = b.environment();

        // Transmit the prefdir as env variable
        String matlabPrefdir = System.getProperty(MATLAB_PREFDIR);
        if (matlabPrefdir != null) {
            env.put("MATLAB_PREFDIR", matlabPrefdir);
        }
        // Transmit the tmpdir as env variable
        String matlabTmpvar = System.getProperty(MATLAB_TASK_TMPDIR);
        if (matlabTmpvar != null) {
            env.put("TEMP", matlabTmpvar);
            env.put("TMP", matlabTmpvar);
        }

        return b.start();
    }
}
