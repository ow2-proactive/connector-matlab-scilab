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
package org.ow2.proactive.scheduler.ext.scilab.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveFile;
import org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveMatSciTaskConfig;


/**
 * PASolveScilabTaskConfig
 *
 * @author The ProActive Team
 */
public class PASolveScilabTaskConfig extends PASolveMatSciTaskConfig {

    /**
     * name of the scilab function used
     */
    private String functionName;

    /**
     * definition of the function (obsolete)
     */
    private String functionDefinition;

    /**
     * list of files used to store the definition of the function
     */
    private List<PASolveFile> functionVarFiles = new ArrayList<PASolveFile>();

    /**
     * name of the output variables used (obsolete)
     */
    private String outputs;

    public PASolveScilabTaskConfig() {

    }

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionDefinition() {
        return functionDefinition;
    }

    public void setFunctionDefinition(String functionDefinition) {
        this.functionDefinition = functionDefinition;
    }

    public List<PASolveFile> getFunctionVarFiles() {
        return functionVarFiles;
    }

    public void setFunctionVarFiles(PASolveFile[] functionVarFiles) {
        this.functionVarFiles = Arrays.asList(functionVarFiles);
    }

    public void addFunctionVarFile(PASolveFile file) {
        this.functionVarFiles.add(file);
    }

    public void addFunctionVarFile(String pathName) {
        functionVarFiles.add(new PASolveFile(pathName));
    }

    public void addFunctionVarFile(String relativePath, String name) {
        functionVarFiles.add(new PASolveFile(relativePath, name));
    }

}
