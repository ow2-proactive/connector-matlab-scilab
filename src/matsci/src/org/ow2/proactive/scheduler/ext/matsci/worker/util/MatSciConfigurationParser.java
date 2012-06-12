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
import org.ow2.proactive.scheduler.ext.matsci.common.properties.MatSciProperties;
import org.ow2.proactive.scheduler.ext.scilab.worker.util.ScilabEngineConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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

    static final String DEFAULT_CONFIG_PATH = "addons/MatSciWorkerConfiguration.xml";

    static String HOSTNAME;
    static String IP;

    static {
        try {
            HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();
            IP = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {

        }
    }

    protected static OperatingSystem os = OperatingSystem.getOperatingSystem();

    static Document document;
    static Element racine;

    public static ArrayList<MatSciEngineConfig> getConfigs(boolean debug, Type type) throws Exception {

        ArrayList<MatSciEngineConfig> configs = new ArrayList<MatSciEngineConfig>();

        File schedHome = MatSciProperties.findSchedulerHome();

        String configFilePath = MatSciProperties.MATSCI_WORKER_CONFIGURATION_FILE.getValueAsString();
        if (configFilePath == null || "".equals(configFilePath)) {
            // 2 - If not found check for property
            configFilePath = System.getProperty(MatSciProperties.MATSCI_WORKER_CONFIGURATION_FILE.getKey());

            if (configFilePath == null || "".equals(configFilePath)) {
                // 3 - If not defined use default config path relative to scheduler home
                configFilePath = DEFAULT_CONFIG_PATH;
            }
        }

        File configFile = null;
        try {
            // Check if the config file exists at the specified path
            configFile = new File(configFilePath);
        } catch (Exception e) {
            System.out.println("MatSciConfigurationParser.getConfigs() --> path " + configFilePath);
            e.printStackTrace();
        }

        if (!configFile.isAbsolute()) {
            configFile = new File(schedHome + File.separator + configFilePath);
        }

        if (!configFile.exists() || !configFile.canRead()) {
            throw new FileNotFoundException(configFile + " not found, aborting...");
        }

        if (debug) {
            System.out.println("Parsing configuration file :" + configFile);
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
                        throw new IllegalArgumentException("In " + configFile +
                            ", version element must not be empty");
                    }
                    version = version.trim();
                    if (type.equals(Type.matlab)) {
                        if (!version.matches("^[1-9][\\d]*\\.[\\d]+$")) {
                            throw new IllegalArgumentException("In " + configFile +
                                ", version element must match XX.xx, received : " + version);
                        }
                    } else {
                        if (!version.matches("^([1-9][\\d]*\\.)*[\\d]+$")) {
                            throw new IllegalArgumentException("In " + configFile +
                                ", version element must match XX.xx.xx, received : " + version);
                        }
                    }
                    String home = installation.getChild("home").getText();
                    if ((home == null) || (home.trim().length() == 0)) {
                        throw new IllegalArgumentException("In " + configFile +
                            ", home element must not be empty");
                    }

                    home = home.trim().replaceAll("/", Matcher.quoteReplacement("" + os.fileSeparator()));
                    File filehome = new File(home);
                    checkDir(filehome, configFile);

                    String bindir = installation.getChild("bindir").getText();
                    if ((bindir == null) || (bindir.trim().length() == 0)) {
                        throw new IllegalArgumentException("In " + configFile +
                            ", bindir element must not be empty");
                    }
                    bindir = bindir.trim().replaceAll("/", Matcher.quoteReplacement("" + os.fileSeparator()));
                    File filebin = new File(filehome, bindir.trim());
                    checkDir(filebin, configFile);

                    String command = installation.getChild("command").getText();
                    if ((command == null) || (command.trim().length() == 0)) {
                        throw new IllegalArgumentException("In " + configFile +
                            ", command element must not be empty");
                    }
                    command = command.trim();
                    File filecommand = new File(filebin, command);
                    checkFile(filecommand, configFile, true);

                    if (type.equals(Type.matlab)) {
                        configs.add(new MatlabEngineConfig(home, version, bindir, command));
                    } else {
                        configs.add(new ScilabEngineConfig(home, version, bindir, command));
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

    protected static void checkDir(File dir, File conf) {
        if (!dir.exists()) {
            System.out.println("In " + conf + ", " + dir + " doesn't exist");
        }
        if (!dir.isDirectory()) {
            System.out.println("In " + conf + ", " + dir + " is not a directory");
        }
        if (!dir.canRead()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            System.out.println("In " + conf + ", " + dir + " is not readable");
        }
    }

    protected static void checkFile(File file, File conf, boolean executable) throws Exception {
        if (!file.exists()) {
            System.out.println("In " + conf + ", " + file + " doesn't exist");
        }
        if (!file.isFile()) {
            System.out.println("In " + conf + ", " + file + " is not a file");
        }
        if (!file.canRead()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            System.out.println("In " + conf + ", " + file + " is not readable");
        }
    }
}
