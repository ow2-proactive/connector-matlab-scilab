/*
 *  *
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
 *  * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.common.util;

import org.ow2.proactive.scheduler.ext.matsci.client.common.data.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


/**
 * BitMatrix
 *
 * @author The ProActive Team
 */
public class BitMatrix {

    private ArrayList<BitSet> lines;
    private int ncolumns;

    private BitMatrix(ArrayList<BitSet> lines) {
        this.lines = lines;
    }

    public BitMatrix(int nlines, int ncolumns) {
        this.lines = new ArrayList<BitSet>(nlines);
        for (int i = 0; i < nlines; i++) {
            this.lines.add(new BitSet(ncolumns));
        }
        this.ncolumns = ncolumns;
    }

    public int getNbLines() {
        return this.lines.size();
    }

    public int getNbColumns() {
        return ncolumns;
    }

    public void set(int line, int col, boolean value) {
        this.lines.get(line).set(col, value);
    }

    public boolean get(int line, int col) {
        return this.lines.get(line).get(col);
    }

    public boolean isTrue(int line) {
        return this.lines.get(line).cardinality() == ncolumns;
    }

    public boolean isTrue() {
        boolean istrue = true;
        for (int i = 0; (i < lines.size()) && istrue; i++) {
            BitSet line = lines.get(i);
            istrue = (line.cardinality() == ncolumns);
        }
        return istrue;
    }

    public boolean areLinesTrue(List<Integer> xlines) {
        boolean istrue = true;
        for (int i = 0; (i < xlines.size()) && istrue; i++) {
            istrue = isTrue(xlines.get(i));
        }
        return istrue;
    }

    public int isAnyLineTrue(List<Integer> xlines) {
        boolean istrue = false;
        int line_ind = -1;
        for (int i = 0; (i < xlines.size()) && !istrue; i++) {
            istrue = isTrue(xlines.get(i));
            if (istrue) {
                line_ind = i;
            }
        }
        return line_ind;
    }

    public List<Boolean> get(List<Integer> xlines) {
        ArrayList<Boolean> answer = new ArrayList<Boolean>();
        for (int i = 0; (i < xlines.size()); i++) {
            BitSet line = lines.get(i);
            answer.add(line.cardinality() == ncolumns);
        }
        return answer;
    }

    public ArrayList<Boolean> not(List<Integer> xlines) {
        ArrayList<Boolean> answer = new ArrayList<Boolean>();
        for (int i = 0; (i < xlines.size()); i++) {
            BitSet line = lines.get(i);
            answer.add(line.cardinality() < ncolumns);
        }
        return answer;
    }

    public boolean isTrue(List<Pair<Integer, Integer>> indices) {
        boolean istrue = true;
        for (int i = 0; (i < indices.size()) && istrue; i++) {
            Pair<Integer, Integer> index = indices.get(i);
            istrue = (istrue && (lines.get(index.getX()).get(index.getY())));
        }
        return istrue;
    }

    public String toString() {
        return lines.toString();
    }

}
