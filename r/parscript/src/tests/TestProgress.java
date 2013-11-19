package tests;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ow2.parscript.PARScriptFactory;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

/**
 * Test set_progress(x) method in R script.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestProgress {

	@Test
	public void test() throws Exception {
		int expectedProgress = 50;
		String rScript = "set_progress(" + expectedProgress + ");";

		AtomicInteger progress = new AtomicInteger();
		Map<String, Object> aBindings = Collections.singletonMap(
				TaskScript.PROGRESS_VARIABLE, (Object) progress);
		SimpleScript ss = new SimpleScript(rScript,
				PARScriptFactory.ENGINE_NAME);
		TaskScript taskScript = new TaskScript(ss);
		/* ScriptResult<Serializable> res = */taskScript.execute(aBindings);

		// System.out.println("Test ---> " + res.getOutput());
		// System.out.println("TestProgress.test1() ---> result " +
		// res.getResult());
		// System.out.println("TestProgress.test1() error ocured ---> " +
		// res.errorOccured());
		// System.out.println("TestProgress.test1() exception ---> " +
		// res.getException());

		org.junit.Assert.assertEquals(
				"The progress is incorrect, it seems the engine doesn't transmit "
						+ " the progress to the script as expected",
				expectedProgress, progress.intValue());
	}
}
