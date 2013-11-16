package org.ow2.parscript;

import static javax.script.ScriptEngine.NAME;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;
import org.rosuda.jrs.RScriptFactory;

/**
 * R implementation of ScriptEngineFactory for the ProActive Scheduler Worker.
 * @author Activeeon Team
 */
public final class PARScriptFactory extends RScriptFactory {

	public static final String ENGINE_NAME = "parscript";

	@Override
	public String getEngineName() {
		return ENGINE_NAME;
	}
	
	@Override
	public List<String> getNames() {
		return Collections.singletonList(ENGINE_NAME);
	}

	@Override
	public Object getParameter(String key) {
		if(key == null)						return null;
		if(key.equals(ScriptEngine.ENGINE))	return getEngineName();
		if(key.equals(NAME))				return getNames().get(0);
		return super.getParameter(key);
	}

	@Override
	public ScriptEngine getScriptEngine() {		
		this.setLibraryPath();
		return PARScriptEngine.create();
	}
	
	private void setLibraryPath() {
		String rHome = System.getenv("R_HOME");
		if (StringUtils.isBlank(rHome)) {
			throw new IllegalStateException("Unable to locate R homedir, be sure the R_HOME env variable is defined");
		}
		
		// Check if the path to rJava is already setted
		String libPath = System.getProperty("java.library.path");
		if (libPath != null && libPath.contains("jri")) {
			return;
		}

		// Depending on the os, add the library path
		if (System.getProperty("os.name").startsWith("Windows")) {
			String fs = java.io.File.separator;
			String libraryPath = rHome + fs + "library" + fs + "rJava" + fs	+ "jri";
			try {
				PARScriptFactory.addLibraryPath(libraryPath);
			} catch (Exception e) {
				throw new IllegalStateException(
						"Unable to add jri to library path " + libraryPath, e);
			}
		} else {
			// TODO Linux here
		}
	}	
	
	/**
	* Adds the specified path to the java library path
	*
	* @param pathToAdd the path to add
	* @throws Exception
	*/
	public static void addLibraryPath(String pathToAdd) throws Exception {
	    final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
	    usrPathsField.setAccessible(true);
	 
	    //get array of paths
	    final String[] paths = (String[])usrPathsField.get(null);
	 
	    //check if the path to add is already present
	    for(String path : paths) {
	        if(path.equals(pathToAdd)) {
	            return;
	        }
	    }
	 
	    //add the new path
	    final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
	    newPaths[newPaths.length-1] = pathToAdd;
	    usrPathsField.set(null, newPaths);
	}
}