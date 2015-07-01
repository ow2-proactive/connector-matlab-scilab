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
package org.ow2.proactive.scheduler.ext.common.util;

/**
 * StackTraceUtil
 *
 * @author The ProActive Team
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;


/**
* Simple utilities to return the stack trace of an
* exception as a String.
*/
public final class StackTraceUtil {

    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    public static boolean equalsStackTraces(Throwable a, Throwable b) {
        if ((a == null) && (b == null)) {
            return true;
        }
        if (a == null) {
            return false;
        }
        if (b == null) {
            return false;
        }
        if (!a.getClass().equals(b.getClass())) {
            return false;
        }
        if ((a.getMessage() == null) && (b.getMessage() != null)) {
            return false;
        }
        if ((b.getMessage() == null) && (a.getMessage() != null)) {
            return false;
        }

        if ((a.getMessage() != null) && (b.getMessage() != null) && !a.getMessage().equals(b.getMessage())) {
            return false;
        }
        return equalsStackTraces(a.getCause(), b.getCause());

    }

    /**
    * Defines a custom format for the stack trace as String.
    */
    public static String getCustomStackTrace(Throwable aThrowable) {
        //add the class name and any message passed to constructor
        final StringBuilder result = new StringBuilder("BOO-BOO: ");
        result.append(aThrowable.toString());
        final String NEW_LINE = System.lineSeparator();
        result.append(NEW_LINE);

        //add each element of the stack trace
        for (StackTraceElement element : aThrowable.getStackTrace()) {
            result.append(element);
            result.append(NEW_LINE);
        }
        return result.toString();
    }

    /** Demonstrate output.  */
    public static void main(String... aArguments) {
        final Throwable throwable = new IllegalArgumentException("Blah");
        System.out.println(getStackTrace(throwable));
        System.out.println(getCustomStackTrace(throwable));
    }
}
