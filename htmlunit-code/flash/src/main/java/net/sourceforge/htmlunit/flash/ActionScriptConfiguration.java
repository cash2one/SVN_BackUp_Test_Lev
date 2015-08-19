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

/**
 * An actual object of actionscript, to be used inside the engine.
 *
 * @version $Revision: 7359 $
 * @author Ahmed Ashour
 */
public class ActionScriptConfiguration {

    private static Map<String, ScriptObject> prototypes = new HashMap<String, ScriptObject>();

    static {
        try {
            final Package p = Function.class.getPackage();
            final int prefixLength = p.getName().length() + 1;
            for (final String name : Util.getClassNamesForPackage(p)) {
                final Class<?> klass = Class.forName(name);
                String superClassName = klass.getSuperclass().getName();
                if (superClassName.equals(Object.class.getName())) {
                    superClassName = null;
                }
                else {
                    superClassName = superClassName.substring(prefixLength);
                }
                String className = name.substring(prefixLength);
                final ScriptObject object = new NativeScriptObject(klass, className, superClassName);
                prototypes.put(className, object);
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static ScriptObject getPrototypeOf(final String className) {
        for (final String key : prototypes.keySet()) {
            if (key.equals(className) || key.endsWith("." + className)) {
                return prototypes.get(key);
            }
        }
        return null;
    }

    public static void setPrototype(final String className, final RuntimeScriptObject object) {
        prototypes.put(className, object);
    }
}
