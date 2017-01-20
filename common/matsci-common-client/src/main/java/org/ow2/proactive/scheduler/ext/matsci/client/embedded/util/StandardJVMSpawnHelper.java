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
package org.ow2.proactive.scheduler.ext.matsci.client.embedded.util;

import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.common.util.StackTraceUtil;
import org.ow2.proactive.scheduler.ext.matsci.client.common.data.Pair;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * StandardJVMSpawnHelper class used to start a Java Virtual Machine on the local host and deploy RMI interfaces on it.
 * This class MUST NOT contain any reference other than matsci or common extension, as it is run within Matlab/Scilab without access to Scheduler or ProActive jars
 *
 * @author The ProActive Team
 */
public abstract class StandardJVMSpawnHelper {

    protected final static String POLICY_OPTION = "-Djava.security.policy=";
    protected final static String LOG4J_OPTION = "-Dlog4j.configuration=file:";
    protected final static String PA_CONFIGURATION_OPTION = "-Dproactive.configuration=";

    private static final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");

    /**
     * Timeout used to deploy the JVM (times 50ms)
     */
    protected static int TIMEOUT = 60000;

    /**
     * Default classpath (classpath of the current JVM)
     */
    protected final static String DEFAULT_CLASSPATH = convertClasspathToAbsolutePath(System
            .getProperty("java.class.path"));

    /**
     * Default Java executable path (java path of the current JVM)
     */
    protected static String DEFAULT_JAVAPATH;

    static {
        if (System.getProperty("os.name").startsWith("Windows")) {
            DEFAULT_JAVAPATH = System.getProperty("java.home") + File.separator + "bin" + File.separator +
                "java.exe";
        } else {
            DEFAULT_JAVAPATH = System.getProperty("java.home") + File.separator + "bin" + File.separator +
                "java";
        }
    }

    /**
     * Options for the JVM
     */
    protected ArrayList<String> jvmOptions = new ArrayList<String>();

    /**
     * Entries of the classpath
     */
    protected String[] cpEntries;

    /**
     * Full classpath
     */
    protected String classPath;

    /**
     * Path to ProActive Configuration
     */
    protected String proactiveConf;

    /**
     * Path to log4J file
     */
    protected String log4JFile;

    /**
     * Path to Java security policy file
     */
    protected String policyFile;

    /**
     * Path to Java executable
     */
    protected String javaPath;

    /**
     * Name of the Main class
     */
    protected String className;

    /**
     * scheduler URI used when creating this middleman JVM
     */
    protected static String oldSchedulerURI;

    /**
     * scheduler URI used now
     */
    protected String newSchedulerURI;

    /**
     * RMI port to use
     */
    protected int rmi_port = 1099;

    /**
     * Debug mode
     */
    protected boolean debug = false;

    /**
     * Command line arguments
     */
    protected ArrayList<String> arguments = new ArrayList<String>();

    /**
     * Names of the RMI interfaces
     */
    protected ArrayList<String> itfNames = new ArrayList<String>();

    /**
     * Names of the RMI interfaces
     */
    protected HashMap<String, Object> stubs = new HashMap<String, Object>();

    /**
     * Path to tmp dir
     */
    protected static String tmpPath = System.getProperty("java.io.tmpdir");

    protected String matSciDir = null;

    /**
     * Stream to the Debug file
     */
    protected PrintStream outDebug;

    protected File logFile;

    /**
     * Minimum RMI port number
     */
    protected static final int MIN_PORT_NUMBER = 1000;

    /**
     * Maximum RMI port number
     */
    private static final int MAX_PORT_NUMBER = 9999;

    protected StandardJVMSpawnHelper() {
    }

    protected void printLog(String message) {
        Date d = new Date();
        final String log2 = "[" + ISO8601FORMAT.format(d) + "][" + this.getClass().getSimpleName() + "] " +
            message;
        if (outDebug != null) {
            outDebug.println(log2);
            outDebug.flush();
        }

    }

    protected void printLog(Throwable ex) {
        Date d = new Date();
        final String log2 = "[" + ISO8601FORMAT.format(d) + "][" + this.getClass().getSimpleName() + "] " +
            StackTraceUtil.getStackTrace(ex);
        if (outDebug != null) {
            outDebug.println(log2);
            outDebug.flush();
        }

    }

