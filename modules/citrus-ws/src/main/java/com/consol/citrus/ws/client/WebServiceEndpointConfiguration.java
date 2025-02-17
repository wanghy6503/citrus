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

package com.consol.citrus.ws.client;

import com.consol.citrus.endpoint.AbstractEndpointConfiguration;
import com.consol.citrus.endpoint.resolver.DynamicEndpointUriResolver;
import com.consol.citrus.endpoint.resolver.EndpointUriResolver;
import com.consol.citrus.message.ErrorHandlingStrategy;
import com.consol.citrus.message.ReplyMessageCorrelator;
import com.consol.citrus.ws.addressing.WsAddressingHeaders;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class WebServiceEndpointConfiguration extends AbstractEndpointConfiguration {

    /** Web service tempalte */
    private WebServiceTemplate webServiceTemplate;

    /** Web service message factory */
    private WebServiceMessageFactory messageFactory;

    /** Web service message sender */
    private WebServiceMessageSender messageSender;

    /** List of client interceptors */
    private List<ClientInterceptor> interceptors = new ArrayList<ClientInterceptor>();

    /* Single client interceptor */
    private ClientInterceptor interceptor;

    /** Default uri */
    private String defaultUri;

    /** Reply message correlator */
    private ReplyMessageCorrelator correlator = null;

    /** Resolves dynamic endpoint uri */
    private EndpointUriResolver endpointResolver = new DynamicEndpointUriResolver();

    /** WS adressing specific headers */
    private WsAddressingHeaders addressingHeaders;

    /** Should http errors be handled with reply message handler or simply throw exception */
    private ErrorHandlingStrategy errorHandlingStrategy = ErrorHandlingStrategy.THROWS_EXCEPTION;

    /** Polling interval when waiting for synchronous reply message to arrive */
    private long pollingInterval = 500;

    /**
     * Creates default web service template with settings in this configuration.
     * @return
     */
    protected WebServiceTemplate createWebServiceTemplate() {
        return new WebServiceTemplate();
    }

    /**
     * Set reply message correlator.
     * @param correlator the correlator to set
     */
    public void setCorrelator(ReplyMessageCorrelator correlator) {
        this.correlator = correlator;
    }

    /**
     * Sets the endpoint uri resolver.
     * @param endpointResolver the endpointUriResolver to set
     */
    public void setEndpointResolver(EndpointUriResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    /**
     * Sets the ws addressing headers for this message sender.
     * @param addressingHeaders the addressingHeaders to set
     */
    public void setAddressingHeaders(WsAddressingHeaders addressingHeaders) {
        this.addressingHeaders = addressingHeaders;
    }

    /**
     * Gets the errorHandlingStrategy.
     * @return the errorHandlingStrategy
     */
    public ErrorHandlingStrategy getErrorHandlingStrategy() {
        return errorHandlingStrategy;
    }

    /**
     * Sets the errorHandlingStrategy.
     * @param errorHandlingStrategy the errorHandlingStrategy to set
     */
    public void setErrorHandlingStrategy(ErrorHandlingStrategy errorHandlingStrategy) {
        this.errorHandlingStrategy = errorHandlingStrategy;
    }

    /**
     * Gets the correlator.
     * @return the correlator the correlator to get.
     */
    public ReplyMessageCorrelator getCorrelator() {
        return correlator;
    }

    /**
     * Gets the endpointResolver.
     * @return the endpointResolver the endpointResolver to get.
     */
    public EndpointUriResolver getEndpointResolver() {
        return endpointResolver;
    }

    /**
     * Gets the addressingHeaders.
     * @return the addressingHeaders the addressingHeaders to get.
     */
    public WsAddressingHeaders getAddressingHeaders() {
        return addressingHeaders;
    }

    /**
     * Gets the web service template.
     * @return
     */
    public WebServiceTemplate getWebServiceTemplate() {
        if (webServiceTemplate == null) {
            webServiceTemplate = createWebServiceTemplate();
        }

        return webServiceTemplate;
    }

    /**
     * Sets the web service template.
     * @param webServiceTemplate
     */
    public void setWebServiceTemplate(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    /**
     * Gets the message factory.
     * @return
     */
    public WebServiceMessageFactory getMessageFactory() {
        return messageFactory;
    }

    /**
     * Sets the message factory.
     * @param messageFactory
     */
    public void setMessageFactory(WebServiceMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
        getWebServiceTemplate().setMessageFactory(messageFactory);
    }

    /**
     * Gets the message sender.
     * @return
     */
    public WebServiceMessageSender getMessageSender() {
        return messageSender;
    }

    /**
     * Sets the message sender.
     * @param messageSender
     */
    public void setMessageSender(WebServiceMessageSender messageSender) {
        this.messageSender = messageSender;
        getWebServiceTemplate().setMessageSender(messageSender);
    }

    /**
     * Gets the default uri from web service template.
     * @return
     */
    public String getDefaultUri() {
        return defaultUri;
    }

    /**
     * Gets the default uri.
     * @param defaultUri
     */
    public void setDefaultUri(String defaultUri) {
        this.defaultUri = defaultUri;
        getWebServiceTemplate().setDefaultUri(defaultUri);
    }

    /**
     * Gets the client interceptors.
     * @return
     */
    public List<ClientInterceptor> getInterceptors() {
        return interceptors;
    }

    /**
     * Sets the client interceptors.
     * @param interceptors
     */
    public void setInterceptors(List<ClientInterceptor> interceptors) {
        this.interceptors = interceptors;
        getWebServiceTemplate().setInterceptors(interceptors.toArray(new ClientInterceptor[interceptors.size()]));
    }

    /**
     * Gets the pollingInterval.
     * @return the pollingInterval the pollingInterval to get.
     */
    public long getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the pollingInterval.
     * @param pollingInterval the pollingInterval to set
     */
    public void setPollingInterval(long pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    /**
     * Gets the single client interceptor.
     * @return
     */
    public ClientInterceptor getInterceptor() {
        return interceptor;
    }

    /**
     * Sets the single client interceptor.
     * @param interceptor
     */
    public void setInterceptor(ClientInterceptor interceptor) {
        this.interceptor = interceptor;
        getWebServiceTemplate().setInterceptors(new ClientInterceptor[] { interceptor });
    }
}
