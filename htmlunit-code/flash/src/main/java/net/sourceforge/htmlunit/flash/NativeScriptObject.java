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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.htmlunit.flash.annotations.AsConstant;
import net.sourceforge.htmlunit.flash.annotations.AsFunction;
import net.sourceforge.htmlunit.flash.annotations.AsGetter;
import net.sourceforge.htmlunit.flash.annotations.AsSetter;

/**
 * An actual object of actionscript, to be used inside the engine.
 *
 * @version $Revision: 7462 $
 * @author Ahmed Ashour
 */
public class NativeScriptObject extends ScriptObject {

    private NativeScriptObject prototype_;
    private Class<?> hostClass_;
    private Object object_;
    private String className_;
    private String simpleClassName_;
    private String superClassName_;
    private Map<String, Field> constants_ = new HashMap<String, Field>();
    private Map<String, Method> getters_ = new HashMap<String, Method>();
    private Map<String, Method> setters_ = new HashMap<String, Method>();
    private Map<String, Method> functions_ = new HashMap<String, Method>();
    
    NativeScriptObject(final Class<?> c, final String className, final String superClassName) {
        hostClass_ = c;
        if (hostClass_ == null) {
            new Exception().printStackTrace();
        }
        simpleClassName_ = className_ = className;
        int p0 = className.lastIndexOf('.');
        if (p0 != -1) {
            simpleClassName_ = simpleClassName_.substring(p0 + 1);
        }
        superClassName_ = superClassName;
        for (final Method m : c.getDeclaredMethods()) {
            String name = m.getName();
            if (m.getAnnotation(AsFunction.class) != null) {
                functions_.put(name, m);
            }
            else if (m.getAnnotation(AsGetter.class) != null && name.startsWith("get")) {
                name = name.substring(3);
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                getters_.put(name, m);
            }
            else if (m.getAnnotation(AsSetter.class) != null && name.startsWith("set")) {
                name = name.substring(3);
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                setters_.put(name, m);
            }
        }
        for (final Field f : c.getDeclaredFields()) {
            if (f.getAnnotation(AsConstant.class) != null) {
                constants_.put(f.getName(), f);
            }
        }
    }

    NativeScriptObject(final NativeScriptObject prototype) {
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
        
        Object o = getters_.get(property);
        if (o != null) {
            try {
                o = ((java.lang.reflect.Method) o).invoke(start, new Object[0]);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        if (property.equals(simpleClassName_)) {
            return this;
        }
        if (o == null) {
            o = constants_.get(property);
        }
        if (o == null) {
            o = setters_.get(property);
        }
        if (o == null) {
            o = functions_.get(property);
        }
        if (o == null) {
            o = ActionScriptConfiguration.getPrototypeOf(superClassName_).getProperty(start, property);
        }
        return o;
    }

    public Object getObject() {
        if (object_ == null) {
            try {
                Class<?> klass = hostClass_;
                if (klass == null) {
                    klass = prototype_.hostClass_;
                }
                object_ = klass.newInstance();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return object_;
    }
}
