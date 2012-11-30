/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
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
package org.ow2.proactive.scheduler.ext.matsci.worker.util;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.ext.matlab.worker.util.MatlabEngineConfig;
import org.ow2.proactive.scheduler.ext.matsci.worker.properties.MatSciProperties;
import org.ow2.proactive.scheduler.ext.scilab.worker.util.ScilabEngineConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;


/**
 * MatSciConfigurationParser
 *
 * @author The ProActive Team
 */
public class MatSciConfigurationParser {

    public static enum Type {
        matlab, scilab
    }

    static String HOSTNAME;
    static String IP;
    static String TMPDIR;
    static String FS;
    static ArrayList<String> matlabConfigPaths;
    static ArrayList<String> scilabConfigPaths;

    static {
        try {
            matlabConfigPaths = new ArrayList<String>();
            scilabConfigPaths = new ArrayList<String>();
            HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();
            IP = java.net.InetAddress.getLocalHost().getHostAddress();
            TMPDIR = System.getProperty("java.io.tmpdir");
            FS = File.separator;
            matlabConfigPaths.add(TMPDIR + FS + "MatlabWorkerConfiguration.xml");
            matlabConfigPaths.add("addons/MatlabWorkerConfiguration.xml");
            scilabConfigPaths.add(TMPDIR + FS + "ScilabWorkerConfiguration.xml");
            scilabConfigPaths.add("addons/ScilabWorkerConfiguration.xml");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static OperatingSystem os = OperatingSystem.getOperatingSystem();

    static Document document;
    static Element racine;

    public static HashSet<MatSciEngineConfig> getConfigs(boolean debug, Type type) throws Exception {

        HashSet<MatSciEngineConfig> configs = new HashSet<MatSciEngineConfig>();

        File schedHome = MatSciProperties.findSchedulerHome();
        ArrayList<String> configFilePaths = new ArrayList<String>();
        if (type.equals(Type.matlab)) {
            String prop = MatSciProperties.MATLAB_WORKER_CONFIGURATION_FILE.getValueAsString();
            if ((prop != null) && (prop.length() > 0)) {
                configFilePaths.add(0, prop);
            }
            prop = System.getProperty(MatSciProperties.MATLAB_WORKER_CONFIGURATION_FILE.getKey());
            if ((prop != null) && (prop.length() > 0)) {
                configFilePaths.add(0, prop);
            }
            configFilePaths.addAll(matlabConfigPaths);
        } else {
            String prop = MatSciProperties.SCILAB_WORKER_CONFIGURATION_FILE.getValueAsString();
            if ((prop != null) && (prop.length() > 0)) {
                configFilePaths.add(0, prop);
            }
            prop = System.getProperty(MatSciProperties.SCILAB_WORKER_CONFIGURATION_FILE.getKey());
            if ((prop != null) && (prop.length() > 0)) {
                configFilePaths.add(0, prop);
            }
            configFilePaths.addAll(scilabConfigPaths);
        }
        File configFile = null;
        for (String path : configFilePaths) {
            configFile = new File(path);

            // Check if the config file exists at the specified path
            if (!configFile.isAbsolute()) {
                configFile = new File(schedHome + File.separator + path);
            }

            if (!configFile.exists() || !configFile.canRead()) {
                System.out.println(configFile + " not found, skipping...");
                continue;
            } else if (!configFile.getName().endsWith(".xml")) {
                System.out.println(configFile + " : unrecognized extension, skipping...");
                continue;
            } else {
                if (debug) {
                    System.out.println("Parsing configuration file :" + configFile);
                }
            }
            SAXBuilder sxb = new SAXBuilder();
            Document document = sxb.build(configFile);
            racine = document.getRootElement();

            List<Element> machineGroups = racine.getChildren("MachineGroup");

            for (Element machineGroup : machineGroups) {
                if (matchesHost(machineGroup)) {
                    List<Element> listInstallations = null;

                    if (type.equals(Type.matlab)) {
                        listInstallations = machineGroup.getChildren("matlab");
                    } else {
                        listInstallations = machineGroup.getChildren("scilab");
                    }

                    for (Element installation : listInstallations) {

                        String version = installation.getChild("version").getText();
                        if ((version == null) || (version.trim().length() == 0)) {
                            System.out.println("In " + configFile + ", version element must not be empty");
                            continue;
                        }
                        version = version.trim();
                        if (type.equals(Type.matlab)) {
                            if (!version.matches("^[1-9][\\d]*\\.[\\d]+$")) {
                                System.out.println("In " + configFile +
                                    ", version element must match XX.xx, received : " + version);
                                continue;
                            }
                        } else {
                            if (!version.matches("^([1-9][\\d]*\\.)*[\\d]+$")) {
                                System.out.println("In " + configFile +
                                    ", version element must match XX.xx.xx, received : " + version);
                                continue;
                            }
                        }
                        String home = installation.getChild("home").getText();
                        if ((home == null) || (home.trim().length() == 0)) {
                            System.out.println("In " + configFile + ", home element must not be empty");
                            continue;
                        }

                        home = home.trim().replaceAll("/", Matcher.quoteReplacement("" + os.fileSeparator()));
                        File filehome = new File(home);
                        if (!checkDir(filehome, configFile)) {
                            continue;
                        }

                        String bindir = installation.getChild("bindir").getText();
                        if ((bindir == null) || (bindir.trim().length() == 0)) {
                            System.out.println("In " + configFile + ", bindir element must not be empty");
                            continue;
                        }
                        bindir = bindir.trim().replaceAll("/",
                                Matcher.quoteReplacement("" + os.fileSeparator()));
                        File filebin = new File(filehome, bindir);
                        checkDir(filebin, configFile);

                        String command = installation.getChild("command").getText();
                        if ((command == null) || (command.trim().length() == 0)) {
                            System.out.println("In " + configFile + ", command element must not be empty");
                            continue;
                        }
                        command = command.trim();
                        File filecommand = new File(filebin, command);
                        if (!checkFile(filecommand, configFile, true)) {
                            continue;
                        }

                        String arch = installation.getChild("arch").getText();
                        if ((arch == null) || (arch.trim().length() == 0)) {
                            System.out.println("In " + configFile + ", arch element must not be empty");
                            continue;
                        }
                        arch = arch.trim();

                        if (!(arch.equals("32") || arch.equals("64"))) {
                            System.out.println("In " + configFile +
                                ", arch element must be 32 or 64 received : " + arch);
                            continue;
                        }

                        if (type.equals(Type.matlab)) {
                            MatlabEngineConfig conf = new MatlabEngineConfig(home, version, bindir, command,
                                arch);
                            if (debug) {
                                System.out.println("Found : " + conf);
                            }
                            configs.add(conf);
                        } else {
                            ScilabEngineConfig conf = new ScilabEngineConfig(home, version, bindir, command,
                                arch);
                            if (debug) {
                                System.out.println("Found : " + conf);
                            }
                            configs.add(conf);
                        }
                    }
                }
            }
        }

        return configs;

    }

    private static boolean matchesHost(Element machineGroup) {
        Attribute hostmatch = machineGroup.getAttribute("hostname");
        if (hostmatch != null) {
            if (HOSTNAME.matches(hostmatch.getValue())) {
                return true;
            }
            return false;
        }
        Attribute ipmatch = machineGroup.getAttribute("ip");
        if (ipmatch != null) {
            if (IP.matches(ipmatch.getValue())) {
                return true;
            }
            return false;
        }
        return false;

    }

    protected static boolean checkDir(File dir, File conf) {
        if (!dir.exists()) {
            System.out.println("In " + conf + ", " + dir + " doesn't exist");
            return false;
        }
        if (!dir.isDirectory()) {
            System.out.println("In " + conf + ", " + dir + " is not a directory");
            return false;
        }
        if (!dir.canRead()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            System.out.println("In " + conf + ", " + dir + " is not readable");
            return false;
        }
        return true;
    }

    protected static boolean checkFile(File file, File conf, boolean executable) throws Exception {
        if (!file.exists()) {
            System.out.println("In " + conf + ", " + file + " doesn't exist");
            return false;
        }
        if (!file.isFile()) {
            System.out.println("In " + conf + ", " + file + " is not a file");
            return false;
        }
        if (!file.canRead()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            System.out.println("In " + conf + ", " + file + " is not readable");
            return false;
        }
        if (executable && !file.canExecute()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            System.out.println("In " + conf + ", " + file + " is not executable");
            return false;
        }
        return true;
    }
}
