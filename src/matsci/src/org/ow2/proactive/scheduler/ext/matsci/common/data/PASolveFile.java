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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;


/**
 * PASolveFile
 * <p/>
 * Stores every info relative to a File for the MatSci Connector, some info have a meaning and are only available in the
 * space written in parenthesis
 *
 * @author The ProActive Team
 */
public class PASolveFile implements Serializable {

    private static final long serialVersionUID = 11;

    /**
     * Source Dataspace of this File, if applicable
     */
    private DSSource source = DSSource.AUTOMATIC;

    /**
     * Destination Dataspace of this File, if applicable
     */
    private DSSource destination = DSSource.AUTOMATIC;

    /**
     * Name of this file, with extension (ANY)
     */
    private String name;

    /**
     * Relative path of this file (relative to the current used space) (ANY)
     */
    private String relativePath;

    /**
     * Root directory
     */
    private File rootDirectory;

    public PASolveFile() {
    }

    public PASolveFile(String pathName) {
        this();
        File f = new File(pathName);
        setName(f.getName());
        setRelativePath(f.getParent());
    }

    public PASolveFile(String relativePath, String name) {
        this();
        setName(name);
        setRelativePath(relativePath);
    }

    public PASolveFile(String originalDirectory, String relativePath, String name) {
        this(name, relativePath);
        setRootDirectory(originalDirectory);
    }

    public DSSource getSource() {
        return source;
    }

    public void setSource(DSSource source) {
        this.source = source;
    }

    public DSSource getDestination() {
        return destination;
    }

    public void setDestination(DSSource destination) {
        this.destination = destination;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPortablePathName() {
        if (relativePath != null) {
            return relativePath + "/" + name;
        } else {
            return name;
        }
    }

    public String getFullPathName() {
        if (rootDirectory == null) {
            throw new UnsupportedOperationException("Root Directory must be defined");
        }
        File parent = null;
        if (relativePath != null) {
            parent = new File(rootDirectory, relativePath.replace("/", File.separator));
        } else {
            parent = rootDirectory;
        }
        String full = null;
        try {
            full = new File(parent, name).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return full;
    }

    public void setRelativePath(String path) {
        if (path != null) {
            if (path.contains("\\")) {
                this.relativePath = path.replace("\\", "/");
            } else {
                this.relativePath = path;
            }
        } else {
            relativePath = null;
        }
    }

    public String getRootDirectory() {
        return rootDirectory.getAbsolutePath();
    }

    public void setRootDirectory(String directory) {
        try {
            this.rootDirectory = new File(directory).getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRootDirectory(File directory) {
        try {
            this.rootDirectory = directory.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
