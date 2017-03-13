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
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

    private final Logger logger = Logger.getLogger(MatlabExecutable.class);

    protected static String HOSTNAME;

    static {
        try {
            HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
        }
    }

    /** The global configuration */
    private PASolveMatlabGlobalConfig paconfig;

    /** The task configuration */
    private PASolveMatlabTaskConfig taskconfig;

    /** The MATLAB configuration */
    private MatlabEngineConfig matlabEngineConfig;

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

        Object obj = args.get("global_config");
        if (obj != null) {
            this.paconfig = (PASolveMatlabGlobalConfig) obj;
        }

        // Set the log4j level according to the config
        if (paconfig.isDebug())
            logger.setLevel(Level.DEBUG);

        obj = args.get("task_config");
        if (obj != null) {
            this.taskconfig = (PASolveMatlabTaskConfig) obj;
        }

        logger.debug("Reading the main script to execute");
        this.script = (String) args.get("script");
        if (this.script == null || "".equals(this.script)) {
            throw new IllegalArgumentException("Unable to execute task, no script specified");
        }

        logger.debug("Initializing the MATLAB location");
        this.initMatlabConfig();

        logger.debug("Initializing the local space");
        this.initLocalSpace();

        logger.debug("Initializing transfers");
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

        final String matlabCmd = this.matlabEngineConfig.getFullCommand();

        logger.debug("Acquiring MATLAB connection using " + matlabCmd);
        if (paconfig.isUseMatlabControl()) {
            this.matlabConnection = new MatlabConnectionMCImpl(getOut());
        } else {
            this.matlabConnection = new MatlabConnectionRImpl(getOut());
        }

        final String jobId = (String) this.getVariables().get("PA_JOB_ID");
        final String taskId = (String) this.getVariables().get("PA_TASK_ID");

        matlabConnection.acquire(matlabCmd, this.localSpaceRootDir, this.paconfig, this.taskconfig, jobId, taskId);

        Serializable result = null;

        try {
            logger.debug("Executing the MATLAB script and receiving the result");
            result = this.executeScript();
        } finally {
            logger.debug("Closing MATLAB...");
            this.matlabConnection.release();
            logger.debug("End of Task");
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

        logger.debug("Adding sources, loading workspace and input variables");
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

        logger.debug("Evaluating the MATLAB command: " + this.script);
        matlabConnection.evalString(this.script);

        logger.debug("Receiving output");
        storeOutputVariable();

        // outputFiles
        transferOutputFiles();

        matlabConnection.beforeLaunch();

        logger.debug("Launching the MATLAB command");
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
        logger.debug("Initializing the MATLAB config");
        MatlabEngineConfig conf = (MatlabEngineConfig) MatlabEngineConfig.getCurrentConfiguration();
        if (conf == null) {
            conf = (MatlabEngineConfig) new MatlabFinder(paconfig.isDebug()).findMatSci(paconfig.getVersionPref(),
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
        logger.debug("Initializing the local space");
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

                logger.debug("Unzipping source files from " + sourceZip);
                if (!sourceZip.exists() || !sourceZip.canRead()) {
                    logger.error("Error, source zip file cannot be accessed at " + sourceZip);
                    throw new IllegalStateException("Error, source zip file cannot be accessed at " +
                        sourceZip);
                }

                // Uncompress the source files into the temp dir
                if (!FileUtils.unzip(sourceZip, sourceZip.getParentFile())) {
                    logger.error("Unable to unzip source file " + sourceZip);
                    throw new IllegalStateException("Unable to unzip source file " + sourceZip);
                }
            }
        }

        logger.debug("Contents of " + tempSubDir);
        for (File f : tempSubDir.listFiles()) {
            logger.debug(f.getName());
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
            logger.debug("Adding sources from " + tempSubDir + " to the MATALAB path");
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

        logger.debug(checktoolboxesCommand.toString());

        matlabConnection.execCheckToolboxes(checktoolboxesCommand.toString());
    }

    private void execKeepAlive() throws Exception {
        logger.debug("Executing Keep-Alive timer");

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

        logger.debug(keepAliveCommand.toString());

        matlabConnection.evalString(keepAliveCommand.toString());
    }

    private void loadWorkspace() throws Exception {
        if (paconfig.isTransferEnv()) {
            PASolveEnvFile paenv = paconfig.getEnvMatFile();
            paenv.setRootDirectory(this.localSpaceRootDir.getCanonicalPath());
            File envMat = new File(paenv.getFullPathName());

            logger.debug("Loading workspace from " + envMat);
            if (paconfig.isDebug()) {
                matlabConnection.evalString("disp('Contents of " + envMat + "');");
                matlabConnection.evalString("whos('-file','" + envMat + "')");
            }

            logger.debug("Load workspace using MATLAB command");
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

        logger.debug("Loading input variables from " + inMat);

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

        logger.debug("Storing 'out' variable into " + outputFile);
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
        logger.debug("Receiving and testing output...");

        PASolveFile pafile = taskconfig.getOutputVariablesFile();
        pafile.setRootDirectory(localSpaceRootDir);
        File outputFile = new File(pafile.getFullPathName());

        if (!outputFile.exists()) {
            throw new MatlabTaskException("Cannot find output variable file.");
        }
    }
}
