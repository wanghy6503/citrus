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

package com.consol.citrus.javadsl;

import com.consol.citrus.dsl.TestNGCitrusTestBuilder;
import com.consol.citrus.dsl.annotations.CitrusTest;
import org.testng.annotations.Test;

/**
 * @author Christoph Deppisch
 */
@Test
public class SoapHttpErrorJavaITest extends TestNGCitrusTestBuilder {
    
    @CitrusTest
    public void SoapHttpErrorJavaITest() {
        variable("correlationId", "citrus:randomNumber(10)");      
        variable("messageId", "citrus:randomNumber(10)");
        variable("user", "Christoph");
        
        parallel(
            assertException(
                send("webServiceClient")
                    .payload("<ns0:HelloRequest xmlns:ns0=\"http://www.consol.de/schemas/samples/sayHello.xsd\">" +
                                  "<ns0:MessageId>${messageId}</ns0:MessageId>" +
                                  "<ns0:CorrelationId>${correlationId}</ns0:CorrelationId>" +
                                  "<ns0:User>${user}</ns0:User>" +
                                  "<ns0:Text>Hello WebServer</ns0:Text>" +
                              "</ns0:HelloRequest>")
                    .header("{http://citrusframework.org/test}Operation", "sayHello")
                    .header("citrus_http_operation", "sayHello")
                    .header("citrus_soap_action", "sayHello")
            ).exception(org.springframework.ws.client.WebServiceTransportException.class)
            .message("Server Error [500]"),
            sequential(
                receive("webServiceRequestReceiver")
                    .payload("<ns0:HelloRequest xmlns:ns0=\"http://www.consol.de/schemas/samples/sayHello.xsd\">" +
                                  "<ns0:MessageId>${messageId}</ns0:MessageId>" +
                                  "<ns0:CorrelationId>${correlationId}</ns0:CorrelationId>" +
                                  "<ns0:User>${user}</ns0:User>" +
                                  "<ns0:Text>Hello WebServer</ns0:Text>" +
                              "</ns0:HelloRequest>")
                    .header("Operation", "sayHello")
                    .header("operation", "sayHello")
                    .header("citrus_soap_action", "sayHello")
                    .schemaValidation(false)
                    .extractFromHeader("jms_messageId", "internal_correlation_id"),
                send("webServiceResponseSender")
                    .header("citrus_http_status_code", "500")
                    .header("jms_correlationId", "${internal_correlation_id}")
            )
        );
    }
}