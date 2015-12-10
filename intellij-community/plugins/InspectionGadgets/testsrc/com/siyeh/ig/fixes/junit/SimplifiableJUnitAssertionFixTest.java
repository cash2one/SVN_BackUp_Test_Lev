/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.fixes.junit;

import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.IGQuickFixesTestCase;
import com.siyeh.ig.junit.SimplifiableJUnitAssertionInspection;

/**
 * @author Bas Leijdekkers
 */
public class SimplifiableJUnitAssertionFixTest extends IGQuickFixesTestCase {

  public void testJUnit3TestCase() {
    doTest();
  }

  public void testJUnit4TestCase() {
    doTest();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new SimplifiableJUnitAssertionInspection());
    myRelativePath = "junit/simplifiable_junit_assertion";
    myDefaultHint = InspectionGadgetsBundle.message("simplify.junit.assertion.simplify.quickfix");

    myFixture.addClass("package junit.framework;" +
                       "public abstract class TestCase extends Assert {" +
                       "    protected void setUp() throws Exception {}" +
                       "    protected void tearDown() throws Exception {}" +
                       "}");

    myFixture.addClass("package junit.framework;" +
                       "public class Assert {" +
                       "    public static void assertTrue(String message, boolean condition) {}" +
                       "    public static void assertTrue(boolean condition) {}" +
                       "    public static void assertEquals(String message, Object expected, Object actual) {}" +
                       "    public static void assertEquals(Object expected, Object actual) {}" +
                       "    public static void assertFalse(String message, boolean condition) {}" +
                       "    public static void assertFalse(boolean condition) {}" +
                       "}");

    myFixture.addClass("package org.junit;" +
                       "public class Assert {" +
                       "    public static public void assertTrue(boolean condition) {}" +
                       "    public static void assertEquals(boolean expected, boolean actual) {}" +
                       "    public static void assertFalse(String message, boolean condition) {}" +
                       "}");

    myFixture.addClass("package org.junit;" +
                       "import java.lang.annotation.ElementType;" +
                       "import java.lang.annotation.Retention;" +
                       "import java.lang.annotation.RetentionPolicy;" +
                       "import java.lang.annotation.Target;" +
                       "@Retention(RetentionPolicy.RUNTIME)" +
                       "@Target({ElementType.METHOD})" +
                       "public @interface Test {}");
  }
}
