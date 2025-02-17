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

package com.consol.citrus.endpoint.adapter;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Christoph Deppisch
 */
public class StaticResponseEndpointAdapterTest {

    @Test
    public void testHandleMessage() {
        StaticResponseEndpointAdapter endpointAdapter = new StaticResponseEndpointAdapter();
        Map<String, Object> header = new HashMap<String, Object>();
        header.put("Operation", "UnitTest");

        endpointAdapter.setMessageHeader(header);
        endpointAdapter.setMessagePayload("<TestMessage>Hello User!</TestMessage>");

        Message<?> response = endpointAdapter.handleMessage(
                MessageBuilder.withPayload("<TestMessage>Hello World!</TestMessage>").build());

        Assert.assertEquals(response.getPayload(), "<TestMessage>Hello User!</TestMessage>");
        Assert.assertNotNull(response.getHeaders().get("Operation"));
        Assert.assertEquals(response.getHeaders().get("Operation"), "UnitTest");
    }
}
