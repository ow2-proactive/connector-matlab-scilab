package org.ow2.parscript;

import static javax.script.ScriptEngine.NAME;

import java.io.File;
import java.io.FileNotFoundException;
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
	public static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
	public static final boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

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
			try {
				this.setLibraryPath();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return PARScriptEngine.create();
	}
	
	/** Tries to locate from registry or usual paths then adds rhome to the library path */
	private void setLibraryPath() {
		String rHome = System.getenv("R_HOME");
		boolean isBlank = StringUtils.isBlank(rHome);
		if (isWindows) {
			// On Windows try to locate from Windows Registry
			if (isBlank) {
				try {
					rHome = WinRegistry.readString(
							WinRegistry.HKEY_LOCAL_MACHINE, // HKEY
							"SOFTWARE\\R-core\\R", // Key
							"InstallPath");
					if (!(new File(rHome).exists())) {
						throw new FileNotFoundException("The path " + rHome + " given by the Windows Registry key does not exists");
					}
					Environment.setenv("R_HOME", rHome, true);
				} catch (Exception e) {
					throw new IllegalStateException("Unable to locate R homedir from Windows Registry, it seems R is not installed, please define the R_HOME env variable", e);
				}
			}
			dynamicAddLibraryPathWindows(rHome);
		} else if (isMac) {
			// On Mac try to locate from usual install path
			if (isBlank) {
				rHome = "/Library/Frameworks/R.framework/Resources";
				try {
					if (!(new File(rHome).exists())) {
						throw new FileNotFoundException("The usual " + rHome + " path does not exists");
					}
					Environment.setenv("R_HOME", rHome, true);
				} catch (Exception e) {
					throw new IllegalStateException("Unable to locate R homedir, the R_HOME env variable must be defined", e);
				}
			}
			dynamicAddLibraryPathLinuxMac(rHome, "library");
		} else {
			// On Linux try to locate from usual install path
			if (isBlank) {
				rHome = "/usr/lib/R";
				try {
					if (!(new File(rHome).exists())) {
						throw new FileNotFoundException("The usual " + rHome + " path does not exists");
					}
					Environment.setenv("R_HOME", rHome, true);
				} catch (Exception e) {
					throw new IllegalStateException("Unable to locate R homedir, the R_HOME env variable must be defined", e);
				}
			}			
			dynamicAddLibraryPathLinuxMac(rHome, "site-library");
		}
	}

	private static void dynamicAddLibraryPathWindows(final String rHome) {
		String fs = java.io.File.separator;
		// Get the architecture of the jvm not the os
		String sunArchDataModel = System.getProperty("sun.arch.data.model");
		String rLibraryPath = rHome + fs + "bin" + fs;
		String packagesLibraryPath = rHome + fs + "library";
		// If R_LIBS env var is defined locate rJava there
		String rLibs = System.getenv("R_LIBS");
		if (!StringUtils.isBlank(rLibs)) {
			packagesLibraryPath = rLibs;
		}
		String rJavaPath = packagesLibraryPath + fs + "rJava";
		if (!new File(rJavaPath).exists()) {
			throw new IllegalStateException("Unable to locate rJava in " + rJavaPath + " the R_LIBS env variable must be defined");
		}
		String jriLibraryPath = rJavaPath + fs + "jri" + fs;
		// Use correct libraries depending on jvm architecture 
		if ("32".equals(sunArchDataModel)) {
			rLibraryPath += "i386";
			jriLibraryPath += "i386";
		} else if ("64".equals(sunArchDataModel)) {
			rLibraryPath += "x64";
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
			Environment.setenv("Path", varValue + File.pathSeparator + rLibraryPath, true);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Unable to add R lib to Path environment variable " + rLibraryPath, e);
		}
	}

	private static void dynamicAddLibraryPathLinuxMac(final String rHome, String defaultPackagesDirname) {
		String fs = java.io.File.separator;
		String packagesLibraryPath = rHome + fs + defaultPackagesDirname;
		// If R_LIBS env var is defined locate rJava there
		String rLibs = System.getenv("R_LIBS");
		if (!StringUtils.isBlank(rLibs)) {
			packagesLibraryPath = rLibs;
		}
		String rJavaPath = packagesLibraryPath + fs + "rJava";
		if (!new File(rJavaPath).exists()) {
			throw new IllegalStateException("Unable to locate rJava package in " + rJavaPath + " the R_LIBS env variable must be defined");
		}
		// Dynamically add to java library path
		String jriLibraryPath = rJavaPath + fs + "jri" + fs;
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