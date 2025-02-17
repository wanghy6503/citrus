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

package com.consol.citrus.admin.converter;

import com.consol.citrus.TestAction;
import com.consol.citrus.TestCase;
import com.consol.citrus.actions.*;
import com.consol.citrus.admin.converter.actions.*;
import com.consol.citrus.admin.converter.container.RepeatOnErrorContainerConverter;
import com.consol.citrus.container.RepeatOnErrorUntilTrue;
import com.consol.citrus.model.testcase.core.*;

import java.util.Map;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class TestcaseModelConverter implements ObjectConverter<Testcase, TestCase> {

    @Override
    public Testcase convert(TestCase definition) {
        Testcase testModel = new Testcase();

        testModel.setName(definition.getName());
        testModel.setDescription(definition.getDescription());

        MetaInfoType metaInfoType = new MetaInfoType();
        metaInfoType.setAuthor(definition.getMetaInfo().getAuthor());
        metaInfoType.setStatus(definition.getMetaInfo().getStatus().name());
        testModel.setMetaInfo(metaInfoType);

        Variables variables = new Variables();
        for (Map.Entry<String, ?> variableEntry : definition.getVariableDefinitions().entrySet()) {
            Variables.Variable variable = new Variables.Variable();

            variable.setName(variableEntry.getKey());
            variable.setValue(variableEntry.getValue().toString());

            variables.getVariables().add(variable);
        }

        testModel.setVariables(variables);

        ActionListType actions = new ActionListType();
        for (TestAction testAction : definition.getActions()) {
            if (testAction instanceof ReceiveMessageAction) {
                actions.getActionsAndSendsAndReceives().add(new ReceiveMessageActionConverter().convert((ReceiveMessageAction) testAction));
            } else if (testAction instanceof SendMessageAction) {
                actions.getActionsAndSendsAndReceives().add(new SendMessageActionConverter().convert((SendMessageAction) testAction));
            } else if (testAction instanceof SleepAction) {
                actions.getActionsAndSendsAndReceives().add(new SleepActionConverter().convert((SleepAction) testAction));
            } else if (testAction instanceof ExecuteSQLAction) {
                actions.getActionsAndSendsAndReceives().add(new ExecuteSqlActionConverter().convert((ExecuteSQLAction) testAction));
            } else if (testAction instanceof RepeatOnErrorUntilTrue) {
                actions.getActionsAndSendsAndReceives().add(new RepeatOnErrorContainerConverter().convert((RepeatOnErrorUntilTrue) testAction));
            } else {
                actions.getActionsAndSendsAndReceives().add(new TestActionConverter().convert(testAction));
            }
        }

        testModel.setActions(actions);

        return testModel;
    }

    @Override
    public Class<TestCase> getModelClass() {
        return TestCase.class;
    }
}
