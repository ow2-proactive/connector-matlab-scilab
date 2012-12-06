// In case of RunAsMe, this script must be executed inside the forked JVM
// It test if the java.io.tmpdir is writable then creates a MatlabPrefdir inside and
// sets matlab.task.tmpdir (transmitted as env var to the MATLAB process) to
// the java.io.tmpdir and matlab.prefdir to <java.io.tmpdir>/MatlabForkedTasksTmp/Task<id>/MatlabPrefdir

importClass(java.lang.System);
importClass(java.io.File);
importClass(java.lang.Class);


importClass(org.apache.commons.io.FileUtils);
importClass(org.ow2.proactive.scheduler.task.launcher.TaskLauncher);


var tmpDir = System.getProperty("java.io.tmpdir");

// Fix for SCHEDULING-1308: With RunAsMe on windows the forked jvm can have a non-writable java.io.tmpdir
if (!new File(tmpDir).canWrite()) {
    throw new RuntimeException("Unable to execute task, java.io.tmpdir: " + tmpDir + " is not writable");
}

// Creates dir <NODE_TMPDIR || SCRATCH_DIR>/MatlabForkedTasksTmp
var rootDir = new File(tmpDir, "MatlabForkedTasksTmp");
if (!rootDir.exists()) {
    if (!rootDir.mkdir()) {
        throw new RuntimeException("Unable to execute task, unable to mkdir " + rootDir);
    }
}

rootDir.setReadable(true, false);
rootDir.setWritable(true, false);
rootDir.setExecutable(true, false);

// Get the task id
var taskId = System.getProperty(TaskLauncher.SchedulerVars.JAVAENV_TASK_ID_VARNAME.toString());

// Create task specific dir <NODE_TMPDIR || SCRATCH_DIR>/MatlabForkedTasksTmp/Task<id>
var taskIdDir = new File(rootDir, "Task" + taskId);
if (taskIdDir.exists()) {
    // Delete previous data
    FileUtils.deleteDirectory(taskIdDir);
}
if (!taskIdDir.mkdir()) {
    throw new RuntimeException("Unable to execute task, unable to mkdir " + taskIdDir);
}

taskIdDir.setReadable(true, false);
taskIdDir.setWritable(true, false);
taskIdDir.setExecutable(true, false);

System.setProperty("matlab.task.tmpdir", taskIdDir);

// Create dir for MATLAB preferences
var matlabPrefdir = new File(taskIdDir, "MatlabPrefdir");
if (!matlabPrefdir.mkdir()) {
    throw new RuntimeException("Unable to execute task, unable to mkdir " + matlabPrefdir);
}
matlabPrefdir.setReadable(true, false);
matlabPrefdir.setWritable(true, false);
matlabPrefdir.setExecutable(true, false);
matlabPrefdir.deleteOnExit();

System.setProperty("matlab.task.tmpdir", matlabPrefdir.getAbsolutePath());
