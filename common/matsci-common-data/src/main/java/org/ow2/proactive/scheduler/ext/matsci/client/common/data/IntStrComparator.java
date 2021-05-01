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
package org.ow2.proactive.scheduler.ext.matsci.client.common.data;

import java.io.Serializable;
import java.util.Comparator;


/**
 * IntStrComparator
 *
 * @author The ProActive Team
 */
public class IntStrComparator implements Comparator<String>, Serializable {

    public IntStrComparator() {

    }

    public int compare(String o1, String o2) {
        if (o1 == null && o2 == null)
            return 0;
        // assuming you want null values shown last
        if (o1 != null && o2 == null)
            return -1;
        if (o1 == null && o2 != null)
            return 1;
        int answer = 0;
        try {
            answer = Integer.parseInt(o1) - Integer.parseInt(o2);
        } catch (NumberFormatException e) {
            answer = o1.compareTo(o2);
        }
        return answer;
    }
}
