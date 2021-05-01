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

import java.util.HashSet;

import org.apache.log4j.Level;
import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciEngineConfig;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciFinder;


public class MatlabFinder extends MatSciFinder {

    /**
     * the OS where this JVM is running *
     */
    private static OperatingSystem os = OperatingSystem.getOperatingSystem();

    public MatlabFinder(boolean isDebug) {

        // Set the log4j level according to the config
        if (isDebug)
            logger.setLevel(Level.DEBUG);
    }

    /**
     * Utility function to find Matlab
     */
    public MatSciEngineConfig findMatSci(String version_pref, String versionsRej, String versionMin, String versionMax,
            String versionArch, boolean debug) throws Exception {
        return findMatSci(version_pref, parseVersionRej(versionsRej), versionMin, versionMax, versionArch, debug);
    }

    /**
     * Utility function to find Matlab
     */
    public MatSciEngineConfig findMatSci(String version_pref, HashSet<String> versionsRej, String versionMin,
            String versionMax, String versionArch, boolean debug) throws Exception {

        HashSet<MatSciEngineConfig> confs = new MatlabConfigurationParser(debug).getConfigs();
        if (confs == null)
            return null;

        MatSciEngineConfig answer = chooseMatSciConfig(confs,
                                                       version_pref,
                                                       versionsRej,
                                                       versionMin,
                                                       versionMax,
                                                       versionArch);

        return answer;

    }

}
