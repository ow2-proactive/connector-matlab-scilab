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
package org.ow2.proactive.scheduler.ext.scilab.client.embedded;

import java.io.File;

import org.ow2.proactive.scheduler.ext.matsci.client.embedded.MatSciTaskRepository;


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
