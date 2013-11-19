package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ow2.parscript.PARScriptFactory;
import org.ow2.proactive.scripting.ScriptResult;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

/**
 * Tests R homedir detection from Windows registry.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestDetectFromRegistry {

	public static String TAG = "test.detect.registry";

	@Test
	public void test() throws Exception {
		if (!PARScriptFactory.isWindows) {
			return;
		}

		if (System.getProperty(TAG) == null) {
			// Spawn a new process without R_HOME in the env
			try {
				ProcessBuilder pb = new ProcessBuilder();
				pb.redirectErrorStream(true);
				pb.environment().remove("R_HOME");
				pb.directory(new File(System.getProperty("user.dir")));
				String fs = File.separator;
				String javaExe = System.getProperty("java.home") + fs + "bin"
						+ fs
						+ (PARScriptFactory.isWindows ? "java.exe" : "java");
				ArrayList<String> command = new ArrayList<String>();
				command.add(javaExe);
				command.add("-D" + TAG);
				command.add("-Djava.class.path="
						+ System.getProperty("java.class.path"));
				command.addAll(Arrays.asList(System.getProperty(
						"sun.java.command").split(" ")));
				pb.command(command);

				Process p = pb.start();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					System.out.println("> " + line);
				}
				in.close();
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			org.junit.Assert.assertNull("There should be no R_HOME env var",
					System.getenv("R_HOME"));

			// We suppose the R_HOME is undefined
			String rScript = "result=version[['os']]";
			SimpleScript ss = new SimpleScript(rScript,
					PARScriptFactory.ENGINE_NAME);
			TaskScript taskScript = new TaskScript(ss);
			ScriptResult<Serializable> res = taskScript.execute();

			org.junit.Assert
					.assertEquals(
							"The detection of R homedir from Windows Registry is broken",
							"mingw32", (String) res.getResult());
		}
	}
}
