package tests;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.objectweb.proactive.extensions.dataspaces.api.Capability;
import org.objectweb.proactive.extensions.dataspaces.api.DataSpacesFileObject;
import org.objectweb.proactive.extensions.dataspaces.api.FileContent;
import org.objectweb.proactive.extensions.dataspaces.api.FileSelector;
import org.objectweb.proactive.extensions.dataspaces.api.FileType;
import org.objectweb.proactive.extensions.dataspaces.exceptions.FileSystemException;
import org.objectweb.proactive.extensions.dataspaces.exceptions.SpaceNotFoundException;
import org.ow2.parscript.PARScriptFactory;
import org.ow2.proactive.scheduler.task.launcher.TaskLauncher;
import org.ow2.proactive.scripting.ScriptResult;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

/**
 * Basic PARScript tests.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestLocalspace {

	@Test
	public void testPrintHelloWorld() throws Exception {
		File f = new File(System.getProperty("java.io.tmpdir"));
		String path = f.getCanonicalPath();
		DataSpacesFileObject dsfo = new MockedDSFO(path);

		String rScript = "result=getwd();";

		Map<String, Object> aBindings = Collections.singletonMap(
				TaskLauncher.DS_SCRATCH_BINDING_NAME, (Object) dsfo);
		SimpleScript ss = new SimpleScript(rScript,
				PARScriptFactory.ENGINE_NAME);
		TaskScript taskScript = new TaskScript(ss);
		ScriptResult<Serializable> res = taskScript.execute(aBindings);

		String resPath = (String) res.getResult();
		org.junit.Assert.assertNotNull("No result from R script", resPath);
		org.junit.Assert.assertEquals(
				"R script working directory is incorrect", path,
				resPath.replace("/", "\\"));
	}

	class MockedDSFO implements DataSpacesFileObject {
		private String path;

		public MockedDSFO(String path) {
			this.path = path;
		}

		@Override
		public void close() throws FileSystemException {
		}

		@Override
		public void copyFrom(DataSpacesFileObject arg0, FileSelector arg1)
				throws FileSystemException {
		}

		@Override
		public void createFile() throws FileSystemException {
		}

		@Override
		public void createFolder() throws FileSystemException {
		}

		@Override
		public boolean delete() throws FileSystemException {
			return false;
		}

		@Override
		public int delete(FileSelector arg0) throws FileSystemException {
			return 0;
		}

		@Override
		public DataSpacesFileObject ensureExistingOrSwitch()
				throws FileSystemException, SpaceNotFoundException {

			return null;
		}

		@Override
		public boolean exists() throws FileSystemException {

			return false;
		}

		@Override
		public List<DataSpacesFileObject> findFiles(FileSelector arg0)
				throws FileSystemException {

			return null;
		}

		@Override
		public void findFiles(FileSelector arg0, boolean arg1,
				List<DataSpacesFileObject> arg2) throws FileSystemException {

		}

		@Override
		public List<String> getAllRealURIs() {

			return null;
		}

		@Override
		public List<String> getAllSpaceRootURIs() {

			return null;
		}

		@Override
		public DataSpacesFileObject getChild(String arg0)
				throws FileSystemException {
			return null;
		}

		@Override
		public List<DataSpacesFileObject> getChildren()
				throws FileSystemException {

			return null;
		}

		@Override
		public FileContent getContent() throws FileSystemException {

			return null;
		}

		@Override
		public DataSpacesFileObject getParent() throws FileSystemException {

			return null;
		}

		@Override
		public String getRealURI() {
			return "file://" + path;
		}

		@Override
		public String getSpaceRootURI() {

			return null;
		}

		@Override
		public FileType getType() throws FileSystemException {

			return null;
		}

		@Override
		public String getVirtualURI() {

			return null;
		}

		@Override
		public boolean hasSpaceCapability(Capability arg0) {

			return false;
		}

		@Override
		public boolean isContentOpen() {

			return false;
		}

		@Override
		public boolean isHidden() throws FileSystemException {

			return false;
		}

		@Override
		public boolean isReadable() throws FileSystemException {

			return false;
		}

		@Override
		public boolean isWritable() throws FileSystemException {

			return false;
		}

		@Override
		public void moveTo(DataSpacesFileObject arg0)
				throws FileSystemException {
		}

		@Override
		public void refresh() throws FileSystemException {
		}

		@Override
		public DataSpacesFileObject resolveFile(String arg0)
				throws FileSystemException {
			return null;
		}

		@Override
		public DataSpacesFileObject switchToSpaceRoot(String arg0)
				throws FileSystemException, SpaceNotFoundException {
			return null;
		}

	}
}
