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
package org.ow2.proactive.scheduler.ext.matsci.middleman;

/**
 * LogMode defines for each logging attempt in the MiddlemanJVM, where the log will be printed
 *
 * @author The ProActive Team
 */
public enum LogMode {
    /**
     * In this mode the log is printed only if the debug mode is activated (and it's printed on both the STDOUT and the logfile)
     */
    STD,
    /**
     * In this mode the log is printed only in the log file and only if the debug mode is activated
     */
    FILEONLY,
    /**
     * In this mode the log is always printed in the log file and it's printed on the STDOUT if in debug
     */
    FILEALWAYS,
    /**
     * In this mode the log is always printed in the log file, and never on the STDOUT
     */
    FILEALWAYSNEVEROUT,
    /**
     * In this mode the log is printed only in the STDOUT and only if the debug mode is activated
     */
    OUTONLY,
    /**
     * In this mode the log is always printed in the STDOUT and it's printed on the log file if in debug
     */
    OUTAWAYS,
    /**
     * In this mode the log is always printed in the STDOUT, and never on the log file
     */
    OUTAWAYSNEVERFILE,
    /**
     * In this mode the log is always printed in both the STDOUT and the logfile
     */
    FILEANDOUTALWAYS
}
