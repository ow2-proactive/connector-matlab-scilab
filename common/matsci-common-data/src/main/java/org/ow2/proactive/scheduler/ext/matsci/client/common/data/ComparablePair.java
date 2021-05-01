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

/**
 * ComparablePair
 *
 * @author The ProActive Team
 */
public class ComparablePair implements Comparable {
    private int x;

    private int y;

    public ComparablePair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComparablePair))
            return false;

        ComparablePair comparablePair = (ComparablePair) o;

        if (x != comparablePair.x)
            return false;
        if (y != comparablePair.y)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    public int compareTo(Object o) {
        if (this == o)
            return 0;
        if (!(o instanceof ComparablePair))
            throw new IllegalArgumentException();

        ComparablePair comparablePair = (ComparablePair) o;

        if (x < comparablePair.x)
            return -1;
        if (x > comparablePair.x)
            return 1;
        if (y < comparablePair.y)
            return -1;
        if (y > comparablePair.y)
            return -1;
        return 0;

    }
}
