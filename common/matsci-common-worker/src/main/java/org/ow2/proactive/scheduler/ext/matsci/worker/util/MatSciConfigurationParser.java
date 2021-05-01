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
package org.ow2.proactive.scheduler.ext.matsci.worker.util;

import java.io.File;
import java.util.HashSet;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.ext.matsci.worker.properties.MatSciProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * MatSciConfigurationParser
 *
 * @author The ProActive Team
 */
public abstract class MatSciConfigurationParser {

    protected static String HOSTNAME;

    protected static String IP;

    protected static String TMPDIR;

    protected static String FS;

    protected static File schedHome;

    protected final Logger logger = Logger.getLogger(MatSciConfigurationParser.class);

    static {
        try {
            HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();
            IP = java.net.InetAddress.getLocalHost().getHostAddress();
            TMPDIR = System.getProperty("java.io.tmpdir");
            FS = File.separator;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static OperatingSystem os = OperatingSystem.getOperatingSystem();

    protected abstract NodeList getListInstallations(Element machineGroup);

    protected abstract boolean checkVersion(String version, File configFile);

    protected abstract MatSciEngineConfig buildConfig(String home, String version, String binDir, String command,
            String arch);

    protected MatSciEngineConfig parseInstallation(File configFile, Element installation) throws Exception {
        NodeList nl = installation.getElementsByTagName("version");
        if (nl.getLength() != 1) {
            logger.error("In " + configFile + ", version element must not be empty or duplicate");
            return null;
        }
        String version = ((Element) (nl.item(0))).getTextContent();
        if ((version == null) || (version.trim().length() == 0)) {
            logger.error("In " + configFile + ", version element must not be empty");
            return null;
        }
        version = version.trim();
        if (!checkVersion(version, configFile)) {
            return null;
        }
        nl = installation.getElementsByTagName("home");
        if (nl.getLength() != 1) {
            logger.error("In " + configFile + ", home element must not be empty");
            return null;
        }
        String home = ((Element) (nl.item(0))).getTextContent();
        if ((home == null) || (home.trim().length() == 0)) {
            logger.error("In " + configFile + ", home element must not be empty");
            return null;
        }

        home = home.trim().replaceAll("/", Matcher.quoteReplacement("" + os.fileSeparator()));
        File filehome = new File(home);
        if (!checkDir(filehome, configFile)) {
            return null;
        }
        nl = installation.getElementsByTagName("bindir");
        if (nl.getLength() != 1) {
            logger.error("In " + configFile + ", bindir element must not be empty");
            return null;
        }
        String bindir = ((Element) (nl.item(0))).getTextContent();
        if ((bindir == null) || (bindir.trim().length() == 0)) {
            logger.error("In " + configFile + ", bindir element must not be empty");
            return null;
        }
        bindir = bindir.trim().replaceAll("/", Matcher.quoteReplacement("" + os.fileSeparator()));
        File filebin = new File(filehome, bindir);
        checkDir(filebin, configFile);

        nl = installation.getElementsByTagName("command");
        if (nl.getLength() != 1) {
            logger.error("In " + configFile + ", command element must not be empty");
            return null;
        }
        String command = ((Element) (nl.item(0))).getTextContent();
        if ((command == null) || (command.trim().length() == 0)) {
            logger.error("In " + configFile + ", command element must not be empty");
            return null;
        }
        command = command.trim();
        File filecommand = new File(filebin, command);
        if (!checkFile(filecommand, configFile, true)) {
            return null;
        }

        nl = installation.getElementsByTagName("arch");
        if (nl.getLength() != 1) {
            logger.error("In " + configFile + ", arch element must not be empty");
            return null;
        }
        String arch = ((Element) (nl.item(0))).getTextContent();
        if ((arch == null) || (arch.trim().length() == 0)) {
            logger.error("In " + configFile + ", arch element must not be empty");
            return null;
        }
        arch = arch.trim();

        if (!(arch.equals("32") || arch.equals("64"))) {
            logger.error("In " + configFile + ", arch element must be 32 or 64 received : " + arch);
            return null;
        }

        MatSciEngineConfig conf = buildConfig(home, version, bindir, command, arch);
        logger.debug("Found : " + conf);

        return conf;
    }

    protected HashSet<MatSciEngineConfig> parseConfigFile(String path) throws Exception {

        HashSet<MatSciEngineConfig> configs = new HashSet<MatSciEngineConfig>();

        if (schedHome == null) {
            schedHome = MatSciProperties.findSchedulerHome();
        }

        File configFile = new File(path);

        // Check if the config file exists at the specified path
        if (!configFile.isAbsolute()) {
            configFile = new File(schedHome + File.separator + path);
        }

        if (!configFile.exists() || !configFile.canRead()) {
            logger.debug(configFile + " not found, skipping...");
            return configs;
        } else if (!configFile.getName().endsWith(".xml")) {
            logger.debug(configFile + " : unrecognized extension, skipping...");
            return configs;
        } else {
            logger.debug("Parsing configuration file :" + configFile);
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(configFile);
        Element racine = document.getDocumentElement();

        NodeList machineGroups = racine.getElementsByTagName("MachineGroup");

        for (int i = 0; i < machineGroups.getLength(); i++) {
            Node nNode = machineGroups.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element machineGroup = (Element) nNode;
                if (matchesHost(machineGroup)) {

                    NodeList listInstallations = getListInstallations(machineGroup);

                    for (int j = 0; j < listInstallations.getLength(); j++) {
                        Node n = listInstallations.item(j);
                        if (n instanceof Element) {
                            MatSciEngineConfig conf = parseInstallation(configFile, (Element) n);
                            if (conf != null) {
                                configs.add(conf);
                            }
                        }
                    }
                }
            }
        }
        return configs;
    }

    private static boolean matchesHost(Element machineGroup) {
        String hostmatch = machineGroup.getAttribute("hostname");
        if (hostmatch != null && !hostmatch.equals("")) {
            if (HOSTNAME.matches(hostmatch)) {
                return true;
            }
            return false;
        }
        String ipmatch = machineGroup.getAttribute("ip");
        if (ipmatch != null && !ipmatch.equals("")) {
            if (IP.matches(ipmatch)) {
                return true;
            }
            return false;
        }
        return false;

    }

    protected boolean checkDir(File dir, File conf) {
        if (!dir.exists()) {
            logger.error("In " + conf + ", " + dir + " doesn't exist");
            return false;
        }
        if (!dir.isDirectory()) {
            logger.error("In " + conf + ", " + dir + " is not a directory");
            return false;
        }
        if (!dir.canRead()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            logger.error("In " + conf + ", " + dir + " is not readable");
            return false;
        }
        return true;
    }

    protected boolean checkFile(File file, File conf, boolean executable) throws Exception {
        if (!file.exists()) {
            logger.error("In " + conf + ", " + file + " doesn't exist");
            return false;
        }
        if (!file.isFile()) {
            logger.error("In " + conf + ", " + file + " is not a file");
            return false;
        }
        if (!file.canRead()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            logger.error("In " + conf + ", " + file + " is not readable");
            return false;
        }
        if (executable && !file.canExecute()) {
            // When using RunAsMe, we cannot be sure the current user has the right permissions
            logger.error("In " + conf + ", " + file + " is not executable");
            return false;
        }
        return true;
    }
}
