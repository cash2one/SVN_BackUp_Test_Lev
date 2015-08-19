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

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.htmlunit.flash.actionscript.Function;
import adobe.abc.Method;

/**
 * An actual object of actionscript, to be used inside the engine.
 *
 * @version $Revision: 7462 $
 * @author Ahmed Ashour
 */
public class RuntimeScriptObject extends ScriptObject {

    private Flash flash_;
    private RuntimeScriptObject prototype_;
    private String className_;
    private ScriptObject nativeObject_;
    private String superClassName_;
    private Map<String, Object> properties = new HashMap<String, Object>();

    //for prototype only
    RuntimeScriptObject(final String className, final String superClassName) {
        className_ = className;
        superClassName_ = superClassName;
        nativeObject_ = new NativeScriptObject((NativeScriptObject) ActionScriptConfiguration.getPrototypeOf(superClassName_));
    }

    RuntimeScriptObject(final Flash flash, final RuntimeScriptObject prototype) {
        flash_ = flash;
        prototype_ = prototype;
    }

    public String getClassName() {
        return className_;
    }

    public String getSuperClassName() {
        if (prototype_ != null) {
            return prototype_.getSuperClassName();
        }
        return superClassName_;
    }

    public Object getProperty(final Object start, final String property) {
        if (prototype_ != null) {
            return prototype_.getProperty(start, property);
        }
        
        Object o = properties.get(property);
        if (o == null) {
            o = ActionScriptConfiguration.getPrototypeOf(superClassName_).getProperty(start, property);
        }
        if (o instanceof Method) {
            o = new Function(flash_, this, o);
        }
        return o;
    }

    public void putProperty(final String property, final Object object) {
        properties.put(property, object);
    }

    public NativeScriptObject getNativeObject() {
        if (prototype_ != null) {
            return prototype_.getNativeObject();
        }
        if (nativeObject_ instanceof NativeScriptObject) {
            return (NativeScriptObject) nativeObject_;
        }
        return ((RuntimeScriptObject) ActionScriptConfiguration.getPrototypeOf(superClassName_)).getNativeObject();
    }
}
