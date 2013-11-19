package tests;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.swing.JPanel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ow2.parscript.PARScriptFactory;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.task.TaskId;
import org.ow2.proactive.scheduler.common.task.TaskLogs;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.flow.FlowAction;
import org.ow2.proactive.scripting.ScriptResult;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

/**
 * Tests for results from previous tasks. In the R script the variable 'results'
 * will be a map with the taskname as key and the value being the result of
 * {@link TaskResult#value()}.
 * 
 * @author Activeeon Team
 */
@RunWith(JUnit4.class)
public class TestResults {
	@Test
	public void testPrintHelloWorld() throws Exception {
		// Results from hypothetical previous tasks
		String task1Name = "task1";
		double result1 = 1d;
		TaskId id1 = new MockedTaskId(task1Name);
		TaskResult tr1 = new MockedTaskResult(id1, result1);

		String task2Name = "task2";
		double result2 = 2d;
		TaskId id2 = new MockedTaskId(task2Name);
		TaskResult tr2 = new MockedTaskResult(id2, result2);

		TaskResult[] results = new TaskResult[] { tr1, tr2 };

		String rScript = "result=c(results[['" + task1Name + "']],results[['"
				+ task2Name + "']])";

		Map<String, Object> aBindings = Collections.singletonMap(
				TaskScript.RESULTS_VARIABLE, (Object) results);
		SimpleScript ss = new SimpleScript(rScript,
				PARScriptFactory.ENGINE_NAME);
		TaskScript taskScript = new TaskScript(ss);
		ScriptResult<Serializable> res = taskScript.execute(aBindings);

		Serializable value = res.getResult();
		org.junit.Assert.assertTrue("Invalid result type of the R script",
				value instanceof double[]);
		org.junit.Assert.assertArrayEquals(new double[] { result1, result2 },
				(double[]) res.getResult(), 0);
	}

	final class MockedTaskId implements TaskId {
		private String name;

		public MockedTaskId(String name) {
			this.name = name;
		}

		@Override
		public int compareTo(TaskId o) {
			return 0;
		}

		@Override
		public int getIterationIndex() {
			return 0;
		}

		@Override
		public JobId getJobId() {
			return null;
		}

		@Override
		public String getReadableName() {
			return this.name;
		}

		@Override
		public int getReplicationIndex() {
			return 0;
		}

		@Override
		public String value() {
			return this.name;
		}
	}

	final class MockedTaskResult implements TaskResult {
		private TaskId taskId;
		private Serializable value;

		public MockedTaskResult(TaskId taskId, Serializable value) {
			this.taskId = taskId;
			this.value = value;
		}

		@Override
		public FlowAction getAction() {
			return null;
		}

		@Override
		public Throwable getException() {
			return null;
		}

		@Override
		public JPanel getGraphicalDescription() {
			return null;
		}

		@Override
		public TaskLogs getOutput() {
			return null;
		}

		@Override
		public Map<String, String> getPropagatedProperties() {
			return null;
		}

		@Override
		public byte[] getSerializedValue() {
			return null;
		}

		@Override
		public TaskId getTaskId() {
			return this.taskId;
		}

		@Override
		public String getTextualDescription() {
			return null;
		}

		@Override
		public boolean hadException() {
			return false;
		}

		@Override
		public Serializable value() throws Throwable {
			return this.value;
		}
	}
}