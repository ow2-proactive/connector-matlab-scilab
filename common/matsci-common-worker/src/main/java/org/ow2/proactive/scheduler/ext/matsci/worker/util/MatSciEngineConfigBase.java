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

import java.util.Scanner;

import org.objectweb.proactive.core.UniqueID;


/**
 * MatSciEngineConfigBase
 *
 * @author The ProActive Team
 */
public abstract class MatSciEngineConfigBase implements MatSciEngineConfig {

    protected static MatSciEngineConfigBase currentConf = null;

    public static void setCurrentConfiguration(MatSciEngineConfigBase conf) {
        currentConf = conf;
    }

    public static MatSciEngineConfigBase getCurrentConfiguration() {
        return currentConf;
    }

    public static int compareVersions(String version1, String version2) {
        Scanner s1 = new Scanner(version1);
        Scanner s2 = new Scanner(version2);
        s1.useDelimiter("\\.");
        s2.useDelimiter("\\.");

        while (s1.hasNextInt() && s2.hasNextInt()) {
            int v1 = s1.nextInt();
            int v2 = s2.nextInt();
            if (v1 < v2) {
                return -1;
            } else if (v1 > v2) {
                return 1;
            }
        }

        if (s1.hasNextInt())
            return 1; //v1 has an additional lower-level version number
        if (s2.hasNextInt())
            return -1; //v2 has an additional lower-level version number
        return 0;
    }

    public static String getNodeName() throws Exception {

        return UniqueID.getCurrentVMID().toString().replace('-', '_').replace(':', '_');
        //return PAActiveObject.getNode().getVMInformation().getName().replace('-', '_')+"_"+ PAActiveObject.getNode().getNodeInformation().getName().replace('-', '_');
    }

}
