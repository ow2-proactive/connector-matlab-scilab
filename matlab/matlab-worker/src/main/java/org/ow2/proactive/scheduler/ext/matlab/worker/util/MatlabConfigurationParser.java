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
package org.ow2.proactive.scheduler.ext.matlab.worker.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Level;
import org.ow2.proactive.scheduler.ext.matsci.worker.properties.MatSciProperties;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciConfigurationParser;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciEngineConfig;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * MatlabConfigurationParser
 *
 * @author The ProActive Team
 **/
public class MatlabConfigurationParser extends MatSciConfigurationParser {

    ArrayList<String> matlabConfigPaths;

    public MatlabConfigurationParser(boolean isDebug) {
        try {
            // Set the log4j level according to the config
            if (isDebug)
                logger.setLevel(Level.DEBUG);

            matlabConfigPaths = new ArrayList<String>();
            matlabConfigPaths.add(TMPDIR + FS + "MatlabWorkerConfiguration.xml");
            matlabConfigPaths.add("addons/client/MatlabWorkerConfiguration.xml");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HashSet<MatSciEngineConfig> getConfigs() throws Exception {

        HashSet<MatSciEngineConfig> configs = new HashSet<MatSciEngineConfig>();

        ArrayList<String> configFilePaths = new ArrayList<String>();
        String prop = MatSciProperties.MATLAB_WORKER_CONFIGURATION_FILE.getValueAsString();
        if ((prop != null) && (prop.length() > 0)) {
            configFilePaths.add(0, prop);
        }
        prop = System.getProperty(MatSciProperties.MATLAB_WORKER_CONFIGURATION_FILE.getKey());
        if ((prop != null) && (prop.length() > 0)) {
            configFilePaths.add(0, prop);
        }
        configFilePaths.addAll(matlabConfigPaths);

        for (String path : configFilePaths) {
            HashSet<MatSciEngineConfig> confset = parseConfigFile(path);
            configs.addAll(confset);
        }
        return configs;
    }

    @Override
    protected NodeList getListInstallations(Element machineGroup) {
        return machineGroup.getElementsByTagName("matlab");
    }

    @Override
    protected boolean checkVersion(String version, File configFile) {
        if (!version.matches("^[1-9][\\d]*\\.[\\d]+$")) {
            logger.error("In " + configFile + ", version element must match XX.xx, received : " + version);
            return false;
        }
        return true;
    }

    @Override
    protected MatSciEngineConfig buildConfig(String home, String version, String binDir, String command, String arch) {
        return new MatlabEngineConfig(home, version, binDir, command, arch);
    }
}
