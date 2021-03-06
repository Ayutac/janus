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
package org.janusproject.jaak.environment.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.arakhne.afc.math.continous.object2d.Vector2f;
import org.arakhne.afc.math.discrete.object2d.Point2i;
import org.janusproject.jaak.envinterface.EnvironmentArea;
import org.janusproject.jaak.envinterface.body.TurtleBody;
import org.janusproject.jaak.envinterface.body.TurtleBodyFactory;
import org.janusproject.jaak.envinterface.frustum.SquareTurtleFrustum;
import org.janusproject.jaak.envinterface.frustum.TurtleFrustum;
import org.janusproject.jaak.envinterface.influence.Influence;
import org.janusproject.jaak.envinterface.perception.EnvironmentalObject;
import org.janusproject.jaak.envinterface.perception.PerceivedTurtle;
import org.janusproject.jaak.envinterface.perception.StandardObjectManipulator;
import org.janusproject.jaak.envinterface.time.JaakTimeManager;
import org.janusproject.jaak.environment.ValidationResult;
import org.janusproject.jaak.environment.endogenousengine.EnvironmentEndogenousEngine;
import org.janusproject.jaak.environment.solver.ActionApplier;
import org.janusproject.jaak.environment.solver.InfluenceSolver;
import org.janusproject.jaak.environment.solver.PathBasedInfluenceSolver;
import org.janusproject.kernel.address.AgentAddress;
import org.janusproject.kernel.util.multicollection.MultiCollection;
import org.janusproject.kernel.util.random.RandomNumber;

