package tests;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ow2.parscript.PARScriptFactory;
import org.ow2.proactive.scripting.ScriptResult;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

/**
 * Tests arguments of a script task.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestArgs {

	@Test
	public void test() throws Exception {
		String[] args = { "one", "two", "three" };
		String rScript = "result = args[3];";

		Map<String, Object> aBindings = Collections.singletonMap(
				TaskScript.ARGUMENTS_NAME, (Object) args);
		SimpleScript ss = new SimpleScript(rScript,
				PARScriptFactory.ENGINE_NAME);
		TaskScript taskScript = new TaskScript(ss);
		ScriptResult<Serializable> res = taskScript.execute(aBindings);

		Assert.assertEquals(
				"The arguments are not transfered by the engine to the script",
				args[2], (String) res.getResult());
	}
}
