package org.janusproject.acl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.janusproject.acl.encoding.PayloadEncoding;
import org.janusproject.acl.protocol.EnumFipaProtocol;
import org.janusproject.kernel.address.AgentAddress;
import org.janusproject.kernel.agent.Agent;
import org.janusproject.kernel.message.Message;

/**
 * Implements the agent concept of the Janus Metamodel
 * with the ability to manage ACL Messages.
 * 
 * @author $Author: madeline$
 * @author $Author: kleroy$
 * @author $Author: ptalagrand$
 * @author $Author: ngaud$
 * @version $Name$ $Revision$ $Date$
 * @mavengroupid $Groupid$
 * @mavenartifactid $ArtifactId$
 */
public class ACLAgent extends Agent {
	
	private static final long serialVersionUID = -8831445952166271312L;
	
	/**
	 * The ACL Message Handler of this agent
	 */
	private ACLMessageHandler aclMessageHandler;
	
	/**
	 * The text encoding of the ACLTransportMessage
	 * Default UTF-8
	 */
	private PayloadEncoding payloadEncoding;
	
	/**
	 * The ACL representation of the ACLTransportMessage (Bit-efficient, String or XML)
	 * Default Bit-efficient
	 */
	private ACLRepresentation aclRepresentation;
	
	/**
	 * Creates a new ACL Agent with default payload encoding (UTF8) and default acl representation (string).
	 */
	public ACLAgent() {
		super();
		this.payloadEncoding = PayloadEncoding.UTF8;
		this.aclRepresentation = ACLRepresentation.STRING;
	}
	
	/**
	 * Creates a new ACL Agent.
	 * 
	 * @param aclRepresentation
	 * @param payloadEncoding
	 * 
	 * @see ACLRepresentation
	 * @see PayloadEncoding
	 */
	public ACLAgent(ACLRepresentation aclRepresentation, PayloadEncoding payloadEncoding) {
		super();
		this.payloadEncoding = payloadEncoding;
		this.aclRepresentation = aclRepresentation;
	}

	/**
	 * Sends the specified <code>ACL Message</code> to one randomly selected agent.
	 * <p>
	 * This function force the emitter of the message to be this agent.
	 * </p>
	 * 
	 * @param message is the ACL Message to send
	 * @param agents is the collection of receivers.
	 * @return the address of the receiver of the freshly sent message if it
	 *         was found, or <code>null</code> otherwise.
	 */
	public final AgentAddress sendACLMessage(ACLMessage message, AgentAddress... agents) {
		
		initACLMessage(message);
		ACLTransportMessage transportMessage = getAclMessageHandler().prepareOutgoingACLMessage(message, agents);
		
		return sendMessage(transportMessage, agents);
	}
	
