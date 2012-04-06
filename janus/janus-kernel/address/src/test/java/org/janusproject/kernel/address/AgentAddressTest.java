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
package org.janusproject.kernel.address;

import java.net.UnknownHostException;
import java.util.UUID;

import junit.framework.TestCase;

/**
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class AgentAddressTest extends TestCase {

	private UUID uid;
	private String name;

	/**
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.uid = UUID.randomUUID();
		this.name = UUID.randomUUID().toString();
	}

	/**
	 */
	@Override
	protected void tearDown() throws Exception {
		this.name = null;
		this.uid = null;
		super.tearDown();
	}

	/**
	 */
	public void testGetName() {
		AgentAddress hAdr1 = new AgentAddressStub(this.uid, null);
		assertEquals(AgentAddress.NO_NAME, hAdr1.getName());

		AgentAddress hAdr2 = new AgentAddressStub(this.uid, AgentAddress.NO_NAME);
		assertEquals(AgentAddress.NO_NAME, hAdr2.getName());

		AgentAddress hAdr3 = new AgentAddressStub(this.uid, this.name);
		assertEquals(this.name, hAdr3.getName());
	}


	/**
	 */
	public void testSetNameString() {
		AgentAddress hAdr1 = new AgentAddressStub(this.uid, null);
		hAdr1.setName(this.name);
		assertEquals(this.name, hAdr1.getName());

		AgentAddress hAdr2 = new AgentAddressStub(this.uid, AgentAddress.NO_NAME);
		hAdr2.setName(this.name);
		assertEquals(this.name, hAdr2.getName());

		AgentAddress hAdr3 = new AgentAddressStub(this.uid, this.name);
		hAdr3.setName(this.name);
		assertEquals(this.name, hAdr3.getName());
	}

	/**
	 */
	public void testToString() {
		AgentAddress hAdr1 = new AgentAddressStub(this.uid, null);
		assertEquals("::"+this.uid.toString(), //$NON-NLS-1$
				hAdr1.toString());

		AgentAddress hAdr2 = new AgentAddressStub(this.uid, AgentAddress.NO_NAME);
		assertEquals("::"+this.uid.toString(), //$NON-NLS-1$
				hAdr2.toString());

		AgentAddress hAdr3 = new AgentAddressStub(this.uid, this.name);
		assertEquals(this.name+"::"+this.uid.toString(), //$NON-NLS-1$
				hAdr3.toString());
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void testValueOfString() {
		try {
			AgentAddress.valueOf(null);
			fail("IllegalArgumentException was expected"); //$NON-NLS-1$
		}
		catch(IllegalArgumentException _) {
			// Expected exception
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}
		
		try {
			AgentAddress.valueOf(""); //$NON-NLS-1$
			fail("IllegalArgumentException was expected"); //$NON-NLS-1$
		}
		catch(IllegalArgumentException _) {
			// Expected exception
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}

		try {
			AgentAddress.valueOf("a"); //$NON-NLS-1$
			fail("IllegalArgumentException was expected"); //$NON-NLS-1$
		}
		catch(IllegalArgumentException _) {
			// Expected exception
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}

		try {
			AgentAddress.valueOf("a a"); //$NON-NLS-1$
			fail("IllegalArgumentException was expected"); //$NON-NLS-1$
		}
		catch(IllegalArgumentException _) {
			// Expected exception
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}
		
		try {
			AgentAddress.valueOf("a::a"); //$NON-NLS-1$
			fail("IllegalArgumentException was expected"); //$NON-NLS-1$
		}
		catch(IllegalArgumentException _) {
			// Expected exception
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}

		try {
			AgentAddress.valueOf("a::a@a"); //$NON-NLS-1$
			fail("IllegalArgumentException was expected"); //$NON-NLS-1$
		}
		catch(IllegalArgumentException _) {
			// Expected exception
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}


		try {
			AgentAddress hAdr = AgentAddress.valueOf("a::"+this.uid.toString()); //$NON-NLS-1$
			assertNotNull(hAdr);
			assertEquals(this.uid, hAdr.getUUID());
		}
		catch(IllegalArgumentException _) {
			fail("Unexpected exception: IllegalArgumentException"); //$NON-NLS-1$
		}
		catch (UnknownHostException _) {
			fail("Unexpected exception: UnknownHostException"); //$NON-NLS-1$
		}
	}

}
