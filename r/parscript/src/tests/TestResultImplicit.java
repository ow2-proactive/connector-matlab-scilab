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
 * Implicit result test.
 * The result of the script is the last valid expression.
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestResultImplicit {
    
	@Test
    public void testResult() throws Exception {
		double expectedResult = 1d;
    	String rScript = "v="+expectedResult;
    	
    	SimpleScript ss = new SimpleScript(rScript, PARScriptFactory.ENGINE_NAME);
    	TaskScript taskScript = new TaskScript(ss);
    	ScriptResult<Serializable> res = taskScript.execute();
    	
    	org.junit.Assert.assertEquals("The implicit result is not used by the engine", (Double)expectedResult, (Double)res.getResult());
    }		
}