    public void setMatSciDir(String matSciDir) {
        this.matSciDir = null;
    }

    public void setSchedulerURI(String uri) {
        this.newSchedulerURI = uri;
    }

    public void setDebug(boolean d) {
        this.debug = d;
    }

    public void setRmiPort(int port) {
        this.rmi_port = port;
    }

    public void setTimeout(int timeout) {
        TIMEOUT = timeout;
    }

    public void setJavaPath(String jpath) {
        File test = new File(jpath);
        if (!test.exists() || !test.canExecute()) {
            throw new IllegalArgumentException(jpath + " does not exist or is not readable.");
        }
        this.javaPath = jpath;
    }

    public void setClasspathEntries(String[] entries) {
        StringBuffer absoluteClasspath = new StringBuffer();
        String pathSeparator = File.pathSeparator;
        for (String e : entries) {
            absoluteClasspath.append(new File(e).getAbsolutePath());
            absoluteClasspath.append(pathSeparator);
        }
        this.cpEntries = entries;
        this.classPath = absoluteClasspath.substring(0, absoluteClasspath.length() - 1);
    }

    public void setProActiveConfiguration(String confpath) {
        File test = new File(confpath);
        if (!test.exists() || !test.canRead()) {
            throw new IllegalArgumentException(confpath + " does not exist or is not readable.");
        }
        this.proactiveConf = confpath;
    }

    public void setLog4JFile(String logpath) {
        File test = new File(logpath);
        if (!test.exists() || !test.canRead()) {
            throw new IllegalArgumentException(logpath + " does not exist or is not readable.");
        }
        this.log4JFile = logpath;
    }

    public void setPolicyFile(String policy) {
        File test = new File(policy);
        if (!test.exists() || !test.canRead()) {
            throw new IllegalArgumentException(policy + " does not exist or is not readable.");
        }
        this.policyFile = policy;
    }

    public void addInterfaceName(String name) {
        this.itfNames.add(name);
    }

    public void addJvmOption(String option) {
        jvmOptions.add(option);
    }

    public void setClassName(String cn) {
        this.className = cn;
    }

    public void addArgument(String arg) {
        this.arguments.add(arg);
    }

    /**
     * Tests if the given port is available to deploy a rmiregistry
     *
     * @param port
     * @return
     */
    public static boolean available(int port) {
        if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    protected abstract void updateStubs(Registry registry) throws RemoteException, NotBoundException;

    public void updateAllStubs(boolean keepTrying) {
        boolean stubsFound = false;
        Exception lasterr = null;
        String lastMessage = "NO MESSAGE";
        long total_waited = 0;
        do {
            long begin_millis = System.currentTimeMillis();
            try {

                Registry registry = LocateRegistry.getRegistry(rmi_port);

                updateStubs(registry);
                stubsFound = true;

            } catch (Exception e) {
                lasterr = e;
                if (!e.getMessage().equals(lastMessage)) {
                    printLog(e);
                    lastMessage = e.getMessage();
                }
                long middle_millis = System.currentTimeMillis();
                long required_wait = 100 - (middle_millis - begin_millis);
                if (required_wait > 0) {
                    try {
                        Thread.sleep(required_wait);
                    } catch (InterruptedException e1) {
                        printLog(e1);
                        break;
                    }
                }
            }
            long end_millis = System.currentTimeMillis();
            total_waited += (end_millis - begin_millis);
        } while (!stubsFound && keepTrying && total_waited < TIMEOUT);
        if (total_waited >= TIMEOUT) {
            throw new RuntimeException(
                "Timeout occured when trying to update stubs, errors have been written in file : " + logFile,
                lasterr);
        }
    }

    public abstract void shutdown();

    /**
     * Starts a new JVM or lookup an existing one
     *
     * @return a map containing RMI interfaces found (name/stub), and the RMI port used
     * @throws java.io.IOException
     */
    public Pair<HashMap<String, Object>, Integer> deployOrLookup() throws Exception {
        try {

            //HashMap<String, Object> stubs = new HashMap<String, Object>();

            boolean av = false;

            logFile = new File(tmpPath, "" + this.getClass().getSimpleName() + ".log");
            if (!logFile.exists()) {

                logFile.createNewFile();

            }

            System.out.println("log file in use : " + logFile);

            outDebug = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, true)));
            printLog("Starting Deployment of Middleman");
            updateAllStubs(false);
            boolean stubsFound = !stubs.isEmpty();
            if (stubsFound) {
                printLog("Found existing Middleman JVM");
            }

