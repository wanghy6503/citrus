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

package com.consol.citrus.camel.message;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import org.apache.camel.*;
import org.apache.camel.impl.DefaultExchange;
import org.easymock.EasyMock;
import org.springframework.integration.Message;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;

/**
 * @author Christoph Deppisch
 * @since 1.4.1
 */
public class CamelMessageConverterTest {

    private CamelContext camelContext = EasyMock.createMock(CamelContext.class);
    private CamelMessageConverter messageConverter = new CamelMessageConverter();

    @Test
    public void testConvertMessage() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setExchangeId(UUID.randomUUID().toString());
        exchange.setFromRouteId("helloRoute");
        exchange.getIn().setBody("Hello from Citrus!");
        exchange.getIn().setHeader("operation", "sayHello");

        Message result = messageConverter.convertMessage(exchange);

        Assert.assertEquals(result.getPayload(), "Hello from Citrus!");
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_ID), exchange.getExchangeId());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_PATTERN), ExchangePattern.InOnly.name());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.ROUTE_ID), "helloRoute");
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_FAILED), false);
        Assert.assertEquals(result.getHeaders().get("operation"), "sayHello");
    }

    @Test
    public void testConvertMessageWithProperties() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setExchangeId(UUID.randomUUID().toString());
        exchange.setFromRouteId("helloRoute");
        exchange.getIn().setBody("Hello from Citrus!");
        exchange.getIn().setHeader("operation", "sayHello");

        exchange.setProperty("SpecialProperty", "foo");
        exchange.setProperty("VerySpecialProperty", "bar");

        Message result = messageConverter.convertMessage(exchange);

        Assert.assertEquals(result.getPayload(), "Hello from Citrus!");
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_ID), exchange.getExchangeId());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_PATTERN), ExchangePattern.InOnly.name());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.ROUTE_ID), "helloRoute");
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_FAILED), false);
        Assert.assertEquals(result.getHeaders().get("operation"), "sayHello");
        Assert.assertEquals(result.getHeaders().get("SpecialProperty"), "foo");
        Assert.assertEquals(result.getHeaders().get("VerySpecialProperty"), "bar");
    }

    @Test
    public void testConvertMessageWithException() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setExchangeId(UUID.randomUUID().toString());
        exchange.setFromRouteId("helloRoute");
        exchange.getIn().setBody("Hello from Citrus!");
        exchange.getIn().setHeader("operation", "sayHello");
        exchange.setException(new CitrusRuntimeException("Something went wrong"));

        Message result = messageConverter.convertMessage(exchange);

        Assert.assertEquals(result.getPayload(), "Hello from Citrus!");
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_ID), exchange.getExchangeId());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_PATTERN), ExchangePattern.InOnly.name());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.ROUTE_ID), "helloRoute");
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_FAILED), true);
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_EXCEPTION), CitrusRuntimeException.class.getName());
        Assert.assertEquals(result.getHeaders().get(CitrusCamelMessageHeaders.EXCHANGE_EXCEPTION_MESSAGE), "Something went wrong");
        Assert.assertEquals(result.getHeaders().get("operation"), "sayHello");
    }
}
