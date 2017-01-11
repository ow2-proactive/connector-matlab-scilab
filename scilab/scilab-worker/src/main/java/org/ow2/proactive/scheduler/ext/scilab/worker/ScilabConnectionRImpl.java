/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
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
package org.ow2.proactive.scheduler.ext.scilab.worker;

import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabGlobalConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabTaskConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabInitException;
import org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabTaskException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


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

    protected File logFile;
    protected boolean debug;

    protected File mainFuncFile;

    protected Process process;

    protected OperatingSystem os = OperatingSystem.getOperatingSystem();

    private static final String startPattern = "---- SCILAB START ----";

    private final PrintStream outDebug;

    private final String tmpDir;

    PASolveScilabGlobalConfig paconfig;

    PASolveScilabTaskConfig tconfig;

    IOTools.LoggingThread lt1;

    public ScilabConnectionRImpl(final String tmpDir, final PrintStream outDebug) {
        this.tmpDir = tmpDir;
        this.outDebug = outDebug;
    }

    public void acquire(String scilabExecutablePath, File workingDir, PASolveScilabGlobalConfig paconfig,
            PASolveScilabTaskConfig tconfig, final String taskLogId) throws ScilabInitException {
        this.scilabLocation = scilabExecutablePath;
        this.workingDirectory = workingDir;
        this.debug = paconfig.isDebug();
        this.paconfig = paconfig;
        this.tconfig = tconfig;
        this.TIMEOUT_START = paconfig.getWorkerTimeoutStart();
        if (this.os == OperatingSystem.windows) {
            this.startUpOptions = paconfig.getWindowsStartupOptions();
        } else {
            this.startUpOptions = paconfig.getLinuxStartupOptions();
        }

        this.logFile = new File(this.tmpDir, "ScilabStart_" + taskLogId + ".log");
        this.mainFuncFile = new File(workingDir, "PAMain.sce");
        if (!this.mainFuncFile.exists()) {
            try {
                this.mainFuncFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void init() {
        fullcommand.append("disp('" + startPattern + "');" + nl);
        fullcommand.append("try" + nl);
        fullcommand.append("lines(0);" + nl);
        fullcommand.append("funcprot(0);" + nl);
    }

    public void release() {
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception e) {

            }
        }
    }

    @Override
    public String getOutput(boolean debug) {
        String output = "";
        try {
            if (debug) {
                output = IOTools.readFileAsString(this.logFile, 20000, null, null);
            } else {
                output = IOTools.readFileAsString(this.logFile, 20000, this.startPattern, null);
            }
        } catch (InterruptedException e) {

        } catch (TimeoutException e) {

        }
        return output;
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

    public void launch() throws Exception {
        fullcommand
                .append("catch" +
                    nl +
                    "[str2,n2,line2,func2]=lasterror(%t);printf('!-- error %i\\n%s\\n at line %i of function %s\\n',n2,str2,line2,func2)" +
                    nl + "errclear();" + nl + "end" + nl + "exit();");
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

        if (debug) {
            lt1 = new IOTools.LoggingThread(process, "[SCILAB Engine]", System.out, System.err, outDebug,
                null, null, null);

        } else {
            lt1 = new IOTools.LoggingThread(process, "[SCILAB]", System.out, System.err, startPattern, null,
                null);
        }
        Thread t1 = new Thread(lt1, "OUT SCILAB");
        t1.setDaemon(true);
        t1.start();

        int exitValue = process.waitFor();
        lt1.goon = false;
        if (exitValue != 0) {
            throw new ScilabInitException("Scilab process exited with code : " + exitValue);
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

        ProcessBuilder b = new ProcessBuilder().redirectOutput(this.logFile).redirectError(this.logFile);
        // invalid on windows ?
        b.directory(this.workingDirectory);
        b.command(command);

        Process p = b.start();

        return p;

    }

}
