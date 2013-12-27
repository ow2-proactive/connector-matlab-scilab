/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Integration test for the PARConnector with Scheduler 3.4.0
// 
// - builds the PARConnector
// - copies the contents to the scheduler's home addons directory
// - starts the rm+scheduler+4nodes using the standard start-server.js script
// - runs the integration test that uses PARConnector api functions and submit jobs
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import groovy.util.GroovyTestSuite
import junit.textui.TestRunner

class TestIntegration extends GroovyTestCase {

	def userDir = new File(System.getProperty('user.dir'));
	// Check that the current dir is 'r' or its parent
	def homeDir = userDir.getName() == 'r' ? userDir : new File(userDir, 'r'); 

	def fs = File.separator
	def parConnectorDir = new File(homeDir, 'PARConnector');
	def schedHome = System.getenv()['SCHEDULER_340']
	def rHome = System.getenv()['R_HOME']
	def rLibraryDir = new File(rHome,'library') // check if on linux it works
	def rLibraryPath = rLibraryDir.getAbsolutePath()
	// todo check for rJava
	def testsDir = new File(homeDir,'tests')	
	def newEnv = [];
	def paArch = System.getenv()['ProgramFiles(x86)'] != null ? 'x64' : 'i386'
	def rExe = rHome+fs+'bin'+fs+paArch+fs+'R.exe'
	def distDir = new File(homeDir, 'dist');
	def schedProcess

	// Executes a command and redirects stdout/stderr
	def run = {command, env, dir ->
		println 'Running ' + command.join(" ")
		def proc = command.execute(env, dir)
		proc.waitForProcessOutput(System.out, System.err)
		return proc
	}

	void setUp(){		
		assert parConnectorDir.exists() : '!!! Unable to locate PARConnector dir !!!'
		assert schedHome != null : '!!! Unable to locate Scheduler 3.4.0 home dir, the SCHEDULER_340 env var is undefined !!!'
		assert rHome != null : '!!! Unable to locate R home dir, the R_HOME env var is undefined !!!'
		assert distDir.exists() : 'No dist dir ? ' + distDir

		// ! THIS IS A FIX FOR rJava that requires JAVA_HOME to be the location of the JRE !
		System.getenv().each() {k,v ->
			if ('JAVA_HOME'.equals(k)) { v = v+fs+'jre' }
			newEnv << k+'='+v
		}	
		
		println '\n######################\n#   CHECKING R packages from package sources ... \n######################'
		//(new File(homeDir, 'PARConnector.Rcheck')).deleteDir()
		//assert run([rExe, 'CMD', 'check', '--no-codoc', '--no-manual', '--no-multiarch', 'PARConnector'], newEnv, homeDir
		//	).waitFor() == 0 : 'It seems R CMD check failed'	
	
		distDir.deleteDir()
		distDir.mkdir()

		println '\n######################\n#   BUILDING PARConnector ... \n######################'
		assert  run([rExe, 'CMD', 'build', parConnectorDir.getAbsolutePath()], newEnv, distDir
			).waitFor() == 0 : 'It seems R CMD build failed'

		assert distDir.listFiles().length > 0 : 'The dist dir is empty'
		def archiveFile = distDir.listFiles().first();
		assert archiveFile.getName().endsWith('.tar.gz') : 'It seems the archive was not build correctly'
	
		println '\n######################\n#   REMOVING previous PARConnector ... \n######################'
		run([rExe, 'CMD', 'REMOVE', '--library='+rLibraryPath, 'PARConnector'], newEnv, homeDir
			).waitFor();

		println '\n######################\n#   INSTALLING PARConnector ... \n######################'
		assert run([rExe, 'CMD', 'INSTALL', '--no-multiarch', '--library='+rLibraryPath, archiveFile.getAbsolutePath()], newEnv, homeDir
			).waitFor() == 0 : 'It seems R CMD install failed'

		println '\n######################\n#   COPYING parscript + deps to scheduler addons ... \n######################'		
		new AntBuilder().copy(todir: schedHome+'/addons/') {
		    fileset(dir: parConnectorDir.getPath() + '/inst/java/') {
		        include(name: "JRI.jar")
		        include(name: "JRS.jar")
		        include(name: "JRIEngine.jar")
		        include(name: "REngine.jar")
		        include(name: "RserveEngine.jar")
		        include(name: "parscript.jar")
		    }
		}

		println '\n######################\n#   STARTING the Scheduler using start-server.js ... \n######################'
		schedProcess = ["jrunscript", "start-server.js"].execute(null, new File(schedHome, 'bin'))
		try {
			schedProcess.inputStream.eachLine {
				println '>> ' + it
				if (it.contains('terminate all')) {	
					throw new Exception('ready')
				}
			}
		} catch (e) {}
	}

