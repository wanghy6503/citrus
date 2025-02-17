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

import com.consol.citrus.CitrusConstants;
import com.consol.citrus.actions.ReceiveMessageAction;
import com.consol.citrus.dsl.util.PositionHandle;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.util.FileUtils;
import com.consol.citrus.validation.ControlMessageValidationContext;
import com.consol.citrus.validation.MessageValidator;
import com.consol.citrus.validation.builder.*;
import com.consol.citrus.validation.callback.ValidationCallback;
import com.consol.citrus.validation.context.ValidationContext;
import com.consol.citrus.validation.script.ScriptValidationContext;
import com.consol.citrus.validation.xml.XmlMessageValidationContext;
import com.consol.citrus.variable.MessageHeaderVariableExtractor;
import com.consol.citrus.variable.XpathPayloadVariableExtractor;
import com.consol.citrus.ws.actions.ReceiveSoapMessageAction;
import com.consol.citrus.xml.namespace.NamespaceContextBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Map;

/**
 * Receive message action definition offers configuration methods for a receive test action. Build options 
 * include construction of control message payload and headers as well as value extraction.
 * 
 * @author Christoph Deppisch
 */
public class ReceiveMessageActionDefinition<A extends ReceiveMessageAction, T extends ReceiveMessageActionDefinition> extends AbstractActionDefinition<A> {

    /** Self reference for generics support */
    private final T self;

    /** Message type for this action definition */
    private MessageType messageType = MessageType.valueOf(CitrusConstants.DEFAULT_MESSAGE_TYPE);

    /** Validation context used in this action definition */
    private ControlMessageValidationContext validationContext;
    
    /** Script validation context used in this action definition */
    private ScriptValidationContext scriptValidationContext;
    
    /** Variable extractors filled within this action definition */
    private MessageHeaderVariableExtractor headerExtractor;
    private XpathPayloadVariableExtractor xpathExtractor;
    
    /** Basic application context */
    private ApplicationContext applicationContext;
    
    /** Handle for test action position in test case sequence use when switching to SOAP specific definition */
    private PositionHandle positionHandle;
    
    /**
     * Default constructor using test action, basic application context and position handle.
     * @param action
     * @param ctx
     * @param positionHandle
     */
    public ReceiveMessageActionDefinition(A action, ApplicationContext ctx, PositionHandle positionHandle) {
        super(action);
        this.applicationContext = ctx;
        this.positionHandle = positionHandle;

        this.self = (T) this;
    }
    
    /**
     * Adds a custom timeout to this message receiving action. 
     * @param receiveTimeout
     * @return
     */
    public T timeout(long receiveTimeout) {
        action.setReceiveTimeout(receiveTimeout);
        return self;
    }
    
    /**
     * Expect a control message in this receive action.
     * @param controlMessage
     * @return
     */
    public T message(Message<?> controlMessage) {
        if (validationContext != null) {
            throw new CitrusRuntimeException("Unable to set control message object when header and/or payload was set before");
        }
        initializeValidationContext();
        
        validationContext.setControlMessage(controlMessage);
        
        return self;
    }
    
    /**
     * Expect this message payload data in received message.
     * @param payload
     * @return
     */
    public T payload(String payload) {
        getPayloadTemplateMessageBuilder().setPayloadData(payload);
        return self;
    }
    
    /**
     * Expect this message payload data in received message.
     * @param payloadResource
     * @return
     */
    public T payload(Resource payloadResource) {
        try {
            getPayloadTemplateMessageBuilder().setPayloadData(FileUtils.readToString(payloadResource));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read payload resource", e);
        }

        return self;
    }
    
    /**
     * Expect this message header entry in received message.
     * @param name
     * @param value
     * @return
     */
    public T header(String name, Object value) {
        getMessageContentBuilder().getMessageHeaders().put(name, value);
        return self;
    }
    
    /**
     * Expect this message header data in received message. Message header data is used in 
     * SOAP messages as XML fragment for instance.
     * @param data
     * @return
     */
    public T header(String data) {
        getMessageContentBuilder().setMessageHeaderData(data);
        return self;
    }

