/*
 * Copyright 2006-2013 the original author or authors.
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
import com.consol.citrus.messaging.ReplyConsumer;
import com.consol.citrus.report.MessageListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class JmsSyncProducer extends JmsProducer implements ReplyConsumer {

    /** JMS connection */
    private Connection connection = null;

    /** JMS session */
    private Session session = null;

    /** This producer's name used for durable subscriptions and as topic client id*/
    private final String name;

    /** Store of reply messages */
    private Map<String, Message<?>> replyMessages = new HashMap<String, Message<?>>();

    /** Endpoint configuration */
    private final JmsSyncEndpointConfiguration endpointConfiguration;

    /** Logger */
    private static Logger log = LoggerFactory.getLogger(JmsSyncProducer.class);

    /** Retry logger */
    private static final Logger RETRY_LOG = LoggerFactory.getLogger("com.consol.citrus.MessageRetryLogger");

    /**
     * Default constructor using endpoint configuration.
     * @param endpointConfiguration
     * @param messageListeners
     * @param name
     */
    public JmsSyncProducer(JmsSyncEndpointConfiguration endpointConfiguration,
                           MessageListeners messageListeners,
                           String name) {
        super(endpointConfiguration, messageListeners);
        this.endpointConfiguration = endpointConfiguration;
        this.name = name;
    }

    @Override
    public void send(Message<?> message) {
        Assert.notNull(message, "Message is empty - unable to send empty message");

        String defaultDestinationName = endpointConfiguration.getDefaultDestinationName();

        log.info("Sending JMS message to destination: '" + defaultDestinationName + "'");

        onOutboundMessage(message);

        MessageProducer messageProducer = null;
        MessageConsumer messageConsumer = null;
        Destination replyToDestination = null;

        try {
            createConnection();
            createSession(connection);

            javax.jms.Message jmsRequest = endpointConfiguration.getMessageConverter().toMessage(message, session);

            messageProducer = session.createProducer(getDefaultDestination(session));

            replyToDestination = getReplyDestination(session, message);
            if (replyToDestination instanceof TemporaryQueue || replyToDestination instanceof TemporaryTopic) {
                messageConsumer = session.createConsumer(replyToDestination);
            }

            jmsRequest.setJMSReplyTo(replyToDestination);

            messageProducer.send(jmsRequest);

            if (messageConsumer == null) {
                messageConsumer = createMessageConsumer(replyToDestination, jmsRequest.getJMSMessageID());
            }

            log.info("Message was successfully sent to destination: '{}'", defaultDestinationName);
            log.info("Waiting for reply message on destination: '{}'", replyToDestination);

            javax.jms.Message jmsReplyMessage = (endpointConfiguration.getTimeout() >= 0) ? messageConsumer.receive(endpointConfiguration.getTimeout()) : messageConsumer.receive();
            Message<?> responseMessage = (Message<?>)endpointConfiguration.getMessageConverter().fromMessage(jmsReplyMessage);

            log.info("Received reply message on destination: '{}'", replyToDestination);

            onInboundMessage(responseMessage);

            onReplyMessage(message, responseMessage);
        } catch (JMSException e) {
            throw new CitrusRuntimeException(e);
        } finally {
            JmsUtils.closeMessageProducer(messageProducer);
            JmsUtils.closeMessageConsumer(messageConsumer);
            deleteTemporaryDestination(replyToDestination);
        }
    }

    @Override
    public Message<?> receive() {
        return receive("", endpointConfiguration.getTimeout());
    }

    @Override
    public Message<?> receive(String selector) {
        return receive(selector, endpointConfiguration.getTimeout());
    }

    @Override
    public Message<?> receive(long timeout) {
        return receive("", timeout);
    }

    @Override
    public Message<?> receive(String selector, long timeout) {
        long timeLeft = timeout;
        Message<?> message = findReplyMessage(selector);

        while (message == null && timeLeft > 0) {
            timeLeft -= endpointConfiguration.getPollingInterval();

            if (RETRY_LOG.isDebugEnabled()) {
                RETRY_LOG.debug("Reply message did not arrive yet - retrying in " + (timeLeft > 0 ? endpointConfiguration.getPollingInterval() : endpointConfiguration.getPollingInterval() + timeLeft) + "ms");
            }

            try {
                Thread.sleep(timeLeft > 0 ? endpointConfiguration.getPollingInterval() : endpointConfiguration.getPollingInterval() + timeLeft);
            } catch (InterruptedException e) {
                RETRY_LOG.warn("Thread interrupted while waiting for retry", e);
            }

            message = findReplyMessage(selector);
        }

        return message;
    }

    /**
     * Create new JMS connection.
     * @return connection
     * @throws JMSException
     */
    protected void createConnection() throws JMSException {
        if (connection == null) {
            if (!endpointConfiguration.isPubSubDomain() && endpointConfiguration.getConnectionFactory() instanceof QueueConnectionFactory) {
                connection = ((QueueConnectionFactory) endpointConfiguration.getConnectionFactory()).createQueueConnection();
            } else if (endpointConfiguration.isPubSubDomain() && endpointConfiguration.getConnectionFactory() instanceof TopicConnectionFactory) {
                connection = ((TopicConnectionFactory) endpointConfiguration.getConnectionFactory()).createTopicConnection();
                connection.setClientID(name);
            } else {
                log.warn("Not able to create a connection with connection factory '" + endpointConfiguration.getConnectionFactory() + "'" +
                        " when using setting 'publish-subscribe-domain' (=" + endpointConfiguration.isPubSubDomain() + ")");

                connection = endpointConfiguration.getConnectionFactory().createConnection();
            }

            connection.start();
        }
    }

    /**
     * Create new JMS session.
     * @param connection to use for session creation.
     * @return session.
     * @throws JMSException
     */
    protected void createSession(Connection connection) throws JMSException {
        if (session == null) {
            if (!endpointConfiguration.isPubSubDomain() && connection instanceof QueueConnection) {
                session = ((QueueConnection) connection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            } else if (endpointConfiguration.isPubSubDomain() && endpointConfiguration.getConnectionFactory() instanceof TopicConnectionFactory) {
                session = ((TopicConnection) connection).createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            } else {
                log.warn("Not able to create a session with connection factory '" + endpointConfiguration.getConnectionFactory() + "'" +
                        " when using setting 'publish-subscribe-domain' (=" + endpointConfiguration.isPubSubDomain() + ")");

                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        }
    }

    /**
     * Creates a message consumer on temporary/durable queue or topic. Durable queue/topic destinations
     * require a message selector to be set.
     *
     * @param replyToDestination the reply destination.
     * @param messageId the messageId used for optional message selector.
     * @return
     * @throws JMSException
     */
    private MessageConsumer createMessageConsumer(Destination replyToDestination, String messageId) throws JMSException {
        MessageConsumer messageConsumer;

        if (replyToDestination instanceof Queue) {
            messageConsumer = session.createConsumer(replyToDestination,
                    "JMSCorrelationID = '" + messageId.replaceAll("'", "''") + "'");
        } else {
            messageConsumer = session.createDurableSubscriber((Topic)replyToDestination, name,
                    "JMSCorrelationID = '" + messageId.replaceAll("'", "''") + "'", false);
        }

        return messageConsumer;
    }

    /**
     * Delete temporary destinations.
     * @param destination
     */
    private void deleteTemporaryDestination(Destination destination) {
        log.debug("Delete temporary destination: '{}'", destination);

        try {
            if (destination instanceof TemporaryQueue) {
                ((TemporaryQueue) destination).delete();
            } else if (destination instanceof TemporaryTopic) {
                ((TemporaryTopic) destination).delete();
            }
        } catch (JMSException e) {
            log.error("Error while deleting temporary destination '" + destination + "'", e);
        }
    }

    /**
     * Retrieve the reply destination either by injected instance, destination name or
     * by creating a new temporary destination.
     *
     * @param session current JMS session
     * @param message holding possible reply destination in header.
     * @return the reply destination.
     * @throws JMSException
     */
    private Destination getReplyDestination(Session session, Message<?> message) throws JMSException {
        if (message.getHeaders().getReplyChannel() != null) {
            if (message.getHeaders().getReplyChannel() instanceof Destination) {
                return (Destination)message.getHeaders().getReplyChannel();
            } else {
                return resolveDestinationName(message.getHeaders().getReplyChannel().toString(), session);
            }
        } else if (endpointConfiguration.getReplyDestination() != null) {
            return endpointConfiguration.getReplyDestination();
        } else if (StringUtils.hasText(endpointConfiguration.getReplyDestinationName())) {
            return resolveDestinationName(endpointConfiguration.getReplyDestinationName(), session);
        }

        if (endpointConfiguration.isPubSubDomain() && session instanceof TopicSession){
            return session.createTemporaryTopic();
        } else {
            return session.createTemporaryQueue();
        }
    }

    /**
     * Get send destination either from injected destination instance or by resolving
     * a destination name.
     *
     * @param session current JMS session
     * @return the destination.
     * @throws JMSException
     */
    private Destination getDefaultDestination(Session session) throws JMSException {
        if (endpointConfiguration.getDestination() != null) {
            return endpointConfiguration.getDestination();
        }

        return resolveDestinationName(endpointConfiguration.getDestinationName(), session);
    }

    /**
     * Resolves the destination name from Jms session.
     * @param name
     * @param session
     * @return
     */
    private Destination resolveDestinationName(String name, Session session) throws JMSException {
        return new DynamicDestinationResolver().resolveDestinationName(session, name, endpointConfiguration.isPubSubDomain());
    }

    /**
     * Saves reply message with correlation key to local store for later processing.
     * @param correlationKey
     * @param replyMessage the reply message.
     */
    public void onReplyMessage(String correlationKey, Message<?> replyMessage) {
        replyMessages.put(correlationKey, replyMessage);
    }

    /**
     * Saves reply message to local store for later processing. Constructs correlation key from initial request.
     * @param requestMessage
     * @param replyMessage
     */
    public void onReplyMessage(Message<?> requestMessage, Message<?> replyMessage) {
        if (endpointConfiguration.getCorrelator() != null) {
            onReplyMessage(endpointConfiguration.getCorrelator().getCorrelationKey(requestMessage), replyMessage);
        } else {
            onReplyMessage("", replyMessage);
        }
    }

    /**
     * Tries to find reply message for correlation key from local store.
     * @param correlationKey
     * @return
     */
    public Message<?> findReplyMessage(String correlationKey) {
        return replyMessages.remove(correlationKey);
    }

    /**
     * Informs message listeners if present.
     * @param receivedMessage
     */
    protected void onInboundMessage(Message<?> receivedMessage) {
        if (getMessageListener() != null) {
            getMessageListener().onInboundMessage((receivedMessage != null ? receivedMessage.toString() : ""));
        } else {
            log.debug("Received message is:" + System.getProperty("line.separator") + (receivedMessage != null ? receivedMessage.toString() : ""));
        }
    }

    public void destroy() {
        JmsUtils.closeSession(session);

        if (connection != null) {
            ConnectionFactoryUtils.releaseConnection(connection, endpointConfiguration.getConnectionFactory(), true);
        }
    }
}
