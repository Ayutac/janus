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
package org.janusproject.kernel.util.prototype;

import org.arakhne.afc.vmutil.locale.Locale;

/**
 * Exception when a capacity invocation does not
 * respect the capacity prototype.
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public enum PrototypeScope {

	/** Exception during input parameter validation.
	 */
	INPUT,

	/** Exception during output parameter validation.
	 */
	OUTPUT;

	/** Replies the localized string representation
	 * for this scope.
	 * 
	 * @return a localized string representation.
	 */
	public String toLocalizedString() {
		return Locale.getString(PrototypeValidator.class, name());
	}

}
