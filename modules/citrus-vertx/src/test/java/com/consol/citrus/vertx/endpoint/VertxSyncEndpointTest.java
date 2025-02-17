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

package com.consol.citrus.vertx.endpoint;

import com.consol.citrus.report.MessageListeners;
import com.consol.citrus.vertx.factory.SingleVertxInstanceFactory;
import com.consol.citrus.vertx.message.CitrusVertxMessageHeaders;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * @author Christoph Deppisch
 * @since 1.4.1
 */
public class VertxSyncEndpointTest {

    private Vertx vertx = EasyMock.createMock(Vertx.class);
    private EventBus eventBus = EasyMock.createMock(EventBus.class);
    private MessageListeners messageListeners = EasyMock.createMock(MessageListeners.class);
    private org.vertx.java.core.eventbus.Message messageMock = EasyMock.createMock(org.vertx.java.core.eventbus.Message.class);

    private SingleVertxInstanceFactory instanceFactory = new SingleVertxInstanceFactory();

    @BeforeClass
    public void setup() {
        instanceFactory.setVertx(vertx);
    }

    @Test
    public void testVertxSyncEndpointProducer() {
        String eventBusAddress = "news-feed";
        VertxSyncEndpointConfiguration endpointConfiguration = new VertxSyncEndpointConfiguration();
        endpointConfiguration.setAddress(eventBusAddress);

        VertxSyncEndpoint vertxEndpoint = new VertxSyncEndpoint(endpointConfiguration);
        vertxEndpoint.setVertxInstanceFactory(instanceFactory);

        Message<?> requestMessage = MessageBuilder.withPayload("Hello from Citrus!").build();

        reset(vertx, eventBus, messageMock);

        expect(vertx.eventBus()).andReturn(eventBus).once();
        expect(eventBus.send(eq(eventBusAddress), eq(requestMessage.getPayload()), anyObject(Handler.class))).andAnswer(new IAnswer<EventBus>() {
            @Override
            public EventBus answer() throws Throwable {
                Handler handler = (Handler) getCurrentArguments()[2];
                handler.handle(messageMock);
                return eventBus;
            }
        }).once();

        expect(messageMock.body()).andReturn("Hello from Vertx!").once();
        expect(messageMock.address()).andReturn(eventBusAddress).once();
        expect(messageMock.replyAddress()).andReturn("replyAddress").once();

        replay(vertx, eventBus, messageMock);

        vertxEndpoint.createProducer().send(requestMessage);
        Message<?> reply = vertxEndpoint.createConsumer().receive(5000L);

        Assert.assertEquals(reply.getPayload(), "Hello from Vertx!");
        Assert.assertEquals(reply.getHeaders().get(CitrusVertxMessageHeaders.VERTX_ADDRESS), eventBusAddress);
        Assert.assertEquals(reply.getHeaders().get(CitrusVertxMessageHeaders.VERTX_REPLY_ADDRESS), "replyAddress");

        verify(vertx, eventBus, messageMock);
    }

    @Test
    public void testVertxSyncEndpointConsumer() {
        String eventBusAddress = "news-feed";
        VertxSyncEndpointConfiguration endpointConfiguration = new VertxSyncEndpointConfiguration();
        endpointConfiguration.setAddress(eventBusAddress);

        VertxSyncEndpoint vertxEndpoint = new VertxSyncEndpoint(endpointConfiguration);
        vertxEndpoint.setVertxInstanceFactory(instanceFactory);

        Message<?> replyMessage = MessageBuilder.withPayload("Hello from Citrus!").build();

        reset(vertx, eventBus, messageMock);

        expect(messageMock.body()).andReturn("Hello from Vertx!").once();
        expect(messageMock.address()).andReturn(eventBusAddress).once();
        expect(messageMock.replyAddress()).andReturn("replyAddress").once();

        expect(vertx.eventBus()).andReturn(eventBus).times(3);
        expect(eventBus.registerHandler(eq(eventBusAddress), anyObject(Handler.class))).andAnswer(new IAnswer<EventBus>() {
            @Override
            public EventBus answer() throws Throwable {
                Handler handler = (Handler) getCurrentArguments()[1];
                handler.handle(messageMock);
                return eventBus;
            }
        }).once();

        expect(eventBus.unregisterHandler(eq(eventBusAddress), anyObject(Handler.class))).andReturn(eventBus).once();

        expect(eventBus.send("replyAddress", replyMessage.getPayload())).andReturn(eventBus).once();

        replay(vertx, eventBus, messageMock);

        Message receivedMessage = vertxEndpoint.createConsumer().receive(endpointConfiguration.getTimeout());
        Assert.assertEquals(receivedMessage.getPayload(), "Hello from Vertx!");
        Assert.assertEquals(receivedMessage.getHeaders().get(CitrusVertxMessageHeaders.VERTX_ADDRESS), eventBusAddress);
        Assert.assertEquals(receivedMessage.getHeaders().get(CitrusVertxMessageHeaders.VERTX_REPLY_ADDRESS), "replyAddress");

        vertxEndpoint.createProducer().send(replyMessage);

        verify(vertx, eventBus, messageMock);
    }

    @Test
    public void testVertxSyncEndpointWithOutboundMessageListeners() {
        String eventBusAddress = "news-feed";
        VertxSyncEndpointConfiguration endpointConfiguration = new VertxSyncEndpointConfiguration();
        endpointConfiguration.setAddress(eventBusAddress);

        VertxSyncEndpoint vertxEndpoint = new VertxSyncEndpoint(endpointConfiguration);
        vertxEndpoint.setVertxInstanceFactory(instanceFactory);
        vertxEndpoint.setMessageListener(messageListeners);

        Message<?> requestMessage = MessageBuilder.withPayload("Hello from Citrus!").build();

        reset(vertx, eventBus, messageListeners);

        expect(vertx.eventBus()).andReturn(eventBus).once();
        expect(eventBus.send(eq(eventBusAddress), eq(requestMessage.getPayload()), anyObject(Handler.class))).andReturn(eventBus).once();
        messageListeners.onOutboundMessage(requestMessage.toString());
        expectLastCall().once();

        replay(vertx, eventBus, messageListeners);

        vertxEndpoint.createProducer().send(requestMessage);

        verify(vertx, eventBus, messageListeners);
    }
}
