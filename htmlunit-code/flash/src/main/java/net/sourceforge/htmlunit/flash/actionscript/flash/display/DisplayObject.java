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

import net.sourceforge.htmlunit.flash.actionscript.flash.events.EventDispatcher;
import net.sourceforge.htmlunit.flash.annotations.AsGetter;

/**
 * The DisplayObject class is the base class for all objects that can be placed on the display list.
 * The display list manages all objects displayed in the Flash runtimes.
 * Use the DisplayObjectContainer class to arrange the display objects in the display list.
 * DisplayObjectContainer objects can have child display objects, while other display objects,
 * such as Shape and TextField objects, are "leaf" nodes that have only parents and siblings, no children.
 *
 * @version $Revision: 7462 $
 * @author Ahmed Ashour
 */
public class DisplayObject extends EventDispatcher {

    private Stage stage_;
    private DisplayObject root_;

    /**
     * For a display object in a loaded SWF file, the root property is the top-most display object
     * in the portion of the display list's tree structure represented by that SWF file.
     * @return
     */
    @AsGetter
    public DisplayObject getRoot() {
        return root_;
    }

    /**
     * The Stage of the display object.
     * @return the stage
     */
    @AsGetter
    public Stage getStage() {
        return stage_;
    }

    public void setStage(final Stage stage) {
        stage_ = stage;
    }

}
