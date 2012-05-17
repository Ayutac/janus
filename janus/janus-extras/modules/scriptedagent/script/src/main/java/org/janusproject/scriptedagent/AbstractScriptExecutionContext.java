/* 
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on <http://www.janus-project.org>
 * Copyright (C) 2012 Janus Core Developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.janusproject.scriptedagent;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.arakhne.vmutil.FileSystem;
import org.arakhne.vmutil.locale.Locale;
import org.janusproject.kernel.util.event.ListenerCollection;

/**
 * Abstraxct execution context for a script language.
 * 
 * @author $Author: sgalland$
 * @version $Name$ $Revision$ $Date$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 * @since 0.5
 */
public abstract class AbstractScriptExecutionContext implements ScriptExecutionContext {

	private Logger logger = null;
	
	private ScriptRepository repository;
	
	private final ScriptEngine engine;
	
	private ListenerCollection<ScriptErrorListener> listeners = null;

	/** Index used to generate unique temp variable names.
	 * This field is mandatory due to the possibility to invoke
	 * functions as parameters of function calls, all of them
	 * build through {@link #makeFunctionCall(String, Object...)}. 
	 */
	private int tempVariableIndex = 0;
	
	/**
	 * @param engine is the real script engine implementation to use.
	 */
	public AbstractScriptExecutionContext(ScriptEngine engine) {
		this.engine = engine;
		this.repository = new ScriptRepository();
		if (this.engine==null) throw new Error(Locale.getString("NO_ENGINE", getClass().getCanonicalName())); //$NON-NLS-1$
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLanguageName() {
		return this.engine.getFactory().getLanguageName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLanguageVersion() {
		return this.engine.getFactory().getLanguageVersion();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ScriptFileFilter getFileFilter(boolean allowDirectories) {
		ScriptEngineFactory factory = this.engine.getFactory();
		return new ScriptFileFilter(
				factory.getLanguageName(),
				allowDirectories,
				factory.getExtensions());
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public synchronized final void addScriptErrorListener(ScriptErrorListener listener) {
		if (this.listeners==null)
			this.listeners = new ListenerCollection<>();
		this.listeners.add(ScriptErrorListener.class, listener);
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public synchronized void removeScriptErrorListener(ScriptErrorListener listener) {
		if (this.listeners!=null) {
			this.listeners.remove(ScriptErrorListener.class, listener);
			if (this.listeners.isEmpty())
				this.listeners = null;
		}
	}
	
	/** Notifies the listeners about the script error.
	 * 
	 * @param e
	 * @return <code>true</code> if a listener was notified; <code>false</code>
	 * if no listener was notified.
	 */
	protected synchronized final boolean fireScriptError(ScriptException e) {
		boolean notified = false;
		if (this.listeners!=null) {
			for(ScriptErrorListener listener : this.listeners.getListeners(ScriptErrorListener.class)) {
				listener.onScriptError(e);
				notified = true;
			}
		}
		return notified;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ScriptEngine getScriptEngine() {
		return this.engine;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void setStandardInput(Reader stdin) {
		this.engine.getContext().setReader(stdin);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Reader getStandardInput() {
		return this.engine.getContext().getReader();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void setStandardOutput(Writer stdout) {
		this.engine.getContext().setWriter(stdout);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Writer getStandardOutput() {
		return this.engine.getContext().getWriter();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void setStandardError(Writer stderr) {
		this.engine.getContext().setErrorWriter(stderr);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Writer getStandardError() {
		return this.engine.getContext().getErrorWriter();
	}

	/** {@inheritDoc}
	 */
	@Override
	public final Logger getLogger() {
		if (this.logger==null)
			this.logger = Logger.getLogger(getClass().getCanonicalName());
		return this.logger;
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public final void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public final void setScriptRepository(ScriptRepository repository) {
		assert(repository!=null);
		this.repository = repository;
	}

	/** {@inheritDoc}
	 */
	@Override
	public final ScriptRepository getScriptRepository() {
		return this.repository;
	}
	
	/** Log the given script exception.
	 * 
	 * @param e
	 */
	protected void log(ScriptException e) {
		if (e!=null && !fireScriptError(e)) {
			Logger logger = getLogger();
			if (logger!=null)
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}
	
	/** Invoked to evaluate the script in the given stream.
	 * <p>
	 * This function was created to be overridden by the subclasses
	 * to invoke the appropriate "eval" function on the engine.
	 * The default implementation, invode {@link ScriptEngine#eval(Reader)}.
	 * 
	 * @param engine is the engine to use for evaluation.
	 * @param stream is the stream to read.
	 * @return the result of the evaluation.
	 * @throws ScriptException 
	 */
	protected Object evaluate(ScriptEngine engine, Reader stream) throws ScriptException {
		return engine.eval(stream);
	}
	
	/** Invoked to evaluate the script in the string.
	 * <p>
	 * This function was created to be overridden by the subclasses
	 * to invoke the appropriate "eval" function on the engine.
	 * The default implementation, invode {@link ScriptEngine#eval(String)}.
	 * 
	 * @param engine is the engine to use for evaluation.
	 * @param script is the script to evaluate.
	 * @return the result of the evaluation.
	 * @throws ScriptException 
	 */
	protected Object evaluate(ScriptEngine engine, String script) throws ScriptException {
		return engine.eval(script);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object runCommand(String aCommand) {
		try {
			return evaluate(getScriptEngine(), aCommand);
		}
		catch (ScriptException e) {
			if (!fireScriptError(e)) log(e);
			return null;
		}
		catch (Exception e) {
			ScriptException se = new ScriptException(e);
			if (!fireScriptError(se)) log(se);
			return null;
		}
		catch (Throwable e) {
			ScriptException se = new ScriptException(new Exception(e));
			if (!fireScriptError(se)) log(se);
			return null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object runScript(String scriptBasename) {
		ScriptFileFilter filter = getFileFilter(false);
		ScriptException firstException = null;
		Throwable firstThrowable = null;
		if (filter==null || filter.accept(new File(scriptBasename))) {
			Iterator<URL> iterator = this.repository.getDirectories();
			URL bu, fu;
			ScriptEngine engine = getScriptEngine();
			while (iterator.hasNext()) {
				bu = iterator.next();
				fu = FileSystem.join(bu, scriptBasename);
				try {
					return evaluate(engine, new InputStreamReader(fu.openStream()));
				}
				catch (ScriptException e) {
					if (firstException==null) {
						firstException = e;
					}
				}
				catch (Throwable e) {
					if (firstThrowable==null) {
						firstThrowable = e;
					}
				}
				
			}
		}
		if (firstException!=null) {
			log(firstException);
		}
		else if (firstThrowable!=null) {
			if (firstThrowable instanceof Exception)
				log(new ScriptException((Exception)firstThrowable));
			else
				log(new ScriptException(new Exception(firstThrowable)));
		}
		else {
			log(new ScriptException(Locale.getString("NO_SCRIPT", scriptBasename))); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object runScript(File scriptFilename) {
		try {
			return evaluate(getScriptEngine(), new FileReader(scriptFilename));
		}
		catch (ScriptException e) {
			log(e);
		}
		catch (Exception e) {
			log(new ScriptException(e));
		}
		catch (Throwable e) {
			log(new ScriptException(new Exception(e)));
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object runScript(URL scriptFilename) {
		try {
			return evaluate(getScriptEngine(), new InputStreamReader(scriptFilename.openStream()));
		}
		catch (ScriptException e) {
			log(e);
		}
		catch (Exception e) {
			log(new ScriptException(e));
		}
		catch (Throwable e) {
			log(new ScriptException(new Exception(e)));
		}
		return null;
	}
	
	/** Create and replies the call to the function with the specified
	 * name and the specified parameters.
	 * 
	 * @param functionName is the name of the function to call.
	 * @param params are the parameters to pass to the function.
	 * @return the script command.
	 */
	public abstract String makeFunctionCall(String functionName, Object... params);

	/** Create the unique name for a temp variable.
	 * 
	 * @return the name of the temporary variable.
	 */
	protected final String makeTempVariable() {
		StringBuilder t = new StringBuilder();
		t.append("___JANUS_KERNEL_TEMP_VARIABLE__"); //$NON-NLS-1$
		t.append(this.tempVariableIndex);
		++this.tempVariableIndex;
		return t.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object runFunction(String scriptBasename, String functionName,
			Object... params) {
		runScript(scriptBasename);
		try {
			return runCommand(
					makeFunctionCall(functionName, params));
		}
		finally {
			this.tempVariableIndex = 0;
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object runFunction(File scriptFilename, String functionName,
			Object... params) {
		runScript(scriptFilename);
		try {
			return runCommand(makeFunctionCall(functionName, params));
		}
		finally {
			this.tempVariableIndex = 0;
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object runFunction(URL scriptFilename, String functionName,
			Object... params) {
		runScript(scriptFilename);
		try {
			return runCommand(makeFunctionCall(functionName, params));
		}
		finally {
			this.tempVariableIndex = 0;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setGlobalValue(String name, Object value) {
		getScriptEngine().getContext().setAttribute(name, value, ScriptContext.ENGINE_SCOPE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getGlobalValue(String name) {
		return getScriptEngine().getContext().getAttribute(name);
	}

}
