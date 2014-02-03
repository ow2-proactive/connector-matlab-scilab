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
package org.ow2.proactive.scheduler.ext.matsci.middleman;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PALifeCycle;
import org.objectweb.proactive.core.body.request.BlockingRequestQueueImpl;
import org.ow2.proactive.scheduler.ext.common.util.StackTraceUtil;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciJVMProcessInterface;


/**
 * JVMProcessInterfaceImpl
 *
 * @author The ProActive Team
 */
public class MatSciJVMProcessInterfaceImpl implements InitActive, RunActive, MatSciJVMProcessInterface {

    MatSciEnvironment env;

    MatSciJVMProcessInterfaceImpl stubOnThis;

    private static File logFile;
    private static PrintWriter outDebugWriter;
    private static FileWriter outFile;
    private static long lastMillis = System.currentTimeMillis();
    private static long DELAY = 2;
    private static Throwable lastException = new Exception();

    private static final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");

    private static final String TMPDIR = System.getProperty("java.io.tmpdir");

    private boolean terminated;
    private Thread serviceThread;
    protected Body bodyOnThis;

    /**
     * host name
     */
    protected static String host = null;

    static {
        if (host == null) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public MatSciJVMProcessInterfaceImpl() {

    }

    public MatSciJVMProcessInterfaceImpl(MatSciEnvironment env) {
        this.env = env;
        try {
            createLogFileOnDebug();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Creates a log file in the java.io.tmpdir if debug is enabled */
    protected void createLogFileOnDebug() throws Exception {

        logFile = new File(this.TMPDIR, "MatSci_Middleman_JVM.log");
        if (!logFile.exists()) {
            logFile.createNewFile();
        }

        outFile = new FileWriter(logFile, false);
        outDebugWriter = new PrintWriter(outFile);
    }

    private void closeLogFileOnDebug() {
        try {
            outDebugWriter.close();
            outFile.close();
        } catch (Exception e) {

        }
    }

    public static String getLogFilePath() {
        return logFile.getAbsolutePath();
    }

    /**
     * Display the given exception coming from the given class as log
     * @param origin object which issued the log
     * @param ex exception to print
     * @param mode printing onde
     * @param isDebug was the origin in debug mode ?
     */
    public static void printLog(Object origin, final Throwable ex, LogMode mode, boolean isDebug) {
        final Date d = new Date();

        if (!isDebug && ((mode == LogMode.STD) || (mode == LogMode.FILEONLY) || (mode == LogMode.OUTONLY))) {
            return;
        }
        // This avoids ever growing logs because of Exceptions occurring inside loops, we print only
        // exceptions which changed from the last printed exception.
        // if the exception didn't change we wait an increasing time before printing a new one (so we keep a track of ongoing
        // exceptions
        if (ex != null) {
            long millis = System.currentTimeMillis();
            boolean eq = StackTraceUtil.equalsStackTraces(ex, lastException);
            if (eq && ((millis - lastMillis) < (DELAY * 1000))) {
                return;
            }
            if (eq) {
                DELAY = DELAY * 2;
            } else {
                DELAY = 2;
            }
            lastException = ex;
            lastMillis = millis;
        }

        if ((mode == LogMode.OUTAWAYS) || (mode == LogMode.FILEANDOUTALWAYS) ||
            (mode == LogMode.OUTAWAYSNEVERFILE) ||
            (isDebug && ((mode == LogMode.STD) || (mode == LogMode.OUTONLY)))) {
            final String log1 = "[" + origin.getClass().getSimpleName() + "] " +
                StackTraceUtil.getStackTrace(ex);

            System.out.println(log1);
            System.out.flush();
        }

        if ((mode == LogMode.FILEALWAYS) || (mode == LogMode.FILEANDOUTALWAYS) ||
            (mode == LogMode.FILEALWAYSNEVEROUT) ||
            (isDebug && ((mode == LogMode.STD) || (mode == LogMode.FILEONLY)))) {
            final String log2 = "[" + ISO8601FORMAT.format(d) + " " + host + "][" +
                origin.getClass().getSimpleName() + "] " + StackTraceUtil.getStackTrace(ex);
            if (outDebugWriter != null) {
                outDebugWriter.println(log2);
                outDebugWriter.flush();
            }
        }
    }

    /**
     * Display the given message coming from the given class as log
     * @param origin object which issued the log
     * @param message message to print
     * @param mode printing onde
     * @param isDebug was the origin in debug mode ?
     */
    public static void printLog(Object origin, final String message, LogMode mode, boolean isDebug) {
        final Date d = new Date();
        if ((mode == LogMode.OUTAWAYS) || (mode == LogMode.FILEANDOUTALWAYS) ||
            (mode == LogMode.OUTAWAYSNEVERFILE) ||
            (isDebug && ((mode == LogMode.STD) || (mode == LogMode.OUTONLY)))) {
            final String log1 = "[" + origin.getClass().getSimpleName() + "] " + message;

            System.out.println(log1);
            System.out.flush();
        }

        if ((mode == LogMode.FILEALWAYS) || (mode == LogMode.FILEANDOUTALWAYS) ||
            (mode == LogMode.FILEALWAYSNEVEROUT) ||
            (isDebug && ((mode == LogMode.STD) || (mode == LogMode.FILEONLY)))) {
            final String log2 = "[" + ISO8601FORMAT.format(d) + " " + host + "][" +
                origin.getClass().getSimpleName() + "] " + message;
            if (outDebugWriter != null) {
                outDebugWriter.println(log2);
                outDebugWriter.flush();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
     */
    public void initActivity(Body body) {
        stubOnThis = (MatSciJVMProcessInterfaceImpl) PAActiveObject.getStubOnThis();
        terminated = false;
        serviceThread = Thread.currentThread();
        bodyOnThis = PAActiveObject.getBodyOnThis();

    }

    /**
     * {@inheritDoc}
     */
    public void terminateFast() {
        this.terminated = true;
        BlockingRequestQueueImpl rq = (BlockingRequestQueueImpl) bodyOnThis.getRequestQueue();
        rq.destroy();
        try {
            bodyOnThis.terminate(false);
        } catch (Exception e) {

        }
        while (serviceThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public Integer getPID() {
        RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
        String processName = rtb.getName();

        Integer result = null;

        /* tested on: */
        /* - windows xp sp 2, java 1.5.0_13 */
        /* - mac os x 10.4.10, java 1.5.0 */
        /* - debian linux, java 1.5.0_13 */
        /* all return pid@host, e.g 2204@antonius */

        Pattern pattern = Pattern.compile("^([0-9]+)@.+$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(processName);
        if (matcher.matches()) {
            result = new Integer(Integer.parseInt(matcher.group(1)));
        }
        return result;

    }

    /** {@inheritDoc} */
    public void shutdown() {
        try {
            //matlab_env.disconnect();
            env.terminate();
        } catch (Throwable e) {
        }
        stubOnThis.destroyJVM();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    protected void destroyJVM() {
        closeLogFileOnDebug();
        PALifeCycle.exitSuccess();
    }

    @Override
    public void runActivity(Body body) {
        Service service = new Service(body);
        try {
            while (!terminated) {
                service.blockingServeOldest();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
