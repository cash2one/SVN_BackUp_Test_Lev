/*
 * Copyright (c) 2002-2015 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.javascript.configuration;

/**
 * Represents a real browser family.
 *
 * @version $Revision: 9837 $
 * @author Ahmed Ashour
 */
public enum BrowserName {
//    Could be renamed 'Browser', but it is used by the test cases in BrowserRunner, which is heavily used

    /** Firefox. */
    FF,

    /** Internet Explorer. */
    IE,

    /** Chrome. */
    CHROME
}