    /**
     * Expect this message header data in received message from file resource. Message header data is used in 
     * SOAP messages as XML fragment for instance.
     * @param resource
     * @return
     */
    public T header(Resource resource) {
        try {
            getMessageContentBuilder().setMessageHeaderData(FileUtils.readToString(resource));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read header resource", e);
        }

        return self;
    }
    
    /**
     * Adds script validation.
     * @param validationScript
     * @return
     */
    public T validateScript(String validationScript) {
        initializeScriptValidationContext();
        scriptValidationContext.setValidationScript(validationScript);

        return self;
    }
    
    /**
     * Adds script validation by file resource.
     * @param scriptResource
     * @return
     */
    public T validateScript(Resource scriptResource) {
        initializeScriptValidationContext();
        
        try {
            scriptValidationContext.setValidationScriptResourcePath(scriptResource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read script resource file", e);
        }

        return self;
    }
    
    /**
     * Adds custom validation script type.
     * @param type
     * @return
     */
    public T validateScriptType(String type) {
        initializeScriptValidationContext();
        scriptValidationContext.setScriptType(type);

        return self;
    }
    
    /**
     * Sets a explicit message type for this receive action.
     * @param messageType
     * @return
     */
    public T messageType(MessageType messageType) {
        this.messageType = messageType;
        action.setMessageType(messageType.toString());
        return self;
    }
    
    /**
     * Sets schema validation enabled/disabled for this message.
     * @param enabled
     * @return
     */
    public T schemaValidation(boolean enabled) {
        getXmlValidationContext().setSchemaValidation(enabled);
        return self;
    }

    /**
     * Validates XML namespace with prefix and uri.
     * @param prefix
     * @param namespaceUri
     * @return
     */
    public T validateNamespace(String prefix, String namespaceUri) {
        getXmlValidationContext().getControlNamespaces().put(prefix, namespaceUri);
        return self;
    }
    
    /**
     * Adds message element validation.
     * @param path
     * @param controlValue
     * @return
     */
    public T validate(String path, String controlValue) {
        getXmlValidationContext().getPathValidationExpressions().put(path, controlValue);
        return self;
    }
    
    /**
     * Adds ignore path expression for message element.
     * @param path
     * @return
     */
    public T ignore(String path) {
        getXmlValidationContext().getIgnoreExpressions().add(path);
        return self;
    }
    
    /**
     * Adds XPath message element validation.
     * @param xPathExpression
     * @param controlValue
     * @return
     */
    public T xpath(String xPathExpression, String controlValue) {
        validate(xPathExpression, controlValue);
        return self;
    }
    
    /**
     * Sets explicit schema instance name to use for schema validation.
     * @param schemaName
     * @return
     */
    public T xsd(String schemaName) {
        getXmlValidationContext().setSchema(schemaName);
        return self;
    }
    
    /**
     * Sets explicit xsd schema repository instance to use for validation.
     * @param schemaRepository
     * @return
     */
    public T xsdSchemaRepository(String schemaRepository) {
        getXmlValidationContext().setSchemaRepository(schemaRepository);
        return self;
    }
    
    /**
     * Adds explicit namespace declaration for later path validation expressions.
     * @param prefix
     * @param namespaceUri
     * @return
     */
    public T namespace(String prefix, String namespaceUri) {
        initializeXpathVariableExtractor();
        xpathExtractor.getNamespaces().put(prefix, namespaceUri);

        getXmlValidationContext().getNamespaces().put(prefix, namespaceUri);
        return self;
    }
    
    /**
     * Sets default namespace declarations on this action definition.
     * @param namespaceMappings
     * @return
     */
    public T namespaces(Map<String, String> namespaceMappings) {
        initializeXpathVariableExtractor();
        xpathExtractor.getNamespaces().putAll(namespaceMappings);

        getXmlValidationContext().getNamespaces().putAll(namespaceMappings);
        return self;
    }
    
    /**
     * Sets message selector string.
     * @param messageSelector
     * @return
     */
    public T selector(String messageSelector) {
        action.setMessageSelectorString(messageSelector);

        return self;
    }
    
    /**
     * Sets message selector elements.
     * @param messageSelector
     * @return
     */
    public T selector(Map<String, String> messageSelector) {
        action.setMessageSelector(messageSelector);

        return self;
    }
    
    /**
     * Sets explicit message validator for this receive action.
     * @param validator
     * @return
     */
    public T validator(MessageValidator<? extends ValidationContext> validator) {
        action.setValidator(validator);
        return self;
    }
    
    /**
     * Sets explicit message validator by name.
     * @param validatorName
     * @return
     */
    @SuppressWarnings("unchecked")
    public T validator(String validatorName) {
        Assert.notNull(applicationContext, "Citrus application context is not initialized!");
        
        MessageValidator<? extends ValidationContext> validator = applicationContext.getBean(validatorName, MessageValidator.class);
        
        action.setValidator(validator);
        return self;
    }
    
    /**
     * Extract message header entry as variable.
     * @param headerName
     * @param variable
     * @return
     */
    public T extractFromHeader(String headerName, String variable) {
        if (headerExtractor == null) {
            headerExtractor = new MessageHeaderVariableExtractor();
            
            action.getVariableExtractors().add(headerExtractor);
        }
        
        headerExtractor.getHeaderMappings().put(headerName, variable);
        return self;
    }
    
    /**
     * Extract message element via XPath from message payload as new test variable.
     * @param xpath
     * @param variable
     * @return
     */
    public T extractFromPayload(String xpath, String variable) {
        initializeXpathVariableExtractor();
        xpathExtractor.getxPathExpressions().put(xpath, variable);
        return self;
    }
    
    /**
     * Adds validation callback to the receive action for validating 
     * the received message with Java code.
     * @param callback
     * @return
     */
    public T validationCallback(ValidationCallback callback) {
        callback.setApplicationContext(applicationContext);
        action.setValidationCallback(callback);

        return self;
    }
    
    /**
     * Enable SOAP specific properties on this receiving message action.
     * @return
     */
    public ReceiveSoapMessageActionDefinition soap() {
        ReceiveSoapMessageAction receiveSoapMessageAction = new ReceiveSoapMessageAction();
        
        receiveSoapMessageAction.setActor(action.getActor());
        receiveSoapMessageAction.setDescription(action.getDescription());
        receiveSoapMessageAction.setEndpoint(action.getEndpoint());
        receiveSoapMessageAction.setEndpointUri(action.getEndpointUri());
        receiveSoapMessageAction.setMessageSelector(action.getMessageSelector());
        receiveSoapMessageAction.setMessageSelectorString(action.getMessageSelectorString());
        receiveSoapMessageAction.setMessageType(action.getMessageType());
        receiveSoapMessageAction.setReceiveTimeout(action.getReceiveTimeout());
        receiveSoapMessageAction.setValidationCallback(action.getValidationCallback());
        receiveSoapMessageAction.setValidationContexts(action.getValidationContexts());
        receiveSoapMessageAction.setValidator(action.getValidator());
        receiveSoapMessageAction.setVariableExtractors(action.getVariableExtractors());
        
        positionHandle.switchTestAction(receiveSoapMessageAction);
        
        ReceiveSoapMessageActionDefinition soapMessageActionDefinition = new ReceiveSoapMessageActionDefinition(receiveSoapMessageAction, applicationContext);
        soapMessageActionDefinition.setMessageType(messageType);
        soapMessageActionDefinition.setValidationContext(validationContext);
        soapMessageActionDefinition.setScriptValidationContext(scriptValidationContext);
        soapMessageActionDefinition.setHeaderExtractor(headerExtractor);
        soapMessageActionDefinition.setXpathExtractor(xpathExtractor);

        return soapMessageActionDefinition;
    }

    /**
     * Enable HTTP specific properties on this receiving message action.
     * @return
     */
    public ReceiveHttpMessageActionDefinition http() {
        ReceiveHttpMessageActionDefinition httpMessageActionDefinition = new ReceiveHttpMessageActionDefinition(action, applicationContext, positionHandle);
        httpMessageActionDefinition.setMessageType(messageType);
        httpMessageActionDefinition.setValidationContext(validationContext);
        httpMessageActionDefinition.setScriptValidationContext(scriptValidationContext);
        httpMessageActionDefinition.setHeaderExtractor(headerExtractor);
        httpMessageActionDefinition.setXpathExtractor(xpathExtractor);

        return httpMessageActionDefinition;
    }

    /**
     * Gets the message builder on the validation context. Constructs message content builder if necessary.
     * @return
     */
    protected AbstractMessageContentBuilder<?> getMessageContentBuilder() {
        if (validationContext == null) {
            initializeValidationContext();
        }

        if (validationContext.getMessageBuilder() instanceof AbstractMessageContentBuilder) {
            return (AbstractMessageContentBuilder<?>) validationContext.getMessageBuilder();
        } else {
            PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
            validationContext.setMessageBuilder(messageBuilder);
            return messageBuilder;
        }
    }

    /**
     * Forces a payload template message builder.
     * @return
     */
    protected PayloadTemplateMessageBuilder getPayloadTemplateMessageBuilder() {
        MessageContentBuilder messageContentBuilder = getMessageContentBuilder();

        if (messageContentBuilder instanceof PayloadTemplateMessageBuilder) {
            return (PayloadTemplateMessageBuilder) messageContentBuilder;
        } else {
            PayloadTemplateMessageBuilder messageBuilder = new PayloadTemplateMessageBuilder();
            validationContext.setMessageBuilder(messageBuilder);
            return messageBuilder;
        }
    }

    /**
     * Creates new validation context according to message type.
     */
    private void initializeValidationContext() {
        if (validationContext == null) {
            if (messageType.equals(MessageType.XML)) {
                validationContext = new XmlMessageValidationContext();
            } else {
                validationContext = new ControlMessageValidationContext(messageType.toString());
            }

            action.getValidationContexts().add(validationContext);
        }
    }

    /**
     * Creates new script validation context.
     */
    private void initializeScriptValidationContext() {
        if (scriptValidationContext == null) {
            scriptValidationContext = new ScriptValidationContext(messageType.toString());

            action.getValidationContexts().add(scriptValidationContext);
        }
    }

    /**
     * Creates new variable extractor and adds it to test action.
     */
    private void initializeXpathVariableExtractor() {
        if (xpathExtractor == null) {
            xpathExtractor = new XpathPayloadVariableExtractor();

            if (applicationContext.getBeansOfType(NamespaceContextBuilder.class).size() > 0) {
                xpathExtractor.setNamespaceContextBuilder(applicationContext.getBean(NamespaceContextBuilder.class));
            }

            action.getVariableExtractors().add(xpathExtractor);
        }
    }

    /**
     * Gets the validation context as XML validation context an raises exception if existing validation context is
     * not a XML validation context.
     * @return
     */
    private XmlMessageValidationContext getXmlValidationContext() {
        initializeValidationContext();

        if (validationContext instanceof XmlMessageValidationContext) {
            return ((XmlMessageValidationContext)validationContext);
        } else {
            throw new CitrusRuntimeException("Unable to set XML property on validation context type " + validationContext);
        }
    }

    /**
     * Sets the message type.
     * @param messageType
     */
    protected void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Sets the xpath extractor.
     * @param xpathExtractor
     */
    protected void setXpathExtractor(XpathPayloadVariableExtractor xpathExtractor) {
        this.xpathExtractor = xpathExtractor;
    }

    /**
     * Sets the header extractor.
     * @param headerExtractor
     */
    protected void setHeaderExtractor(MessageHeaderVariableExtractor headerExtractor) {
        this.headerExtractor = headerExtractor;
    }

    /**
     * Sets the script message validator.
     * @param scriptValidationContext
     */
    protected void setScriptValidationContext(ScriptValidationContext scriptValidationContext) {
        this.scriptValidationContext = scriptValidationContext;
    }

    /**
     * Sets the validation context.
     * @param validationContext
     */
    protected void setValidationContext(ControlMessageValidationContext validationContext) {
        this.validationContext = validationContext;
    }

}
