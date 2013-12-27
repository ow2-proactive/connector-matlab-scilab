/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Integration test for the PARConnector with Scheduler 3.4.0
// 
// - builds the PARConnector
// - copies the contents to the scheduler's home addons directory
// - starts the rm+scheduler+4nodes using the standard start-server.js script
// - runs the integration test that uses PARConnector api functions and submit jobs
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Executes a command and redirects stdout/stderr
def run = {command, env, dir ->
	println 'Running ' + command.join(" ")
	def proc = command.execute(env, dir)
	proc.waitForProcessOutput(System.out, System.err)
	return proc
}

// Check that the current dir is 'r' or its parent
def homeDir = new File(System.getProperty('user.dir'));
if ('r' != homeDir.getName()) {
	rDir = new File(homeDir, 'r')
	if (rDir.exists()) {
		homeDir = rDir
	} else {
		throw new IllegalStateException('!!! Please run the script from r dir or from the parent of r dir !!!')
	}
}

def fs = File.separator

def parConnectorDir = new File(homeDir, 'PARConnector')
assert parConnectorDir.exists() : '!!! Unable to PARConnector dir !!!'

def schedHome = System.getenv()['SCHEDULER_340']
assert schedHome != null : '!!! Unable to locate Scheduler 3.4.0 home dir, the SCHEDULER_340 env var is undefined !!!'

def rHome = System.getenv()['R_HOME']
assert rHome != null : '!!! Unable to locate R home dir, the R_HOME env var is undefined !!!'

def rLibraryDir = new File(rHome,'library') // check if on linux it works
def rLibraryPath = rLibraryDir.getAbsolutePath()
// todo check for rJava

def testsDir = new File(homeDir,'tests')

// ! THIS IS A FIX FOR rJava that requires JAVA_HOME to be the location of the JRE !
def newEnv = []
System.getenv().each() {k,v ->
	if ('JAVA_HOME'.equals(k)) { v = v+fs+'jre' }
	newEnv << k+'='+v
}

def paArch = System.getenv()['ProgramFiles(x86)'] != null ? 'x64' : 'i386'
def rExe = rHome+fs+'bin'+fs+paArch+fs+'R.exe'

println '\n######################\n#   CHECKING R packages from package sources ... \n######################'
//(new File(homeDir, 'PARConnector.Rcheck')).deleteDir()
//proc = run([rExe, 'CMD', 'check', '--no-codoc', '--no-manual', '--no-multiarch', 'PARConnector'], newEnv, homeDir)
//assert proc.waitFor() == 0 : 'It seems R CMD check failed'

println '\n######################\n#   BUILDING PARConnector ... \n######################'
def distDir = new File(homeDir, 'dist')
distDir.deleteDir()
distDir.mkdir()
assert distDir.exists() : 'No dist dir ? ' + distDir

proc = run([rExe, 'CMD', 'build', parConnectorDir.getAbsolutePath()], newEnv, distDir)
assert proc.waitFor() == 0 : 'It seems R CMD build failed'

assert distDir.listFiles().length > 0 : 'The dist dir is empty'
def archiveFile = distDir.listFiles().first();
assert archiveFile.getName().endsWith('.tar.gz') : 'It seems the archive was not build correctly'

println '\n######################\n#   REMOVING previous PARConnector ... \n######################'
proc = run([rExe, 'CMD', 'REMOVE', '--library='+rLibraryPath, 'PARConnector'], newEnv, homeDir)
//assert removeProcess.waitFor() == 0 : 'It seems R CMD remove failed'

println '\n######################\n#   INSTALLING PARConnector ... \n######################'
proc = run([rExe, 'CMD', 'INSTALL', '--no-multiarch', '--library='+rLibraryPath, archiveFile.getAbsolutePath()], newEnv, homeDir)
assert proc.waitFor() == 0 : 'It seems R CMD install failed'

println '\n######################\n#   COPYING parscript + deps to scheduler addons ... \n######################'
def ant = new AntBuilder()
ant.copy(todir: schedHome+'/addons/') {
    fileset(dir: parConnectorDir.getPath() + '/inst/java/') {
        include(name: "JRI.jar")
        include(name: "JRS.jar")
        include(name: "JRIEngine.jar")
        include(name: "REngine.jar")
        include(name: "RserveEngine.jar")
        include(name: "parscript.jar")
    }
}

try {
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

	println '\n######################\n#   RUNNING integration tests ... \n######################'	
	def rTestFile = new File(testsDir, 'test.R')
	proc = [rExe, '--vanilla', '<', rTestFile].execute(newEnv, homeDir)
	proc.out << rTestFile.getText()
	proc.out.close()
	proc.waitForProcessOutput(System.out, System.err)
	assert proc.waitFor() == 0 : 'It seems integration tests failed'

} finally {
	println '\n######################\n#   SHUTTING down the Scheduler ... \n######################'    
	try {
		schedProcess.out << 'exit\n';
		schedProcess.out.close();
	} catch (e) {e.printStackTrace()}
	schedProcess.waitFor();	
}
