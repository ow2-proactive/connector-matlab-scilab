/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Integration test for the PARConnector with Scheduler 3.4.0
// 
// - builds the PARConnector
// - copies the contents to the scheduler's home addons directory
// - starts the rm+scheduler+4nodes using the standard start-server.js script
// - runs the integration test that uses PARConnector api functions and submit jobs
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Check that the current dir is matlab_scilab_connector
def rtoolboxDir = new File('r');
assert rtoolboxDir.exists() : '!!! Unable to locate the r dir, please run this script from the correct location !!!'

def fs = File.separator
def schedHome = System.getenv()['SCHEDULER_340']
assert schedHome != null : '!!! Unable to locate Scheduler 3.4.0 home dir, the SCHEDULER_340 env var is undefined !!!'

def rHome = System.getenv()['R_HOME']
assert rHome != null : '!!! Unable to locate R home dir, the R_HOME env var is undefined !!!'

// ! THIS IS A FIX FOR rJava that requires JAVA_HOME to be the location of the JRE !
def currentEnv = System.getenv();
currentEnv['JAVA_HOME'] = System.getenv()['JAVA_HOME']+fs+'jre';
def newEnv = []
currentEnv.each() {k,v -> newEnv << k+'='+'v'}

println '----> new env = ' + newEnv

def paArch = System.getenv()['PROGRAMFILES(X86)'] != null ? 'x64' : 'i386'
def rExe = rHome+fs+'bin'+fs+paArch+fs+'R.exe'

println '\n######################\n#   CHECKING R packages from package sources ... \n######################'

// Delete previous checks
(new File(rtoolboxDir, 'PARConnector.Rcheck')).deleteDir(); 
def rcheckProcess = [rExe, 'CMD', 'check', '--no-codoc', '--no-manual', '--no-multiarch', 'PARConnector'].execute(newEnv, rtoolboxDir)
rcheckProcess.inputStream.eachLine {
	println '>> ' + it
}
rcheckProcess.waitFor()

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
