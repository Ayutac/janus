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
package org.janusproject.demos.jaak.ants.environment;

import java.awt.Color;

/** This class defines a pheromon which is permitting to return to a food source.
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class FoodPheromone extends Pheromone {

	private static final long serialVersionUID = 4699281292431261802L;

	/** Semantic of a colony pheromone.
	 */
	public static Object SEMANTIC = new Object(); 
	
	/** Evaporation amount for the pheromone.
	 */
	public static float EVAPORATION = 50f; 
	
	/**
	 */
	public FoodPheromone() {
		this(10f);
	}
		
	/**
	 * @param amount
	 */
	public FoodPheromone(float amount) {
		super(Math.max(0, amount), EVAPORATION, SEMANTIC, Color.PINK);
	}

}