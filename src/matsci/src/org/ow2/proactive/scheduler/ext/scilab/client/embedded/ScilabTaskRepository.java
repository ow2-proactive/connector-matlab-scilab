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
package org.ow2.proactive.scheduler.ext.scilab.client.embedded;

import org.ow2.proactive.scheduler.ext.matsci.client.embedded.MatSciTaskRepository;

import java.io.File;


/**
 * ScilabTaskRepository
 *
 * @author The ProActive Team
 */
public class ScilabTaskRepository extends MatSciTaskRepository {
    /**
     * Name of the jobs backup file
     */
    public static final String SCILAB_EMBEDDED_JOBS_FILE_NAME = "ScilabEmbeddedJobs";

    // in Scilab the temp dir is a subdir of the system temp dir
    protected static final File TMPDIR = new File(System.getProperty("java.io.tmpdir")).getParentFile();

    public static final File SCILAB_EMBEDDED_JOBS_FILE = new File(TMPDIR, SCILAB_EMBEDDED_JOBS_FILE_NAME);

    private ScilabTaskRepository() {
        init();
    }

    @Override
    protected File getMatSciTaskRepFile() {
        return new File(TMPDIR, SCILAB_EMBEDDED_JOBS_FILE_NAME);
    }

    public static MatSciTaskRepository getInstance() {
        if (instance == null) {
            instance = new ScilabTaskRepository();
        }
        return instance;
    }
}
