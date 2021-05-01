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
package org.ow2.proactive.scheduler.ext.scilab.worker;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabGlobalConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabTaskConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabInitException;
import org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabTaskException;


/**
 * ScilabConnectionRImpl
 *
 * @author The ProActive Team
 */
public class ScilabConnectionRImpl implements ScilabConnection {
    protected StringBuilder fullcommand = new StringBuilder();

    protected String nl = System.lineSeparator();

    protected String[] startUpOptions;

    protected String scilabLocation;

    protected File workingDirectory;

    protected int TIMEOUT_START = 6000;

    protected boolean debug;

    protected File mainFuncFile;

    protected Process process;

    protected OperatingSystem os = OperatingSystem.getOperatingSystem();

    private final String startPattern = "---- SCILAB START ----";

    private final PrintStream taskOut;

    private final PrintStream taskErr;

    PASolveScilabGlobalConfig paconfig;

    PASolveScilabTaskConfig tconfig;

    IOTools.LoggingThread lt1;

    IOTools.LoggingThread lt2;

    private File ackFile;

    private Thread outputThread;

    private Thread errorThread;

    public ScilabConnectionRImpl(final PrintStream taskOut, final PrintStream taskErr) {
        this.taskOut = taskOut;
        this.taskErr = taskErr;
    }

    public void acquire(String scilabExecutablePath, File workingDir, PASolveScilabGlobalConfig paconfig,
            PASolveScilabTaskConfig tconfig) throws ScilabInitException {
        this.scilabLocation = scilabExecutablePath;
        this.workingDirectory = workingDir;
        this.ackFile = new File(workingDirectory, "scilab.ack");
        this.debug = paconfig.isDebug();
        this.paconfig = paconfig;
        this.tconfig = tconfig;
        this.TIMEOUT_START = paconfig.getWorkerTimeoutStart();
        if (this.os == OperatingSystem.windows) {
            this.startUpOptions = paconfig.getWindowsStartupOptions();
        } else {
            this.startUpOptions = paconfig.getLinuxStartupOptions();
        }

        this.mainFuncFile = new File(workingDir, "PAMain.sce");
    }

    public void init() {
        fullcommand.append("disp('" + startPattern + "');" + nl);
        fullcommand.append("try" + nl);
        fullcommand.append("ok=%T;save('" + ackFile.getAbsolutePath() + "','ok');" + nl);
        fullcommand.append("lines(0);" + nl);
        fullcommand.append("funcprot(0);" + nl);
    }

    public void release() {
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception e) {

            }
            try {
                interruptThreads();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void evalString(String command) throws ScilabTaskException {
        fullcommand.append(command + nl);
    }

    public Object get(String variableName) throws ScilabTaskException {
        throw new UnsupportedOperationException();
    }

    public void put(String variableName, Object value) throws ScilabTaskException {
        throw new UnsupportedOperationException();
    }

    public void beforeLaunch() {
        fullcommand.append("catch" + nl +
                           "[str2,n2,line2,func2]=lasterror(%t);printf('!-- error %i\\n%s\\n at line %i of function %s\\n',n2,str2,line2,func2)" +
                           nl + "errclear();" + nl + "end" + nl + "exit();");
    }

    public void launch() throws Exception {

        if (this.mainFuncFile.exists()) {
            this.mainFuncFile.delete();
        }

        try {
            this.mainFuncFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PrintStream out = null;
        try {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(mainFuncFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        out.println(fullcommand);
        out.flush();
        out.close();

        process = createScilabProcess("PAMain.sce");

        // Logging threads creation & start
        lt1 = new IOTools.LoggingThread(process.getInputStream(),
                                        "[SCILAB OUT]",
                                        this.taskOut,
                                        this.debug ? null : startPattern,
                                        null,
                                        null);
        lt2 = new IOTools.LoggingThread(process.getErrorStream(), "[SCILAB ERR]", this.taskErr, null, null, null);
        outputThread = new Thread(lt1, "SCILAB OUT");
        errorThread = new Thread(lt2, "SCILAB ERR");
        outputThread.setDaemon(true);
        errorThread.setDaemon(true);
        outputThread.start();
        errorThread.start();

        int exitValue = process.waitFor();

        if (exitValue != 0) {
            throw new ScilabInitException("Scilab process exited with code : " + exitValue);
        }

        if (!ackFile.exists()) {
            interruptThreads();
            // scilab exited silently without executing any code, relaunch the process
            launch();
        }
    }

    private void interruptThreads() throws InterruptedException {
        if (outputThread != null) {
            outputThread.interrupt();
            outputThread.join();
        }
        if (errorThread != null) {
            errorThread.interrupt();
            errorThread.join();
        }
    }

    protected Process createScilabProcess(String runArg) throws Exception {
        // Attempt to run SCILAB
        final ArrayList<String> commandList = new ArrayList<String>();
        commandList.add(this.scilabLocation);
        commandList.addAll(Arrays.asList(this.startUpOptions));
        commandList.add("-f");
        commandList.add(runArg);

        String[] command = (String[]) commandList.toArray(new String[commandList.size()]);

        ProcessBuilder b = new ProcessBuilder();

        // invalid on windows ?
        b.directory(this.workingDirectory);
        b.command(command);

        Process p = b.start();

        return p;

    }

}
