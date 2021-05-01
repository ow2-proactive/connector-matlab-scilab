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
package org.ow2.proactive.scheduler.ext.matlab.common.data;

import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciGlobalConfig;


/**
 * PASolveMatlabGlobalConfig configuration of a Matlab PAsolve Job (a job represents a call to PAsolve)
 *
 * @author The ProActive Team
 */
public class PASolveMatlabGlobalConfig extends PASolveMatSciGlobalConfig {

    /**
     * URL of the script used to check licenses
     */
    private String checkLicenceScriptUrl = null;

    /**
     * Options used when writing MAT files (binary Matlab files)
     */
    private String matFileOptions = null;

    /**
     * URL of the license server proxy
     */
    private String licenseSaverURL = null;

    /**
     * Name of the matlab function used to keep checked out tokens alive. This function is executed periodically by matlab.
     */
    private String keepaliveCallbackFunctionName = null;

    /**
     * Name of the matlab function used to check out toolboxes tokens right after matlab starts.
     */
    private String checktoolboxesFunctionName = null;

    /**
     * Do we use Matlab Control or standard matlab batch mode
     */
    private boolean useMatlabControl = false;

    public PASolveMatlabGlobalConfig() {
    }

    public String getCheckLicenceScriptUrl() {
        return checkLicenceScriptUrl;
    }

    public void setCheckLicenceScriptUrl(String checkLicenceScript) {
        this.checkLicenceScriptUrl = checkLicenceScript;
    }

    public String getMatFileOptions() {
        return matFileOptions;
    }

    public void setMatFileOptions(String matFileOptions) {
        this.matFileOptions = matFileOptions;
    }

    public String getLicenseSaverURL() {
        return licenseSaverURL;
    }

    public void setLicenseSaverURL(String licenseSaverURL) {
        this.licenseSaverURL = licenseSaverURL;
    }

    public String getKeepaliveCallbackFunctionName() {
        return keepaliveCallbackFunctionName;
    }

    public void setKeepaliveCallbackFunctionName(String keepaliveCallbackFunctionName) {
        this.keepaliveCallbackFunctionName = keepaliveCallbackFunctionName;
    }

    public String getChecktoolboxesFunctionName() {
        return checktoolboxesFunctionName;
    }

    public void setChecktoolboxesFunctionName(String checktoolboxesFunctionName) {
        this.checktoolboxesFunctionName = checktoolboxesFunctionName;
    }

    public boolean isUseMatlabControl() {
        return useMatlabControl;
    }

    public void setUseMatlabControl(boolean useMatlabControl) {
        this.useMatlabControl = useMatlabControl;
    }
}
