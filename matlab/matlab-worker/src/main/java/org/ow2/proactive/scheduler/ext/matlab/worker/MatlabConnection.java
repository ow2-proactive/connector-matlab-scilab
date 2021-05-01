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

import java.io.File;

import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabGlobalConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabInitException;
import org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException;


/**
 * This interface defines the connection to the matlab engine. This connection can either be via Matlab Control (live connection)
 * or via Matlab batch mode. In the first case, commands sent are executed interactively by the matlab engine, in the latter case,
 * they are scheduled and run as one in a generated matlab script.
 */
public interface MatlabConnection {

    /**
     * Each time this method is called creates a new MATLAB process using
     * either the matlabcontrol API or matlab batch mode.
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
            PASolveMatlabTaskConfig tconfig, final String jobId, final String taskId) throws MatlabInitException;

    /**
     * Used to send initialization matlab commands to the connection (in case of command grouping)
     */
    public void init();

    /**
     * Releases the connection, after a call to this method
     * the connection becomes unusable !
     */
    public void release();

    /**
     * Evaluate the given string in the workspace.
     *
     * @param command the command to evaluate
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException If unable to evaluate the command
     */
    public void evalString(final String command) throws MatlabTaskException;

    /**
     * Extract a variable from the workspace.
     *
     * @param variableName name of the variable
     * @return value of the variable
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException if unable to get the variable
     */
    public Object get(String variableName) throws MatlabTaskException;

    /**
     * Push a variable in to the workspace.
     *
     * @param variableName name of the variable
     * @param value the value of the variable
     * @throws org.ow2.proactive.scheduler.ext.matlab.common.exception.MatlabTaskException if unable to set a variable
     */
    public void put(final String variableName, final Object value) throws MatlabTaskException;

    /**
     * This method is executed before the launch method
     */
    public void beforeLaunch();

    /**
     * Used to send finalization matlab commands to the connection and launch the command buffer (in case of command grouping)
     */
    public void launch() throws Exception;

    /**
     * Checks if toolboxes used by this task are available. Throws exceptions otherwise
     * @param command command which checks the toolboxes
     */
    public void execCheckToolboxes(String command) throws Exception;

    /**
     * Is Matlab run via a starter process (the real Matlab process is hidden) ?
     * @return answer
     */
    public boolean isMatlabRunViaAStarter();
}