            if (stubsFound && oldSchedulerURI != null && !oldSchedulerURI.equals(newSchedulerURI)) {
                // We force the old MiddlemanJVM to die and be replaced by a new one
                if (stubsFound) {
                    printLog("Killing exsting JVM because of scheduler url change");
                }
                shutdown();
                Thread.sleep(1000);
                stubsFound = false;
            }

            oldSchedulerURI = newSchedulerURI;

            if (stubsFound) {
                return new Pair<HashMap<String, Object>, Integer>(stubs, rmi_port);
            }
            if (stubsFound) {
                printLog("No existing JVM found, create a new one");
            }

            do {
                av = available(rmi_port);
                if (!av) {
                    int new_rmi_port = rmi_port + (int) Math.round(Math.random() * 10);
                    if (new_rmi_port > MAX_PORT_NUMBER) {

                    }
                    System.out.println("Port " + rmi_port + " in use, trying port " + new_rmi_port);
                    printLog("Port " + rmi_port + " in use, trying port " + new_rmi_port);
                    rmi_port = new_rmi_port;
                }
            } while (!av);

            ArrayList<String> cmd = new ArrayList<String>();
            if (javaPath != null) {
                cmd.add(javaPath);
            } else {
                cmd.add(DEFAULT_JAVAPATH);
            }

            if (policyFile != null) {
                cmd.add(POLICY_OPTION + policyFile);
            }

            if (log4JFile != null) {
                cmd.add(LOG4J_OPTION + log4JFile);
            }

            if (proactiveConf != null) {
                cmd.add(PA_CONFIGURATION_OPTION + proactiveConf);
            }

            jvmOptions.add("-Drmi.port=" + rmi_port);

            if (jvmOptions.size() > 0) {
                cmd.addAll(jvmOptions);
            }

            if (className == null) {
                throw new IllegalStateException("Missing class name.");
            }

            cmd.add(className);

            cmd.add(debug ? "true" : "false");

            for (String arg : arguments) {
                cmd.add(arg);
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);

            if (this.matSciDir != null) {
                File cd = new File(this.matSciDir);
                if (cd.exists() && cd.isDirectory()) {
                    pb.directory(new File(this.matSciDir));
                } else {
                    System.err.println("Warning : Can't find directory " + cd + ", using " +
                        new File(".").getAbsolutePath() + " instead.");
                    printLog("Warning : Can't find directory " + cd + ", using " +
                        new File(".").getAbsolutePath() + " instead.");
                }
            }

            Map<String, String> env = pb.environment();
            if (classPath != null) {
                env.put("CLASSPATH", classPath);
            } else {
                env.put("CLASSPATH", DEFAULT_CLASSPATH);
            }

            if (debug) {
                System.out.println("Running Java command :");
                System.out.println(cmd);
                printLog("Running Java command :");
                printLog(cmd.toString());
            }
            Process process = pb.start();
            printLog("JVM process started");

            IOTools.LoggingThread lt1;

            lt1 = new IOTools.LoggingThread(process.getInputStream(), "[MIDDLEMAN]", System.out);

            Thread t1 = new Thread(lt1, "MIDDLEMAN");
            t1.setDaemon(true);
            t1.start();

            if (itfNames.size() > 0) {

                updateAllStubs(true);

                return new Pair<HashMap<String, Object>, Integer>(stubs, rmi_port);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    private static String convertClasspathToAbsolutePath(String classpath) {
        StringBuffer absoluteClasspath = new StringBuffer();
        String pathSeparator = File.pathSeparator;
        java.util.StringTokenizer st = new java.util.StringTokenizer(classpath, pathSeparator);
        while (st.hasMoreTokens()) {
            absoluteClasspath.append(new File(st.nextToken()).getAbsolutePath());
            absoluteClasspath.append(pathSeparator);
        }
        return absoluteClasspath.substring(0, absoluteClasspath.length() - 1);
    }

}
