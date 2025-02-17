/*
 * Copyright 2006-2014 the original author or authors.
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

package com.consol.citrus.camel.endpoint;

import com.consol.citrus.camel.message.CitrusCamelMessageHeaders;
import com.consol.citrus.report.MessageListeners;
import org.apache.camel.*;
import org.apache.camel.impl.*;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 * @since 1.4.1
 */
public class CamelSyncEndpointTest {

    private CamelContext camelContext = EasyMock.createMock(CamelContext.class);
    private ProducerTemplate producerTemplate = EasyMock.createMock(ProducerTemplate.class);
    private ConsumerTemplate consumerTemplate = EasyMock.createMock(ConsumerTemplate.class);
    private MessageListeners messageListeners = EasyMock.createMock(MessageListeners.class);

    @Test
    public void testCamelSyncEndpointProducer() {
        String endpointUri = "direct:news-feed";
        CamelSyncEndpointConfiguration endpointConfiguration = new CamelSyncEndpointConfiguration();
        endpointConfiguration.setCamelContext(camelContext);
        endpointConfiguration.setEndpointUri(endpointUri);

        CamelSyncEndpoint camelEndpoint = new CamelSyncEndpoint(endpointConfiguration);

        Message<?> requestMessage = MessageBuilder.withPayload("Hello from Citrus!").build();

        DefaultMessage message = new DefaultMessage();
        message.setBody("Hello from Camel!");
        message.setHeader("operation", "newsFeed");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setIn(message);

        reset(camelContext, producerTemplate);

        expect(camelContext.createProducerTemplate()).andReturn(producerTemplate).once();
        expect(camelContext.getUuidGenerator()).andReturn(new JavaUuidGenerator()).once();
        expect(producerTemplate.request(eq(endpointUri), anyObject(Processor.class))).andReturn(exchange).once();

        replay(camelContext, producerTemplate);

        camelEndpoint.createProducer().send(requestMessage);
        Message<?> reply = camelEndpoint.createConsumer().receive(5000L);

        Assert.assertEquals(reply.getPayload(), "Hello from Camel!");
        Assert.assertEquals(reply.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_ID), exchange.getExchangeId());
        Assert.assertTrue(reply.getHeaders().containsKey(CitrusCamelMessageHeaders.EXCHANGE_PATTERN));
        Assert.assertTrue(reply.getHeaders().containsKey(CitrusCamelMessageHeaders.EXCHANGE_FAILED));
        Assert.assertEquals(reply.getHeaders().get("operation"), "newsFeed");

        verify(camelContext, producerTemplate);
    }

    @Test
    public void testCamelSyncEndpointConsumer() {
        String endpointUri = "direct:news-feed";
        CamelSyncEndpointConfiguration endpointConfiguration = new CamelSyncEndpointConfiguration();
        endpointConfiguration.setCamelContext(camelContext);
        endpointConfiguration.setEndpointUri(endpointUri);

        CamelSyncEndpoint camelEndpoint = new CamelSyncEndpoint(endpointConfiguration);

        DefaultMessage message = new DefaultMessage();
        message.setBody("Hello from Camel!");
        message.setHeader("operation", "newsFeed");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setIn(message);

        Message<?> replyMessage = MessageBuilder.withPayload("Hello from Citrus!")
                                                .setHeader("operation", "hello")
                                                .build();

        reset(camelContext, consumerTemplate);

        expect(camelContext.createConsumerTemplate()).andReturn(consumerTemplate).times(2);
        expect(camelContext.getUuidGenerator()).andReturn(new JavaUuidGenerator()).once();
        expect(consumerTemplate.receive(endpointUri, endpointConfiguration.getTimeout())).andReturn(exchange).once();
        consumerTemplate.doneUoW(exchange);
        expectLastCall().once();

        replay(camelContext, consumerTemplate);

        Message receivedMessage = camelEndpoint.createConsumer().receive(endpointConfiguration.getTimeout());
        Assert.assertEquals(receivedMessage.getPayload(), "Hello from Camel!");
        Assert.assertEquals(receivedMessage.getHeaders().get("operation"), "newsFeed");
        Assert.assertTrue(receivedMessage.getHeaders().containsKey(CitrusCamelMessageHeaders.EXCHANGE_ID));
        Assert.assertTrue(receivedMessage.getHeaders().containsKey(CitrusCamelMessageHeaders.EXCHANGE_PATTERN));
        Assert.assertTrue(receivedMessage.getHeaders().containsKey(CitrusCamelMessageHeaders.EXCHANGE_FAILED));

        camelEndpoint.createProducer().send(replyMessage);

        Assert.assertEquals(exchange.getOut().getBody().toString(), replyMessage.getPayload());
        Assert.assertEquals(exchange.getOut().getHeaders().get("operation"), "hello");

        verify(camelContext, consumerTemplate);
    }

    @Test
    public void testCamelSyncEndpointWithMessageListeners() {
        String endpointUri = "direct:news-feed";
        CamelSyncEndpointConfiguration endpointConfiguration = new CamelSyncEndpointConfiguration();
        endpointConfiguration.setCamelContext(camelContext);
        endpointConfiguration.setEndpointUri(endpointUri);

        CamelSyncEndpoint camelEndpoint = new CamelSyncEndpoint(endpointConfiguration);
        camelEndpoint.setMessageListener(messageListeners);

        Message<?> requestMessage = MessageBuilder.withPayload("Hello from Citrus!").build();

        DefaultMessage message = new DefaultMessage();
        message.setBody("Hello from Camel!");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setIn(message);

        reset(camelContext, producerTemplate, messageListeners);

        expect(camelContext.createProducerTemplate()).andReturn(producerTemplate).once();
        expect(camelContext.getUuidGenerator()).andReturn(new JavaUuidGenerator()).once();
        expect(producerTemplate.request(eq(endpointUri), anyObject(Processor.class))).andReturn(exchange).once();
        messageListeners.onOutboundMessage(requestMessage.toString());
        messageListeners.onInboundMessage(anyObject(String.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                String inboundMessage = (String) getCurrentArguments()[0];
                Assert.assertTrue(inboundMessage.contains("Hello from Camel!"));
                return null;
            }
        }).once();

        replay(camelContext, producerTemplate, messageListeners);

        camelEndpoint.createProducer().send(requestMessage);
        camelEndpoint.createConsumer().receive(5000L);

        verify(camelContext, producerTemplate, messageListeners);
    }
}
