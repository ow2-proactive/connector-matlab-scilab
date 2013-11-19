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
 * Implicit result test. The default value of an empty script is always true.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestResultDefault {

	@Test
	public void testResult() throws Exception {
		String rScript = "";

		SimpleScript ss = new SimpleScript(rScript,
				PARScriptFactory.ENGINE_NAME);
		TaskScript taskScript = new TaskScript(ss);
		ScriptResult<Serializable> res = taskScript.execute();

		org.junit.Assert
				.assertEquals(
						"The default result of an empty script is not set by the engine",
						Boolean.TRUE, (Boolean) res.getResult());
	}
}
