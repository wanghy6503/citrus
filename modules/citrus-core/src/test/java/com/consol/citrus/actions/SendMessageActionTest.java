/*
 * Copyright 2006-2010 the original author or authors.
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

package com.consol.citrus.actions;

import com.consol.citrus.TestActor;
import com.consol.citrus.TestCase;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.endpoint.EndpointConfiguration;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.messaging.Producer;
import com.consol.citrus.testng.AbstractTestNGUnitTest;
import com.consol.citrus.validation.builder.PayloadTemplateMessageBuilder;
import com.consol.citrus.validation.interceptor.XpathMessageConstructionInterceptor;
import com.consol.citrus.validation.script.GroovyScriptMessageBuilder;
import com.consol.citrus.validation.xml.DomXmlMessageValidator;
import com.consol.citrus.validation.xml.XmlMessageValidationContext;
import com.consol.citrus.variable.MessageHeaderVariableExtractor;
import com.consol.citrus.variable.VariableExtractor;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 */
public class SendMessageActionTest extends AbstractTestNGUnitTest {

    private Endpoint endpoint = EasyMock.createMock(Endpoint.class);
    private Producer producer = EasyMock.createMock(Producer.class);
    private EndpointConfiguration endpointConfiguration = EasyMock.createMock(EndpointConfiguration.class);
    
