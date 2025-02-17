/*
 * Copyright 2006-2012 the original author or authors.
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

package com.consol.citrus.dsl.definition;

import com.consol.citrus.actions.ReceiveMessageAction;
import com.consol.citrus.container.SequenceBeforeTest;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.report.TestActionListeners;
import com.consol.citrus.report.TestListeners;
import com.consol.citrus.testng.AbstractTestNGUnitTest;
import com.consol.citrus.validation.builder.PayloadTemplateMessageBuilder;
import com.consol.citrus.validation.builder.StaticMessageContentBuilder;
import com.consol.citrus.validation.xml.XmlMessageValidationContext;
import com.consol.citrus.ws.SoapAttachment;
import com.consol.citrus.ws.actions.ReceiveSoapMessageAction;
import com.consol.citrus.ws.server.WebServiceServer;
import org.easymock.EasyMock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.support.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 */
public class ReceiveSoapMessageDefinitionTest extends AbstractTestNGUnitTest {
    
    private Endpoint messageEndpoint = EasyMock.createMock(Endpoint.class);
    private WebServiceServer server = EasyMock.createMock(WebServiceServer.class);

    private ApplicationContext applicationContextMock = EasyMock.createMock(ApplicationContext.class);
    private Resource resource = EasyMock.createMock(Resource.class);
    
    private SoapAttachment testAttachment = new SoapAttachment();
    
    /**
     * Setup test attachment.
     */
    @BeforeClass
    public void setup() {
        testAttachment.setContentId("attachment01");
        testAttachment.setContent("This is an attachment");
        testAttachment.setContentType("text/plain");
        testAttachment.setCharsetName("UTF-8");
    }

    @Test
    public void testWebServiceServerReceive() {
        MockBuilder builder = new MockBuilder(applicationContext) {
            @Override
            public void configure() {
                receive(server)
                        .message(MessageBuilder.withPayload("Foo").setHeader("operation", "foo").build())
                        .attachment(testAttachment);
            }
        };

        builder.execute();

        Assert.assertEquals(builder.testCase().getActions().size(), 1);
        Assert.assertEquals(builder.testCase().getActions().get(0).getClass(), ReceiveSoapMessageAction.class);

        ReceiveSoapMessageAction action = ((ReceiveSoapMessageAction)builder.testCase().getActions().get(0));
        Assert.assertEquals(action.getName(), "receive");

        Assert.assertEquals(action.getMessageType(), MessageType.XML.name());
        Assert.assertEquals(action.getEndpoint(), server);
        Assert.assertEquals(action.getValidationContexts().size(), 1);
        Assert.assertEquals(action.getValidationContexts().get(0).getClass(), XmlMessageValidationContext.class);

        XmlMessageValidationContext validationContext = (XmlMessageValidationContext) action.getValidationContexts().get(0);

        Assert.assertTrue(validationContext.getMessageBuilder() instanceof StaticMessageContentBuilder);
        Assert.assertEquals(((StaticMessageContentBuilder<?>)validationContext.getMessageBuilder()).getMessage().getPayload(), "Foo");
        Assert.assertTrue(((StaticMessageContentBuilder<?>)validationContext.getMessageBuilder()).getMessage().getHeaders().containsKey("operation"));

        Assert.assertNull(action.getAttachmentResourcePath());
        Assert.assertEquals(action.getAttachmentData(), testAttachment.getContent());
        Assert.assertEquals(action.getControlAttachment().getContentId(), testAttachment.getContentId());
        Assert.assertEquals(action.getControlAttachment().getContentType(), testAttachment.getContentType());
        Assert.assertEquals(action.getControlAttachment().getCharsetName(), testAttachment.getCharsetName());
    }
    
