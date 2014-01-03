/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Integration test for the PARConnector with Scheduler 3.4.0
// JAVA_HOME must be at least a jdk 1.7
// R_HOME must be at least R 3.0.2
// 
// MainTest class:
// - creates a Context that fires a check and build of the PARConnector package
// - creates a TestSetup that wraps the tests with scheduler start/stop
// - creates a junit test case for each r script locates in 'tests' dir
// 
// Context class:
// - builds the PARConnector
// - copies the contents to the scheduler's home addons directory
// - starts the rm+scheduler+4nodes using the standard start-server.js script
//
// TestRScript class:
// - behaves as a single method junit test case
// - runs a r script and checks the exit value
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

class MainTest extends TestCase {

	static Test suite() {

		def ctx = new Context()
		ctx.check();
		ctx.build()

		def ts = new TestSuite();

        def testSetup = new TestSetup(ts) {
            protected void setUp(  ) throws Exception {
                ctx.startScheduler()
            }

            protected void tearDown(  ) throws Exception {
            	ctx.stopScheduler()
            }
        };

        ctx.testsDir.listFiles().each {
			def t = new TestRscript()
			t.ctx = ctx
			t.rTestFile = it
			ts.addTest(t);
		}

		return testSetup
    }
}

class TestRscript extends TestCase {
	def ctx
	def rTestFile

	void testRScript(){
		println '\n######################\n#   RUNNING integration test ' + rTestFile + ' ... \n######################'			
		def proc = [ctx.rExe, '--vanilla', '<', rTestFile].execute(ctx.newEnv, ctx.homeDir)
		proc.out << rTestFile.getText()
		proc.out.close()
		proc.waitForProcessOutput(System.out, System.err)
		assert proc.waitFor() == 0 : 'It seems integration test failed'
	}

	String getName() {
		return rTestFile.getName()
	}

	TestRscript(){ super('testRScript') }
}

class Context {	
	def isWindows = System.properties['os.name'].toLowerCase().contains('windows')
	def schedHome = System.getenv()['SCHEDULER_340']
	def rHome = System.getenv()['R_HOME']
		
	def rExe, homeDir, distDir, parConnectorDir, testsDir, rLibraryDir, rLibraryPath
	def newEnv = []
	def schedProcess

	void check(){
		assert schedHome != null : '!!! Unable to locate Scheduler 3.4.0 home dir, the SCHEDULER_340 env var is undefined !!!'
		assert rHome != null : '!!! Unable to locate R home dir, the R_HOME env var is undefined !!!'

		// Locate r binary
		def fs = File.separator
		if (isWindows) {
			def arch = System.getenv()['ProgramFiles(x86)'] != null ? 'x64' : 'i386'
			rExe = rHome+fs+'bin'+fs+arch+fs+'R.exe'
			rLibraryDir = new File(rHome,'library')
		} else {
			rExe = rHome+fs+'bin'+fs+'R'
			//rLibraryDir = new File(rHome,'site-library')
			rLibraryDir = new File(System.getenv()['R_LIBS_USER'])
			assert rLibraryDir.exists() : '!!! Unable to locate R library, please set R_LIBS_USER env var !!!'
		}
		rLibraryPath = rLibraryDir.getAbsolutePath()
		// todo check for rJava	

		// Check that the current dir is 'r' or its parent
		def cd = new File(System.getProperty('user.dir'));
		homeDir = cd.getName() == 'r' ? cd : new File(cd, 'r');
		distDir = new File(homeDir, 'dist');
		parConnectorDir = new File(homeDir, 'PARConnector');
		testsDir = new File(homeDir,'tests')

		assert parConnectorDir.exists() : '!!! Unable to locate PARConnector dir !!!'
		
		def jreHome = System.getenv()['JAVA_HOME'] + fs + 'jre'
		assert (new File(jreHome)).exists() : "!!! Unable to locate the jre !!!"

		// ! THIS IS A FIX FOR rJava that requires JAVA_HOME to be the location of the JRE !
		System.getenv().each() {k,v ->
			if ('JAVA_HOME'.equals(k)) { v = jreHome }
			newEnv << k+'='+v
		}
		if (!isWindows) {
			// LD_LIBRARY_PATH=/home/jenkins/shared/java/x86_64/sun/jdk1.7.0_45/jre/lib/amd64:/home/jenkins/shared/java/x86_64/sun/jdk1.7.0_45/jre/lib/amd64/server/		
			def libDir = new File(jreHome, 'lib')
			assert libDir.exists() : "!!! Unable to locate the lib dir inside the jre dir !!!"

			def so64Dir = new File(libDir, 'amd64')
			if (so64Dir.exists()) { // 64 bits jre
				newEnv << 'LD_LIBRARY_PATH='+so64Dir+File.pathSeparator+(new File(so64Dir, 'server'))
			} 
			
			def so32Dir = new File(libDir, 'i386')
			if (so32Dir.exists()){ // 32 bits jre
				newEnv << 'LD_LIBRARY_PATH='+so32Dir+File.pathSeparator+(new File(so32Dir, 'client'))
			}
		}
	}

	void build(){
		println '\n######################\n#   CHECKING R packages from package sources ... \n######################'
		(new File(homeDir, 'PARConnector.Rcheck')).deleteDir()
		assert run([rExe, 'CMD', 'check', '--no-codoc', '--no-manual', '--no-multiarch', 'PARConnector'], newEnv, homeDir
			).waitFor() == 0 : 'It seems R CMD check failed'
	    
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
	}

	void startScheduler() {
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

	void stopScheduler() {
		println '\n######################\n#   SHUTTING down the Scheduler ... \n######################'    
		try {
			schedProcess.out << 'exit\n';
			schedProcess.out.close();
		} catch (e) {e.printStackTrace()}
		schedProcess.waitFor();	
	}


	// Executes a command and redirects stdout/stderr
	def run = {command, env, dir ->
		println 'Running ' + command.join(" ")
		def proc = command.execute(env, dir)
		proc.waitForProcessOutput(System.out, System.err)
		return proc
	}
}

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration

def scriptPath = getClass().protectionDomain.codeSource.location.path
def currentDir = new File(scriptPath).parent

def binDir = new File(currentDir, 'bin')
binDir.deleteDir()
binDir.mkdir()

def reportDir = new File(currentDir, 'report')
reportDir.deleteDir()
reportDir.mkdir()

CompilerConfiguration config = new CompilerConfiguration()
config.setTargetDirectory(binDir)

def compiler = new FileSystemCompiler(config);
compiler.compile(scriptPath)

def ant = new AntBuilder()
//ant.project.getBuildListeners().firstElement().setMessageOutputLevel(3)
ant.junit (haltonfailure: 'yes', showoutput: 'yes'){
	classpath(){
		pathelement( location: binDir )
	}
    batchtest(todir: reportDir, fork:'no'){
    	fileset( dir: binDir, includes: 'MainTest.class')
    	formatter( type: "plain", usefile:'false')
    	formatter( type: "xml", usefile:'true')
    }
}