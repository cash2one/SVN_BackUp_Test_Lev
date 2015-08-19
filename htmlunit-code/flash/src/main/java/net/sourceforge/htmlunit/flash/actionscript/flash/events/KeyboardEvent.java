/*
 * Copyright (c) 2002-2012 Gargoyle Software Inc.
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
package net.sourceforge.htmlunit.flash.actionscript.flash.events;

import net.sourceforge.htmlunit.flash.annotations.AsConstant;

/**
 * A KeyboardEvent object id dispatched in response to user input through a keyboard. 
 *
 * @version $Revision: 7350 $
 * @author Ahmed Ashour
 */
public class KeyboardEvent extends Event {

    /** Defines the value of the type property of a keyDown event object. */
    @AsConstant
    public static final String KEY_DOWN = "keyDown";

    /** Defines the value of the type property of a keyUp event object. */
    @AsConstant
    public static final String KEY_UP = "keyUp";
}
