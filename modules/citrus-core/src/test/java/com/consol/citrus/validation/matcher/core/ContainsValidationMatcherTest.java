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

package com.consol.citrus.validation.matcher.core;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.consol.citrus.exceptions.ValidationException;
import com.consol.citrus.testng.AbstractTestNGUnitTest;

public class ContainsValidationMatcherTest extends AbstractTestNGUnitTest {
    
    private ContainsValidationMatcher matcher = new ContainsValidationMatcher();
    
    @Test
    public void testValidateSuccess() {
        matcher.validate("field", "This is a test", "is a");
        matcher.validate("field", "This is a test", "This");
        matcher.validate("field", "This is a test", "test");
        matcher.validate("field", "This is a 0815test", "0815");
        matcher.validate("field", "This is a test", " ");
    }
    
    @Test
    public void testValidateError() {
    	assertException("field", "This is a test", "0815");
    	assertException("field", null, "control");
    }

    private void assertException(String fieldName, String value, String control) {
    	try {
    		matcher.validate(fieldName, value, control);
    		Assert.fail("Expected exception not thrown!");
    	} catch (ValidationException e) {
			Assert.assertTrue(e.getMessage().contains(fieldName));
			Assert.assertTrue(e.getMessage().contains(control));

            if (value != null) {
			    Assert.assertTrue(e.getMessage().contains(value));
            } else {
                Assert.assertTrue(e.getMessage().contains("null"));
            }
		}
    }
}
