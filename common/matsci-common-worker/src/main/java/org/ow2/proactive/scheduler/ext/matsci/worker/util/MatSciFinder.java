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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.objectweb.proactive.utils.OperatingSystem;


/**
* MatSciFinder
*
* @author The ProActive Team
*/
public abstract class MatSciFinder {

    protected final Logger logger = Logger.getLogger(MatSciFinder.class);

    // the OS where this JVM is running
    private static OperatingSystem os = OperatingSystem.getOperatingSystem();

    private static MatSciFinder instance = null;

    public abstract MatSciEngineConfig findMatSci(String version_pref, HashSet<String> versionsRej, String versionMin,
            String versionMax, String versionArch, boolean debug) throws Exception;

    protected MatSciEngineConfig chooseMatSciConfig(HashSet<MatSciEngineConfig> configs, String version_pref,
            Set<String> versionsRej, String versionMin, String versionMax, String versionArch) {
        List<MatSciEngineConfig> selected = new ArrayList<MatSciEngineConfig>();
        logger.debug("Choosing config with version_pref=" + version_pref + ", versionRej=" + versionsRej +
                     ", versionMin=" + versionMin + ", versionMax=" + versionMax + ", versionArch=" + versionArch);
        for (MatSciEngineConfig conf : configs) {

            String version = conf.getVersion();
            logger.debug("Version : " + version + "(" + conf.getArch() + ")");

            if (versionsRej != null && !versionsRej.isEmpty() && versionsRej.contains(version)) {
                logger.debug("... rejected");
                continue;
            }
            if (versionArch != null && !versionArch.equalsIgnoreCase("any") && !versionArch.equals(conf.getArch())) {
                logger.debug("... architecture rejected");
                continue;
            }
            if (versionMin != null && MatSciEngineConfigBase.compareVersions(version, versionMin) < 0) {
                logger.debug("... too low");
                continue;
            }
            if (versionMax != null && MatSciEngineConfigBase.compareVersions(versionMax, version) < 0) {
                logger.debug("... too high");
                continue;
            }
            if (version_pref != null && version_pref.equals(version)) {
                logger.debug("... preferred");
                return conf;
            }
            logger.debug("... accepted");
            selected.add(conf);
        }
        if (selected.size() > 0) {
            return selected.get(0);
        }
        return null;
    }

    protected HashSet<String> parseVersionRej(String vrej) {
        HashSet<String> vrejSet = new HashSet<String>();
        if ((vrej != null) && (vrej.length() > 0)) {
            vrej = vrej.trim();
            String[] vRejArr = vrej.split("[ ,;]+");

            for (String rej : vRejArr) {
                if (rej != null) {
                    vrejSet.add(rej);
                }
            }
        }
        return vrejSet;
    }
}
