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

package com.consol.citrus.context;

import com.consol.citrus.endpoint.EndpointFactory;
import com.consol.citrus.functions.FunctionRegistry;
import com.consol.citrus.report.TestListeners;
import com.consol.citrus.validation.MessageValidatorRegistry;
import com.consol.citrus.validation.interceptor.MessageConstructionInterceptors;
import com.consol.citrus.validation.matcher.ValidationMatcherRegistry;
import com.consol.citrus.variable.GlobalVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory bean implementation taking care of {@link FunctionRegistry} and {@link GlobalVariables}.
 * 
 * @author Christoph Deppisch
 */
public class TestContextFactoryBean implements FactoryBean<TestContext>, ApplicationContextAware {
    
    @Autowired
    private FunctionRegistry functionRegistry;
    
    @Autowired
    private ValidationMatcherRegistry validationMatcherRegistry;
    
    @Autowired(required = false)
    private GlobalVariables globalVariables = new GlobalVariables();
    
    @Autowired
    private MessageValidatorRegistry messageValidatorRegistry;

    @Autowired
    private TestListeners testListeners;

    @Autowired
    private EndpointFactory endpointFactory;

    @Autowired
    private MessageConstructionInterceptors messageConstructionInterceptors;

    /** Spring bean application context */
    private ApplicationContext applicationContext;
    
    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(TestContextFactoryBean.class);
    
    /**
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public TestContext getObject() {
        TestContext context = new TestContext();
        context.setFunctionRegistry(functionRegistry);
        context.setValidationMatcherRegistry(validationMatcherRegistry);
        context.setGlobalVariables(globalVariables);
        context.setMessageValidatorRegistry(messageValidatorRegistry);
        context.setTestListeners(testListeners);
        context.setMessageConstructionInterceptors(messageConstructionInterceptors);
        context.setEndpointFactory(endpointFactory);
        context.setApplicationContext(applicationContext);
        
        if (log.isDebugEnabled()) {
            log.debug("Created new test context - using global variables: '"
                    + context.getGlobalVariables() + "'");
        }
        
        return context;
    }

    /**
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
	@SuppressWarnings({ "unchecked", "rawtypes" })
    public Class getObjectType() {
        return TestContext.class;
    }

	/**
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
    public boolean isSingleton() {
        return false;
    }

    /**
     * @param functionRegistry the functionRegistry to set
     */
    public void setFunctionRegistry(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }

    /**
     * @return the functionRegistry
     */
    public FunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }
    
    /**
     * @param validationMatcherRegistry the validationMatcherRegistry to set
     */
    public void setValidationMatcherRegistry(
            ValidationMatcherRegistry validationMatcherRegistry) {
        this.validationMatcherRegistry = validationMatcherRegistry;
    }

    /**
     * @return the validationMatcherRegistry
     */
    public ValidationMatcherRegistry getValidationMatcherRegistry() {
        return validationMatcherRegistry;
    }

    /**
     * @param globalVariables the globalVariables to set
     */
    public void setGlobalVariables(GlobalVariables globalVariables) {
        this.globalVariables = globalVariables;
    }

    /**
     * @return the globalVariables
     */
    public GlobalVariables getGlobalVariables() {
        return globalVariables;
    }

    /**
     * Gets the endpoint factory.
     * @return
     */
    public EndpointFactory getEndpointFactory() {
        return endpointFactory;
    }

    /**
     * Sets the endpoint factory.
     * @param endpointFactory
     */
    public void setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
