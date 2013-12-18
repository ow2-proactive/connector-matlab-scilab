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
		// Check if the path to rJava is already setted
		String libPath = System.getProperty("java.library.path");
		if (libPath == null || !libPath.contains("jri")) {
			this.setLibraryPath();
		}
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
				File usual = new File("/usr/lib/R");
				if (usual.exists()) {
					rHome = usual.getAbsolutePath();
					// Set the current process 'R_HOME' environment variable 
					try {
						Environment.setenv("R_HOME", rHome, true);
					} catch (Exception e) {
						throw new IllegalStateException(
								"Unable to set R_HOME environment variable to " + rHome, e);
					}
				}
			}
			if (rHome == null) {
				throw new IllegalStateException(
						"Unable to locate R homedir, be sure the R_HOME env variable is defined");
			}
		}
		// Add the library path
		if (isWindows) {
			dynamicAddLibraryPathWindows(rHome);
		} else {
			dynamicAddLibraryPathLinux(rHome);
		}
	}

	private static void dynamicAddLibraryPathWindows(final String rHome) {
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
		// Update the current process 'Path' environment variable
		try {
			String varValue = System.getenv("Path");
			Environment.setenv("Path", varValue + fs + rLibrarayPath, true);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Unable to add R lib to Path environment variable " + rLibrarayPath, e);
		}
	}
	
	private static void dynamicAddLibraryPathLinux(final String rHome) {
		String fs = java.io.File.separator;
		// Dynamically add to java library path
		String jriLibraryPath = rHome + fs + "site-library" + fs + "rJava" + fs + "jri" + fs;
		try {
			PARScriptFactory.addLibraryPath(jriLibraryPath);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Unable to add jri to library path " + jriLibraryPath, e);
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