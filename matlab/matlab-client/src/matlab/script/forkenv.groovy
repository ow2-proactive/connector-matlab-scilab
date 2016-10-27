import org.ow2.proactive.resourcemanager.nodesource.dataspace.DataSpaceNodeConfigurationAgent;

// In case of RunAsMe, this script must be executed inside the node JVM before the creation of the forked JVM
// The java.io.tmpdir of the forked jvm is <NODE_TMPDIR || SCRATCH_DIR>
tmpDir = System.getProperty(DataSpaceNodeConfigurationAgent.NODE_DATASPACE_SCRATCHDIR);
if (tmpDir == null) {
    tmpDir = System.getProperty("java.io.tmpdir");
}

file = new File(tmpDir)

file.setReadable(true, false);
file.setWritable(true, false);
file.setExecutable(true, false);

// Set the fork environment java.io.tmpdir to the formed dir
forkEnvironment.addJVMArgument("-Djava.io.tmpdir=" + tmpDir);

// ProActive Home is not defined in the forked jvm process by default.
proactiveHome = System.getProperty("proactive.home");
// Set the fork environment java.io.tmpdir to the formed dir
forkEnvironment.addJVMArgument("-Dproactive.home=" + proactiveHome);

// security policy is needed to execute matlab/scilab task.
securityPolicy = System.getProperty("java.security.policy");
// Set the fork environment java.io.tmpdir to the formed dir
forkEnvironment.addJVMArgument("-Djava.security.policy=" + securityPolicy);





