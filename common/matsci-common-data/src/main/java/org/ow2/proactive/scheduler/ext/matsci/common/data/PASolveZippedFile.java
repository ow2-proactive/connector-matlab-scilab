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
 * PASolveZippedFile
 *
 * @author The ProActive Team
 */
public class PASolveZippedFile extends PASolveFile {

    private static final long serialVersionUID = 13L;

    /**
     * Content if this zip (if applicable)
     */
    private List<String> zipContent = new ArrayList<String>();

    public PASolveZippedFile() {
        super();
    }

    public PASolveZippedFile(String pathName) {
        super(pathName);
    }

    public PASolveZippedFile(String relativePath, String name) {
        super(relativePath, name);
    }

    public PASolveZippedFile(String originalDirectory, String relativePath, String name) {
        super(originalDirectory, relativePath, name);
    }

    public List<String> getZipContent() {
        return zipContent;
    }

    public void setZipContent(String[] zipContent) {
        this.zipContent = Arrays.asList(zipContent);
    }

    public void addToZipContent(String file) {
        this.zipContent.add(file);
    }

}
