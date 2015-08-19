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
package net.sourceforge.htmlunit.flash;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.htmlunit.flash.actionscript.flash.display.Stage;
import adobe.abc.GlobalOptimizer;
import adobe.abc.GlobalOptimizer.InputAbc;
import flash.swf.Tag;
import flash.swf.TagDecoder;
import flash.swf.TagHandler;
import flash.swf.tags.DoABC;

/**
 * The entry point.
 *
 * @version $Revision: 7462 $
 * @author Ahmed Ashour
 */
public class Flash {

    private Stage stage_ = new Stage();
    
    public Flash(final InputStream is) throws IOException {
        TagDecoder decoder = new TagDecoder(is);
        decoder.parse(new FlashTagHandler());
    }

    private class FlashTagHandler extends TagHandler {
        public void any(Tag tag) {
            //System.out.println(tag.getClass().getName());
        }
        public void doABC(final DoABC tag) {
            try {
                final GlobalOptimizer go = new GlobalOptimizer();
                
                final InputAbc ia = go.new InputAbc();
                ia.readAbc(tag.abc);

                ActionScriptEngine.execute(ia, Flash.this);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public Stage getStage() {
        return stage_;
    }
}
