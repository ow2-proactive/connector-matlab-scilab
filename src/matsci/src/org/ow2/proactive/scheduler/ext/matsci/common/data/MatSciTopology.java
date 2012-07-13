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
package org.ow2.proactive.scheduler.ext.matsci.common.data;

/**
 * MatSciTopology a duplicate of the Resource Manager topology definitions, used inside MatSci
 *
 * @author The ProActive Team
 */
public enum MatSciTopology {

    ARBITRARY("arbitrary"),

    BEST_PROXIMITY("bestProximity"),

    SINGLE_HOST("singleHost"),

    SINGLE_HOST_EXCLUSIVE("singleHostExclusive"),

    MULTIPLE_HOSTS_EXCLUSIVE("multipleHostsExclusive"),

    DIFFERENT_HOSTS_EXCLUSIVE("differentHostsExclusive"),

    THRESHHOLD_PROXIMITY("thresholdProximity");

    private String ref;

    private MatSciTopology(String str) {
        this.ref = str;
    }

    public String toString() {
        return ref;
    }

    public static MatSciTopology getTopology(String str) {
        for (MatSciTopology s : MatSciTopology.values()) {
            if (s.toString().equals(str)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Wrong topology : " + str);
    }
}