    @Test
    public void testSoapAttachment() {
        MockBuilder builder = new MockBuilder(applicationContext) {
            @Override
            public void configure() {
                receive(messageEndpoint)
                    .soap()
                    .message(MessageBuilder.withPayload("Foo").setHeader("operation", "foo").build())
                    .attachment(testAttachment);
            }
        };
        
        builder.execute();
        
        Assert.assertEquals(builder.testCase().getActions().size(), 1);
        Assert.assertEquals(builder.testCase().getActions().get(0).getClass(), ReceiveSoapMessageAction.class);
        
        ReceiveSoapMessageAction action = ((ReceiveSoapMessageAction)builder.testCase().getActions().get(0));
        Assert.assertEquals(action.getName(), "receive");
        
        Assert.assertEquals(action.getMessageType(), MessageType.XML.name());
        Assert.assertEquals(action.getEndpoint(), messageEndpoint);
        Assert.assertEquals(action.getValidationContexts().size(), 1);
        Assert.assertEquals(action.getValidationContexts().get(0).getClass(), XmlMessageValidationContext.class);
        
        XmlMessageValidationContext validationContext = (XmlMessageValidationContext) action.getValidationContexts().get(0);
        
        Assert.assertTrue(validationContext.getMessageBuilder() instanceof StaticMessageContentBuilder);
        Assert.assertEquals(((StaticMessageContentBuilder<?>)validationContext.getMessageBuilder()).getMessage().getPayload(), "Foo");
        Assert.assertTrue(((StaticMessageContentBuilder<?>)validationContext.getMessageBuilder()).getMessage().getHeaders().containsKey("operation"));
        
        Assert.assertNull(action.getAttachmentResourcePath());
        Assert.assertEquals(action.getAttachmentData(), testAttachment.getContent());
        Assert.assertEquals(action.getControlAttachment().getContentId(), testAttachment.getContentId());
        Assert.assertEquals(action.getControlAttachment().getContentType(), testAttachment.getContentType());
        Assert.assertEquals(action.getControlAttachment().getCharsetName(), testAttachment.getCharsetName());
    }
    
