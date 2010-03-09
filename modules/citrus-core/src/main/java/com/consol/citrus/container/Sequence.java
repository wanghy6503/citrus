/*
 * Copyright 2006-2010 ConSol* Software GmbH.
 * 
 * This file is part of Citrus.
 * 
 * Citrus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Citrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Citrus. If not, see <http://www.gnu.org/licenses/>.
 */

package com.consol.citrus.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.consol.citrus.TestAction;
import com.consol.citrus.context.TestContext;

/**
 * Sequence container executing a set of nested test actions in simple sequence. 
 *
 * @author Christoph Deppisch
 * @since 2007
 */
public class Sequence extends AbstractActionContainer {

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(Sequence.class);

    /**
     * @see com.consol.citrus.TestAction#execute(TestContext)
     */
    @Override
    public void execute(TestContext context) {
        for (TestAction action: actions) {
            setLastExecutedAction(action);
            action.execute(context);
        }

        log.info("Action sequence finished successfully");
    }
}
