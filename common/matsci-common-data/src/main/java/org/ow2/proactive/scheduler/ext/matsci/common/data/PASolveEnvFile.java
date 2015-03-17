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
package org.ow2.proactive.scheduler.ext.matsci.common.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * PASolveEnvFile
 *
 * @author The ProActive Team
 */
public class PASolveEnvFile extends PASolveFile {

    private static final long serialVersionUID = 61L;

    private List<String> envGlobalNames = new ArrayList<String>();

    public PASolveEnvFile() {
        super();
    }

    public PASolveEnvFile(String pathName) {
        super(pathName);
    }

    public PASolveEnvFile(String relativePath, String name) {
        super(relativePath, name);
    }

    public PASolveEnvFile(String originalDirectory, String relativePath, String name) {
        super(originalDirectory, relativePath, name);
    }

    public List<String> getEnvGlobalNames() {
        return envGlobalNames;
    }

    public void setEnvGlobalNames(List<String> envGlobalNames) {
        this.envGlobalNames = envGlobalNames;
    }

    public void setEnvGlobalNames(String[] envGlobalNames) {
        if (envGlobalNames != null) {
            this.envGlobalNames = Arrays.asList(envGlobalNames);
        } else {
            this.envGlobalNames = new ArrayList<String>();
        }
    }
}
