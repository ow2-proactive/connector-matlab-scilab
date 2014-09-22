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

    private static final long serialVersionUID = 60L;

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
