package org.ow2.parscript;

import static javax.script.ScriptEngine.NAME;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;
import org.ow2.proactive.scheduler.util.process.Environment;
import org.rosuda.jrs.RScriptFactory;

/**
 * R implementation of ScriptEngineFactory for the ProActive Scheduler Worker.
 * 
 * @author Activeeon Team
 */
public final class PARScriptFactory extends RScriptFactory {
	public static final String ENGINE_NAME = "parscript";
	public static final boolean isWindows = System.getProperty("os.name")
			.startsWith("Windows");

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
		if (key == null)
			return null;
		if (key.equals(ScriptEngine.ENGINE))
			return getEngineName();
		if (key.equals(NAME))
			return getNames().get(0);
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
			if (isWindows) {
				try {
					rHome = WinRegistry.readString(
							WinRegistry.HKEY_LOCAL_MACHINE, // HKEY
							"SOFTWARE\\R-core\\R", // Key
							"InstallPath");
				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException(
							"Unable to locate R homedir, it seems R is not installed", e);
				}
			} else {
				// Nothing to do on Linux, maybe check in path ...
			}
			if (rHome == null) {
				throw new IllegalStateException(
						"Unable to locate R homedir, be sure the R_HOME env variable is defined");
			}
		}
		// Check if the path to rJava is already setted
		String libPath = System.getProperty("java.library.path");
		if (libPath != null && libPath.contains("jri")) {
			return;
		}
		// Add the library path
		if (isWindows) {
			this.dynamicAddLibraryPath(rHome, "Path");
		} else {
			this.dynamicAddLibraryPath(rHome, "LD_LIBRARY_PATH");
		}
	}

	private void dynamicAddLibraryPath(final String rHome, final String libPathVarName) {				
		String fs = java.io.File.separator;
		// Get the architecture of the jvm not the os
		String sunArchDataModel = System.getProperty("sun.arch.data.model");
		String rLibrarayPath = rHome + fs + "bin" + fs;
		String jriLibraryPath = rHome + fs + "library" + fs + "rJava" + fs + "jri" + fs;
		// Use correct libraries depending on jvm architecture 
		if ("32".equals(sunArchDataModel)) {
			rLibrarayPath += "i386";
			jriLibraryPath += "i386";
		} else if ("64".equals(sunArchDataModel)) {
			rLibrarayPath += "x64";
			jriLibraryPath += "x64";
		}
		// Dynamically add to java library path
		try {
			PARScriptFactory.addLibraryPath(jriLibraryPath);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Unable to add jri to library path " + jriLibraryPath, e);
		}
		// Update the current process 'libPathVarName' environment variable
		try {
			String varValue = System.getenv(libPathVarName);
			Environment.setenv(libPathVarName, varValue + File.pathSeparator + rLibrarayPath, true);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Unable to add R lib to " + libPathVarName + " environment variable " + rLibrarayPath, e);
		}
	}	

	/**
	 * Adds the specified path to the java library path
	 * 
	 * @param pathToAdd
	 *            the path to add
	 * @throws Exception
	 */
	public static void addLibraryPath(String pathToAdd) throws Exception {
		final Field usrPathsField = ClassLoader.class
				.getDeclaredField("usr_paths");
		usrPathsField.setAccessible(true);

		// get array of paths
		final String[] paths = (String[]) usrPathsField.get(null);

		// check if the path to add is already present
		for (String path : paths) {
			if (path.equals(pathToAdd)) {
				return;
			}
		}

		// add the new path
		final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
		newPaths[newPaths.length - 1] = pathToAdd;
		usrPathsField.set(null, newPaths);
	}
}