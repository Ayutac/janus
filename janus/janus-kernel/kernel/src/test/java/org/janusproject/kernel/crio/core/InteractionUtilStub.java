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
package org.janusproject.kernel.crio.core;

import org.janusproject.kernel.message.Message;
import org.janusproject.kernel.message.MessageFactory;


/**
 * Private utilities to enable interactions between roles.
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
class InteractionUtilStub extends MessageFactory {

	/**
	 * @param message
	 * @param sender
	 * @param receiver
	 * @param date
	 */
	static void updateContext(Message message, RoleAddress sender, RoleAddress receiver, float date) {
		setCreationDate(message, date);
		setSender(message, sender);
		setReceiver(message, receiver);
	}
	
}