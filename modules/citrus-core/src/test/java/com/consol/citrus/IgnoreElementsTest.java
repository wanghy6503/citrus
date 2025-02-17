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

package com.consol.citrus;

import com.consol.citrus.actions.ReceiveMessageAction;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.endpoint.EndpointConfiguration;
import com.consol.citrus.messaging.Consumer;
import com.consol.citrus.testng.AbstractTestNGUnitTest;
import com.consol.citrus.validation.builder.PayloadTemplateMessageBuilder;
import com.consol.citrus.validation.context.ValidationContext;
import com.consol.citrus.validation.xml.DomXmlMessageValidator;
import com.consol.citrus.validation.xml.XmlMessageValidationContext;
import org.easymock.EasyMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.easymock.EasyMock.*;

/**
 * @author Christoph Deppisch
 */
public class IgnoreElementsTest extends AbstractTestNGUnitTest {
    @Autowired
    private DomXmlMessageValidator validator;

    private Endpoint endpoint = EasyMock.createMock(Endpoint.class);
    private Consumer consumer = EasyMock.createMock(Consumer.class);
    private EndpointConfiguration endpointConfiguration = EasyMock.createMock(EndpointConfiguration.class);
    
    private ReceiveMessageAction receiveMessageBean;
    
    @Override
    @BeforeMethod
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void prepareTest() {
        super.prepareTest();

        reset(endpoint, consumer, endpointConfiguration);
        expect(endpoint.createConsumer()).andReturn(consumer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpointConfiguration.getTimeout()).andReturn(5000L).anyTimes();
        
        Message message = MessageBuilder.withPayload("<root>"
                        + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                            + "<sub-elementA attribute='A'>text-value</sub-elementA>"
                            + "<sub-elementB attribute='B'>text-value</sub-elementB>"
                            + "<sub-elementC attribute='C'>text-value</sub-elementC>"
                        + "</element>" 
                        + "</root>").build();

        expect(consumer.receive(anyLong())).andReturn(message);
        expect(endpoint.getActor()).andReturn(null).anyTimes();
        replay(endpoint, consumer, endpointConfiguration);
        
        receiveMessageBean = new ReceiveMessageAction();
        receiveMessageBean.setEndpoint(endpoint);

        receiveMessageBean.setValidator(validator);
    }

