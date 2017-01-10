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

import org.apache.log4j.Level;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;
import org.ow2.proactive.scheduler.ext.common.util.FileUtils;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabGlobalConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException;
import org.ow2.proactive.scheduler.ext.matlab.worker.util.MatlabEngineConfig;
import org.ow2.proactive.scheduler.ext.matlab.worker.util.MatlabFinder;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveEnvFile;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveFile;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveZippedFile;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciEngineConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * This class represents the executable of a MATLAB task. It's configuration is
 * composed of :
 * <ul>
 * <li>An instance of {@link org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabGlobalConfig}: The global PAsolve job configuration.
 * <li>An instance of {@link org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig}: The PAsolve task configuration.
 * <li>An instance of {@link org.ow2.proactive.scheduler.ext.matlab.worker.util.MatlabEngineConfig}: The local matlab engine configuration.
 * </ul>
 * The incoming calls order are: the {@link MatlabExecutable#MatlabExecutable()},
 * the {@link MatlabExecutable#init(java.util.Map)} method is called, then the {@link MatlabExecutable#execute(TaskResult...)} method.
 * The {@link MatlabExecutable#init(java.util.Map)} initializes configuration and file transfer logic, then
 * once all checks are done the execute method create a connection with MATLAB.
 *
 * @author The ProActive Team
 */
public class MatlabExecutable extends JavaExecutable {

    /** The name of the property that defines tmp directory */
    public static final String MATLAB_TASK_TMPDIR = "matlab.task.tmpdir";

    /** The name of the property that defines MATLAB preferences directory */
    public static final String MATLAB_PREFDIR = "matlab.prefdir";

    protected static String HOSTNAME;

    static {
        try {
            HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
        }
    }

    /** The ISO8601 for debug format of the date that precedes the log message */
    protected static final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");

    /** For debug purpose see {@link MatlabExecutable#createLogFileOnDebug()} */
    private FileOutputStream debugfos;
    private PrintStream outDebug;

    /** The global configuration */
    private PASolveMatlabGlobalConfig paconfig;

    /** The task configuration */
    private PASolveMatlabTaskConfig taskconfig;

    /** The MATLAB configuration */
    private MatlabEngineConfig matlabEngineConfig;

    /** The temp dir where all temp files are stores it doesn't include files received from dataspaces */
    protected String tmpDir;

    /** The root of the local space and a temporary dir */
    private File localSpaceRootDir, tempSubDir;

    /** The connection to MATLAB from matlabcontrol API */
    private MatlabConnection matlabConnection;

    /** The MATLAB script to execute */
    private String script;

    public MatlabExecutable() {
        this.paconfig = new PASolveMatlabGlobalConfig();
        this.taskconfig = new PASolveMatlabTaskConfig();
    }

    @Override
    public void init(final Map<String, Serializable> args) throws Exception {

        // The tmp dir is matlab.task.tmpdir if defined otherwise it is java.io.tmpdir
        this.tmpDir = System.getProperty(MATLAB_TASK_TMPDIR) == null ? System.getProperty("java.io.tmpdir")
                : System.getProperty(MATLAB_TASK_TMPDIR);

        // Fix for SCHEDULING-1308: With RunAsMe on windows the forked jvm can have a non-writable java.io.tmpdir
        if (!new File(this.tmpDir).canWrite()) {
            throw new RuntimeException("Unable to execute task, TMPDIR: " + this.tmpDir + " is not writable.");
        }

        // Read global configuration
        Object obj = args.get("global_config");
        if (obj != null) {
            this.paconfig = (PASolveMatlabGlobalConfig) obj;
        }

        // Create a log file if debug is enabled
        this.createLogFileOnDebug();

        // Read task configuration
        obj = args.get("task_config");
        if (obj != null) {
            this.taskconfig = (PASolveMatlabTaskConfig) obj;
        }

        // Read the main script to execute
        this.script = (String) args.get("script");
        if (this.script == null || "".equals(this.script)) {
            throw new IllegalArgumentException("Unable to execute task, no script specified");
        }

        // Initialize MATLAB location
        this.initMatlabConfig();

        // Initialize LOCAL SPACE
        this.initLocalSpace();

        // Initialize transfers
        this.initTransferSource();
        this.initTransferEnv();
        this.initTransferInputFiles();
        this.initTransferVariables();
    }

    @Override
    public Serializable execute(final TaskResult... results) throws Throwable {
        if (results != null) {
            for (TaskResult res : results) {
                if (res.hadException()) {
                    throw res.getException();
                }
            }
        }

        if (paconfig.isDebug()) {
            ProActiveLogger.getLogger(MatlabExecutable.class).setLevel(Level.DEBUG);
        }

        final String matlabCmd = this.matlabEngineConfig.getFullCommand();
        this.printLog("Acquiring MATLAB connection using " + matlabCmd);

        // Acquire a connection to MATLAB
        if (paconfig.isUseMatlabControl()) {
            this.matlabConnection = new MatlabConnectionMCImpl(this.tmpDir, this.outDebug);
        } else {
            this.matlabConnection = new MatlabConnectionRImpl(this.tmpDir, this.outDebug);
        }

        final String taskId = (String) this.getVariables().get("PA_TASK_ID");
        matlabConnection.acquire(matlabCmd, this.localSpaceRootDir, this.paconfig, this.taskconfig, taskId);

        Serializable result = null;

        try {
            // Execute the MATLAB script and receive the result
            result = this.executeScript();
        } finally {
            this.printLog("Closing MATLAB...");
            this.matlabConnection.release();
            if (paconfig.isDebug() || matlabConnection.isMatlabRunViaAStarter()) {
                printLog(this.matlabConnection.getOutput(paconfig.isDebug()), true);
            }
            printLog("End of Task");
            this.closeLogFileOnDebug();
        }

        return result;
    }

    public void kill() { // TODO how to clean without kill?
        if (this.matlabConnection != null) {
            // Release the connection
            this.matlabConnection.release();
            this.matlabConnection = null;
        }
    }

    /**
     * Executes both input and main scripts on the engine
     *
     * @throws Throwable
     */
    protected final Serializable executeScript() throws Throwable {

        // Add sources, load workspace and input variables
        matlabConnection.init();

        this.addSources();
        this.execCheckToolboxes();
        this.execKeepAlive();
        this.loadWorkspace();
        this.loadInputVariables();
        this.loadTopologyNodeNames();

        if (paconfig.isDebug()) {
            matlabConnection.evalString("who");
        }

        printLog("Running MATLAB command: " + this.script);

        matlabConnection.evalString(this.script);

        printLog("MATLAB command completed successfully, receiving output... ");

        storeOutputVariable();

        // outputFiles
        transferOutputFiles();

        matlabConnection.launch();

        testOutput();

        return new Boolean(true);
    }

    /*********** PRIVATE METHODS ***********/

    /**
     * Initialize the Matlab Engine configuration
     * @return the found configuration
     * @throws Exception if no valid configuration could be found.
     */
    protected MatSciEngineConfig initMatlabConfig() throws Exception {
        MatlabEngineConfig conf = (MatlabEngineConfig) MatlabEngineConfig.getCurrentConfiguration();
        if (conf == null) {
            conf = (MatlabEngineConfig) MatlabFinder.getInstance().findMatSci(paconfig.getVersionPref(),
                    paconfig.getVersionRej(), paconfig.getVersionMin(), paconfig.getVersionMax(),
                    paconfig.getVersionArch(), paconfig.isDebug());
            if (conf == null) {
                throw new IllegalStateException("No valid Matlab configuration found, aborting...");
            }
        }
        matlabEngineConfig = conf;
        return matlabEngineConfig;
    }

    /**
     * Initialize the local DataSpace
     * @throws Exception
     */
    private void initLocalSpace() throws Exception {
        final File localSpaceFile = new File(getLocalSpace());
        final URI localSpaceURI = localSpaceFile.toURI();
        final String localSpaceURIstr = localSpaceURI .toString();

        if (!localSpaceFile.exists()) {
            throw new IllegalStateException("Unable to execute task, the local space " + localSpaceURIstr +
                " doesn't exists");
        }
        if (!localSpaceFile.canRead()) {
            throw new IllegalStateException("Unable to execute task, the local space " + localSpaceURIstr +
                " is not readable");
        }
        if (!localSpaceFile.canWrite()) {
            throw new IllegalStateException("Unable to execute task, the local space " + localSpaceURIstr +
                " is not writable");
        }

        // Create a temp dir in the root dir of the local space
        this.localSpaceRootDir = localSpaceFile.getCanonicalFile();
        this.tempSubDir = new File(this.localSpaceRootDir, paconfig.getJobSubDirOSPath()).getCanonicalFile();

        if (!tempSubDir.exists()) {
            tempSubDir.mkdirs();
        }

        // Set the local space of the global configuration
        this.paconfig.setLocalSpace(localSpaceURI);
    }

    /**
     * Check that source files have been transferred and unzip them locally
     * @throws Exception
     */
    private void initTransferSource() throws Exception {

        for (PASolveFile file : taskconfig.getSourceFiles()) {
            if (file instanceof PASolveZippedFile) {
                PASolveZippedFile zippedFile = (PASolveZippedFile) file;
                zippedFile.setRootDirectory(localSpaceRootDir);
                File sourceZip = new File(zippedFile.getFullPathName());

                printLog("Unzipping source files from " + sourceZip);

                if (!sourceZip.exists() || !sourceZip.canRead()) {
                    System.err.println("Error, source zip file cannot be accessed at " + sourceZip);
                    throw new IllegalStateException("Error, source zip file cannot be accessed at " +
                        sourceZip);
                }

                // Uncompress the source files into the temp dir
                if (!FileUtils.unzip(sourceZip, sourceZip.getParentFile())) {
                    System.err.println("Unable to unzip source file " + sourceZip);
                    throw new IllegalStateException("Unable to unzip source file " + sourceZip);
                }
            }
        }

        if (paconfig.isDebug()) {
            printLog("Contents of " + tempSubDir);
            for (File f : tempSubDir.listFiles()) {
                printLog(f.getName());
            }
        }
    }

    /**
     * Checks that the remote Matlab environnment has been transferred
     * @throws Exception
     */
    private void initTransferEnv() throws Exception {
        if (!paconfig.isTransferEnv()) {
            return;
        }

        PASolveFile file = paconfig.getEnvMatFile();
        file.setRootDirectory(localSpaceRootDir);

    }

    /**
     * Checks that the input files have been transferred, unzip them if asked
     * @throws Exception
     */
    private void initTransferInputFiles() throws Exception {
        // do nothing
    }

    /**
     * Checks that input variables have been transferred
     * @throws Exception
     */
    private void initTransferVariables() throws Exception {
        PASolveFile file = taskconfig.getInputVariablesFile();
        file.setRootDirectory(localSpaceRootDir);

        if (taskconfig.getComposedInputVariablesFile() != null) {
            file = taskconfig.getComposedInputVariablesFile();
            file.setRootDirectory(localSpaceRootDir);
        }

    }

    private void addSources() throws Exception {
        if (tempSubDir != null) {
            printLog("Adding to matlabpath sources from " + tempSubDir);
            // Add unzipped source files to the MATALAB path
            matlabConnection.evalString("addpath('" + tempSubDir + "');");
        }
    }

    private void execCheckToolboxes() throws Exception {
        StringBuilder checktoolboxesCommand = new StringBuilder(paconfig.getChecktoolboxesFunctionName() +
            "( {");
        String[] used = taskconfig.getToolboxesUsed();
        for (int i = 0; i < used.length; i++) {
            if (i < used.length - 1) {
                checktoolboxesCommand.append("'" + used[i] + "',");
            } else {
                checktoolboxesCommand.append("'" + used[i] + "'");
            }
        }
        checktoolboxesCommand.append("},'" + localSpaceRootDir.toString() + "');");
        printLog(checktoolboxesCommand.toString());
        matlabConnection.execCheckToolboxes(checktoolboxesCommand.toString());
    }

    private void execKeepAlive() throws Exception {
        printLog("Executing Keep-Alive timer");

        StringBuilder keepAliveCommand = new StringBuilder(
            "t = timer('Period', 300,'ExecutionMode','fixedRate');t.TimerFcn = { @" +
                paconfig.getKeepaliveCallbackFunctionName() + ", {");
        String[] used = taskconfig.getToolboxesUsed();
        for (int i = 0; i < used.length; i++) {
            if (i < used.length - 1) {
                keepAliveCommand.append("'" + used[i] + "',");
            } else {
                keepAliveCommand.append("'" + used[i] + "'");
            }
        }
        keepAliveCommand.append("}};start(t);");

        printLog(keepAliveCommand.toString());
        matlabConnection.evalString(keepAliveCommand.toString());
    }

    private void loadWorkspace() throws Exception {
        if (paconfig.isTransferEnv()) {
            PASolveEnvFile paenv = paconfig.getEnvMatFile();
            paenv.setRootDirectory(this.localSpaceRootDir.getCanonicalPath());
            File envMat = new File(paenv.getFullPathName());
            printLog("Loading workspace from " + envMat);
            if (paconfig.isDebug()) {
                matlabConnection.evalString("disp('Contents of " + envMat + "');");
                matlabConnection.evalString("whos('-file','" + envMat + "')");
            }
            // Load workspace using MATLAB command
            List<String> globals = paenv.getEnvGlobalNames();
            if (globals != null && globals.size() > 0) {
                String globalstr = "";
                for (String name : globals) {
                    globalstr += " " + name;
                }
                matlabConnection.evalString("global" + globalstr);
            }
            matlabConnection.evalString("load('" + envMat + "');");
        }
    }

    private void loadInputVariables() throws Exception {

        File inMat = new File(taskconfig.getInputVariablesFile().getFullPathName());

        printLog("Loading input variables from " + inMat);

        matlabConnection.evalString("load('" + inMat + "');");
        if (taskconfig.getComposedInputVariablesFile() != null) {
            File compinMat = new File(taskconfig.getComposedInputVariablesFile().getFullPathName());
            matlabConnection.evalString("load('" + compinMat + "');in=out;clear out;");
        }

    }

    private void loadTopologyNodeNames() throws Exception {
        String urllist = "NODE_URL_LIST = { ";
        for (String nodeUrl : this.getNodesURL()) {
            urllist += "'" + nodeUrl + "' ";
        }
        urllist += " };";
        matlabConnection.evalString(urllist);
    }

    private void storeOutputVariable() throws Exception {
        PASolveFile pafile = taskconfig.getOutputVariablesFile();
        pafile.setRootDirectory(localSpaceRootDir);
        File outputFile = new File(pafile.getFullPathName());

        printLog("Storing 'out' variable into " + outputFile);

        if (paconfig.getMatFileOptions() != null) {
            matlabConnection.evalString("save('" + outputFile + "','out','" + paconfig.getMatFileOptions() +
                "');");
        } else {
            matlabConnection.evalString("save('" + outputFile + "','out');");
        }

        //if (!outputFile.exists()) {
        //    throw new MatlabTaskException("Unable to store 'out' variable, the output file does not exist");
        //}
    }

    private void transferOutputFiles() throws Exception {
        // do nothing
    }

    private void testOutput() throws Exception {

        PASolveFile pafile = taskconfig.getOutputVariablesFile();
        pafile.setRootDirectory(localSpaceRootDir);
        File outputFile = new File(pafile.getFullPathName());
        printLog("Testing output file : " + outputFile);
        if (!outputFile.exists()) {
            throw new MatlabTaskException("Cannot find output variable file.");
        }
    }

    private void printLog(final String message) {
        printLog(message, false);
    }

    private void printLog(final String message, boolean force) {
        if (!this.paconfig.isDebug() && !force) {
            return;
        }
        final Date d = new Date();
        final String log = "[" + ISO8601FORMAT.format(d) + " " + HOSTNAME + "][" +
            this.getClass().getSimpleName() + "] " + message;

        // In case of non forked mode, the message is skipped after the first line break.
        // To avoid this, lets print line per line
        String[] lines = log.split(System.lineSeparator());
        for (String line  : lines)
        {
            getOut().println(line);
        }

        if (this.outDebug != null) {
            this.outDebug.println(log);
            this.outDebug.flush();
        }

    }

    /** Creates a log file in the java.io.tmpdir if debug is enabled */
    private void createLogFileOnDebug() throws Exception {
        if (!this.paconfig.isDebug()) {
            return;
        }

        final String taskId = (String) this.getVariables().get("PA_TASK_ID");
        final File logFile = new File(this.tmpDir, "MatlabExecutable_" + taskId + ".log");
        if (!logFile.exists()) {
            logFile.createNewFile();
        }

        try {
            this.debugfos = new FileOutputStream(logFile);
            this.outDebug = new PrintStream(this.debugfos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeLogFileOnDebug() {
        try {
            if (this.outDebug != null) {
                this.outDebug.close();
            }
        } catch (Exception e) {
        }
        try {
            if (this.debugfos != null) {
                this.debugfos.close();
            }
        } catch (Exception e) {
        }

    }
}
