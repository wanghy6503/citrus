/*
 * Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.jms;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import org.easymock.EasyMock;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 */
public class JmsEndpointProducerTest {

    private ConnectionFactory connectionFactory = org.easymock.EasyMock.createMock(ConnectionFactory.class);
    private Connection connection = EasyMock.createMock(Connection.class);
    private Session session = EasyMock.createMock(Session.class);
    private Destination destination = EasyMock.createMock(Destination.class);
    private Queue destinationQueue = EasyMock.createMock(Queue.class);
    private MessageProducer messageProducer = EasyMock.createMock(MessageProducer.class);
    
    private JmsTemplate jmsTemplate = EasyMock.createMock(JmsTemplate.class);
    
    @Test
    public void testSendMessageWithJmsTemplate() {
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.getEndpointConfiguration().setJmsTemplate(jmsTemplate);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(jmsTemplate, connectionFactory, destination, messageProducer);

        expect(jmsTemplate.getDefaultDestination()).andReturn(destination).atLeastOnce();
        
        jmsTemplate.convertAndSend(message);
        expectLastCall().once();
        
        replay(jmsTemplate, connectionFactory, destination, messageProducer);

        endpoint.createProducer().send(message);
        
        verify(jmsTemplate, connectionFactory, destination, messageProducer);
    }
    
    @Test
    public void testSendMessageWithDestination() throws JMSException {
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.getEndpointConfiguration().setConnectionFactory(connectionFactory);

        endpoint.getEndpointConfiguration().setDestination(destination);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(jmsTemplate, connectionFactory, destination, messageProducer, connection, session);

        expect(connectionFactory.createConnection()).andReturn(connection).once();
        expect(connection.createSession(anyBoolean(), anyInt())).andReturn(session).once();

        expect(session.createProducer(destination)).andReturn(messageProducer).once();
        messageProducer.send((TextMessage)anyObject());
        expectLastCall().once();
        
        expect(session.createTextMessage("<TestRequest><Message>Hello World!</Message></TestRequest>")).andReturn(
                new TextMessageImpl("<TestRequest><Message>Hello World!</Message></TestRequest>", new HashMap<String, String>()));
        
        expect(session.getTransacted()).andReturn(false).once();
        
        replay(jmsTemplate, connectionFactory, destination, messageProducer, connection, session);

        endpoint.createProducer().send(message);
        
        verify(jmsTemplate, connectionFactory, destination, messageProducer, connection, session);
    }
    
    @Test
    public void testSendMessageWithDestinationName() throws JMSException {
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.getEndpointConfiguration().setConnectionFactory(connectionFactory);

        endpoint.getEndpointConfiguration().setDestinationName("myDestination");
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(jmsTemplate, connectionFactory, destination, messageProducer, connection, session);

        expect(connectionFactory.createConnection()).andReturn(connection).once();
        expect(connection.createSession(anyBoolean(), anyInt())).andReturn(session).once();

        expect(session.createProducer(destinationQueue)).andReturn(messageProducer).once();
        messageProducer.send((TextMessage)anyObject());
        expectLastCall().once();

        expect(session.createTextMessage("<TestRequest><Message>Hello World!</Message></TestRequest>")).andReturn(
                new TextMessageImpl("<TestRequest><Message>Hello World!</Message></TestRequest>", new HashMap<String, String>()));
        
        expect(session.getTransacted()).andReturn(false).once();
        
        expect(session.createQueue("myDestination")).andReturn(destinationQueue).once();
        
        replay(jmsTemplate, connectionFactory, destination, messageProducer, connection, session);

        endpoint.createProducer().send(message);
        
        verify(jmsTemplate, connectionFactory, destination, messageProducer, connection, session);
    }
    
    @Test
    public void testSendEmptyMessage() throws JMSException {
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.getEndpointConfiguration().setConnectionFactory(connectionFactory);

        endpoint.getEndpointConfiguration().setDestination(destination);
        
        try {
            endpoint.createProducer().send(null);
        } catch(IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Message is empty - unable to send empty message");
            return;
        }
        
        Assert.fail("Missing " + CitrusRuntimeException.class + " because of sending empty message");
    }
    
}
