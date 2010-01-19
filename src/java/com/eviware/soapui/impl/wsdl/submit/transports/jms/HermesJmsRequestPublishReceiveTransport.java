/*
 *  soapUI, copyright (C) 2004-2009 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.submit.transports.jms;

import hermes.Domain;
import hermes.Hermes;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.iface.Request;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.SubmitContext;

public class HermesJmsRequestPublishReceiveTransport extends HermesJmsRequestTransport
{

	public Response execute( SubmitContext submitContext, Request request, long timeStarted ) throws Exception
	{
		TopicConnectionFactory topicConnectionFactory = null;
		TopicConnection topicConnection = null;
		TopicSession topicSession = null;

		ConnectionFactory connectionFactory = null;
		Connection connection = null;
		Session session = null;

		try
		{
			JMSEndpoint jmsEndpoint = new JMSEndpoint( request, submitContext );

			submitContext.setProperty( HERMES_SESSION_NAME, jmsEndpoint.getSessionName());

			Hermes hermes = getHermes(jmsEndpoint.getSessionName(), request );
			// connection factory
			topicConnectionFactory = ( TopicConnectionFactory )hermes.getConnectionFactory();
			connectionFactory = ( ConnectionFactory )hermes.getConnectionFactory();

			// connection
			topicConnection = topicConnectionFactory.createTopicConnection();
			topicConnection.start();

			connection = createConnection( submitContext, request, connectionFactory );
			connection.start();
			// session
			topicSession = topicConnection.createTopicSession( false, Session.AUTO_ACKNOWLEDGE );
			session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );

			// destination
			Topic topicPublish = ( Topic )hermes.getDestination( jmsEndpoint.getSend(), Domain.TOPIC );
			Queue queueReceive = ( Queue )hermes.getDestination( jmsEndpoint.getReceive(), Domain.QUEUE );

			Message messagePublish = messagePublish( submitContext, request, topicSession, hermes, topicPublish );

			MessageConsumer messageConsumer = session.createConsumer( queueReceive );

			return makeResponse( submitContext, request, timeStarted, messagePublish, messageConsumer );
		}
		catch( JMSException jmse )
		{
			return errorResponse( submitContext, request, timeStarted, jmse );
		}
		catch( Throwable t )
		{
			SoapUI.logError( t );
		}
		finally
		{
			closeSessionAndConnection( topicConnection, topicSession );
			closeSessionAndConnection( connection, session );
		}
		return null;

	}
}
