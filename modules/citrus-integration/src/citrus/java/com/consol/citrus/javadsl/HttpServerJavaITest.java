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
public class HttpServerJavaITest extends TestNGCitrusTestBuilder {
    
    @CitrusTest
    public void HttpServerJavaITest() {
        variable("custom_header_id", "123456789");
        
        echo("Send Http message and respond with 200 OK");
        
        parallel(
            send("httpClient")
                .payload("<testRequestMessage>" +
                                "<text>Hello HttpServer</text>" +
                            "</testRequestMessage>")
                .header("CustomHeaderId", "${custom_header_id}")
                .header("Content-Type", "text/xml")
                .header("Accept", "text/xml,*/*")
                .header("citrus_http_method", "POST"),
            
            sequential(
                receive("httpServerRequestEndpoint")
                    .payload("<testRequestMessage>" +
                                "<text>Hello HttpServer</text>" +
                            "</testRequestMessage>")
                    .header("CustomHeaderId", "${custom_header_id}")
                    .header("Content-Type", "text/xml;charset=UTF-8")
                    .header("Accept", "text/xml,*/*")
                    .header("Authorization", "Basic c29tZVVzZXJuYW1lOnNvbWVQYXNzd29yZA==")
                    .header("citrus_http_method", "POST")
                    .header("citrus_http_request_uri", "/test")
                    .extractFromHeader("jms_messageId", "correlation_id"),
                    
               send("httpServerResponseEndpoint")
                   .payload("<testResponseMessage>" +
                                "<text>Hello Citrus</text>" +
                            "</testResponseMessage>")
                    .header("CustomHeaderId", "${custom_header_id}")
                    .header("citrus_http_status_code", "200")
                    .header("citrus_http_version", "HTTP/1.1")
                    .header("citrus_http_reason_phrase", "OK")
                    .header("Content-Type", "text/xml")
                    .header("jms_correlationId", "${correlation_id}")
            )
        );
        
        receive("httpClient")
            .payload("<testResponseMessage>" +
                        "<text>Hello Citrus</text>" +
                    "</testResponseMessage>")
            .header("CustomHeaderId", "${custom_header_id}")
            .header("citrus_http_status_code", "200")
            .header("citrus_http_version", "HTTP/1.1")
            .header("citrus_http_reason_phrase", "OK");
        
        echo("Send Http request and respond with 404 status code");
        
        
        parallel(
            send("httpClient")
                .payload("<testRequestMessage>" +
                                "<text>Hello HttpServer</text>" +
                            "</testRequestMessage>")
                .header("CustomHeaderId", "${custom_header_id}")
                .header("Content-Type", "text/xml")
                .header("Accept", "text/xml,*/*")
                .header("citrus_http_method", "POST"),
            
            sequential(
                receive("httpServerRequestEndpoint")
                    .payload("<testRequestMessage>" +
                                "<text>Hello HttpServer</text>" +
                            "</testRequestMessage>")
                    .header("CustomHeaderId", "${custom_header_id}")
                    .header("Content-Type", "text/xml;charset=UTF-8")
                    .header("Accept", "text/xml,*/*")
                    .header("Authorization", "Basic c29tZVVzZXJuYW1lOnNvbWVQYXNzd29yZA==")
                    .header("citrus_http_method", "POST")
                    .header("citrus_http_request_uri", "/test")
                    .extractFromHeader("jms_messageId", "correlation_id"),
                    
               send("httpServerResponseEndpoint")
                   .payload("<testResponseMessage>" +
                                "<text>Hello Citrus</text>" +
                            "</testResponseMessage>")
                    .header("CustomHeaderId", "${custom_header_id}")
                    .header("citrus_http_status_code", "404")
                    .header("citrus_http_version", "HTTP/1.1")
                    .header("citrus_http_reason_phrase", "OK")
                    .header("Content-Type", "text/xml")
                    .header("jms_correlationId", "${correlation_id}")
            )
        );
        
        receive("httpClient")
            .payload("<testResponseMessage>" +
                        "<text>Hello Citrus</text>" +
                    "</testResponseMessage>")
            .header("CustomHeaderId", "${custom_header_id}")
            .header("citrus_http_status_code", "404")
            .header("citrus_http_version", "HTTP/1.1")
            .header("citrus_http_reason_phrase", "NOT_FOUND");
        
        echo("Skip response and use fallback message handler");
        
        send("httpClient")
            .payload("<testRequestMessage>" +
                            "<text>Hello HttpServer</text>" +
                        "</testRequestMessage>")
            .header("CustomHeaderId", "${custom_header_id}")
            .header("Content-Type", "text/xml")
            .header("Accept", "text/xml,*/*")
            .header("citrus_http_method", "POST");
        
        
        receive("httpClient")
            .timeout(2000L)
            .header("citrus_http_status_code", "200")
            .header("citrus_http_version", "HTTP/1.1")
            .header("citrus_http_reason_phrase", "OK");
        
    }
}