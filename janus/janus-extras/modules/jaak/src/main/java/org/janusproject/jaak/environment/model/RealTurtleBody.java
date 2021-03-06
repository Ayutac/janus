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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.arakhne.afc.math.MathUtil;
import org.arakhne.afc.math.continous.object2d.Vector2f;
import org.arakhne.afc.math.discrete.object2d.Point2i;
import org.janusproject.jaak.envinterface.body.TurtleBody;
import org.janusproject.jaak.envinterface.frustum.TurtleFrustum;
import org.janusproject.jaak.envinterface.influence.DropDownInfluence;
import org.janusproject.jaak.envinterface.influence.Influence;
import org.janusproject.jaak.envinterface.influence.MotionInfluence;
import org.janusproject.jaak.envinterface.influence.MotionInfluenceStatus;
import org.janusproject.jaak.envinterface.influence.PickUpInfluence;
import org.janusproject.jaak.envinterface.perception.EnvironmentalObject;
import org.janusproject.jaak.envinterface.perception.Perceivable;
import org.janusproject.jaak.envinterface.perception.PerceivedTurtle;
import org.janusproject.jaak.envinterface.perception.PickedObject;
import org.janusproject.kernel.address.AgentAddress;
import org.janusproject.kernel.util.multicollection.MultiCollection;