	void testPAConnect(){
		println '\n######################\n#   RUNNING integration tests ... \n######################'	
		def rTestFile = new File(testsDir, 'test.R')
		def proc = [rExe, '--vanilla', '<', rTestFile].execute(newEnv, homeDir)
		proc.out << rTestFile.getText()
		proc.out.close()
		proc.waitForProcessOutput(System.out, System.err)
		assert proc.waitFor() == 0 : 'It seems integration tests failed'
	}

	void tearDown(){
		println '\n######################\n#   SHUTTING down the Scheduler ... \n######################'    
		try {
			schedProcess.out << 'exit\n';
			schedProcess.out.close();
		} catch (e) {e.printStackTrace()}
		schedProcess.waitFor();	
	}
}

/*
import org.junit.runner.JUnitCore

result = JUnitCore.runClasses TestIntegration//, another class

String message = "Ran: " + result.getRunCount() + ", Ignored: " + result.getIgnoreCount() + ", Failed: " + result.getFailureCount()
if (result.wasSuccessful()) {
    println "SUCCESS! " + message
} else {
    println "FAILURE! " + message
    result.getFailures().each {
        println it.toString() 
    }
}
*/
class Test1 extends GroovyTestCase {
	void testToto(){
		assert 1+1 == 2 : 'fuck'
	}
}



def allTests = new GroovyTestSuite()
allTests.addTestSuite(Test1.class)//TestIntegration.class)
TestRunner.run(allTests)

//JUnitTask task = new JUnitTask();

//def ant = new AntBuilder()
//ant.junit (showoutput: 'yes'){
//    test(name:Test1.class)
//}

import org.apache.tools.ant.Project
//import org.apache.tools.ant.ProjectHelper

    String pathToReports = System.getProperty('user.dir');
    Project project = new Project();

    try {
        new File(pathToReports).mkdir();
        JUnitTask task = new JUnitTask();

        project.setProperty("java.io.tmpdir",pathToReports);
        task.setProject(project);

        FormatterElement.TypeAttribute type = new FormatterElement.TypeAttribute();
        type.setValue("xml");

        FormatterElement formater = new FormatterElement();   
        formater.setType(type);
        task.addFormatter(formater);

        JUnitTest test = new JUnitTest(YOURTEST.class.getName());
        test.setTodir(new File(pathToReports));

        task.addTest(test);         
        task.execute(); 
    } catch (Exception e) {
    }

Project project = new Project();
project.setName("myproject");
project.init();

Target target = new Target();
target.setName("junitreport");
project.addTarget(target);

FileSet fs = new FileSet();
fs.setDir(new File("./junitreports"));
fs.createInclude().setName("*.xml");
XMLResultAggregator aggregator = new XMLResultAggregator();
aggregator.setProject(project);
aggregator.addFileSet(fs);
AggregateTransformer transformer = aggregator.createReport();
transformer.setTodir(new File("./testreport"));

target.addTask(aggregator);
project.executeTarget("junitreport");