    @Test
    public void testSoapAttachmentData() {
        MockBuilder builder = new MockBuilder(applicationContext) {
            @Override
            public void configure() {
                receive(messageEndpoint)
                    .payload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                    .soap()
                    .attachment(testAttachment.getContentId(), testAttachment.getContentType(), testAttachment.getContent());
            }
        };
        
        builder.execute();
        
        Assert.assertEquals(builder.testCase().getActions().size(), 1);
        Assert.assertEquals(builder.testCase().getActions().get(0).getClass(), ReceiveSoapMessageAction.class);
        
        ReceiveSoapMessageAction action = ((ReceiveSoapMessageAction)builder.testCase().getActions().get(0));
        Assert.assertEquals(action.getName(), "receive");
        
        Assert.assertEquals(action.getMessageType(), MessageType.XML.name());
        Assert.assertEquals(action.getEndpoint(), messageEndpoint);
        Assert.assertEquals(action.getValidationContexts().size(), 1);
        Assert.assertEquals(action.getValidationContexts().get(0).getClass(), XmlMessageValidationContext.class);
        
        XmlMessageValidationContext validationContext = (XmlMessageValidationContext) action.getValidationContexts().get(0);
        
        Assert.assertTrue(validationContext.getMessageBuilder() instanceof PayloadTemplateMessageBuilder);
        Assert.assertEquals(((PayloadTemplateMessageBuilder)validationContext.getMessageBuilder()).getPayloadData(), "<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        Assert.assertNull(action.getAttachmentResourcePath());
        Assert.assertEquals(action.getAttachmentData(), testAttachment.getContent());
        Assert.assertEquals(action.getControlAttachment().getContentId(), testAttachment.getContentId());
        Assert.assertEquals(action.getControlAttachment().getContentType(), testAttachment.getContentType());
        Assert.assertEquals(action.getControlAttachment().getCharsetName(), testAttachment.getCharsetName());
    }
    
    @Test
    public void testSoapAttachmentResource() throws IOException {
        final Resource attachmentResource = EasyMock.createMock(Resource.class);
        
        MockBuilder builder = new MockBuilder(applicationContext) {
            @Override
            public void configure() {
                receive(messageEndpoint)
                    .payload(resource)
                    .soap()
                    .attachment(testAttachment.getContentId(), testAttachment.getContentType(), attachmentResource);
            }
        };
        
        reset(resource, attachmentResource);
        expect(resource.getInputStream()).andReturn(new ByteArrayInputStream("somePayloadData".getBytes())).once();
        expect(attachmentResource.getInputStream()).andReturn(new ByteArrayInputStream("someAttachmentData".getBytes())).once();
        replay(resource, attachmentResource);
        
        builder.execute();
        
        Assert.assertEquals(builder.testCase().getActions().size(), 1);
        Assert.assertEquals(builder.testCase().getActions().get(0).getClass(), ReceiveSoapMessageAction.class);
        
        ReceiveSoapMessageAction action = ((ReceiveSoapMessageAction)builder.testCase().getActions().get(0));
        Assert.assertEquals(action.getName(), "receive");
        
        Assert.assertEquals(action.getMessageType(), MessageType.XML.name());
        Assert.assertEquals(action.getEndpoint(), messageEndpoint);
        Assert.assertEquals(action.getValidationContexts().size(), 1);
        Assert.assertEquals(action.getValidationContexts().get(0).getClass(), XmlMessageValidationContext.class);
        
        XmlMessageValidationContext validationContext = (XmlMessageValidationContext) action.getValidationContexts().get(0);
        
        Assert.assertTrue(validationContext.getMessageBuilder() instanceof PayloadTemplateMessageBuilder);
        Assert.assertEquals(((PayloadTemplateMessageBuilder)validationContext.getMessageBuilder()).getPayloadData(), "somePayloadData");
        
        Assert.assertEquals(action.getAttachmentData(), "someAttachmentData");
        Assert.assertEquals(action.getControlAttachment().getContentId(), testAttachment.getContentId());
        Assert.assertEquals(action.getControlAttachment().getContentType(), testAttachment.getContentType());
        Assert.assertEquals(action.getControlAttachment().getCharsetName(), testAttachment.getCharsetName());
        
        verify(resource, attachmentResource);
    }
    
    @Test
    public void testReceiveBuilderWithEndpointName() {
        reset(applicationContextMock);
        expect(applicationContextMock.getBean(TestListeners.class)).andReturn(new TestListeners()).once();
        expect(applicationContextMock.getBean(TestActionListeners.class)).andReturn(new TestActionListeners()).once();
        expect(applicationContextMock.getBeansOfType(SequenceBeforeTest.class)).andReturn(new HashMap<String, SequenceBeforeTest>()).once();
        replay(applicationContextMock);

        MockBuilder builder = new MockBuilder(applicationContextMock) {
            @Override
            public void configure() {
                receive("replyMessageEndpoint")
                    .payload("<TestRequest><Message>Hello World!</Message></TestRequest>");

                receive("fooMessageEndpoint")
                    .payload("<TestRequest><Message>Hello World!</Message></TestRequest>");
            }
        };

        builder.execute();
        
        Assert.assertEquals(builder.testCase().getActions().size(), 2);
        Assert.assertEquals(builder.testCase().getActions().get(0).getClass(), ReceiveMessageAction.class);
        Assert.assertEquals(builder.testCase().getActions().get(1).getClass(), ReceiveMessageAction.class);
        
        ReceiveMessageAction action = ((ReceiveMessageAction)builder.testCase().getActions().get(0));
        Assert.assertEquals(action.getName(), "receive");
        Assert.assertEquals(action.getEndpointUri(), "replyMessageEndpoint");
        Assert.assertEquals(action.getMessageType(), MessageType.XML.name());
        
        action = ((ReceiveMessageAction)builder.testCase().getActions().get(1));
        Assert.assertEquals(action.getName(), "receive");
        Assert.assertEquals(action.getEndpointUri(), "fooMessageEndpoint");
        Assert.assertEquals(action.getMessageType(), MessageType.XML.name());
        
        verify(applicationContextMock);
    }

    @Test(expectedExceptions = CitrusRuntimeException.class,
            expectedExceptionsMessageRegExp = "Invalid use of http and soap action definition")
    public void testReceiveBuilderWithSoapAndHttpMixed() {
        reset(applicationContextMock);

        expect(applicationContextMock.getBean(TestListeners.class)).andReturn(new TestListeners()).once();
        expect(applicationContextMock.getBean(TestActionListeners.class)).andReturn(new TestActionListeners()).once();
        expect(applicationContextMock.getBeansOfType(SequenceBeforeTest.class)).andReturn(new HashMap<String, SequenceBeforeTest>()).once();

        replay(applicationContextMock);

        MockBuilder builder = new MockBuilder(applicationContextMock) {
            @Override
            public void configure() {
                receive(messageEndpoint)
                        .soap()
                        .payload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                        .header("operation", "soapOperation")
                        .http();
            }
        };

        builder.execute();
    }
    
}
