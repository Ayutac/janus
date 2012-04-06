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
package org.janusproject.kernel.network.jxse.jxta.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.UUID;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.document.Advertisement;
import net.jxta.document.StructuredDocument;
import net.jxta.impl.document.LiteXMLElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PeerGroupAdvertisement;

import org.arakhne.vmutil.locale.Locale;
import org.janusproject.kernel.address.AgentAddress;
import org.janusproject.kernel.crio.organization.GroupCondition;
import org.janusproject.kernel.crio.organization.MembershipService;
import org.janusproject.kernel.message.Message;
import org.janusproject.kernel.network.jxse.jxta.JXTANetworkHandler;
import org.janusproject.kernel.network.jxse.utils.OSGiHelper;
import org.janusproject.kernel.util.throwable.Throwables;

/**
 * JXTA group associated to a whole application.
 * 
 * @author $Author: srodriguez$
 * @author $Author: ngaud$
 * @author $Author: jeremie.laval@gmail.com$
 * @author $Author: robin.geffroy@gmail.com$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
class ApplicationJxtaGroup extends JanusJXTAGroup implements DiscoveryListener {
	
	/**
	 * @param adapter is the network adapter to use.
	 * @param peerGroup is the associated peer group.
	 * @param parent is the parent JXTA group.
	 * @param osgiHelper is the OSGi helper to use in this group.
	 */
	public ApplicationJxtaGroup(JXTANetworkHandler adapter, PeerGroup peerGroup, JxtaGroup parent, OSGiHelper osgiHelper) {
		super(adapter, peerGroup, parent,osgiHelper);

		getPeerGroup().getRendezVousService().setAutoStart(true, 12000);

		this.networkHandler.getJXTAExecutorService().submit(new GroupDiscover());
		
		try {
			join();
		}
		catch (Exception e) {
			getLogger().severe(Throwables.toString(e));			
		}

	}
	
	/** {@inheritDoc}
	 */
	@Override
	protected AgentAddress processIncomingMessage(Message janusMessage, boolean isBroadcast) {
		return this.networkHandler.receiveAgentAgentDistantMessage(janusMessage, isBroadcast);
	}
	
	/** {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void discoveryEvent(DiscoveryEvent event) {
		Enumeration<Advertisement> ads = event.getResponse().getAdvertisements();
		while (ads.hasMoreElements()) {
			String janusorg = null;
			String janusGId = null;
			String janusGroupName = null;

			Collection<GroupCondition> obtainConditions = new HashSet<GroupCondition>();
			Collection<GroupCondition> leaveConditions = new HashSet<GroupCondition>();
			MembershipService membership = null;
			GroupCondition condition;

			ByteArrayInputStream in;
			ObjectInputStream ois;

			PeerGroupAdvertisement advertisement = (PeerGroupAdvertisement) ads.nextElement();
			StructuredDocument<LiteXMLElement> sd = advertisement.getServiceParam(JanusNetworkConstants.JANUS_ORG_CLASS);
			Enumeration<LiteXMLElement> ee = sd.getChildren();
			while (ee.hasMoreElements()) {
				LiteXMLElement xmle = ee.nextElement();
				if (JanusNetworkConstants.TAG_JANUS_ORG.equals(xmle.getKey())) {
					janusorg = xmle.getValue();
				}
				else if (JanusNetworkConstants.TAG_JANUS_GROUP_ID.equals(xmle.getKey())) {
					janusGId = xmle.getValue();
				}
				else if(JanusNetworkConstants.TAG_JANUS_GROUP_NAME.equals(xmle.getKey())) {
					janusGroupName = xmle.getValue();
				}
				else if (JanusNetworkConstants.TAG_JANUS_OBTAIN_CONDITIONS.equals(xmle.getKey())) {// obtain conditions deserialization

					Enumeration<LiteXMLElement> eObtainConditions = xmle.getChildren();
					LiteXMLElement eObtainCondition;
					while (eObtainConditions.hasMoreElements()) {
						eObtainCondition = eObtainConditions.nextElement();
						if (JanusNetworkConstants.TAG_JANUS_CONDITION.equals(eObtainCondition.getKey())) {
							in = new ByteArrayInputStream(eObtainCondition.getValue().getBytes());
							try {
								ois = new ObjectInputStream(in);
								condition = (GroupCondition) ois.readObject();
								obtainConditions.add(condition);
							}
							catch (IOException e) {
								getLogger().fine(Throwables.toString(e));
							}
							catch (ClassNotFoundException e) {
								getLogger().fine(Throwables.toString(e));
							}
						}
					}

				}
				else if (JanusNetworkConstants.TAG_JANUS_LEAVE_CONDITIONS.equals(xmle.getKey())) {// leave conditions deserialization

					Enumeration<LiteXMLElement> eLeaveConditions = xmle.getChildren();
					LiteXMLElement eLeaveCondition;
					while (eLeaveConditions.hasMoreElements()) {
						eLeaveCondition = eLeaveConditions.nextElement();
						if (JanusNetworkConstants.TAG_JANUS_CONDITION.equals(eLeaveCondition.getKey())) {
							in = new ByteArrayInputStream(eLeaveCondition.getValue().getBytes());
							try {
								ois = new ObjectInputStream(in);
								condition = (GroupCondition) ois.readObject();
								leaveConditions.add(condition);
							}
							catch (IOException e) {
								getLogger().fine(Throwables.toString(e));
							}
							catch (ClassNotFoundException e) {
								getLogger().fine(Throwables.toString(e));
							}
						}
					}

				}
				else if (JanusNetworkConstants.TAG_JANUS_MEMBERSHIPSERVICE.equals(xmle.getKey())) {// Membership deserialization
					in = new ByteArrayInputStream(xmle.getValue().getBytes());
					try {
						ois = new ObjectInputStream(in);
						membership = (MembershipService) ois.readObject();
					}
					catch (IOException e) {
						getLogger().severe(Throwables.toString(e));
					}
					catch (ClassNotFoundException e) {
						getLogger().severe(Throwables.toString(e));
					}
				}
			}

			getLogger().finer(Locale.getString(ApplicationJxtaGroup.class,
					"GROUP_DISCOVERED", //$NON-NLS-1$
					janusorg, janusGId, advertisement.getName()));
			try {
				this.networkHandler.informDistantGroupDiscovered(janusorg, UUID.fromString(janusGId), janusGroupName,obtainConditions, leaveConditions, membership, advertisement);
			}
			catch (AssertionError ae) {
				throw ae;
			}
			catch (ClassNotFoundException e) {
				getLogger().severe(Throwables.toString(e));
			}

		}
	}

	/**
	 * @author $Author: srodriguez$
	 * @author $Author: ngaud$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private class GroupDiscover implements Runnable {

		/**
		 */
		public GroupDiscover() {
			//
		}
	
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			try {
				Thread.currentThread().setName(
						Locale.getString(ApplicationJxtaGroup.class,
								"GROUP_DISCOVER_NAME", //$NON-NLS-1$
								getPeerGroup().getPeerName()));
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable _) {
				//
			}
			while (true) {
				PeerGroupUtil.discoverAdvs(getPeerGroup(), null, ApplicationJxtaGroup.this);
				try {
					Thread.sleep(1000);
				} catch (AssertionError ae) {
					throw ae;
				}
				catch (InterruptedException e) {
					getLogger().severe(Throwables.toString(e));
				}
			}
		}

	}
	
}
