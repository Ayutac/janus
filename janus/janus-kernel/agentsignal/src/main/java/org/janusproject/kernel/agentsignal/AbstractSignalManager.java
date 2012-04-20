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
package org.janusproject.kernel.agentsignal;

import java.util.ArrayList;
import java.util.List;

import org.arakhne.vmutil.ReflectionUtil;
import org.janusproject.kernel.configuration.JanusProperties;
import org.janusproject.kernel.configuration.JanusProperty;

/**
 * Abstract implementation of a signal manager.
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 * @see InstantSignalManager
 * @see BufferedSignalManager
 * @since 0.5
 */
public abstract class AbstractSignalManager implements SignalManager, SignalListener {

	private SignalPolicy policy;
	private SignalManager parentManager;

	/**
	 * Signal listeners.
	 */
	protected List<SignalListener> listeners = null;

	/**
	 * Enqueued Signals.
	 */
	protected QueuedSignalAdapter<Signal> events = null;

	/** Top class of the signals.
	 */
	protected Class<? extends Signal> topClass = null;

	/**
	 * @param properties
	 */
	public AbstractSignalManager(JanusProperties properties) {
		this(properties, null);
	}

	/**
	 * @param properties
	 * @param parent is the signal manager that owns this submanager. All
	 * the signals will be fired by the parent manager.
	 */
	public AbstractSignalManager(JanusProperties properties, SignalManager parent) {
		this.parentManager = parent;
		SignalPolicy pol = null;
		if (this.parentManager!=null) {
			pol = this.parentManager.getPolicy();
		}
		if (pol==null && properties!=null) {
			String v = properties.getProperty(JanusProperty.JANUS_AGENT_SIGNAL_POLICY);
			if (v!=null) {
				try {
					pol = SignalPolicy.valueOf(v);
				}
				catch(Throwable _) {
					//
				}
			}
			
		}
		this.policy = pol==null ? SignalPolicy.FIRE_SIGNAL : pol;
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public SignalManager getParent() {
		return this.parentManager;
	}
	
	/** {@inheritDoc}
	 */
	@Override
	public Signal getSignal() {
		switch(this.policy) {
		case IGNORE_ALL:
		case FIRE_SIGNAL:
			break;
		case STORE_IN_QUEUE:
			if (this.events!=null) {
				return this.events.getFirstAvailableSignal();
			}
			break;
		}
		return null;
	}

	/** {@inheritDoc}
	 */
	@Override
	public boolean hasSignal() {
		switch(this.policy) {
		case IGNORE_ALL:
		case FIRE_SIGNAL:
			break;
		case STORE_IN_QUEUE:
			if (this.events!=null) {
				return this.events.getQueueSize()>0;
			}
			break;
		}
		return false;
	}

	/** Reset this manager: its parent, its events, and its listeners.
	 */
	public void reset() {
		if (this.listeners != null) {
			this.listeners.clear();
			this.listeners = null;
		}
		if (this.events != null) {
			this.events.clear();
			this.events = null;
		}
		if (this.parentManager!=null)
			this.parentManager.removeSignalListener(this);
		this.parentManager = null;
		this.topClass = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void addSignalListener(SignalListener listener) {
		if (this.listeners == null)
			this.listeners = new ArrayList<SignalListener>();
		this.listeners.add(listener);
		if (this.topClass == null) {
			if (this.parentManager!=null)
				this.parentManager.addSignalListener(this);
			this.topClass = listener.getSupportedSignalType();
		}
		else {
			Class<? extends Signal> t = listener.getSupportedSignalType();
			Class<?> newTop = ReflectionUtil.getCommonType(this.topClass, t);
			if (newTop!=null && Signal.class.isAssignableFrom(newTop)) {
				this.topClass = (Class<? extends Signal>)newTop;
			}
			else {
				this.topClass = null;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void removeSignalListener(SignalListener listener) {
		if (this.listeners != null) {
			this.listeners.remove(listener);
			if (this.listeners.isEmpty()) {
				if (this.topClass != null) {
					if (this.parentManager!=null)
						this.parentManager.removeSignalListener(this);
					this.topClass = null;
					Class<?> t = null;
					for(SignalListener oldListener : this.listeners) {
						if (t==null) {
							t = oldListener.getSupportedSignalType();
						}
						else {
							t = ReflectionUtil.getCommonType(t, oldListener.getSupportedSignalType());
						}
					}
					if (t!=null && Signal.class.isAssignableFrom(t)) {
						this.topClass = (Class<? extends Signal>)t;
					}
					else {
						this.topClass = null;
					}
				}
				this.listeners = null;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fireSignal(Signal signal) {
		if (this.parentManager!=null) {
			this.parentManager.fireSignal(signal);
		}
		else {
			onSignal(signal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSupportedSignalType(Class<? extends Signal> type) {
		assert (type != null);
		return this.topClass == null
				|| this.topClass.isAssignableFrom(type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<? extends Signal> getSupportedSignalType() {
		return this.topClass == null ? Signal.class : this.topClass;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SignalPolicy getPolicy() {
		return this.policy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPolicy(SignalPolicy policy) {
		this.policy = policy;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.events==null ? "[]" : this.events.toString(); //$NON-NLS-1$
	}

}
