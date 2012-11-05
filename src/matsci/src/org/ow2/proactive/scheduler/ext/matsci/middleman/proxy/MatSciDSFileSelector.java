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
package org.ow2.proactive.scheduler.ext.matsci.middleman.proxy;

import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.dataspaces.vfs.selector.fast.SelectorUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * MatSciDSFileSelector
 *
 * @author The ProActive Team
 */
public class MatSciDSFileSelector implements FileSelector {

    public static final Logger logger_util = ProActiveLogger.getLogger(MatSciDSFileSelector.class);

    Set<String> includes = new HashSet<String>();
    Set<String> excludes = new HashSet<String>();

    public MatSciDSFileSelector() {

    }

    @Override
    public boolean includeFile(FileSelectInfo fileInfo) throws Exception {

        String buri = fileInfo.getBaseFolder().getURL().toString();
        String furi = fileInfo.getFile().getURL().toString();
        // we replace in a raw way the base uri (this replacement is not interpreted as a regex)
        String name = furi.replace(buri, "");
        // we remove any prepending slashes to the path remaining
        name = name.replaceFirst("/*", "");

        logger_util.debug("Checking file " + name + "(" + furi + ")");

        if (isIncluded(name)) {
            if (!isExcluded(name)) {
                logger_util.debug("File " + furi + " selected for copy.");
                return true;
            }
        }
        return false;
    }

    protected boolean isExcluded(String name) {
        if (excludes.contains(name))
            return true;

        for (String pattern : excludes) {
            if (SelectorUtils.matchPath(pattern, name)) {
                logger_util.debug("File " + name + " matches an exclude pattern");
                return true;
            }
        }
        return false;
    }

    protected boolean isIncluded(String name) {
        if (includes.contains(name))
            return true;

        for (String pattern : includes) {
            if (SelectorUtils.matchPath(pattern, name)) {
                return true;
            }
        }

        logger_util.debug("File " + name + " does not match any of the include patterns");

        return false;
    }

    @Override
    public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
        //
        return true;
    }

    public void addIncludes(Collection<String> files) {
        if (files != null)
            includes.addAll(files);
    }

    public void addExcludes(Collection<String> files) {
        if (files != null)
            excludes.addAll(files);
    }

    public Set<String> getIncludes() {
        return new HashSet<String>(includes);
    }

    public Set<String> getExcludes() {
        return new HashSet<String>(excludes);
    }

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes;
    }
}
