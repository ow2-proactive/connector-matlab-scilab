package tests;

import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ow2.parscript.PARScriptFactory;
import org.ow2.proactive.scripting.ScriptResult;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

/**
 * Explicit result test.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestResultExplicit {

	@Test
	public void testResult() throws Exception {
		String rScript = "result=FALSE";

		SimpleScript ss = new SimpleScript(rScript,
				PARScriptFactory.ENGINE_NAME);
		TaskScript taskScript = new TaskScript(ss);
		ScriptResult<Serializable> res = taskScript.execute();

		org.junit.Assert
				.assertEquals(
						"The result variable declared explicitely in the script is not used as the result of the script by the engine",
						Boolean.FALSE, (Boolean) res.getResult());
	}
}
