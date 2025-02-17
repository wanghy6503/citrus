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

import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.messaging.AbstractMessageConsumer;
import com.consol.citrus.report.MessageListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

/**
 * @author Christoph Deppisch
 * @since 1.4.1
 */
public class VertxConsumer extends AbstractMessageConsumer {

    /** Vert.x instance */
    private final Vertx vertx;

    /** Endpoint configuration */
    private final VertxEndpointConfiguration endpointConfiguration;

    /** Message listener  */
    private final MessageListeners messageListener;

    /** Logger */
    private static Logger log = LoggerFactory.getLogger(VertxConsumer.class);

    /** Retry logger */
    private static final Logger RETRY_LOG = LoggerFactory.getLogger("com.consol.citrus.MessageRetryLogger");

    /**
     * Default constructor using endpoint.
     * @param vertx
     * @param endpointConfiguration
     * @param messageListener
     */
    public VertxConsumer(Vertx vertx, VertxEndpointConfiguration endpointConfiguration, MessageListeners messageListener) {
        super(endpointConfiguration);
        this.vertx = vertx;
        this.endpointConfiguration = endpointConfiguration;
        this.messageListener = messageListener;
    }

    @Override
    public Message<?> receive(long timeout) {
        log.info("Receiving message on Vert.x event bus address: '" + endpointConfiguration.getAddress() + "'");

        VertxSingleMessageHandler vertxMessageHandler = new VertxSingleMessageHandler();
        vertx.eventBus().registerHandler(endpointConfiguration.getAddress(), vertxMessageHandler);

        long timeLeft = timeout;
        Message<?> message = endpointConfiguration.getMessageConverter().convertMessage(vertxMessageHandler.getMessage());

        while (message == null && timeLeft > 0) {
            timeLeft -= endpointConfiguration.getPollingInterval();

            if (RETRY_LOG.isDebugEnabled()) {
                RETRY_LOG.debug(String.format("Waiting for message on Vert.x event bus address '%s' - retrying in %s ms",
                        endpointConfiguration.getAddress(),
                        (timeLeft > 0 ? endpointConfiguration.getPollingInterval() : endpointConfiguration.getPollingInterval() + timeLeft)));
            }

            try {
                Thread.sleep(timeLeft > 0 ? endpointConfiguration.getPollingInterval() : endpointConfiguration.getPollingInterval() + timeLeft);
            } catch (InterruptedException e) {
                RETRY_LOG.warn("Thread interrupted while waiting for message on Vert.x event bus", e);
            }

            message = endpointConfiguration.getMessageConverter().convertMessage(vertxMessageHandler.getMessage());
        }

        if (message == null) {
            throw new ActionTimeoutException("Action timed out while receiving message on Vert.x event bus address '" + endpointConfiguration.getAddress() + "'");
        }

        log.info("Received message on Vert.x event bus address: '" + endpointConfiguration.getAddress() + "'");

        onInboundMessage(message);

        return message;
    }

    /**
     * Informs message listeners if present.
     * @param receivedMessage
     */
    protected void onInboundMessage(Message<?> receivedMessage) {
        if (messageListener != null) {
            messageListener.onInboundMessage((receivedMessage != null ? receivedMessage.toString() : ""));
        } else {
            log.debug("Received message is:" + System.getProperty("line.separator") + (receivedMessage != null ? receivedMessage.toString() : ""));
        }
    }

    /**
     * Simple Vert.x message handler stores first message received on event bus and ignores all further messages
     * until subscription is unregistered automatically.
     */
    private class VertxSingleMessageHandler implements Handler<org.vertx.java.core.eventbus.Message> {
        private org.vertx.java.core.eventbus.Message message;

        @Override
        public void handle(org.vertx.java.core.eventbus.Message event) {
            if (message == null) {
                this.message = event;
                vertx.eventBus().unregisterHandler(endpointConfiguration.getAddress(), this);
            } else {
                log.warn("Vert.x message handler ignored message on event bus address '" + endpointConfiguration.getAddress() + "'");
                log.debug("Vert.x message ignored is " + event);
            }
        }

        /**
         * Gets the vert.x message received on event bus.
         * @return
         */
        public org.vertx.java.core.eventbus.Message getMessage() {
            return message;
        }
    }

    /**
     * Gets the message listener.
     * @return
     */
    public MessageListeners getMessageListener() {
        return messageListener;
    }
}