    @Test
    @SuppressWarnings("rawtypes")
	public void testSendMessageWithMessagePayloadData() {
		SendMessageAction sendAction = new SendMessageAction();
		sendAction.setEndpoint(endpoint);
		
		TestActor testActor = new TestActor();
        testActor.setName("TESTACTOR");
        
        sendAction.setActor(testActor);
        
		PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
		messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
		
		sendAction.setMessageBuilder(messageBuilder);
		
		Map<String, Object> headers = new HashMap<String, Object>();
		final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
		                        .copyHeaders(headers)
		                        .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

		producer.send((Message)anyObject());
		expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
		
		sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
	}
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessagePayloadResource() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);
        
        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadResourcePath("classpath:com/consol/citrus/actions/test-request-payload.xml");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
	public void testSendMessageWithMessageBuilderScriptData() {
		SendMessageAction sendAction = new SendMessageAction();
		sendAction.setEndpoint(endpoint);
		StringBuilder sb = new StringBuilder();
		sb.append("markupBuilder.TestRequest(){\n");
		sb.append("Message('Hello World!')\n");
		sb.append("}");
		
		GroovyScriptMessageBuilder scriptMessageBuidler = new GroovyScriptMessageBuilder();
		scriptMessageBuidler.setScriptData(sb.toString());
		
		sendAction.setMessageBuilder(scriptMessageBuidler);
		
		Map<String, Object> headers = new HashMap<String, Object>();
		final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
		                        .copyHeaders(headers)
		                        .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
		expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
		
		sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
	}
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessageBuilderScriptDataVariableSupport() {
        context.setVariable("text", "Hello World!");
        
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);
        StringBuilder sb = new StringBuilder();
        sb.append("markupBuilder.TestRequest(){\n");
        sb.append("Message('${text}')\n");
        sb.append("}");
        
        GroovyScriptMessageBuilder scriptMessageBuidler = new GroovyScriptMessageBuilder();
        scriptMessageBuidler.setScriptData(sb.toString());
        
        sendAction.setMessageBuilder(scriptMessageBuidler);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessageBuilderScriptResource() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);
        
        GroovyScriptMessageBuilder scriptMessageBuidler = new GroovyScriptMessageBuilder();
        scriptMessageBuidler.setScriptResourcePath("classpath:com/consol/citrus/actions/test-request-payload.groovy");
        
        sendAction.setMessageBuilder(scriptMessageBuidler);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessagePayloadDataVariablesSupport() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);
        
        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>${myText}</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        context.setVariable("myText", "Hello World!");
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessagePayloadResourceVariablesSupport() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadResourcePath("classpath:com/consol/citrus/actions/test-request-payload-with-variables.xml");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        context.setVariable("myText", "Hello World!");
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessagePayloadResourceFunctionsSupport() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadResourcePath("classpath:com/consol/citrus/actions/test-request-payload-with-functions.xml");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageOverwriteMessageElementsXPath() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>?</Message></TestRequest>");
        
        Map<String, String> overwriteElements = new HashMap<String, String>();
        overwriteElements.put("/TestRequest/Message", "Hello World!");
        
        XpathMessageConstructionInterceptor interceptor = new XpathMessageConstructionInterceptor(overwriteElements);
        messageBuilder.add(interceptor);
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageOverwriteMessageElementsDotNotation() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>?</Message></TestRequest>");
        
        Map<String, String> overwriteElements = new HashMap<String, String>();
        overwriteElements.put("TestRequest.Message", "Hello World!");
        
        XpathMessageConstructionInterceptor interceptor = new XpathMessageConstructionInterceptor(overwriteElements);
        messageBuilder.add(interceptor);
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageOverwriteMessageElementsXPathWithNamespace() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<ns0:TestRequest xmlns:ns0=\"http://citrusframework.org/unittest\">" +
                "<ns0:Message>?</ns0:Message></ns0:TestRequest>");
        
        Map<String, String> overwriteElements = new HashMap<String, String>();
        overwriteElements.put("/ns0:TestRequest/ns0:Message", "Hello World!");

        XpathMessageConstructionInterceptor interceptor = new XpathMessageConstructionInterceptor(overwriteElements);
        messageBuilder.add(interceptor);
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<ns0:TestRequest xmlns:ns0=\"http://citrusframework.org/unittest\"><ns0:Message>Hello World!</ns0:Message></ns0:TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                validationContext.setSchemaValidation(false);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageOverwriteMessageElementsXPathWithDefaultNamespace() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest xmlns=\"http://citrusframework.org/unittest\">" +
                "<Message>?</Message></TestRequest>");
        
        Map<String, String> overwriteElements = new HashMap<String, String>();
        overwriteElements.put("/:TestRequest/:Message", "Hello World!");

        XpathMessageConstructionInterceptor interceptor = new XpathMessageConstructionInterceptor(overwriteElements);
        messageBuilder.add(interceptor);
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest xmlns=\"http://citrusframework.org/unittest\"><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                validationContext.setSchemaValidation(false);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessageHeaders() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        final Map<String, Object> controlHeaders = new HashMap<String, Object>();
        controlHeaders.put("Operation", "sayHello");
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(controlHeaders)
                                .build();

        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("Operation", "sayHello");
        messageBuilder.setMessageHeaders(headers);
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithHeaderValuesVariableSupport() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        context.setVariable("myOperation", "sayHello");
        
        final Map<String, Object> controlHeaders = new HashMap<String, Object>();
        controlHeaders.put("Operation", "sayHello");
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(controlHeaders)
                                .build();

        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("Operation", "${myOperation}");
        messageBuilder.setMessageHeaders(headers);
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    public void testSendMessageWithUnknwonVariableInMessagePayload() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>${myText}</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        try {
            sendAction.execute(context);
        } catch(CitrusRuntimeException e) {
            Assert.assertEquals(e.getMessage(), "Unknown variable 'myText'");
            return;
        }
        
        Assert.fail("Missing " + CitrusRuntimeException.class + " with unknown variable error message");
    }
    
    @Test
    public void testSendMessageWithUnknwonVariableInHeaders() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("Operation", "${myOperation}");
        messageBuilder.setMessageHeaders(headers);
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        try {
            sendAction.execute(context);
        } catch(CitrusRuntimeException e) {
            Assert.assertEquals(e.getMessage(), "Unknown variable 'myOperation'");
            return;
        }
        
        Assert.fail("Missing " + CitrusRuntimeException.class + " with unknown variable error message");
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithExtractHeaderValues() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        final Map<String, Object> controlHeaders = new HashMap<String, Object>();
        controlHeaders.put("Operation", "sayHello");
        final Message controlMessage = MessageBuilder.withPayload("<TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(controlHeaders)
                                .build();

        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("Operation", "sayHello");
        messageBuilder.setMessageHeaders(headers);
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, String> extractVars = new HashMap<String, String>();
        extractVars.put("Operation", "myOperation");
        extractVars.put(MessageHeaders.ID, "correlationId");
        
        List<VariableExtractor> variableExtractors = new ArrayList<VariableExtractor>();
        MessageHeaderVariableExtractor variableExtractor = new MessageHeaderVariableExtractor();
        variableExtractor.setHeaderMappings(extractVars);
        
        variableExtractors.add(variableExtractor);
        sendAction.setVariableExtractors(variableExtractors);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);
        
        Assert.assertNotNull(context.getVariable("myOperation"));
        Assert.assertNotNull(context.getVariable("correlationId"));

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testMissingMessagePayload() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(MessageBuilder.withPayload("").build());
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithXmlDeclaration() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<?xml version=\"1.0\" encoding=\"UTF-8\"?><TestRequest><Message>Hello World!</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<?xml version=\"1.0\" encoding=\"UTF-8\"?><TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithUTF16Encoding() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<?xml version=\"1.0\" encoding=\"UTF-16\"?><TestRequest><Message>Hello World!</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<?xml version=\"1.0\" encoding=\"UTF-16\"?><TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithISOEncoding() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><TestRequest><Message>Hello World!</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithUnsupportedEncoding() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<?xml version=\"1.0\" encoding=\"MyUnsupportedEncoding\"?><TestRequest><Message>Hello World!</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        try {
            sendAction.execute(context);
        } catch (CitrusRuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof UnsupportedEncodingException);
        }

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    @SuppressWarnings("rawtypes")
    public void testSendMessageWithMessagePayloadResourceISOEncoding() {
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);

        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadResourcePath("classpath:com/consol/citrus/actions/test-request-iso-encoding.xml");
        
        sendAction.setMessageBuilder(messageBuilder);
        
        Map<String, Object> headers = new HashMap<String, Object>();
        final Message controlMessage = MessageBuilder.withPayload("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><TestRequest><Message>Hello World!</Message></TestRequest>")
                                .copyHeaders(headers)
                                .build();

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();

        producer.send((Message)anyObject());
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                DomXmlMessageValidator validator = new DomXmlMessageValidator();
                XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
                validationContext.setControlMessage(controlMessage);
                
                validator.validateMessage(((Message)EasyMock.getCurrentArguments()[0]), context, validationContext);
                return null;
            }
        }).once();

        expect(endpoint.getActor()).andReturn(null).anyTimes();

        replay(endpoint, producer, endpointConfiguration);
        
        sendAction.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    public void testDisabledSendMessage() {
        TestCase testCase = new TestCase();
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);
        
        TestActor disabledActor = new TestActor();
        disabledActor.setDisabled(true);
        sendAction.setActor(disabledActor);
        
        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpoint.getActor()).andReturn(null).anyTimes();
        replay(endpoint, producer, endpointConfiguration);

        testCase.addTestAction(sendAction);
        testCase.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
    @Test
    public void testDisabledSendMessageByEndpointActor() {
        TestCase testCase = new TestCase();
        SendMessageAction sendAction = new SendMessageAction();
        sendAction.setEndpoint(endpoint);
        
        TestActor disabledActor = new TestActor();
        disabledActor.setDisabled(true);
        
        PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
        messageBuilder.setPayloadData("<TestRequest><Message>Hello World!</Message></TestRequest>");
        
        sendAction.setMessageBuilder(messageBuilder);

        reset(endpoint, producer, endpointConfiguration);
        expect(endpoint.createProducer()).andReturn(producer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpoint.getActor()).andReturn(disabledActor).times(2);
        replay(endpoint, producer, endpointConfiguration);

        testCase.addTestAction(sendAction);
        testCase.execute(context);

        verify(endpoint, producer, endpointConfiguration);
    }
    
}
