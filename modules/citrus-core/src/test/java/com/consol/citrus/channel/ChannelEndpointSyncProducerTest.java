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

import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.message.DefaultReplyMessageCorrelator;
import com.consol.citrus.message.ReplyMessageCorrelator;
import org.easymock.EasyMock;
import org.springframework.integration.*;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.ChannelResolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 */
public class ChannelEndpointSyncProducerTest {

    private MessagingTemplate messagingTemplate = EasyMock.createMock(MessagingTemplate.class);
    private MessageChannel channel = org.easymock.EasyMock.createMock(MessageChannel.class);
    private ReplyMessageCorrelator replyMessageCorrelator = org.easymock.EasyMock.createMock(ReplyMessageCorrelator.class);
    private ChannelResolver channelResolver = EasyMock.createMock(ChannelResolver.class);
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSendMessage() throws JMSException {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        endpoint.getEndpointConfiguration().setChannel(channel);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        Map<String, Object> responseHeaders = new HashMap<String, Object>();
        final Message response = MessageBuilder.withPayload("<TestResponse>Hello World!</TestResponse>")
                                .copyHeaders(responseHeaders)
                                .build();

        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.sendAndReceive(channel, message)).andReturn(response).once();
        
        replay(messagingTemplate, channel);

        endpoint.createProducer().send(message);
        
        verify(messagingTemplate, channel);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSendMessageChannelNameResolver() throws JMSException {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        endpoint.getEndpointConfiguration().setChannelName("testChannel");

        endpoint.getEndpointConfiguration().setChannelResolver(channelResolver);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        Map<String, Object> responseHeaders = new HashMap<String, Object>();
        final Message response = MessageBuilder.withPayload("<TestResponse>Hello World!</TestResponse>")
                                .copyHeaders(responseHeaders)
                                .build();

        reset(messagingTemplate, channel, channelResolver);
        
        expect(channelResolver.resolveChannelName("testChannel")).andReturn(channel).once();
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.sendAndReceive(channel, message)).andReturn(response).once();
        
        replay(messagingTemplate, channel, channelResolver);

        endpoint.createProducer().send(message);
        
        verify(messagingTemplate, channel, channelResolver);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSendMessageWithReplyHandler() throws JMSException {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        endpoint.getEndpointConfiguration().setChannel(channel);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        Map<String, Object> responseHeaders = new HashMap<String, Object>();
        final Message response = MessageBuilder.withPayload("<TestResponse>Hello World!</TestResponse>")
                                .copyHeaders(responseHeaders)
                                .build();

        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.sendAndReceive(channel, message)).andReturn(response).once();
        
        replay(messagingTemplate, channel);

        ChannelSyncProducer channelSyncProducer = (ChannelSyncProducer) endpoint.createProducer();
        channelSyncProducer.send(message);

        Message<String> replyMessage = (Message<String>) channelSyncProducer.findReplyMessage("");
        Assert.assertEquals(replyMessage.getPayload(), response.getPayload());
        Assert.assertEquals(replyMessage.getHeaders(), response.getHeaders());
        
        verify(messagingTemplate, channel);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSendMessageWithCustomReplyTimeout() throws JMSException {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        endpoint.getEndpointConfiguration().setChannel(channel);

        endpoint.getEndpointConfiguration().setTimeout(10000L);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        Map<String, Object> responseHeaders = new HashMap<String, Object>();
        final Message response = MessageBuilder.withPayload("<TestResponse>Hello World!</TestResponse>")
                                .copyHeaders(responseHeaders)
                                .build();

        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(10000L);
        expectLastCall().once();
        
        expect(messagingTemplate.sendAndReceive(channel, message)).andReturn(response).once();

        replay(messagingTemplate, channel);

        ChannelSyncProducer channelSyncProducer = (ChannelSyncProducer) endpoint.createProducer();
        channelSyncProducer.send(message);

        Message<String> replyMessage = (Message<String>) channelSyncProducer.findReplyMessage("");
        Assert.assertEquals(replyMessage.getPayload(), response.getPayload());
        Assert.assertEquals(replyMessage.getHeaders(), response.getHeaders());
        
        verify(messagingTemplate, channel);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSendMessageWithReplyMessageCorrelator() throws JMSException {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        endpoint.getEndpointConfiguration().setChannel(channel);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();
        
        Map<String, Object> responseHeaders = new HashMap<String, Object>();
        final Message response = MessageBuilder.withPayload("<TestResponse>Hello World!</TestResponse>")
                                .copyHeaders(responseHeaders)
                                .build();

        endpoint.getEndpointConfiguration().setCorrelator(replyMessageCorrelator);
        
        reset(messagingTemplate, channel, replyMessageCorrelator);
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.sendAndReceive(channel, message)).andReturn(response).once();
        
        expect(replyMessageCorrelator.getCorrelationKey(message)).andReturn(MessageHeaders.ID + " = '123456789'").once();
        
        replay(messagingTemplate, channel, replyMessageCorrelator);

        ChannelSyncProducer channelSyncProducer = (ChannelSyncProducer) endpoint.createProducer();
        channelSyncProducer.send(message);

        Message<String> replyMessage = (Message<String>) channelSyncProducer.findReplyMessage(MessageHeaders.ID + " = '123456789'");
        Assert.assertEquals(replyMessage.getPayload(), response.getPayload());
        Assert.assertEquals(replyMessage.getHeaders(), response.getHeaders());
        
        verify(messagingTemplate, channel, replyMessageCorrelator);
    }
    
    @Test
    public void testSendMessageNoResponse() throws JMSException {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();
        endpoint.getEndpointConfiguration().setMessagingTemplate(messagingTemplate);
        endpoint.getEndpointConfiguration().setChannel(channel);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(messagingTemplate, channel);
        
        messagingTemplate.setReceiveTimeout(5000L);
        expectLastCall().once();
        
        expect(messagingTemplate.sendAndReceive(channel, message)).andReturn(null).once();
        
        replay(messagingTemplate, channel);

        try {
            endpoint.createProducer().send(message);
        } catch(CitrusRuntimeException e) {
            Assert.assertEquals(e.getLocalizedMessage(), "Reply timed out after 5000ms. Did not receive reply message on reply channel");
            verify(messagingTemplate, channel);
            return;
        }
        
        Assert.fail("Missing " + CitrusRuntimeException.class + " because of reply timeout");
    }

    @Test
    public void testOnReplyMessage() {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();

        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                .copyHeaders(headers)
                .build();

        ChannelSyncProducer channelSyncProducer = (ChannelSyncProducer) endpoint.createProducer();
        channelSyncProducer.onReplyMessage("", message);

        Assert.assertEquals(channelSyncProducer.receive(), message);
    }

    @Test
    public void testOnReplyMessageWithCorrelatorKey() {
        ChannelSyncEndpoint endpoint = new ChannelSyncEndpoint();

        Map<String, Object> headers = new HashMap<String, Object>();
        final Message<String> message = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                .copyHeaders(headers)
                .build();

        ChannelSyncProducer channelSyncProducer = (ChannelSyncProducer) endpoint.createProducer();
        channelSyncProducer.onReplyMessage(new DefaultReplyMessageCorrelator().getCorrelationKey(message), message);

        Assert.assertEquals(channelSyncProducer.receive(new DefaultReplyMessageCorrelator().getCorrelationKey(message)), message);
    }
    
}
