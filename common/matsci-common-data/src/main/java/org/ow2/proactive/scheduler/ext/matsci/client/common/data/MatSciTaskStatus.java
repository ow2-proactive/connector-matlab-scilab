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
package org.ow2.proactive.scheduler.ext.matsci.client.common.data;

/**
 * MatSciTaskStatus the result status of the Task, seen from Matlab or Scilab
 *
 * @author The ProActive Team
 */
public enum MatSciTaskStatus {
    /**
     * OK, the task completed successfully
     */
    OK("Ok"),
    /**
     * Global misbehaviour (Scheduler is stopped, job has been killed)
     */
    GLOBAL_ERROR("Global Error"),
    /**
     * An error occured in the task outside of the MatSci code. This should be an internal error
     */
    RUNTIME_ERROR("Runtime Error"),
    /**
     * An error occured in the MatSci code
     */
    MATSCI_ERROR("MatSci Error");

    /** The textual definition of the status */
    private String definition;

    /**
     * Default constructor.
     * @param def the textual definition of the status.
     */
    MatSciTaskStatus(String def) {
        definition = def;
    }

    /**
     * @see Enum#toString()
     */
    @Override
    public String toString() {
        return definition;
    }
}
