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
package net.sourceforge.htmlunit.flash.actionscript.flash.display;

import net.sourceforge.htmlunit.flash.ActionScriptEngine;
import net.sourceforge.htmlunit.flash.actionscript.Function;
import net.sourceforge.htmlunit.flash.annotations.AsFunction;

/**
 * Unlike the Sprite object, a MovieClip object has a timeline.
 *
 * @version $Revision: 7462 $
 * @author Ahmed Ashour
 */
public class MovieClip extends Sprite {

    /**
     * Adds a specified script to the given frame.  
     *
     * @param frameIndex the frame index, 0-based
     * @param function the function to call
     */
    @AsFunction
    public void addFrameScript(final int frameIndex, final Function function) {
        try {
            ActionScriptEngine.call(function, new Object[0]);
        }
        catch(final Exception e) {
            e.printStackTrace();
        }
    }
}
