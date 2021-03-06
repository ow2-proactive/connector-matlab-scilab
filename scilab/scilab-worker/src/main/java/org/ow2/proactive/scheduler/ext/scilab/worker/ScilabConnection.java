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

import java.io.File;

import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabGlobalConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.PASolveScilabTaskConfig;
import org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabInitException;
import org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabTaskException;


/**
 * ScilabConnection
 *
 * @author The ProActive Team
 */
public interface ScilabConnection {

    /**
     * Each time this method is called creates a new SCILAB process using
     * the scilabcontrol API.
     *
     * @param scilabExecutablePath The full path to the SCILAB executable
     * @param workingDir the directory where to start SCILAB
     * @param paconfig configuration of a Scilab PAsolve Job
     * @param tconfig configuration of a Scilab Task
     * @throws org.ow2.proactive.scheduler.ext.scilab.common.exception.ScilabInitException if SCILAB could not be initialized
     */
    public void acquire(String scilabExecutablePath, File workingDir, PASolveScilabGlobalConfig paconfig,
            PASolveScilabTaskConfig tconfig) throws ScilabInitException;

    /**
     * Used to send initialization scilab commands to the connection (in case of command grouping)
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
     * @throws ScilabTaskException If unable to evaluate the command
     */
    public void evalString(final String command) throws ScilabTaskException;

    /**
     * Extract a variable from the workspace.
     *
     * @param variableName name of the variable
     * @return value of the variable
     * @throws ScilabTaskException if unable to get the variable
     */
    public Object get(String variableName) throws ScilabTaskException;

    /**
     * Push a variable in to the workspace.
     *
     * @param variableName name of the variable
     * @param value the value of the variable
     * @throws ScilabTaskException if unable to set a variable
     */
    public void put(final String variableName, final Object value) throws ScilabTaskException;

    /**
     * This method is executed before the launch method
     */
    public void beforeLaunch();

    /**
     * Used to send finalization scilab commands to the connection and launch the command buffer (in case of command grouping)
     */
    public void launch() throws Exception;

}
