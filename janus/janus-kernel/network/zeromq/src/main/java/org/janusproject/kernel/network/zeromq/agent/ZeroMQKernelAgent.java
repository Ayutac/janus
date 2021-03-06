/* 
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on <http://www.janus-project.org>
 * Copyright (C) 2012-2013 Janus Core Developers
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
package org.janusproject.kernel.network.zeromq.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.arakhne.afc.vmutil.locale.Locale;
import org.janusproject.kernel.address.Address;
import org.janusproject.kernel.address.AgentAddress;
import org.janusproject.kernel.agent.AgentActivator;
import org.janusproject.kernel.agent.KernelAgent;
import org.janusproject.kernel.configuration.JanusProperties;
import org.janusproject.kernel.configuration.JanusProperty;
import org.janusproject.kernel.crio.core.AddressUtil;
import org.janusproject.kernel.crio.core.GroupAddress;
import org.janusproject.kernel.crio.core.Organization;
import org.janusproject.kernel.crio.core.Role;
import org.janusproject.kernel.crio.core.RoleAddress;
import org.janusproject.kernel.crio.organization.Group;
import org.janusproject.kernel.crio.organization.GroupCondition;
import org.janusproject.kernel.crio.organization.MembershipService;
import org.janusproject.kernel.message.Message;
import org.janusproject.kernel.network.NetworkAdapter;
import org.janusproject.kernel.network.NetworkListener;
import org.janusproject.kernel.network.zeromq.zeromq.ZeroMQNode;
import org.janusproject.kernel.repository.RepositoryChangeEvent;
import org.janusproject.kernel.repository.RepositoryChangeEvent.ChangeType;
import org.janusproject.kernel.repository.RepositoryChangeListener;
import org.janusproject.kernel.repository.RepositoryOverlooker;
import org.janusproject.kernel.status.Status;
import org.janusproject.kernel.util.throwable.Throwables;

/**
 * Agent that represents and run the kernel of the Janus platform.
 * <p>
 * If the kernel agent is suicidable, it means that it will stop its execution
 * if no more other agent exists. If the kernel agent is not suicidable, it will
 * persist even if no more other agent is registered.
 * <p>
 * This kernel agent supports networking.
 * 
 * @author $Author: bfeld$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class ZeroMQKernelAgent extends KernelAgent implements
		RepositoryChangeListener, NetworkListener {

	private static final long serialVersionUID = -8244141754953009460L;

	/**
	 * Delay between two tries to remove the no-more-used groups (in minutes).
	 */
	public static final long EMPTY_GROUP_CLEANING_DELAY = 10;

	private final NetworkAdapter adapter;
	private ZeroMQNode node;

	/**
	 * Create a kernel agent with the default settings.
	 * 
	 * @param activator
	 *            is the agent activator to use.
	 * @param commitSuicide
	 *            indicates if this agent is able to commit suicide or not
	 * @param startUpListener
	 *            is a listener on kernel events which may be added at startup.
	 * @param applicationName
	 *            is the name of the application supported by this kernel.
	 * @param node
	 * @param networkAdapter
	 *            is the adapter used by this kernel to be connected through a
	 *            network.
	 */
	ZeroMQKernelAgent(AgentActivator activator, Boolean commitSuicide,
			EventListener startUpListener, String applicationName,
			ZeroMQNode node, NetworkAdapter networkAdapter) {
		super(activator, commitSuicide, null, startUpListener, networkAdapter,
				applicationName);
		getAddress().setName(Locale.getString(ZeroMQKernelAgent.class, "NAME")); //$NON-NLS-1$

		this.node = node;

		this.adapter = networkAdapter;
		this.adapter.setNetworkAdapterListener(this);

		JanusProperties prop = getKernelContext().getProperties();

		prop.setProperty(JanusProperty.GROUP_PERSISTENCE, true);
		this.adapter.setJanusProperties(prop);

		try {
			this.adapter.initializeNetwork(getKernelContext().getKernelAgent(),
					getKernelContext().getProperties());
		} catch (AssertionError ae) {
			throw ae;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		getGroupRepository().addRepositoryChangeListener(this);
		getAgentRepository().addRepositoryChangeListener(this);

		submitTaskWithFixedDelay(new GroupCleaner(),
				EMPTY_GROUP_CLEANING_DELAY * 60,
				EMPTY_GROUP_CLEANING_DELAY * 60, TimeUnit.SECONDS);
	}

	@Override
	public Status live() {	
		Status status = super.live();
		if(status.isSuccess()) {
			if (this.node != null) {
				this.node.run();
			}
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Status end() {
		getGroupRepository().removeRepositoryChangeListener(this);
		return super.end();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void repositoryChanged(RepositoryChangeEvent evt) {
		Logger logger = getLogger();
		if (evt.getType() == ChangeType.ADD) {
			if (evt.getChangedObject() instanceof GroupAddress) {
				try {
					GroupAddress group = (GroupAddress) evt.getChangedObject();
					Group groupDescription = getGroupObject(group);
					this.adapter.informLocalGroupCreated(group,
							groupDescription.getObtainConditions(),
							groupDescription.getLeaveConditions(),
							groupDescription.getMembership());
				} catch (AssertionError ae) {
					throw ae;
				} catch (Exception e) {
					logger.fine(Throwables.toString(e));
				}
			} else if (evt.getChangedObject() instanceof AgentAddress) {
				try {
					AgentAddress aagent = (AgentAddress) evt.getChangedObject();
					this.adapter.informLocalAgentAdded(aagent);
				} catch (AssertionError ae) {
					throw ae;
				} catch (Exception e) {
					logger.fine(Throwables.toString(e));
				}
			}
		} else if (evt.getType() == ChangeType.REMOVE) {
			if (evt.getChangedObject() instanceof GroupAddress) {
				try {
					GroupAddress group = (GroupAddress) evt.getChangedObject();
					this.adapter.informLocalGroupRemoved(group);
				} catch (AssertionError ae) {
					throw ae;
				} catch (Exception e) {
					logger.fine(Throwables.toString(e));
				}
			} else if (evt.getChangedObject() instanceof AgentAddress) {
				try {
					AgentAddress agent = (AgentAddress) evt.getChangedObject();
					this.adapter.informLocalAgentRemoved(agent);
				} catch (AssertionError ae) {
					throw ae;
				} catch (Exception e) {
					logger.fine(Throwables.toString(e));
				}
			}
		}
	}

	@Override
	public void distantGroupDiscovered(
			Class<? extends Organization> organization, UUID id,
			Collection<? extends GroupCondition> obtainConditions,
			Collection<? extends GroupCondition> leaveConditions,
			MembershipService membership, boolean persistent, String groupName) {
		// Process message
		Boolean known = isGroup(AddressUtil.createGroupAddress(id, organization, groupName));
		getOrCreateGroup(id, organization, obtainConditions, leaveConditions,
				membership, true, persistent, groupName);
		// Re-inform all local groups
		if (!known) {
			RepositoryOverlooker<GroupAddress> groups = this
					.getGroupRepository();
			for (GroupAddress group : groups) {
				this.adapter.informLocalGroupCreated(group,
						new ArrayList<GroupCondition>(),
						new ArrayList<GroupCondition>(), membership);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RoleAddress receiveOrganizationalDistantMessage(GroupAddress group,
			Class<? extends Role> receiverRole, Message message,
			boolean isBroadcast) {
		if (isBroadcast) {
			forwardBroadcastMessage(message);
			return null;
		}
		Address a = forwardMessage(message);
		assert (a instanceof RoleAddress);
		return (RoleAddress) a;
	}

	@Override
	public String toString() {
		return String.format(
				"ZeroMQKernelAgent [adapter=%s, node=%s]", //$NON-NLS-1$
				this.adapter, this.node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AgentAddress receiveAgentAgentDistantMessage(Message message,
			boolean isBroadcast) {
		if (isBroadcast) {
			forwardBroadcastMessage(message);
			return null;
		}
		Address a = forwardMessage(message);
		assert (a instanceof AgentAddress);
		return (AgentAddress) a;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void networkError(Throwable e) {
		if (fireUncatchedException(e)) {
			getLogger().log(Level.SEVERE, e.toString(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void networkLog(String message) {
		getLogger().log(Level.FINE, message);
	}

	/**
	 * This class tries to delete the no-more-used groups.
	 * 
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private class GroupCleaner implements Runnable {

		/**
		 */
		public GroupCleaner() {
			//
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("synthetic-access")
		public void run() {
			ZeroMQKernelAgent.this.removeInactivePersistentGroups(
					120 * EMPTY_GROUP_CLEANING_DELAY, TimeUnit.SECONDS);
		}

	}

}
