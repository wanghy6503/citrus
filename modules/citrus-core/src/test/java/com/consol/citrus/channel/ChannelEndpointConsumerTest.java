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

package com.consol.citrus.channel;

import com.consol.citrus.channel.selector.HeaderMatchingMessageSelector;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import org.easymock.EasyMock;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.ChannelResolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 */
public class ChannelEndpointConsumerTest {

    private MessagingTemplate messagingTemplate = EasyMock.createMock(MessagingTemplate.class);
    
    private PollableChannel channel = EasyMock.createMock(PollableChannel.class);

    private ChannelResolver channelResolver = EasyMock.createMock(ChannelResolver.class);
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testReceiveMessage() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);

        endpoint.getEndpointConfiguration().setChannel(channel);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.receive(channel)).andReturn(message).once();
        
        replay(messagingTemplate, channel);
        
        Message receivedMessage = endpoint.createConsumer().receive();
        
        Assert.assertEquals(receivedMessage.getPayload(), message.getPayload());
        Assert.assertEquals(receivedMessage.getHeaders(), message.getHeaders());
        verify(messagingTemplate, channel);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testReceiveMessageChannelNameResolver() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);

        endpoint.getEndpointConfiguration().setChannelName("testChannel");

        endpoint.getEndpointConfiguration().setChannelResolver(channelResolver);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(messagingTemplate, channel, channelResolver);
        
        expect(channelResolver.resolveChannelName("testChannel")).andReturn(channel).once();
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.receive(channel)).andReturn(message).once();
        
        replay(messagingTemplate, channel, channelResolver);
        
        Message receivedMessage = endpoint.createConsumer().receive();
        
        Assert.assertEquals(receivedMessage.getPayload(), message.getPayload());
        Assert.assertEquals(receivedMessage.getHeaders(), message.getHeaders());
        verify(messagingTemplate, channel, channelResolver);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testReceiveMessageWithCustomTimeout() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);

        endpoint.getEndpointConfiguration().setChannel(channel);
        endpoint.getEndpointConfiguration().setTimeout(10000L);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(10000L);
        expectLastCall().once();
        
        expect(messagingTemplate.receive(channel)).andReturn(message).once();
        
        replay(messagingTemplate, channel);
        
        Message receivedMessage = endpoint.createConsumer().receive();
        
        Assert.assertEquals(receivedMessage.getPayload(), message.getPayload());
        Assert.assertEquals(receivedMessage.getHeaders(), message.getHeaders());
        verify(messagingTemplate, channel);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testReceiveMessageTimeoutOverride() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);

        endpoint.getEndpointConfiguration().setChannel(channel);
        endpoint.getEndpointConfiguration().setTimeout(10000L);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(25000L);
        expectLastCall().once();
        
        expect(messagingTemplate.receive(channel)).andReturn(message).once();
        
        replay(messagingTemplate, channel);
        
        Message receivedMessage = endpoint.createConsumer().receive(25000L);
        
        Assert.assertEquals(receivedMessage.getPayload(), message.getPayload());
        Assert.assertEquals(receivedMessage.getHeaders(), message.getHeaders());
        verify(messagingTemplate, channel);
    }
    
    @Test
    public void testReceiveTimeout() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);

        endpoint.getEndpointConfiguration().setChannel(channel);
        
        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.receive(channel)).andReturn(null).once();
        
        replay(messagingTemplate, channel);
        
        try {
            endpoint.createConsumer().receive();
            Assert.fail("Missing " + ActionTimeoutException.class + " because no message was received");
        } catch(ActionTimeoutException e) {
            Assert.assertTrue(e.getLocalizedMessage().startsWith("Action timeout while receiving message from channel"));
        }
        
        verify(messagingTemplate, channel);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testReceiveSelected() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);

        endpoint.getEndpointConfiguration().setChannel(channel);
        endpoint.getEndpointConfiguration().setTimeout(0L);

        try {
            endpoint.createConsumer().receive("Operation = 'sayHello'");
            Assert.fail("Missing exception due to unsupported operation");
        } catch (CitrusRuntimeException e) {
            Assert.assertNotNull(e.getMessage());
        }
        
        MessageSelectingQueueChannel queueChannel = EasyMock.createMock(MessageSelectingQueueChannel.class);
        Message message = MessageBuilder.withPayload("Hello").setHeader("Operation", "sayHello").build();
        reset(queueChannel);
        
        expect(queueChannel.receive(anyObject(HeaderMatchingMessageSelector.class)))
                            .andReturn(message).once();
        
        replay(queueChannel);

        endpoint.getEndpointConfiguration().setChannel(queueChannel);
        Message receivedMessage = endpoint.createConsumer().receive("Operation = 'sayHello'");
        
        Assert.assertEquals(receivedMessage.getPayload(), message.getPayload());
        Assert.assertEquals(receivedMessage.getHeaders(), message.getHeaders());
        verify(queueChannel);
    }
    
    @Test
    public void testReceiveSelectedNoMessageWithTimeout() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        
        MessageSelectingQueueChannel queueChannel = EasyMock.createMock(MessageSelectingQueueChannel.class);
        
        reset(queueChannel);
        
        expect(queueChannel.receive(anyObject(HeaderMatchingMessageSelector.class), eq(1500L)))
                            .andReturn(null).once(); // force retry
        
        replay(queueChannel);

        endpoint.getEndpointConfiguration().setChannel(queueChannel);
        
        try {
            endpoint.createConsumer().receive("Operation = 'sayHello'", 1500L);
            Assert.fail("Missing " + ActionTimeoutException.class + " because no message was received");
        } catch(ActionTimeoutException e) {
            Assert.assertTrue(e.getLocalizedMessage().startsWith("Action timeout while receiving message from channel"));
        }
        
        verify(queueChannel);
    }
}
