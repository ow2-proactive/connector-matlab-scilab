/*
 * ################################################################
 *
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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.ext.matlab.worker.util;

import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciEngineConfigBase;


public class MatlabEngineConfig extends MatSciEngineConfigBase {

    private static final long serialVersionUID = 11;

    private static OperatingSystem os = OperatingSystem.getOperatingSystem();

    /**
     * The home dir of Matlab on this machine
     */
    private String home = null;

    /**
     * The path to matlab bin dir
     */
    private String binDir = null;

    /**
     * Version of Matlab
     */
    private String version;

    /**
     * the name of the Matlab command on this machine
     */
    private String command = null;

    private String arch;

    private static final String nl = System.getProperty("line.separator");

    public MatlabEngineConfig(String home, String version, String binDir, String command, String arch) {
        this.home = home.replaceAll("" + '\u0000', "");

        this.version = version.replaceAll("" + '\u0000', "");
        this.binDir = binDir.replaceAll("" + '\u0000', "");
        this.command = command.replaceAll("" + '\u0000', "");
        this.arch = arch.replaceAll("" + '\u0000', "");
    }

    /**
     * returns the home dir of Matlab
     *
     * @return home dir
     */
    public String getHome() {
        return home;
    }

    public String getBinDir() {
        return binDir;
    }

    /**
     * Returns the current matlab version.
     *
     * @return matlab version
     */
    public String getVersion() {
        return version;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getFullCommand() {
        return home + os.fileSeparator() + binDir + os.fileSeparator() + command;
    }

    /**
     * Returns the name of the matlab command.
     *
     * @return returns the String containing the name of the Matlab command
     */
    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "Matlab home : " + home + nl + "Matlab version : " + version + nl +
            "Matlab bin directory  : " + binDir + nl + "Matlab command name : " + command + nl +
            "Matlab arch : " + arch + nl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MatlabEngineConfig that = (MatlabEngineConfig) o;

        if (arch != null ? !arch.equals(that.arch) : that.arch != null)
            return false;
        if (binDir != null ? !binDir.equals(that.binDir) : that.binDir != null)
            return false;
        if (command != null ? !command.equals(that.command) : that.command != null)
            return false;
        if (home != null ? !home.equals(that.home) : that.home != null)
            return false;
        if (version != null ? !version.equals(that.version) : that.version != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = home != null ? home.hashCode() : 0;
        result = 31 * result + (binDir != null ? binDir.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (command != null ? command.hashCode() : 0);
        result = 31 * result + (arch != null ? arch.hashCode() : 0);
        return result;
    }
}
