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
package functionaltests.scilab;/*
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * JIMSLogsParser
 * <p/>
 * class used to parse the logs printed by JIMS in enableTrace mode to check memory leaks.
 * <p/>
 * It displays the list of objects that were created and not released (with their id and line number).
 *
 * @author The ProActive Team
 */
public class JIMSLogsParser {

    private BufferedReader in;

    private PrintWriter out;

    private HashMap<Integer, String> leaks = new HashMap<Integer, String>();

    private HashSet<Integer> ids = new HashSet<Integer>();

    private HashMap<Integer, String> classes = new HashMap<Integer, String>();

    private HashMap<Integer, Integer> line_numbers = new HashMap<Integer, Integer>();

    private HashSet<Integer> array_creations = new HashSet<Integer>();

    public JIMSLogsParser(File logFile, File outFile) throws IOException {
        this.in = new BufferedReader(new FileReader(logFile));
        this.out = new PrintWriter(new FileWriter(outFile));
    }

    public void parse() throws IOException {
        String line = null;
        int line_nb = 0;
        while ((line = in.readLine()) != null) {
            line_nb++;
            if (line.startsWith("INFO: Object creation with id=")) {
                String rest = line.substring(30);
                int sp_ind = rest.indexOf(" ");
                if (sp_ind > 0) {
                    // we skip the object created during array creation
                    if (!array_creations.contains(line_nb - 4)) {
                        Integer id = Integer.parseInt(rest.substring(0, sp_ind));
                        ids.add(id);
                        line_numbers.put(id, line_nb);
                        int cl_ind = rest.indexOf("class=");
                        String clz = rest.substring(cl_ind + 6);
                        classes.put(id, clz);
                    }
                }
            } else if (line.startsWith("INFO: Remove object id=")) {
                String rest = line.substring(23);
                int sp_ind = rest.indexOf(" ");
                Integer id = Integer.parseInt(rest);
                ids.remove(id);
                classes.remove(id);
                line_numbers.remove(id);
            } else if (line.startsWith("INFO: Array creation: base class is ")) {
                array_creations.add(line_nb);
            }
        }
        in.close();
    }

    public void printResults() throws IOException {
        for (Integer id : ids) {
            out.println(id + ": " + classes.get(id) + "(" + line_numbers.get(id) + ")");
            System.out.println(id + ": " + classes.get(id) + "(" + line_numbers.get(id) + ")");
        }
        HashSet<String> classesFound = new HashSet<String>();
        for (Map.Entry<Integer, String> entry : classes.entrySet()) {
            if (classesFound.contains(entry.getValue())) {
                // leak !
                leaks.put(entry.getKey(), entry.getValue());
            } else {
                classesFound.add(entry.getValue());
            }
        }
        for (Integer id : leaks.keySet()) {
            out.println("LEAK!" + id + ": " + classes.get(id) + "(" + line_numbers.get(id) + ")");
            System.out.println("LEAK!" + id + ": " + classes.get(id) + "(" + line_numbers.get(id) + ")");
        }
        out.close();
    }

    public boolean hasLeaks() {
        return !leaks.isEmpty();
    }

    public boolean testok() throws Exception {
        parse();
        printResults();
        return !hasLeaks();
    }

    public static void main(String[] args) throws Exception {
        File logFile = new File(args[0]);
        File outFile = new File(args[1]);
        JIMSLogsParser parser = new JIMSLogsParser(logFile, outFile);
        if (parser.testok()) {
            System.out.println("No leak");
        } else {
            System.out.println("Leaks Found !");
        }

    }

}