	/**
	 * Replies the first available ACL Message in the agent mail box and remove it from the mailbox.
	 * 
	 * @return the first available ACL Message, or <code>null</code> if the mailbox is empty.
	 * 
	 * @see #peekACLMessage()
	 * @see #getACLMessages()
	 * @see #peekACLMessages()
	 * @see #hasACLMessage()
	 */
	protected final ACLMessage getACLMessage() {
		
		ACLMessage aMsg = null;
		Boolean flag = true;

		Iterator<Message> messagesIterator = peekMessages();

		while( ( messagesIterator.hasNext() ) && ( flag == true ) )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;
				
				if(aMsg != null){
					getMailbox().remove( msg );
					flag = false;
				}
			}
		}
		
		return aMsg;
	}
	
	/**
	 * Replies the first available ACL Message
	 * in the agent mail box  
	 * with the given performative 
	 * and the given protocol type 
	 * and, then, removes it from the mailbox.
	 * @param protocolType 
	 * @param performative 
	 * 
	 * @return the first available ACL Message, or <code>null</code> if the mailbox is empty.
	 * @see #peekACLMessage()
	 * @see #getACLMessages()
	 * @see #peekACLMessages()
	 * @see #hasACLMessage()
	 */
	public final ACLMessage getACLMessage(EnumFipaProtocol protocolType, Performative performative) {
		
		ACLMessage aMsg = null;
		Boolean flag = true;

		Iterator<Message> messagesIterator = peekMessages();

		while( ( messagesIterator.hasNext() ) && ( flag == true ) )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;
				
				if(aMsg != null 
						&& ( aMsg.getPerformative() == performative )
						&& ( aMsg.getProtocol() == protocolType)
					)
				{
					getMailbox().remove( msg );
					flag = false;
				}
				else{
					aMsg = null;
				}
			}
		}
		
		return aMsg;
	}
	
	/**
	 * Replies the first available ACL Message 
	 * for a specific conversation 
	 * and, then, removes it from the mailbox.
	 * <p>
	 * This method is essentially used in protocols.
	 * 
	 * @param conversationId
	 * 
	 * @return the first available ACL Message, or <code>null</code>
	 */
	public final ACLMessage getACLMessageForConversationId(UUID conversationId) {
		
		ACLMessage aMsg = null;
		Boolean flag = true;

		Iterator<Message> messagesIterator = peekMessages();

		while( ( messagesIterator.hasNext() ) && ( flag == true ) )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;
				
				if( ( aMsg != null ) && ( aMsg.getConversationId().compareTo(conversationId) == 0 ) ){
					getMailbox().remove( msg );
					flag = false;
				}
				else{
					aMsg = null;
				}
			}
		}
		
		return aMsg;
	}
	
	/**
	 * Replies all the ACL Messages in the agent mailbox.
	 * <p>
	 * Each time a message is consumed
	 * from the replied iterable object,
	 * the corresponding ACL Message is removed
	 * from the mailbox.
	 * 
	 * @return all the ACL Messages, never <code>null</code>.
	 * @see #getACLMessage()
	 * @see #peekACLMessage()
	 * @see #peekACLMessages()
	 * @see #hasACLMessage()
	 */
	protected final Iterator<ACLMessage> getACLMessages() {

		LinkedList<ACLMessage> resultList = new LinkedList<ACLMessage>();

		Iterator<Message> messagesIterator = peekMessages();

		while( messagesIterator.hasNext() )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				ACLMessage aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;

				if(aMsg != null){
					getMailbox().remove( msg );
					resultList.add( aMsg );
				}
			}
		}
		
		return resultList.iterator();
	}
	
	/**
	 * Replies all the ACL Messages in the agent mailbox with the specified performative.
	 * Each time a message is consumed
	 * from the replied iterable object,
	 * the corresponding ACL Message is removed
	 * from the mailbox.
	 * @param performative 
	 * 
	 * @return all the ACL Messages, never <code>null</code>.
	 * @see #getACLMessage()
	 * @see #peekACLMessage()
	 * @see #peekACLMessages()
	 * @see #hasACLMessage()
	 */
	protected final Iterator<ACLMessage> getACLMessages(Performative performative) {

		LinkedList<ACLMessage> resultList = new LinkedList<ACLMessage>();

		Iterator<Message> messagesIterator = peekMessages();

		while( messagesIterator.hasNext() )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				ACLMessage aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;

				if(aMsg != null && aMsg.getPerformative() == performative){
					getMailbox().remove( msg );
					resultList.add( aMsg );
				}
				else{
					aMsg = null;
				}
			}
		}
		
		return resultList.iterator();
	}
	
	/**
	 * Replies the first available ACL Message in the agent mail box
	 * and leave it inside the mailbox.
	 * 
	 * @return the first available ACL Message, or <code>null</code> if
	 * the mailbox is empty.
	 * @see #getACLMessage()
	 * @see #getACLMessages()
	 * @see #peekACLMessages()
	 * @see #hasACLMessage()
	 */
	protected final ACLMessage peekACLMessage() {
		
		ACLMessage aMsg = null;
		Boolean flag = true;

		Iterator<Message> messagesIterator = peekMessages();

		while( ( messagesIterator.hasNext() ) && ( flag == true ) )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;
				
				if(aMsg != null) flag = false;
			}
		}
		
		return aMsg;
	}
	
	/**
	 * Replies the ACL Messages in the agent mailbox.
	 * Each time a message is consumed
	 * from the replied iterable object,
	 * the corresponding message is NOT removed
	 * from the mailbox.
	 * 
	 * @return all the ACL Messages, never <code>null</code>.
	 * @see #getACLMessage()
	 * @see #peekACLMessage()
	 * @see #getACLMessages()
	 * @see #hasACLMessage()
	 */
	protected final Iterator<ACLMessage> peekACLMessages() {

		LinkedList<ACLMessage> resultList = new LinkedList<ACLMessage>();
		
		Iterator<Message> messagesIterator = peekMessages();
		
		while( messagesIterator.hasNext() )
		{
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage )
			{
				ACLMessage aMsg = getAclMessageFromTransportMessage( (ACLTransportMessage) msg ) ;
				
				if(aMsg != null){
					resultList.add( aMsg );
				}
			}
		}
		
		return resultList.iterator();
	}
	
	/** 
	 * Indicates if the agent mailbox contains at least one ACL Message or not.
	 * 
	 * @return <code>true</code> if the mailbox contains at least one ACL Message,
	 * otherwise <code>false</code>
	 */
	protected final boolean hasACLMessage(){
		
		int nbAclMsg = 0;
		
		Iterator<Message> messagesIterator = peekMessages();
		
		while( messagesIterator.hasNext() ){
			Message msg = messagesIterator.next();
			
			if( msg instanceof ACLTransportMessage ) nbAclMsg++;
		}
		
		return ( nbAclMsg > 0 );
	}

	/**
	 * Gets ACL Message from ACL Transport Message 
	 * after verification of the content of this message.
	 * @param tMsg 
	 * 
	 * @return the ACL Message
	 */
	protected final ACLMessage getAclMessageFromTransportMessage( ACLTransportMessage tMsg ){
		
		if( ( tMsg.getContent() != null ) && (tMsg.getContent() instanceof byte[] ) )
		{
			try{
				ACLMessage aMsg = getAclMessageHandler().prepareIncomingMessage( tMsg ) ;
				return aMsg;
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the ACL Message Handler for the agent
	 * 
	 * @return the aclMessageHandler
	 */
	public ACLMessageHandler getAclMessageHandler() {
		if (this.aclMessageHandler == null) {
			this.aclMessageHandler = new ACLMessageHandler();
		}
		return this.aclMessageHandler;
	}

	/**
	 * Sets the ACL Message Handler for the agent
	 * 
	 * @param aclMessageHandler the aclMessageHandler to set
	 */
	public void setAclMessageHandler(ACLMessageHandler aclMessageHandler) {
		if (aclMessageHandler != null) {
			this.aclMessageHandler = aclMessageHandler;
		}
	}
	
	/**
	 * Initiates and completes a given ACL Message before sending it.
	 * 
	 * @param message
	 */
	private void initACLMessage(ACLMessage message) {
		message.setAclRepresentation(this.aclRepresentation.getValue());
		message.setEncoding(this.payloadEncoding.getValue());
		message.setSender(getAddress());
	}
}