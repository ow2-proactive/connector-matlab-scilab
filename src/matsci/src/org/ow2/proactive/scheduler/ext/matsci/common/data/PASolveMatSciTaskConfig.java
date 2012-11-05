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
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.matsci.common.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * PASolveMatSciTaskConfig
 *
 * @author The ProActive Team
 */
public class PASolveMatSciTaskConfig implements Serializable {

    /**
     * Matlab or Scilab source files
     */
    private List<PASolveFile> sourceFiles = new ArrayList<PASolveFile>();

    /**
     * File storing the Matlab or Scilab environment
     */
    private PASolveFile environmentFile = null;

    /**
     * input Variables file (storing the function parameter)
     */
    private PASolveFile inputVariablesFile = null;

    /**
     * input Variables file for composed task
     */
    private PASolveFile composedinputVariablesFile = null;

    /**
     * ouput Variables file (storing the function return)
     */
    private PASolveFile outputVariablesFile = null;

    /**
     * user input files
     */
    private List<PASolveFile> inputFiles = new ArrayList<PASolveFile>();

    /**
     * user output files
     */
    private List<PASolveFile> outputFiles = new ArrayList<PASolveFile>();

    /**
     * url of custom selection Script
     */
    private String customScriptUrl = null;

    /**
     * Is the custom selection script static ?
     */
    private boolean staticScript = false;

    /**
     * Parameters of the custom script
     */
    private String customScriptParams = null;

    /**
     * task description
     */
    private String description = null;

    /**
     * Matlab/Scilab input code
     */
    private String inputScript = null;

    /**
     * Matlab/Scilab main code
     */
    private String mainScript = null;

    /**
     * topology in use
     */
    private MatSciTopology topology = null;

    /**
     * number of nodes needed
     */
    private int nbNodes = 1;

    /**
     * threshold proximity for topology
     */
    private long thresholdProximity = 0;

    public PASolveMatSciTaskConfig() {

    }

    public PASolveFile getEnvironmentFile() {
        return environmentFile;
    }

    public void setEnvironmentFile(PASolveFile file) {
        environmentFile = file;
    }

    public PASolveFile getInputVariablesFile() {
        return inputVariablesFile;
    }

    public void setInputVariablesFile(PASolveFile inputVariablesFile) {
        this.inputVariablesFile = inputVariablesFile;
    }

    public void setInputVariablesFile(String pathname) {
        this.inputVariablesFile = new PASolveFile(pathname);
    }

    public void setInputVariablesFile(String relPath, String name) {
        this.inputVariablesFile = new PASolveFile(relPath, name);
    }

    public PASolveFile getComposedInputVariablesFile() {
        return composedinputVariablesFile;
    }

    public void setComposedInputVariablesFile(PASolveFile composedinputVariablesFile) {
        this.composedinputVariablesFile = composedinputVariablesFile;
    }

    public void setComposedInputVariablesFile(String pathname) {
        this.composedinputVariablesFile = new PASolveFile(pathname);
    }

    public void setComposedInputVariablesFile(String relPath, String name) {
        this.composedinputVariablesFile = new PASolveFile(relPath, name);
    }

    public PASolveFile getOutputVariablesFile() {
        return outputVariablesFile;
    }

    public void setOutputVariablesFile(PASolveFile outputVariablesFile) {
        this.outputVariablesFile = outputVariablesFile;
    }

    public void setOutputVariablesFile(String pathname) {
        this.outputVariablesFile = new PASolveFile(pathname);
    }

    public void setOutputVariablesFile(String relPath, String name) {
        this.outputVariablesFile = new PASolveFile(relPath, name);
    }

    public void setOutputVariablesFile(String rootDir, String relPath, String name) {
        this.outputVariablesFile = new PASolveFile(relPath, name);
        this.outputVariablesFile.setRootDirectory(rootDir);
    }

    public List<PASolveFile> getInputFiles() {
        return inputFiles;
    }

    public void addInputFile(PASolveFile file) {
        this.inputFiles.add(file);
    }

    public void addInputFile(String pathname, DSSource source) {
        PASolveFile file = new PASolveFile(pathname);
        file.setSource(source);
        this.inputFiles.add(file);
    }

    public void addInputFile(String relpath, String name, DSSource source) {
        PASolveFile file = new PASolveFile(relpath, name);
        file.setSource(source);
        this.inputFiles.add(file);
    }

    public void setInputFiles(PASolveFile[] inputFiles) {
        this.inputFiles = Arrays.asList(inputFiles);
    }

    public List<PASolveFile> getOutputFiles() {
        return outputFiles;
    }

    public void addOutputFile(PASolveFile file) {
        this.outputFiles.add(file);
    }

    public void addOutputFile(String pathname, DSSource dest) {
        PASolveFile file = new PASolveFile(pathname);
        file.setDestination(dest);
        this.outputFiles.add(file);
    }

    public void addOutputFile(String relpath, String name, DSSource dest) {
        PASolveFile file = new PASolveFile(relpath, name);
        file.setDestination(dest);
        this.outputFiles.add(file);
    }

    public void setOutputFiles(PASolveFile[] outputFiles) {
        this.outputFiles = Arrays.asList(outputFiles);
    }

    public String getCustomScriptUrl() {
        return customScriptUrl;
    }

    public void setCustomScriptUrl(String customScriptUrl) {
        this.customScriptUrl = customScriptUrl;
    }

    public boolean isStaticScript() {
        return staticScript;
    }

    public void setStaticScript(boolean staticScript) {
        this.staticScript = staticScript;
    }

    public String getCustomScriptParams() {
        return customScriptParams;
    }

    public void setCustomScriptParams(String customScriptParams) {
        this.customScriptParams = customScriptParams;
    }

    public String getInputScript() {
        return inputScript;
    }

    public void setInputScript(String inputScript) {
        this.inputScript = inputScript;
    }

    public String getMainScript() {
        return mainScript;
    }

    public void setMainScript(String mainScript) {
        this.mainScript = mainScript;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PASolveFile> getSourceFiles() {
        return sourceFiles;
    }

    public void addSourceFile(String pathName) {
        sourceFiles.add(new PASolveFile(pathName));
    }

    public void addSourceFile(String relativePath, String name) {
        sourceFiles.add(new PASolveFile(relativePath, name));
    }

    public void addSourceFile(PASolveFile src) {
        sourceFiles.add(src);
    }

    public MatSciTopology getTopology() {
        return topology;
    }

    public void setTopology(MatSciTopology topology) {
        this.topology = topology;
    }

    public void setTopology(String topology) {
        this.topology = MatSciTopology.getTopology(topology);
    }

    public int getNbNodes() {
        return nbNodes;
    }

    public void setNbNodes(double nbNodes) {
        this.nbNodes = (int) Math.round(nbNodes);
    }

    public long getThresholdProximity() {
        return thresholdProximity;
    }

    public void setThresholdProximity(double thresholdProximity) {
        this.thresholdProximity = Math.round(thresholdProximity);
    }
}
