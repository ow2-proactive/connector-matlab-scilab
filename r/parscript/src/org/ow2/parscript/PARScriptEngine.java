package org.ow2.parscript;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.proactive.extensions.dataspaces.api.DataSpacesFileObject;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.task.launcher.TaskLauncher;
import org.ow2.proactive.scripting.Script;
import org.ow2.proactive.scripting.TaskScript;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPJavaReference;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.jrs.RScriptEngine;
import org.rosuda.jrs.RexpConvert;

/**
 * R implementation of ScriptEngine using REngine (JRI or Rserve). Sub-class of
 * the RScriptEngine, adds support for types of objects filled into bindings by
 * the ProActive Scheduler ScriptExecutable.
 * 
 * @author Activeeon Team
 */
public class PARScriptEngine extends RScriptEngine {

	/** Initially we don't know how many messages will be callbacked */
	private final LinkedList<String> callbackedErrorMessages;

	/**
	 * Create a instance of the JREngine by reflection
	 * 
	 * @return the instance of the engine
	 */
	public static PARScriptEngine create() {
		// Create the JRI engine by reflection
		String cls = "org.rosuda.REngine.JRI.JRIEngine";
		String[] args = { "--vanilla", "--slave" };

		PARScriptEngine paRengine = new PARScriptEngine(false);
		try {
			paRengine.engine = REngine.engineForClass(cls, args, paRengine, /* runREPL */
					false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(
					"Unable to instantiate the REngine by reflection", e);
		}
		return paRengine;
	}

	protected PARScriptEngine(boolean closeREPL) {
		super(closeREPL);
		this.callbackedErrorMessages = new LinkedList<String>();
	}

	@Override
	public Object eval(String script, ScriptContext context)
			throws ScriptException {
		// Transfer all bindings from context into the rengine env
		if (context == null) {
			throw new ScriptException("No script context specified");
		}
		Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
		if (bindings == null) {
			throw new ScriptException(
					"No bindings specified in the script context");
		}

		// Assign all script task related objects
		this.assignArguments(bindings);
		this.assignProgress(bindings);
		this.assignResults(bindings);
		this.assignLocalSpace(bindings);
		this.assignUserSpace(bindings);
		this.assignGlobalSpace(bindings);
		this.assignInputSpace(bindings);
		this.assignOutputSpace(bindings);

		Object resultValue;
		try {
			REXP rexp = super.engine.parseAndEval(script);
			// If the 'result' variable is explicitly defined in the global
			// environment it is considered as the task result instead of the
			// result exp
			REXP resultRexp = super.engine.get(TaskScript.RESULT_VARIABLE,
					null, true);
			if (resultRexp != null) {
				resultValue = RexpConvert.rexp2jobj(resultRexp);
			} else {
				resultValue = RexpConvert.rexp2jobj(rexp);
			}
			if (resultValue == null) {
				resultValue = true; // TaskResult.getResult() returns true by
									// default
			}
			bindings.put(TaskScript.RESULT_VARIABLE, resultValue);
		} catch (Exception rme) {
			throw new ScriptException(rme);
		}

		if (!this.callbackedErrorMessages.isEmpty()) {
			String mess = StringUtils.join(this.callbackedErrorMessages,
					System.getProperty("line.separator"));
			throw new ScriptException(mess);
		}

		return resultValue;
	}

	private void assignArguments(Bindings bindings) {
		String[] args = (String[]) bindings.get(Script.ARGUMENTS_NAME);
		if (args == null) {
			return;
		}
		try {
			super.engine.assign("args", new REXPString(args));
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		} catch (REngineException e) {
			e.printStackTrace();
		}
	}

	private void assignProgress(Bindings bindings) {
		AtomicInteger progress = (AtomicInteger) bindings
				.get(TaskScript.PROGRESS_VARIABLE);
		if (progress == null) {
			return;
		}
		try {
			super.engine.parseAndEval("{ library(rJava); .jinit() }");
			super.engine.assign("jTaskProgress",
					new REXPJavaReference(progress));
			super.engine
					.parseAndEval("set_progress = function(x) { .jcall(jTaskProgress, \"V\", \"set\", as.integer(x)) }");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void assignResults(Bindings bindings) {
		TaskResult[] results = (TaskResult[]) bindings
				.get(TaskScript.RESULTS_VARIABLE);
		if (results == null) {
			return;
		}
		Map<String, Object> resultsMap = new HashMap<String, Object>(
				results.length);
		for (TaskResult r : results) {
			Object value;
			try {
				value = r.value();
			} catch (Throwable e) {
				value = null;
			}
			resultsMap.put(r.getTaskId().getReadableName(), value);
		}
		try {
			REXP rexp = RexpConvert.jobj2rexp(resultsMap);
			super.engine.assign(TaskScript.RESULTS_VARIABLE, rexp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets a the variable 'localspace' variable in the env and the working dir
	 * to the local space of the task.
	 */
	private void assignLocalSpace(Bindings bindings) {
		DataSpacesFileObject dsfo = (DataSpacesFileObject) bindings
				.get(TaskLauncher.DS_SCRATCH_BINDING_NAME);
		if (dsfo == null) {
			return;
		}
		try {
			String path = convertToRPath(dsfo);
			super.engine.parseAndEval("setwd('" + path + "')");
			super.engine.assign("localspace", new REXPString(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void assignUserSpace(Bindings bindings) {
		DataSpacesFileObject dsfo = (DataSpacesFileObject) bindings
				.get(TaskLauncher.DS_USER_BINDING_NAME);
		if (dsfo == null) {
			return;
		}
		try {
			String path = dsfo.getRealURI().replace("file://", "");
			super.engine.assign("userspace", new REXPString(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void assignGlobalSpace(Bindings bindings) {
		DataSpacesFileObject dsfo = (DataSpacesFileObject) bindings
				.get(TaskLauncher.DS_GLOBAL_BINDING_NAME);
		if (dsfo == null) {
			return;
		}
		try {
			String path = dsfo.getRealURI().replace("file://", "");
			super.engine.assign("globalspace", new REXPString(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void assignInputSpace(Bindings bindings) {
		DataSpacesFileObject dsfo = (DataSpacesFileObject) bindings
				.get(TaskLauncher.DS_INPUT_BINDING_NAME);
		if (dsfo == null) {
			return;
		}
		try {
			String path = dsfo.getRealURI().replace("file://", "");
			super.engine.assign("inputspace", new REXPString(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void assignOutputSpace(Bindings bindings) {
		DataSpacesFileObject dsfo = (DataSpacesFileObject) bindings
				.get(TaskLauncher.DS_OUTPUT_BINDING_NAME);
		if (dsfo == null) {
			return;
		}
		try {
			String path = dsfo.getRealURI().replace("file://", "");
			super.engine.assign("outputspace", new REXPString(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** R paths are not antislash friendly */
	private String convertToRPath(DataSpacesFileObject dsfo) {
		String path = dsfo.getRealURI().replace("file://", "");
		return path.replace("\\", "/");
	}

	@Override
	public void RWriteConsole(REngine eng, String msg, int otype) {
		Writer writer;
		if (otype != 0) {
			// The message should be something like "Error: "
			this.callbackedErrorMessages.add(msg);
			writer = getContext().getErrorWriter();
		} else {
			writer = getContext().getWriter();
		}
		try {
			writer.write(msg);
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}