/** This class defines the Jaak environment model.
 * <p>
 * The environment is a grid composed of cells.
 * Each cell is able to contain zero or one turtle
 * body and many environmental objects.
 * <p>
 * The environment may be wrapped or not. When the
 * environment is wrapped and a turtle is trying
 * to go outside the environment grid, it is moved
 * on the opposite side of the grid.
 * If the envrionment is not wrapped, when a turtle
 * is trying to move outside the grid, it is moved
 * until it reach the border of the grid. 
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class JaakEnvironment implements EnvironmentArea {

	/** Defines the default perception distance for turtles.
	 */
	public static final int DEFAULT_PERCEPTION_DISTANCE = 7;
	
	private final UUID id = UUID.randomUUID();
	private final Map<AgentAddress,RealTurtleBody> bodies = new TreeMap<AgentAddress,RealTurtleBody>();
	private final JaakGrid grid;
	private JaakTimeManager timeManager;
	private final AtomicBoolean isWrapped = new AtomicBoolean(false);
	private volatile EnvironmentEndogenousEngine endogenousEngine = null;
	private volatile Collection<Influence> endogenousInfluences = null;
	private volatile InfluenceSolver<RealTurtleBody> solver = null;
	private int receivedInfluences = 0;
	private float lastSimulationTime = Float.NaN;
	
	private final LinkedList<JaakEnvironmentListener> listeners = new LinkedList<JaakEnvironmentListener>();
	
	private final RealTurtleBodyFactory factory = new RealTurtleBodyFactory();
	
	/**
	 * @param width is the width of the world grid.
	 * @param height is the height of the world grid.
	 * @param timeManager is the time manager used to run Jaak.
	 */
	public JaakEnvironment(int width, int height, JaakTimeManager timeManager) {
		this.grid = new JaakGrid(width, height, new StandardObjectManipulator());
		this.timeManager = timeManager;
	}
	
	/**
	 * @param width is the width of the world grid.
	 * @param height is the height of the world grid.
	 */
	public JaakEnvironment(int width, int height) {
		this(width, height, null);
	}

	/** Add listener on environment events.
	 * 
	 * @param listener
	 */
	public void addJaakEnvironmentListener(JaakEnvironmentListener listener) {
		synchronized(this.listeners) {
			this.listeners.add(listener);
		}
	}
	
	/** Remove listener on environment events.
	 * 
	 * @param listener
	 */
	public void removeJaakEnvironmentListener(JaakEnvironmentListener listener) {
		synchronized(this.listeners) {
			this.listeners.remove(listener);
		}
	}

	/** Fire pre agent scheudling event.
	 */
	protected void firePreAgentScheduling() {
		JaakEnvironmentListener[] list;
		synchronized(this.listeners) {
			list = new JaakEnvironmentListener[this.listeners.size()];
			this.listeners.toArray(list);
		}
		for(JaakEnvironmentListener listener : list) {
			listener.preAgentScheduling();
		}
	}

	/** Fire post agent scheudling event.
	 */
	protected void firePostAgentScheduling() {
		JaakEnvironmentListener[] list;
		synchronized(this.listeners) {
			list = new JaakEnvironmentListener[this.listeners.size()];
			this.listeners.toArray(list);
		}
		for(JaakEnvironmentListener listener : list) {
			listener.postAgentScheduling();
		}
	}

	/** {@inheritDoc}
	 */
	@Override
	public int getX() {
		return 0;
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public int getY() {
		return 0;
	}

	/** {@inheritDoc}
	 */
	@Override
	public int getWidth() {
		return this.grid.getWidth();
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public int getHeight() {
		return this.grid.getHeight();
	}

	/** Set the time manager for the environment.
	 * 
	 * @param timeManager is the time manager which must be used by the environment.
	 */
	public void setTimeManager(JaakTimeManager timeManager) {
		assert(timeManager!=null);
		this.timeManager = timeManager;
	}
	
	/** Replies the action applier for this environment.
	 * 
	 * @return the action applier for this environment.
	 */
	public ActionApplier getActionApplier() {
		return this.grid;
	}
	
	/** Replies the unique identifier of this environment object.
	 * 
	 * @return the identifier of the environment.
	 */
	public UUID getIdentifier() {
		return this.id;
	}
	
	/** Replies a factory for the bodies which is connected to this environment.
	 * <p>
	 * Caution: this factory create bodies, and adds them inside the environment.
	 * 
	 * @return a body factory.
	 */
	public TurtleBodyFactory getTurtleBodyFactory() {
		return this.factory;
	}
	
	/** Replies if the environment is wrapped.
	 * 
	 * @return <code>true</code> if the environment is
	 * wrapped, otherwise <code>false</code>.
	 */
	public boolean isWrapped() {
		return this.isWrapped.get();
	}
	
	/** Change the wrapping flag of the environment.
	 * 
	 * @param wrapped indicates if the environment is
	 * wrapped or not.
	 */
	public void setWrapped(boolean wrapped) {
		this.isWrapped.set(wrapped);
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public boolean isFree(Point2i position) {
		assert(position!=null);
		return (this.grid.isFree(position.x(), position.y()));
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public boolean hasObstacle(Point2i position) {
		assert(position!=null);
		return (this.grid.hasObstacle(position.x(), position.y()));
	}

	/** Replies if a turtle body is on the cell at the
	 * given coordinate.
	 * 
	 * @param x is the coordinate of the cell.
	 * @param y is the coordinate of the cell.
	 * @return <code>true</code> if a turtle body is 
	 * on the cell, otherwise <code>false</code>.
	 * @throws IndexOutOfBoundsException if the given position
	 * is outside the grid.
	 */
	public synchronized boolean hasTurtle(int x, int y) {
		return this.grid.getTurtle(x, y)!=null;
	}
	
	/** Replies the instant speed of the turtle at the given position.
	 * 
	 * @param x is the coordinate of the cell.
	 * @param y is the coordinate of the cell.
	 * @return the instant speed of the turtle in cells per second,
	 * or {@link Float#NaN} if no turtle.
	 * @throws IndexOutOfBoundsException if the given position
	 * is outside the grid.
	 */
	public synchronized float getTurtleSpeed(int x, int y) {
		TurtleBody body = this.grid.getTurtle(x, y);
		if (body!=null) {
			return body.getSpeed();
		}
		return Float.NaN;
	}

	/** Replies the turtles at the given position.
	 * 
	 * @param x is the coordinate of the cell.
	 * @param y is the coordinate of the cell.
	 * @return the turtles even if they are in a burrow.
	 * @throws IndexOutOfBoundsException if the given position
	 * is outside the grid.
	 */
	public synchronized Collection<TurtleBody> getTurtles(int x, int y) {
		return this.grid.getTurtles(x, y);
	}

	/** Replies the instant orientation of the turtle at the given position.
	 * 
	 * @param x is the coordinate of the cell.
	 * @param y is the coordinate of the cell.
	 * @return the instant orientation of the turtle in radians,
	 * or {@link Float#NaN} if no turtle.
	 * @throws IndexOutOfBoundsException if the given position
	 * is outside the grid.
	 */
	public synchronized float getTurtleOrientation(int x, int y) {
		TurtleBody body = this.grid.getTurtle(x, y);
		if (body!=null) {
			return body.getHeadingAngle();
		}
		return Float.NaN;
	}

	/** Replies the instant direction of the turtle at the given position.
	 * 
	 * @param x is the coordinate of the cell.
	 * @param y is the coordinate of the cell.
	 * @return the instant direction of the turtle in radians,
	 * or <code>null</code> if no turtle.
	 * @throws IndexOutOfBoundsException if the given position
	 * is outside the grid.
	 */
	public synchronized Vector2f getTurtleDirection(int x, int y) {
		TurtleBody body = this.grid.getTurtle(x, y);
		if (body!=null) {
			return body.getHeadingVector();
		}
		return null;
	}

	/** Replies the count of turtle in the environment.
	 * 
	 * @return the count of turtle in the environment.
	 */
	public synchronized int getTurtleCount() {
		return this.bodies.size();
	}
	
	/** Replies the objects in the cell at the given 
	 * position.
	 *
	 * @param <T> is the type of the desired objects.
	 * @param x is the position of the cell.
	 * @param y is the position of the cell.
	 * @param type is the type of the desired objects.
	 * @return the environment objects in the given cell. 
	 */
	public synchronized <T extends EnvironmentalObject> Iterable<T> getEnvironmentalObjects(int x, int y, Class<T> type) {
		return new FilteringIterable<T>(type, this.grid.getObjects(x, y));
	}

	/** Replies the objects in the cell at the given 
	 * position.
	 *
	 * @param x is the position of the cell.
	 * @param y is the position of the cell.
	 * @return the environment objects in the given cell. 
	 */
	public synchronized Collection<EnvironmentalObject> getEnvironmentalObjects(int x, int y) {
		return this.grid.getObjects(x, y);
	}

	/** Set the endogenous engine to use.
	 * 
	 * @param engine is the endogenous engine to use.
	 */
	public void setEndogenousEngine(EnvironmentEndogenousEngine engine) {
		this.endogenousEngine = engine;
	}
	
	/** Set the solver of influence conflicts.
	 * 
	 * @param solver is the solver of influence conflicts to use.
	 */
	public void setInfluenceSolver(InfluenceSolver<RealTurtleBody> solver) {
		this.solver = solver;
	}

	/** Replies a free cell near a random position.
	 * 
	 * @return the position of the first free cell, or <code>null</code>
	 * if no more cell is free.
	 */
	Point2i getFreeRandomPosition() {
		Point2i p = new Point2i();
		int i=0;
		int max = this.grid.getWidth() * this.grid.getHeight();
		do {
			p.set(
					RandomNumber.nextInt(this.grid.getWidth()),
					RandomNumber.nextInt(this.grid.getHeight()));
			++i;
		}
		while (!isFree(p) && i<max);
		return (isFree(p)) ? p : null;
	}

	/** Add a body in the environment.
	 * 
	 * @param body is the body to add.
	 * @param position is the position of the body.
	 * @return <code>true</code> if the body was successfully added,
	 * <code>false</code> otherwise.
	 */
	synchronized boolean addBody(RealTurtleBody body, Point2i position) {
		assert(body!=null);
		assert(position!=null);
		if (!this.bodies.containsKey(body.getTurtleId())) {
			if (this.grid.putTurtle(position.x(), position.y(), body)) {
				this.bodies.put(body.getTurtleId(), body);
				body.setPhysicalState(
						position.x(), position.y(),
						body.getHeadingAngle(),
						0f);
				return true;
			}
		}
		return false;
	}
	
	/** Remove a body from the environment.
	 * 
	 * @param turtle is the identifier of the turtle for which the body must be removed.
	 * @return the success state of the removal action.
	 */
	public synchronized boolean removeBodyFor(AgentAddress turtle) {
		if (turtle!=null) {
			RealTurtleBody body = this.bodies.remove(turtle);
			if (body!=null) {
				Point2i position = body.getPosition();
				this.grid.removeTurtle(position.x(), position.y(), body);
				return true;
			}
		}
		return false;
	}

	/** Replies if the given address is associated to a body in the environment.
	 * 
	 * @param turtle is the identifier of the turtle for which the body must be removed.
	 * @return <code>true</code> if the turtle has a body, otherwise <code>false</code>.
	 */
	public synchronized boolean hasBodyFor(AgentAddress turtle) {
		if (turtle!=null) {
			return this.bodies.containsKey(turtle);
		}
		return false;
	}

	/** Invoked by a body to notify the environment it has received an influence
	 * from its turtle.
	 */
	synchronized void receiveInfluence() {
		++ this.receivedInfluences;
	}
	
	/** Replies if all expected influences were received.
	 * 
	 * @return <code>true</code> if the expected number of received influences
	 * was reached for the current simulation step, or <code>false</code> if not.
	 */
	public synchronized boolean isAllInfluencesReceived() {
		return this.receivedInfluences>=this.bodies.size();
	}

	/** Run the environment behaviour before any turtle execution.
	 */
	public synchronized void runPreTurtles() {
		this.receivedInfluences = 0;
		computePerceptions();
		firePreAgentScheduling();
	}
	
	/** Run the environment behaviour after all turtle executions.
	 */
	public synchronized void runPostTurtles() {
		runEndogenousEngine();
		solveConflicts();
		this.receivedInfluences = 0;
		firePostAgentScheduling();
	}

	private void runEndogenousEngine() {
		// Run autonomous environmental processes
		float currentTime = this.timeManager.getCurrentTime(TimeUnit.SECONDS);
		float simulationStepDuration;
		if (Float.isNaN(this.lastSimulationTime)) {
			simulationStepDuration = 0f;
		}
		else {
			simulationStepDuration = currentTime - this.lastSimulationTime;
		}
		
		MultiCollection<Influence> endoInfluences = new MultiCollection<Influence>();
		
		this.lastSimulationTime = currentTime;
		Collection<Influence> col = this.grid.runAutonomousProcesses(currentTime, simulationStepDuration);
		if (col!=null && !col.isEmpty()) endoInfluences.addCollection(col);
		
		// Run endogenous engine
		EnvironmentEndogenousEngine engine = this.endogenousEngine;
		if (engine!=null) {
			col = engine.computeInfluences(this.grid, this.timeManager);
			if (col!=null && !col.isEmpty()) endoInfluences.addCollection(col);
		}
		
		if (!endoInfluences.isEmpty()) {
			this.endogenousInfluences = endoInfluences;
		}
	}
	
	private void computePerceptions() {
		Point2i position;
		TurtleFrustum frustum;
		TurtleBody turtleBody;
		Iterator<Point2i> iterator;
		List<PerceivedTurtle> bodies;
		MultiCollection<EnvironmentalObject> objects;
		int x,y;
		for(RealTurtleBody body : this.bodies.values()) {
			bodies = new ArrayList<PerceivedTurtle>();
			objects = new MultiCollection<EnvironmentalObject>();
			if (body.isPerceptionEnable()) {
				frustum = body.getPerceptionFrustum();
				if (frustum!=null) {
					iterator = frustum.getPerceivedCells(body.getPosition(), body.getHeadingAngle(), this);
					if (iterator!=null) {
						while (iterator.hasNext()) {
							position = iterator.next();
							if (this.grid.validatePosition(isWrapped(), true, position)!=ValidationResult.DISCARDED) {
								x = position.x();
								y = position.y();
								turtleBody = this.grid.getTurtle(x, y);
								if (turtleBody!=null && turtleBody!=body) {
									bodies.add(new PerceivedTurtle(
											turtleBody.getTurtleId(),
											new Point2i(position),
											turtleBody.getPosition(),
											turtleBody.getSpeed(),
											turtleBody.getHeadingAngle(),
											turtleBody.getSemantic()));
								}
								objects.addCollection(this.grid.getObjects(x, y));
							}
						}
					}
				}
			}
			body.setPerceptions(bodies, objects);
		}
	}
	
	private void solveConflicts() {
		InfluenceSolver<RealTurtleBody> theSolver = this.solver;
		if (theSolver==null) {
			this.solver = theSolver = new PathBasedInfluenceSolver();
			theSolver.setGridModel(this.grid);
			theSolver.setTimeManager(this.timeManager);
		}
		theSolver.setWrapped(isWrapped());
		theSolver.solve(this.endogenousInfluences, this.bodies.values(), getActionApplier());
	}
	
	/** This class defines an iterable object which is able to filter
	 * its content.
	 * 
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private static class FilteringIterable<T> implements Iterable<T> {
		
		private final Class<T> type;
		private final Collection<?> collection;
		
		/**
		 * @param type is the type of the elements to reply
		 * @param collection is the collection to iterate on.
		 */
		public FilteringIterable(Class<T> type, Collection<?> collection) {
			this.type = type;
			this.collection = collection;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<T> iterator() {
			return new FilteringIterator<T>(this.type, this.collection.iterator());
		}
		
	}

	/** This class defines an iterable object which is able to filter
	 * its content.
	 * 
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private static class FilteringIterator<T> implements Iterator<T> {
		
		private final Class<T> type;
		private final Iterator<?> iterator;
		private T next;
		
		/**
		 * @param type is the type of the elements to reply
		 * @param iterator is the iterator to use.
		 */
		public FilteringIterator(Class<T> type, Iterator<?> iterator) {
			this.type = type;
			this.iterator = iterator;
			searchNext();
		}
		
		private void searchNext() {
			this.next = null;
			Object o;
			while (this.iterator.hasNext()) {
				o = this.iterator.next();
				if (o!=null && this.type.isInstance(o)) {
					this.next = this.type.cast(o);
					return;
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return this.next!=null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T next() {
			T n = this.next;
			searchNext();
			return n;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			//
		}
		
	}

	/** This class defines an implementation of turtle body factory.
	 * 
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private class RealTurtleBodyFactory implements TurtleBodyFactory {

		/**
		 */
		public RealTurtleBodyFactory() {
			//
		}
		
		private TurtleFrustum getDefaultFrustum() {
			return new SquareTurtleFrustum(DEFAULT_PERCEPTION_DISTANCE);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isFreeCell(Point2i position) {
			return isFree(position);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(
				AgentAddress turtleId,
				Point2i desiredPosition,
				float desiredAngle,
				Object semantic) {
			return createTurtleBody(
					turtleId, desiredPosition,
					desiredAngle, semantic,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(
				AgentAddress turtleId,
				Point2i desiredPosition,
				float desiredAngle) {
			return createTurtleBody(
					turtleId, desiredPosition,
					desiredAngle,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(
				AgentAddress turtleId,
				Point2i desiredPosition) {
			return createTurtleBody(
					turtleId, desiredPosition,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId) {
			return createTurtleBody(
					turtleId,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(
				AgentAddress turtleId,
				float desiredAngle,
				Object semantic) {
			return createTurtleBody(
					turtleId,
					desiredAngle, semantic,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(
				AgentAddress turtleId,
				Object semantic) {
			return createTurtleBody(
					turtleId, semantic,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(
				AgentAddress turtleId,
				Point2i desiredPosition,
				Object semantic) {
			return createTurtleBody(
					turtleId, desiredPosition,
					semantic,
					getDefaultFrustum());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				Point2i desiredPosition, float desiredAngle, Object semantic,
				TurtleFrustum frustum) {
			RealTurtleBody body = new RealTurtleBody(JaakEnvironment.this, turtleId, frustum, desiredAngle, semantic);
			Point2i position = null;
			if (desiredPosition==null) position = getFreeRandomPosition();
			else if (isFree(desiredPosition)) position = desiredPosition;
			
			if (position==null) return null;

			if (JaakEnvironment.this.addBody(body, position))
				return body;
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				Point2i desiredPosition, float desiredAngle,
				TurtleFrustum frustum) {
			return createTurtleBody(turtleId,
					desiredPosition, desiredAngle,
					null, frustum);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				Point2i desiredPosition, TurtleFrustum frustum) {
			return createTurtleBody(turtleId,
					desiredPosition, Float.NaN,
					null, frustum);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				TurtleFrustum frustum) {
			return createTurtleBody(turtleId,
					null, Float.NaN,
					null, frustum);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				float desiredAngle, Object semantic, TurtleFrustum frustum) {
			return createTurtleBody(turtleId,
					null, desiredAngle,
					semantic, frustum);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				Object semantic, TurtleFrustum frustum) {
			return createTurtleBody(turtleId,
					null, Float.NaN,
					semantic, frustum);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TurtleBody createTurtleBody(AgentAddress turtleId,
				Point2i desiredPosition, Object semantic, TurtleFrustum frustum) {
			return createTurtleBody(turtleId,
					desiredPosition, Float.NaN,
					semantic, frustum);
		}
			
	} /* class RealTurtleBodyFactory */
	
}