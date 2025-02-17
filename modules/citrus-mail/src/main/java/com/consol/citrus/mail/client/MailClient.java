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

package com.consol.citrus.mail.client;

import com.consol.citrus.endpoint.AbstractEndpoint;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.mail.model.AttachmentPart;
import com.consol.citrus.mail.model.MailMessage;
import com.consol.citrus.messaging.Consumer;
import com.consol.citrus.messaging.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.Message;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Properties;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class MailClient extends AbstractEndpoint implements Producer, InitializingBean {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(MailClient.class);

    /**
     * Default constructor initializing endpoint configuration.
     */
    public MailClient() {
        super(new MailEndpointConfiguration());
    }

    /**
     * Default constructor using endpoint configuration.
     * @param endpointConfiguration
     */
    public MailClient(MailEndpointConfiguration endpointConfiguration) {
        super(endpointConfiguration);
    }

    @Override
    public MailEndpointConfiguration getEndpointConfiguration() {
        return (MailEndpointConfiguration) super.getEndpointConfiguration();
    }

    @Override
    public void send(Message<?> message) {
        log.info(String.format("Sending mail message to host: '%s://%s:%s'", getEndpointConfiguration().getProtocol(), getEndpointConfiguration().getHost(), getEndpointConfiguration().getPort()));

        MimeMessage mimeMessage = createMailMessage(message);
        getEndpointConfiguration().getJavaMailSender().send(mimeMessage);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String mailMessageContent;
        try {
            mimeMessage.writeTo(bos);
            mailMessageContent = bos.toString(); //TODO use message charset encoding
        } catch (IOException e) {
            mailMessageContent = message.toString();
        } catch (MessagingException e) {
            mailMessageContent = message.toString();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                log.warn("Failed to close output stream", e);
            }
        }

        if (getMessageListener() != null) {
            getMessageListener().onOutboundMessage(mailMessageContent);
        } else {
            log.info("Sent message is:" + System.getProperty("line.separator") + mailMessageContent);
        }

        log.info(String.format("Message was successfully sent to host: '%s://%s:%s'", getEndpointConfiguration().getProtocol(), getEndpointConfiguration().getHost(), getEndpointConfiguration().getPort()));
    }

    /**
     * Create mime mail message from Citrus mail message object payload.
     * @param message
     * @return
     */
    protected MimeMessage createMailMessage(Message<?> message) {
        Object payload = message.getPayload();

        MailMessage mailMessage = null;
        if (payload != null) {
            if (payload instanceof MailMessage) {
                mailMessage = (MailMessage) payload;
            } else if (payload instanceof String) {
                mailMessage = (MailMessage) getEndpointConfiguration().getMailMessageMapper().fromXML(payload.toString());
            }
        }

        if (mailMessage == null) {
            throw new CitrusRuntimeException("Unable to create proper mail message from paylaod: " + payload);
        }

        try {
            MimeMessage mimeMessage = getEndpointConfiguration().getJavaMailSender().createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, mailMessage.getBody().hasAttachments(), mailMessage.getBody().getCharsetName());

            mimeMessageHelper.setFrom(mailMessage.getFrom());
            mimeMessageHelper.setTo(StringUtils.commaDelimitedListToStringArray(mailMessage.getTo()));

            if (StringUtils.hasText(mailMessage.getCc())) {
                mimeMessageHelper.setCc(StringUtils.commaDelimitedListToStringArray(mailMessage.getCc()));
            }

            if (StringUtils.hasText(mailMessage.getBcc())) {
                mimeMessageHelper.setBcc(StringUtils.commaDelimitedListToStringArray(mailMessage.getBcc()));
            }

            mimeMessageHelper.setReplyTo(mailMessage.getReplyTo() != null ? mailMessage.getReplyTo() : mailMessage.getFrom());
            mimeMessageHelper.setSentDate(new Date());
            mimeMessageHelper.setSubject(mailMessage.getSubject());
            mimeMessageHelper.setText(mailMessage.getBody().getContent());

            if (mailMessage.getBody().hasAttachments()) {
                for (AttachmentPart attachmentPart : mailMessage.getBody().getAttachments()) {
                    mimeMessageHelper.addAttachment(attachmentPart.getFileName(),
                            new ByteArrayResource(attachmentPart.getContent().getBytes(Charset.forName(attachmentPart.getCharsetName()))),
                            attachmentPart.getContentType());
                }
            }

            return mimeMessage;
        } catch (MessagingException e) {
            throw new CitrusRuntimeException("Failed to create mail mime message", e);
        }
    }

    /**
     * Creates a message producer for this endpoint for sending messages
     * to this endpoint.
     */
    @Override
    public Producer createProducer() {
        return this;
    }

    /**
     * Creates a message consumer for this endpoint. Consumer receives
     * messages on this endpoint.
     *
     * @return
     */
    @Override
    public Consumer createConsumer() {
        throw new CitrusRuntimeException("Mail client is unable to create message consumer!");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.hasText(getEndpointConfiguration().getJavaMailSender().getUsername()) ||
                StringUtils.hasText(getEndpointConfiguration().getJavaMailSender().getPassword())) {

            Properties javaMailProperties = getEndpointConfiguration().getJavaMailSender().getJavaMailProperties();
            if (javaMailProperties == null) {
                javaMailProperties = new Properties();
            }

            javaMailProperties.setProperty("mail.smtp.auth", "true");
            getEndpointConfiguration().getJavaMailSender().setJavaMailProperties(javaMailProperties);
        }

        if (!StringUtils.hasText(getEndpointConfiguration().getJavaMailSender().getProtocol())) {
            getEndpointConfiguration().getJavaMailSender().setProtocol("smtp");
        }
    }

}
