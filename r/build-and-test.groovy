/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Integration test for the PARConnector with Scheduler 3.4.0
// 
// - builds the PARConnector
// - copies the contents to the scheduler's home addons directory
// - starts the rm+scheduler+4nodes using the standard start-server.js script
// - runs the integration test that uses PARConnector api functions and submit jobs
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def fs = File.separator

// Check that the current dir is 'r' or its parent
def homeDir = new File(System.getProperty('user.dir'));
if ('r' != homeDir.getName()) {
	rDir = new File(homeDir, 'r')
	if (rDir.exists()) {
		homeDir = rDir
	} else {
		'!!! Please run the script from r dir or from the parent of r dir !!!'
	}
}

def parConnectorDir = new File(homeDir, 'PARConnector')
assert parConnectorDir.exists() : '!!! Unable to PARConnector dir !!!'

def schedHome = System.getenv()['SCHEDULER_340']
assert schedHome != null : '!!! Unable to locate Scheduler 3.4.0 home dir, the SCHEDULER_340 env var is undefined !!!'

def rHome = System.getenv()['R_HOME']
assert rHome != null : '!!! Unable to locate R home dir, the R_HOME env var is undefined !!!'

def rLibraryDir = new File(rHome,'library') // check if on linux it works
def rLibraryPath = rLibraryDir.getAbsolutePath()
// todo check for rJava

// ! THIS IS A FIX FOR rJava that requires JAVA_HOME to be the location of the JRE !
def newEnv = []
System.getenv().each() {k,v ->
	if ('JAVA_HOME'.equals(k)) { v = v+fs+'jre' }	
	newEnv << k+'='+v
}

def paArch = System.getenv()['ProgramFiles(x86)'] != null ? 'x64' : 'i386'
def rExe = rHome+fs+'bin'+fs+paArch+fs+'R.exe'

println '\n######################\n#   CHECKING R packages from package sources ... \n######################'
(new File(homeDir, 'PARConnector.Rcheck')).deleteDir()
def rcheckProcess = [rExe, 'CMD', 'check', '--no-codoc', '--no-manual', '--no-multiarch', 'PARConnector'].execute(newEnv, homeDir)
rcheckProcess.inputStream.eachLine {
	println '>> ' + it
}
assert rcheckProcess.waitFor() == 0 : "!!! It seems R CHECK failed !!!"

println '\n######################\n#   BUILDING PARConnector ... \n######################'
def distDir = new File(homeDir, 'dist')
distDir.deleteDir()
distDir.mkdir()
def rbuildProcess = [rExe, 'CMD', 'build', parConnectorDir.getAbsolutePath()].execute(newEnv, distDir)
rbuildProcess.inputStream.eachLine {
	println '>> ' + it
}
assert rbuildProcess.waitFor() == 0 : "!!! It seems R BUILD failed !!!"

println '\n######################\n#   REMOVING previous PARConnector ... \n######################'
def removeProcess = [rExe, 'CMD', 'REMOVE', '--library='+rLibraryPath, 'PARConnector'].execute(newEnv, homeDir)
removeProcess.inputStream.eachLine {
	println '>> ' + it
}
assert removeProcess.waitFor() == 0 : "!!! It seems REMOVING previous failed !!!"

println '\n######################\n#   INSTALLING PARConnector ... \n######################'
//for /f "tokens=*" %%a in ('dir /b /od') do set newest=%%a
//%R_EXE% CMD INSTALL  --library="%R_HOME%\library" %newest%
//def removeProcess = [rExe, 'CMD', 'INSTALL', '--preclean', '--no-test-load', '--library='+rLibraryPath, 'PARConnector'].execute(newEnv, homeDir)
//removeProcess.inputStream.eachLine {
//	println '>> ' + it
//}
//assert removeProcess.waitFor() == 0 : "!!! It seems R BUILD failed !!!"

/*
echo ************************************************************
echo ********* RUNNING integration tests                *********
echo ************************************************************
%R_EXE% --vanilla < ..\tests\test.R
*/


//rem %R_EXE% CMD check --no-codoc --no-manual --no-multiarch PARConnector
//IF ERRORLEVEL 1 GOTO :exit

/*
try {   
	println '\n######################\n#   Starting the Scheduler using start-server.js ... \n######################'
	schedProcess = ["jrunscript", "start-server.js"].execute(null, new File(schedHome+'/bin'))
	try {
		schedProcess.inputStream.eachLine {
			println '>> ' + it
			if (it.contains('terminate all')) {	
				throw new Exception('ready')
			}
		}
		} catch (e) {}

		} finally {
			println '\n######################\n#   Shutting down the Scheduler ... \n######################'    
			try { 
				def stdin = schedProcess.getOutputStream();
				stdin.write('exit\n'.getBytes() );
				stdin.flush();
				} catch (e) {e.printStackTrace()}
				calcServerProcess.waitForOrKill(500)
				schedProcess.waitFor();
			}
		}
	}
}
*/