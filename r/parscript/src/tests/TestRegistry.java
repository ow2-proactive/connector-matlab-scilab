package tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ow2.parscript.WinRegistry;

/**
 * Tests arguments of a script task.
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestRegistry {
    
	@Test
    public void test() throws Exception {
		
		// if on windows		
		String value = WinRegistry.readString (
			    WinRegistry.HKEY_LOCAL_MACHINE, //HKEY			    
			   "SOFTWARE\\R-core\\R", //Key
			   "Current Version");                                              //ValueName
	
		System.out.println("Windows Distribution = " + value);			   
		    
    }	
}
