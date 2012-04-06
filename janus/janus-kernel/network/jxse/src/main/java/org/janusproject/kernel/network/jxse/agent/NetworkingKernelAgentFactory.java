/* 
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on <http://www.janus-project.org>
 * Copyright (C) 2010-2011 Janus Core Developers
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
package org.janusproject.kernel.network.jxse.agent;

import org.osgi.framework.BundleContext;

/**
 * Creates a networking enabled kernel.
 * <p>
 * If your kernel is running on top of an OSGi framework you need to provided the {@link BundleContext} of your application in order to locate your classes. Otherwise just pass null.
 * 
 * @author $Author: srodriguez$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 * @deprecated see {@link JxtaJxseKernelAgentFactory}
 */
@Deprecated
public class NetworkingKernelAgentFactory extends JxtaJxseKernelAgentFactory {

	/**
	 * 
	 * @param context is the bundle context used to initialize this agent factory
	 */
	public NetworkingKernelAgentFactory(BundleContext context) {
		super(context);
	}

}