/** This class defines an implementation of turtle body.
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public final class RealTurtleBody implements TurtleBody, Comparable<RealTurtleBody> {

	private final WeakReference<JaakEnvironment> environment;
	
	private final AgentAddress turtle;
	private final TurtleFrustum frustum;
	private Object semantic;
	private Collection<EnvironmentalObject> perceivedObjects = null;
	private Collection<PerceivedTurtle> perceivedBodies = null;
	private Collection<PickedObject> pickingResults = null;
	private MotionInfluence lastMotionInfluence = null;
	private MotionInfluenceStatus lastMotionInfluenceStatus = null;
	private List<Influence> otherInfluences = null;
	private int x, y;
	private float heading = 0;
	private Vector2f headingVector = null;
	private float speed = 0f;
	private boolean isPerceptionEnable = true;

	/**
	 * @param environment is the reference to the situated environment in which this body is located.
	 * @param turtle is the identifier of the turtle which is owning this body.
	 * @param frustum is the perception frustum to be used by this body, or <code>null</code>
	 * if this body is not able to perceive.
	 * @param headingAngle is the orientation angle of the turtle head.
	 * @param semantic is the semantic associated with the body.
	 */
	RealTurtleBody(
			JaakEnvironment environment,
			AgentAddress turtle,
			TurtleFrustum frustum,
			float headingAngle,
			Object semantic) {
		assert(environment!=null);
		assert(turtle!=null);
		this.environment = new WeakReference<JaakEnvironment>(environment);
		this.turtle = turtle;
		this.frustum = frustum;
		this.semantic = semantic;
		this.heading = headingAngle;
	}
	
	private void fireInfluenceReception() {
		if (this.lastMotionInfluence==null &&
				(this.otherInfluences==null
						||this.otherInfluences.isEmpty())) {
			this.environment.get().receiveInfluence();
		}
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public synchronized boolean hasInfluences() {
		return this.lastMotionInfluence!=null ||
			(this.otherInfluences!=null && !this.otherInfluences.isEmpty());
	}

	/** Register the perceptions for this body.
	 * 
	 * @param bodies are the perceptions of turtle bodies.
	 * @param objects are the perceptions of environmental objects.
	 */
	synchronized void setPerceptions(Collection<PerceivedTurtle> bodies, Collection<EnvironmentalObject> objects) {
		this.lastMotionInfluence = null;
		this.otherInfluences = null;
		assert(bodies!=null);
		assert(objects!=null);
		this.perceivedBodies = bodies;
		this.perceivedObjects = objects;
	}

	/**
	 * Save the given picking action for the next perception query.
	 * <p>
	 * The picking actions are put back in the turtle perceptions to
	 * notify the turtle about a picking influence result.
	 * 
	 * @param action is the picking action description.
	 */
	synchronized void putBackPickingAction(PickedObject action) {
		if (action!=null) {
			if (this.pickingResults==null)
				this.pickingResults = new LinkedList<PickedObject>();
			this.pickingResults.add(action);
		}
	}

	/**
	 * Save the status of the last motion influence.
	 * 
	 * @param status is the status of the last motion influence.
	 */
	synchronized void putBackMotionInfluenceStatus(MotionInfluenceStatus status) {
		this.lastMotionInfluenceStatus = status;
	}

	/** Update the physical attribute of the body, ie. its position and
	 * its orientation.
	 * 
	 * @param x is the new position of the turtle body.
	 * @param y is the new position of the turtle body.
	 * @param heading is the orientation of the turtle body head.
	 * @param speed is the speed of the body in cells per second.
	 */
	synchronized void setPhysicalState(int x, int y, float heading, float speed) {
		this.x = x;
		this.y = y;
		this.heading = MathUtil.clampRadian(heading);
		this.headingVector = null;
		this.speed = speed;
	}

	/** Replies the last motion influence stored by the body and remove it
	 * from the body.
	 * 
	 * @return the collected motion influence.
	 */
	public synchronized MotionInfluence consumeMotionInfluence() {
		MotionInfluence inf = this.lastMotionInfluence;
		this.lastMotionInfluence = null;
		return inf;
	}
	
	/** Replies the not-motion influences stored by the body and remove them
	 * from the body.
	 * 
	 * @return the collected motion influence.
	 */
	public synchronized List<Influence> consumeOtherInfluences() {
		List<Influence> infs = this.otherInfluences;
		this.otherInfluences = null;
		this.pickingResults = null;
		return infs;
	}

	/** {@inheritDoc}
	 */
	@Override
	public synchronized void beIddle() {
		fireInfluenceReception();
		this.lastMotionInfluence = null;
		this.otherInfluences = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void dropOff(EnvironmentalObject object) {
		fireInfluenceReception();
		if (this.otherInfluences==null)
			this.otherInfluences = new LinkedList<Influence>();
		this.otherInfluences.add(new DropDownInfluence(this, object));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized <T extends Perceivable> T pickUp(Class<T> type) {
		assert(type!=null);
		if (this.perceivedObjects==null || this.perceivedObjects.isEmpty()) return null;
		Iterator<EnvironmentalObject> iterator = this.perceivedObjects.iterator();
		EnvironmentalObject obj;
		while (iterator.hasNext()) {
			obj = iterator.next();
			assert(obj!=null);
			if (type.isInstance(obj) && getPosition().equals(obj.getPosition())) {
				fireInfluenceReception();
				if (this.otherInfluences==null)
					this.otherInfluences = new LinkedList<Influence>();
				this.otherInfluences.add(new PickUpInfluence(this, obj));
				return type.cast(obj);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvironmentalObject pickUpWithSemantic(Object semantic) {
		if (semantic!=null) {
			if (this.perceivedObjects==null || this.perceivedObjects.isEmpty()) return null;
			Iterator<EnvironmentalObject> iterator = this.perceivedObjects.iterator();
			EnvironmentalObject obj;
			Object sem;
			while (iterator.hasNext()) {
				obj = iterator.next();
				assert(obj!=null);
				sem = obj.getSemantic();
				if (sem!=null && sem.equals(semantic) && getPosition().equals(obj.getPosition())) {
					fireInfluenceReception();
					if (this.otherInfluences==null)
						this.otherInfluences = new LinkedList<Influence>();
					this.otherInfluences.add(new PickUpInfluence(this, obj));
					return obj;
				}
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void pickUp(EnvironmentalObject object) {
		fireInfluenceReception();
		if (this.otherInfluences==null)
			this.otherInfluences = new LinkedList<Influence>();
		this.otherInfluences.add(new PickUpInfluence(this, object));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized <T extends EnvironmentalObject> T touchUp(Class<T> type) {
		assert(type!=null);
		if (this.perceivedObjects==null || this.perceivedObjects.isEmpty()) return null;
		Iterator<EnvironmentalObject> iterator = this.perceivedObjects.iterator();
		EnvironmentalObject obj;
		while (iterator.hasNext()) {
			obj = iterator.next();
			assert(obj!=null);
			if (type.isInstance(obj) && getPosition().equals(obj.getPosition())) {
				return type.cast(obj);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvironmentalObject touchUpWithSemantic(Object semantic) {
		if (semantic!=null) {
			if (this.perceivedObjects==null || this.perceivedObjects.isEmpty()) return null;
			Iterator<EnvironmentalObject> iterator = this.perceivedObjects.iterator();
			EnvironmentalObject obj;
			Object sem;
			while (iterator.hasNext()) {
				obj = iterator.next();
				assert(obj!=null);
				sem = obj.getSemantic();
				if (sem!=null && sem.equals(semantic) && getPosition().equals(obj.getPosition())) {
					return obj;
				}
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void move(Vector2f direction, boolean changeHeading) {
		assert(direction!=null);
		fireInfluenceReception();

		if (this.lastMotionInfluence==null) {
			this.lastMotionInfluence = new MotionInfluence(this, new Vector2f(direction.getX(),direction.getY()));
		}
		else {
			this.lastMotionInfluence.setLinearMotion(direction.getX(), direction.getY());
		}
		
		if (changeHeading) setHeading(direction);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void moveBackward(int cells) {
		if (cells>0) {
			fireInfluenceReception();
			Vector2f head = getHeadingVector();
			float f = -cells / head.length();
			float x = head.getX() * f;
			float y = head.getY() * f;
			if (this.lastMotionInfluence==null) {
				this.lastMotionInfluence = new MotionInfluence(this, new Vector2f(x,y));
			}
			else {
				this.lastMotionInfluence.setLinearMotion(x, y);
			}
			this.lastMotionInfluence.setAngularMotion(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void moveForward(int cells) {
		if (cells>0) {
			fireInfluenceReception();
			Vector2f head = getHeadingVector();
			float x = head.getX() * cells;
			float y = head.getY() * cells;
			if (this.lastMotionInfluence==null) {
				this.lastMotionInfluence = new MotionInfluence(this, new Vector2f(x,y));
			}
			else {
				this.lastMotionInfluence.setLinearMotion(x, y);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void setHeading(float radians) {
		fireInfluenceReception();
		float v = radians - this.heading;
		if (this.lastMotionInfluence==null) {
			this.lastMotionInfluence = new MotionInfluence(this, v);
		}
		else {
			this.lastMotionInfluence.setAngularMotion(v);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void setHeading(Vector2f direction) {
		assert(direction!=null);
		setHeading(direction.getOrientationAngle());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void turnLeft(float radians) {
		fireInfluenceReception();
		if (this.lastMotionInfluence==null) {
			this.lastMotionInfluence = new MotionInfluence(this, -radians);
		}
		else {
			this.lastMotionInfluence.setAngularMotion(-radians);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void turnRight(float radians) {
		fireInfluenceReception();
		if (this.lastMotionInfluence==null) {
			this.lastMotionInfluence = new MotionInfluence(this, radians);
		}
		else {
			this.lastMotionInfluence.setAngularMotion(radians);
		}
	}
		
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized float getHeadingAngle() {
		return this.heading;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Vector2f getHeadingVector() {
		if (this.headingVector==null)
			this.headingVector = Vector2f.toOrientationVector(this.heading);
		return this.headingVector;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<EnvironmentalObject> getPerceivedObjects() {
		if (this.perceivedObjects==null) throw new IllegalStateException();
		return Collections.unmodifiableCollection(this.perceivedObjects);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<PerceivedTurtle> getPerceivedTurtles() {
		if (this.perceivedBodies==null) throw new IllegalStateException();
		return Collections.unmodifiableCollection(this.perceivedBodies);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<Perceivable> getPerception() {
		if (this.perceivedBodies==null) throw new IllegalStateException();
		if (this.perceivedObjects==null) throw new IllegalStateException();
		MultiCollection<Perceivable> perceps = new MultiCollection<Perceivable>();
		perceps.addCollection(this.perceivedBodies);
		perceps.addCollection(this.perceivedObjects);
		if (this.pickingResults!=null && !this.pickingResults.isEmpty()) {
			perceps.addCollection(this.pickingResults);
		}
		return perceps;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized <T extends Perceivable> Collection<T> getPerception(Class<T> type) {
		assert(type!=null);
		if (this.perceivedBodies==null) throw new IllegalStateException();
		if (this.perceivedObjects==null) throw new IllegalStateException();
		Collection<T> perceps = new ArrayList<T>();
		if (PerceivedTurtle.class.isAssignableFrom(type)) {
			for(Perceivable p : this.perceivedBodies) {
				if (type.isInstance(p)) {
					perceps.add(type.cast(p));
				}
			}
		}
		if (EnvironmentalObject.class.isAssignableFrom(type)) {
			for(Perceivable p : this.perceivedObjects) {
				if (type.isInstance(p)) {
					perceps.add(type.cast(p));
				}
			}
		}
		if (this.pickingResults!=null && PickedObject.class.isAssignableFrom(type)) {
			for(Perceivable p : this.pickingResults) {
				if (type.isInstance(p)) {
					perceps.add(type.cast(p));
				}
			}
		}
		return perceps;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized <T extends Perceivable> T getFirstPerception(Class<T> type) {
		assert(type!=null);
		if (this.perceivedBodies==null) throw new IllegalStateException();
		if (this.perceivedObjects==null) throw new IllegalStateException();
		if (PerceivedTurtle.class.isAssignableFrom(type)) {
			for(Perceivable p : this.perceivedBodies) {
				if (type.isInstance(p)) {
					return type.cast(p);
				}
			}
		}
		if (EnvironmentalObject.class.isAssignableFrom(type)) {
			for(Perceivable p : this.perceivedObjects) {
				if (type.isInstance(p)) {
					return type.cast(p);
				}
			}
		}
		if (this.pickingResults!=null && PickedObject.class.isAssignableFrom(type)) {
			for(Perceivable p : this.pickingResults) {
				if (type.isInstance(p)) {
					return type.cast(p);
				}
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Point2i getPosition() {
		return new Point2i(this.x,this.y);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized int getX() {
		return this.x;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized int getY() {
		return this.y;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean hasPerceivedObject() {
		return this.perceivedObjects!=null && !this.perceivedObjects.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean hasPerceivedTurtle() {
		return this.perceivedBodies!=null && !this.perceivedBodies.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasPerception() {
		return hasPerceivedObject() || hasPerceivedTurtle();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(RealTurtleBody o) {
		return this.turtle.compareTo(o.turtle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TurtleFrustum getPerceptionFrustum() {
		return this.frustum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getSemantic() {
		return this.semantic;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSemantic(Object semantic) {
		this.semantic = semantic;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AgentAddress getTurtleId() {
		return this.turtle;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getSpeed() {
		return this.speed;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPerceptionEnable(boolean enable) {
		this.isPerceptionEnable = enable;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPerceptionEnable() {
		return this.isPerceptionEnable;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MotionInfluenceStatus getLastMotionInfluenceStatus() {
		return this.lastMotionInfluenceStatus==null ?
				MotionInfluenceStatus.NOT_AVAILABLE : this.lastMotionInfluenceStatus;
	}

}