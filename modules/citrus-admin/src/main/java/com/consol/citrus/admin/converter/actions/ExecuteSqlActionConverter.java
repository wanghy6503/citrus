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
package com.consol.citrus.admin.converter.actions;

import com.consol.citrus.actions.ExecuteSQLAction;
import com.consol.citrus.admin.converter.ObjectConverter;
import com.consol.citrus.model.testcase.core.ObjectFactory;
import com.consol.citrus.model.testcase.core.Sql;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class ExecuteSqlActionConverter implements ObjectConverter<Sql, ExecuteSQLAction> {

    @Override
    public Sql convert(ExecuteSQLAction definition) {
        Sql action = new ObjectFactory().createSql();

        action.setDescription(definition.getDescription());
        action.setDatasource(definition.getDataSource().toString());

        return action;
    }

    @Override
    public Class<ExecuteSQLAction> getModelClass() {
        return ExecuteSQLAction.class;
    }
}
