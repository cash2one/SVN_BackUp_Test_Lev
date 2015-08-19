/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk2.nashorn.internal.objects;

import static jdk2.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import jdk2.nashorn.internal.objects.annotations.Attribute;
import jdk2.nashorn.internal.objects.annotations.Property;
import jdk2.nashorn.internal.objects.annotations.ScriptClass;
import jdk2.nashorn.internal.objects.annotations.Where;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.JSType;
import jdk2.nashorn.internal.runtime.PropertyMap;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.runtime.ScriptObject;

/**
 * ECMA 15.11.6.6 URIError
 */
@ScriptClass("Error")
public final class NativeURIError extends ScriptObject {

    /** message property in instance */
    @Property(name = NativeError.MESSAGE, attributes = Attribute.NOT_ENUMERABLE)
    public Object instMessage;

    /** error name property */
    @Property(attributes = Attribute.NOT_ENUMERABLE, where = Where.PROTOTYPE)
    public Object name;

    /** ECMA 15.1.1.1 message property */
    @Property(attributes = Attribute.NOT_ENUMERABLE, where = Where.PROTOTYPE)
    public Object message;

    /** Nashorn extension: underlying exception */
    @Property(attributes = Attribute.NOT_ENUMERABLE)
    public Object nashornException;

    // initialized by nasgen
    private static PropertyMap $nasgenmap$;

    @SuppressWarnings("LeakingThisInConstructor")
    NativeURIError(final Object msg, final Global global) {
        super(global.getURIErrorPrototype(), $nasgenmap$);
        if (msg != UNDEFINED) {
            this.instMessage = JSType.toString(msg);
        } else {
            this.delete(NativeError.MESSAGE, false);
        }
        NativeError.initException(this);
    }

    private NativeURIError(final Object msg) {
        this(msg, Global.instance());
    }

    @Override
    public String getClassName() {
        return "Error";
    }

    /**
     * ECMA 15.11.6.6 URIError
     *
     * Constructor
     *
     * @param newObj was this error instantiated with the new operator
     * @param self   self reference
     * @param msg    error message
     *
     * @return new URIError
     */
    @jdk2.nashorn.internal.objects.annotations.Constructor(name = "URIError")
    public static NativeURIError constructor(final boolean newObj, final Object self, final Object msg) {
        return new NativeURIError(msg);
    }

    static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(2);
            list.add(AccessorProperty.create("message", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$instMessage", Object.class),
                    virtualHandle("S$instMessage", void.class, Object.class)));
            list.add(AccessorProperty.create("nashornException", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$nashornException", Object.class),
                    virtualHandle("S$nashornException", void.class, Object.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
    }
    public Object G$instMessage() {
        return this.instMessage;
    }

    public void S$instMessage(final Object function) {
        this.instMessage = function;
    }

    public Object G$nashornException() {
        return this.nashornException;
    }

    public void S$nashornException(final Object function) {
        this.nashornException = function;
    }


    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(NativeURIError.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    private static MethodHandle virtualHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findVirtual(NativeURIError.class, name,
                    MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static final class Constructor extends ScriptFunctionImpl {
        private static final PropertyMap $nasgenmap$;

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(1);
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Constructor() {
            super("URIError", 
                    staticHandle("constructor", NativeURIError.class, boolean.class, Object.class, Object.class),
                    $nasgenmap$, null);
            final Prototype prototype = new Prototype();
            PrototypeObject.setConstructor(prototype, this);
            setPrototype(prototype);
        }

        private static MethodHandle virtualHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
            try {
                return MethodHandles.lookup().findVirtual(Constructor.class, name,
                        MethodType.methodType(rtype, ptypes));
            }
            catch (final ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    static final class Prototype extends PrototypeObject {
        private Object name;
        private Object message;
        private static final PropertyMap $nasgenmap$;

        public Object G$name() {
            return this.name;
        }

        public void S$name(final Object function) {
            this.name = function;
        }

        public Object G$message() {
            return this.message;
        }

        public void S$message(final Object function) {
            this.message = function;
        }

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(3);
            list.add(AccessorProperty.create("name", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$name", Object.class),
                    virtualHandle("S$name", void.class, Object.class)));
            list.add(AccessorProperty.create("message", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$message", Object.class),
                    virtualHandle("S$message", void.class, Object.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Prototype() {
            super($nasgenmap$);
        }

       public String getClassName() {
           return "Error";
       }

        private static MethodHandle virtualHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
            try {
                return MethodHandles.lookup().findVirtual(Prototype.class, name,
                        MethodType.methodType(rtype, ptypes));
            }
            catch (final ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
