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

package com.consol.citrus.container;

import com.consol.citrus.TestAction;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.util.BooleanExpressionParser;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractIteratingTestAction extends AbstractActionContainer {
    /** Boolean expression string */
    protected String condition;

    /** Name of index variable */
    protected String indexName = "i";

    /** Looping index */
    protected int index = 1;
    
    @Override
    public void doExecute(TestContext context) {
        condition = context.replaceDynamicContentInString(condition);
        
        executeIteration(context);
    }
    
    /**
     * Execute embedded actions in loop.
     * @param context TestContext holding variable information.
     */
    protected abstract void executeIteration(TestContext context);
    
    /**
     * Executes the nested test actions.
     * @param context
     */
    protected void executeActions(TestContext context) {
        context.setVariable(indexName, String.valueOf(index));

        for (TestAction action: actions) {
            setLastExecutedAction(action);
            action.execute(context);
        }
    }
    
    /** 
     * Check aborting condition.
     * @return
     */
    protected boolean checkCondition() {
        String conditionString = condition;

        if (conditionString.indexOf(indexName) != -1) {
            conditionString = conditionString.replaceAll(indexName, String.valueOf(index));
        }

        return BooleanExpressionParser.evaluate(conditionString);
    }
    
    /**
     * Aborting condition.
     * @param condition
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Name of index variable.
     * @param indexName
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    /**
     * Setter for looping index.
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Gets the condition.
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Gets the indexName.
     * @return the indexName
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Gets the index.
     * @return the index
     */
    public int getIndex() {
        return index;
    }
}