    @Test
    public void testIgnoreElements() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-elementA attribute='A'>no validation</sub-elementA>"
                + "<sub-elementB attribute='B'>no validation</sub-elementB>"
                + "<sub-elementC attribute='C'>text-value</sub-elementC>"
            + "</element>" 
            + "</root>");
        
        Set<String> ignoreMessageElements = new HashSet<String>();
        ignoreMessageElements.add("//root/element/sub-elementA");
        ignoreMessageElements.add("//sub-elementB");
        validationContext.setIgnoreExpressions(ignoreMessageElements);
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    public void testIgnoreAttributes() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-elementA attribute='no validation'>text-value</sub-elementA>"
                + "<sub-elementB attribute='no validation'>text-value</sub-elementB>"
                + "<sub-elementC attribute='C'>text-value</sub-elementC>"
            + "</element>" 
            + "</root>");
        
        Set<String> ignoreMessageElements = new HashSet<String>();
        ignoreMessageElements.add("//root/element/sub-elementA/@attribute");
        ignoreMessageElements.add("//sub-elementB/@attribute");
        validationContext.setIgnoreExpressions(ignoreMessageElements);
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    public void testIgnoreAttributesAll() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-elementA attribute='no validation'>text-value</sub-elementA>"
                + "<sub-elementB attribute='B'>text-value</sub-elementB>" //TODO fix this
                + "<sub-elementC attribute='C'>text-value</sub-elementC>"
            + "</element>" 
            + "</root>");
        
        Set<String> ignoreMessageElements = new HashSet<String>();
        ignoreMessageElements.add("//@attribute");
        validationContext.setIgnoreExpressions(ignoreMessageElements);
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testIgnoreAttributesUsingArrays() {
        reset(endpoint, consumer, endpointConfiguration);
        expect(endpoint.createConsumer()).andReturn(consumer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpointConfiguration.getTimeout()).andReturn(5000L).anyTimes();
        
        Message message = MessageBuilder.withPayload("<root>"
                        + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                            + "<sub-element attribute='A'>text-value</sub-element>"
                            + "<sub-element attribute='B'>text-value</sub-element>"
                            + "<sub-element attribute='C'>text-value</sub-element>"
                        + "</element>" 
                        + "</root>").build();

        expect(consumer.receive(anyLong())).andReturn(message);
        expect(endpoint.getActor()).andReturn(null).anyTimes();
        replay(endpoint, consumer, endpointConfiguration);
        
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-element attribute='no validation'>text-value</sub-element>"
                + "<sub-element attribute='no validation'>text-value</sub-element>"
                + "<sub-element attribute='C'>text-value</sub-element>"
            + "</element>" 
            + "</root>");
        
        Set<String> ignoreMessageElements = new HashSet<String>();
        ignoreMessageElements.add("//sub-element[1]/@attribute");
        ignoreMessageElements.add("//sub-element[2]/@attribute");
        validationContext.setIgnoreExpressions(ignoreMessageElements);
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testIgnoreRootElement() {
        reset(endpoint, consumer, endpointConfiguration);
        expect(endpoint.createConsumer()).andReturn(consumer).anyTimes();
        expect(endpoint.getEndpointConfiguration()).andReturn(endpointConfiguration).anyTimes();
        expect(endpointConfiguration.getTimeout()).andReturn(5000L).anyTimes();
        
        Message message = MessageBuilder.withPayload("<root>"
                        + "<element>Text</element>" 
                        + "</root>").build();

        expect(consumer.receive(anyLong())).andReturn(message);
        expect(endpoint.getActor()).andReturn(null).anyTimes();
        replay(endpoint, consumer, endpointConfiguration);
        
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                        + "<element additonal-attribute='some'>Wrong text</element>" 
                        + "</root>");
        
        Set<String> ignoreMessageElements = new HashSet<String>();
        ignoreMessageElements.add("//root");
        validationContext.setIgnoreExpressions(ignoreMessageElements);
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    public void testIgnoreElementsAndValidate() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-elementA attribute='A'>no validation</sub-elementA>"
                + "<sub-elementB attribute='B'>no validation</sub-elementB>"
                + "<sub-elementC attribute='C'>text-value</sub-elementC>"
            + "</element>" 
            + "</root>");
        
        Set<String> ignoreMessageElements = new HashSet<String>();
        ignoreMessageElements.add("//root/element/sub-elementA");
        ignoreMessageElements.add("//sub-elementB");
        validationContext.setIgnoreExpressions(ignoreMessageElements);
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        Map<String, String> validateElements = new HashMap<String, String>();
        validateElements.put("//root/element/sub-elementA", "wrong value");
        validateElements.put("//sub-elementB", "wrong value");
        validationContext.setPathValidationExpressions(validateElements);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    public void testIgnoreElementsByPlaceholder() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-elementA attribute='A'>@ignore@</sub-elementA>"
                + "<sub-elementB attribute='B'> @ignore@ </sub-elementB>"
                + "<sub-elementC attribute='C'>text-value</sub-elementC>"
            + "</element>" 
            + "</root>");
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    public void testIgnoreSubElementsByPlaceholder() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >@ignore@</element>" 
            + "</root>");
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
    
    @Test
    public void testIgnoreAttributesByPlaceholder() {
        PayloadTemplateMessageBuilder controlMessageBuilder = new PayloadTemplateMessageBuilder();
        XmlMessageValidationContext validationContext = new XmlMessageValidationContext();
        validationContext.setMessageBuilder(controlMessageBuilder);
        controlMessageBuilder.setPayloadData("<root>"
                + "<element attributeA='attribute-value' attributeB='attribute-value' >"
                + "<sub-elementA attribute='@ignore@'>text-value</sub-elementA>"
                + "<sub-elementB attribute=' @ignore@ '>text-value</sub-elementB>"
                + "<sub-elementC attribute='C'>text-value</sub-elementC>"
            + "</element>" 
            + "</root>");
        
        List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
        validationContexts.add(validationContext);
        receiveMessageBean.setValidationContexts(validationContexts);
        
        receiveMessageBean.execute(context);
    }
}
