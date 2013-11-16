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
 * Basic PARScript tests.
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestOutput {
    
	@Test
    public void test() throws Exception {
		String stringToPrint = "Hello World";
    	String rScript = "print('"+stringToPrint+"')";    	
    	SimpleScript ss = new SimpleScript(rScript, PARScriptFactory.ENGINE_NAME);
    	TaskScript taskScript = new TaskScript(ss);
    	ScriptResult<Serializable> res = taskScript.execute();
    	org.junit.Assert.assertEquals("R Script output is broken", stringToPrint, res.getOutput());    	
    }	
}
