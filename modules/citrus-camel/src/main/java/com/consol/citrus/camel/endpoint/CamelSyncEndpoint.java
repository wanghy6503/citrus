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

import com.consol.citrus.messaging.Consumer;
import com.consol.citrus.messaging.Producer;

/**
 * @author Christoph Deppisch
 * @since 1.4.1
 */
public class CamelSyncEndpoint extends CamelEndpoint {

    /** One of producer or consumer for this endpoint */
    private CamelSyncProducer camelSyncMessageProducer;
    private CamelSyncConsumer camelSyncMessageConsumer;

    /**
     * Default constructor initializing endpoint.
     */
    public CamelSyncEndpoint() {
        super(new CamelSyncEndpointConfiguration());
    }

    /**
     * Constructor using endpoint configuration.
     * @param endpointConfiguration
     */
    public CamelSyncEndpoint(CamelSyncEndpointConfiguration endpointConfiguration) {
        super(endpointConfiguration);
    }

    @Override
    public CamelSyncEndpointConfiguration getEndpointConfiguration() {
        return (CamelSyncEndpointConfiguration) super.getEndpointConfiguration();
    }

    @Override
    public Consumer createConsumer() {
        if (camelSyncMessageProducer != null) {
            return camelSyncMessageProducer;
        }

        if (camelSyncMessageConsumer == null) {
            camelSyncMessageConsumer = new CamelSyncConsumer(getEndpointConfiguration(), getMessageListener());
        }

        return camelSyncMessageConsumer;
    }

    @Override
    public Producer createProducer() {
        if (camelSyncMessageConsumer != null) {
            return camelSyncMessageConsumer;
        }

        if (camelSyncMessageProducer == null) {
            camelSyncMessageProducer = new CamelSyncProducer(getEndpointConfiguration(), getMessageListener());
        }

        return camelSyncMessageProducer;
    }
}
