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
package org.ow2.proactive.scheduler.ext.matsci.client.common.exception;

import org.ow2.proactive.scheduler.ext.common.util.StackTraceUtil;


/**
 * PASchedulerException
 *
 * This class encapsulate ProActive Scheduler exceptions and transform them as Strings (for classloading issues).
 *
 * @author The ProActive Team
 */
public class PASchedulerException extends RuntimeException {

    private PASchedulerExceptionType type;

    public PASchedulerException() {
        super();
    }

    public PASchedulerException(PASchedulerExceptionType type) {
        super();
        this.type = type;
    }

    public PASchedulerException(String message) {
        super(message);
    }

    public PASchedulerException(String message, PASchedulerExceptionType type) {
        super(message);
        this.type = type;
    }

    public PASchedulerException(String message, Throwable cause) {
        super(message + "\nCaused By:\n" + StackTraceUtil.getStackTrace(cause));
    }

    public PASchedulerException(String message, Throwable cause, PASchedulerExceptionType type) {
        super(message + "\nCaused By:\n" + StackTraceUtil.getStackTrace(cause));
        this.type = type;
    }

    public PASchedulerException(Throwable cause) {
        super(StackTraceUtil.getStackTrace(cause));
    }

    public PASchedulerException(Throwable cause, PASchedulerExceptionType type) {
        super(StackTraceUtil.getStackTrace(cause));
        this.type = type;
    }

    public PASchedulerExceptionType getType() {
        return type;
    }

}
