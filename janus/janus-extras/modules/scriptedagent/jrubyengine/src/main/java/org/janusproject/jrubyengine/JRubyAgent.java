/* 
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on <http://www.janus-project.org>
 * Copyright (C) 2010-2012 Janus Core Developers
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
package org.janusproject.jrubyengine;

import javax.script.ScriptEngineManager;

import org.janusproject.scriptedagent.ScriptedAgent;

/**
 * Agent created to run JRuby commands and scripts
 * 
 * @author $Author: sgalland$
 * @author $Author: ngaud$
 * @author $Author: gvinson$
 * @author $Author: rbuecher$
 * @version $Name$ $Revision$ $Date$
 * @mavengroupid $Groupid$
 * @mavenartifactid $ArtifactId$
 */
public abstract class JRubyAgent extends ScriptedAgent {

	private static final long serialVersionUID = -2048354743353866599L;

	/**
	 * Creates a new GroovyAgent.
	 * 
	 * @param scriptManager is the manager of the script engines to use.
	 */
	public JRubyAgent(ScriptEngineManager scriptManager) {
		super(new RubyExecutionContext());
	}
	
	/**
	 * Creates a new GroovyAgent. 
	 */
	public JRubyAgent() {
		super(new RubyExecutionContext());
	}
	
}
