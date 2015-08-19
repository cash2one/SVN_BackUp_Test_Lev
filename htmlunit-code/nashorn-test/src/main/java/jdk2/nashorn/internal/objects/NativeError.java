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

import static jdk2.nashorn.internal.lookup.Lookup.MH;
import static jdk2.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import jdk2.nashorn.api.scripting.NashornException;
import jdk2.nashorn.internal.objects.annotations.Attribute;
import jdk2.nashorn.internal.objects.annotations.Function;
import jdk2.nashorn.internal.objects.annotations.Property;
import jdk2.nashorn.internal.objects.annotations.ScriptClass;
import jdk2.nashorn.internal.objects.annotations.Where;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.ECMAException;
import jdk2.nashorn.internal.runtime.JSType;
import jdk2.nashorn.internal.runtime.PropertyMap;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.runtime.ScriptObject;
import jdk2.nashorn.internal.runtime.ScriptRuntime;

/**
 * ECMA 15.11 Error Objects
 */
@ScriptClass("Error")
public final class NativeError extends ScriptObject {

    static final MethodHandle GET_COLUMNNUMBER = findOwnMH("getColumnNumber", Object.class, Object.class);
    static final MethodHandle SET_COLUMNNUMBER = findOwnMH("setColumnNumber", Object.class, Object.class, Object.class);
    static final MethodHandle GET_LINENUMBER   = findOwnMH("getLineNumber", Object.class, Object.class);
    static final MethodHandle SET_LINENUMBER   = findOwnMH("setLineNumber", Object.class, Object.class, Object.class);
    static final MethodHandle GET_FILENAME     = findOwnMH("getFileName", Object.class, Object.class);
    static final MethodHandle SET_FILENAME     = findOwnMH("setFileName", Object.class, Object.class, Object.class);
    static final MethodHandle GET_STACK        = findOwnMH("getStack", Object.class, Object.class);
    static final MethodHandle SET_STACK        = findOwnMH("setStack", Object.class, Object.class, Object.class);

    // message property name
    static final String MESSAGE = "message";
    // name property name
    static final String NAME = "name";
    // stack property name
    static final String STACK = "__stack__";
    // lineNumber property name
    static final String LINENUMBER = "__lineNumber__";
    // columnNumber property name
    static final String COLUMNNUMBER = "__columnNumber__";
    // fileName property name
    static final String FILENAME = "__fileName__";

    /** Message property name */
    @Property(name = NativeError.MESSAGE, attributes = Attribute.NOT_ENUMERABLE)
    public Object instMessage;

    /** ECMA 15.11.4.2 Error.prototype.name */
    @Property(attributes = Attribute.NOT_ENUMERABLE, where = Where.PROTOTYPE)
    public Object name;

    /** ECMA 15.11.4.3 Error.prototype.message */
    @Property(attributes = Attribute.NOT_ENUMERABLE, where = Where.PROTOTYPE)
    public Object message;

    /** Nashorn extension: underlying exception */
    @Property(attributes = Attribute.NOT_ENUMERABLE)
    public Object nashornException;

    // initialized by nasgen
    private static PropertyMap $nasgenmap$;

    @SuppressWarnings("LeakingThisInConstructor")
    private NativeError(final Object msg, final ScriptObject proto, final PropertyMap map) {
        super(proto, map);
        if (msg != UNDEFINED) {
            this.instMessage = JSType.toString(msg);
        } else {
            this.delete(NativeError.MESSAGE, false);
        }
        initException(this);
    }

    NativeError(final Object msg, final Global global) {
        this(msg, global.getErrorPrototype(), $nasgenmap$);
    }

    private NativeError(final Object msg) {
        this(msg, Global.instance());
    }

    @Override
    public String getClassName() {
        return "Error";
    }

    /**
     * ECMA 15.11.2 The Error Constructor
     *
     * @param newObj true if this is being instantiated with a new
     * @param self   self reference
     * @param msg    error message
     *
     * @return NativeError instance
     */
    @jdk2.nashorn.internal.objects.annotations.Constructor
    public static NativeError constructor(final boolean newObj, final Object self, final Object msg) {
        return new NativeError(msg);
    }

    // This is called NativeError, NativeTypeError etc. to
    // associate a ECMAException with the ECMA Error object.
    @SuppressWarnings("unused")
    static void initException(final ScriptObject self) {
        // ECMAException constructor has side effects
        new ECMAException(self, null);
    }

    /**
     * Nashorn extension: Error.captureStackTrace. Capture stack trace at the point of call into the Error object provided.
     *
     * @param self self reference
     * @param errorObj the error object
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object captureStackTrace(final Object self, final Object errorObj) {
        final ScriptObject sobj = Global.checkObject(errorObj);
        initException(sobj);
        sobj.delete(STACK, false);
        if (! sobj.has("stack")) {
            final ScriptFunction getStack = ScriptFunctionImpl.makeFunction("getStack", GET_STACK);
            final ScriptFunction setStack = ScriptFunctionImpl.makeFunction("setStack", SET_STACK);
            sobj.addOwnProperty("stack", Attribute.NOT_ENUMERABLE, getStack, setStack);
        }
        return UNDEFINED;
    }

    /**
     * Nashorn extension: Error.dumpStack
     * dumps the stack of the current thread.
     *
     * @param self self reference
     *
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object dumpStack(final Object self) {
        Thread.dumpStack();
        return UNDEFINED;
    }

    /**
     * Nashorn extension: Error.prototype.printStackTrace
     * prints stack trace associated with the exception (if available).
     * to the standard error stream.
     *
     * @param self self reference
     *
     * @return result of {@link ECMAException#printStackTrace(ScriptObject)}, which is typically undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object printStackTrace(final Object self) {
        return ECMAException.printStackTrace(Global.checkObject(self));
    }

    /**
     * Nashorn extension: Error.prototype.getStackTrace()
     * "stack" property is an array typed value containing {@link StackTraceElement}
     * objects of JavaScript stack frames.
     *
     * @param self  self reference
     *
     * @return      stack trace as a script array.
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object getStackTrace(final Object self) {
        final ScriptObject sobj = Global.checkObject(self);
        final Object exception = ECMAException.getException(sobj);
        Object[] res;
        if (exception instanceof Throwable) {
            res = NashornException.getScriptFrames((Throwable)exception);
        } else {
            res = ScriptRuntime.EMPTY_ARRAY;
        }

        return new NativeArray(res);
    }

    /**
     * Nashorn extension: Error.prototype.lineNumber
     *
     * @param self self reference
     *
     * @return line number from which error was thrown
     */
    public static Object getLineNumber(final Object self) {
        final ScriptObject sobj = Global.checkObject(self);
        return sobj.has(LINENUMBER) ? sobj.get(LINENUMBER) : ECMAException.getLineNumber(sobj);
    }

    /**
     * Nashorn extension: Error.prototype.lineNumber
     *
     * @param self  self reference
     * @param value value of line number
     *
     * @return value that was set
     */
    public static Object setLineNumber(final Object self, final Object value) {
        final ScriptObject sobj = Global.checkObject(self);
        if (sobj.hasOwnProperty(LINENUMBER)) {
            sobj.put(LINENUMBER, value, false);
        } else {
            sobj.addOwnProperty(LINENUMBER, Attribute.NOT_ENUMERABLE, value);
        }
        return value;
    }

    /**
     * Nashorn extension: Error.prototype.columnNumber
     *
     * @param self self reference
     *
     * @return column number from which error was thrown
     */
    public static Object getColumnNumber(final Object self) {
        final ScriptObject sobj = Global.checkObject(self);
        return sobj.has(COLUMNNUMBER) ? sobj.get(COLUMNNUMBER) : ECMAException.getColumnNumber((ScriptObject)self);
    }

    /**
     * Nashorn extension: Error.prototype.columnNumber
     *
     * @param self  self reference
     * @param value value of column number
     *
     * @return value that was set
     */
    public static Object setColumnNumber(final Object self, final Object value) {
        final ScriptObject sobj = Global.checkObject(self);
        if (sobj.hasOwnProperty(COLUMNNUMBER)) {
            sobj.put(COLUMNNUMBER, value, false);
        } else {
            sobj.addOwnProperty(COLUMNNUMBER, Attribute.NOT_ENUMERABLE, value);
        }
        return value;
    }

    /**
     * Nashorn extension: Error.prototype.fileName
     *
     * @param self self reference
     *
     * @return file name from which error was thrown
     */
    public static Object getFileName(final Object self) {
        final ScriptObject sobj = Global.checkObject(self);
        return sobj.has(FILENAME) ? sobj.get(FILENAME) : ECMAException.getFileName((ScriptObject)self);
    }

    /**
     * Nashorn extension: Error.prototype.fileName
     *
     * @param self  self reference
     * @param value value of file name
     *
     * @return value that was set
     */
    public static Object setFileName(final Object self, final Object value) {
        final ScriptObject sobj = Global.checkObject(self);
        if (sobj.hasOwnProperty(FILENAME)) {
            sobj.put(FILENAME, value, false);
        } else {
            sobj.addOwnProperty(FILENAME, Attribute.NOT_ENUMERABLE, value);
        }
        return value;
    }

    /**
     * Nashorn extension: Error.prototype.stack
     * "stack" property is a string typed value containing JavaScript stack frames.
     * Each frame information is separated bv "\n" character.
     *
     * @param self  self reference
     *
     * @return      value of "stack" property
     */
    public static Object getStack(final Object self) {
        final ScriptObject sobj = Global.checkObject(self);
        if (sobj.has(STACK)) {
            return sobj.get(STACK);
        }

        final Object exception = ECMAException.getException(sobj);
        if (exception instanceof Throwable) {
            final Object value = getScriptStackString(sobj, (Throwable)exception);
            if (sobj.hasOwnProperty(STACK)) {
                sobj.put(STACK, value, false);
            } else {
                sobj.addOwnProperty(STACK, Attribute.NOT_ENUMERABLE, value);
            }

            return value;
        }

        return UNDEFINED;
    }

    /**
     * Nashorn extension
     * Accessed from {@link Global} while setting up the Error.prototype
     *
     * @param self   self reference
     * @param value  value to set "stack" property to, must be {@code ScriptObject}
     *
     * @return value that was set
     */
    public static Object setStack(final Object self, final Object value) {
        final ScriptObject sobj = Global.checkObject(self);
        if (sobj.hasOwnProperty(STACK)) {
            sobj.put(STACK, value, false);
        } else {
            sobj.addOwnProperty(STACK, Attribute.NOT_ENUMERABLE, value);
        }
        return value;
    }

    /**
     * ECMA 15.11.4.4 Error.prototype.toString ( )
     *
     * @param self  self reference
     *
     * @return this NativeError as a string
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object toString(final Object self) {
        // Step 1 and 2 : check if 'self' is object it not throw TypeError
        final ScriptObject sobj = Global.checkObject(self);

        // Step 3 & 4 : get "name" and convert to String.
        // But if message is undefined make it "Error".
        Object name = sobj.get("name");
        if (name == UNDEFINED) {
            name = "Error";
        } else {
            name = JSType.toString(name);
        }

        // Steps 5, 6, & 7 : get "message" and convert to String.
        // if 'message' is undefined make it "" (empty String).
        Object msg = sobj.get("message");
        if (msg == UNDEFINED) {
            msg = "";
        } else {
            msg = JSType.toString(msg);
        }

        // Step 8 : if name is empty, return msg
        if (((String)name).isEmpty()) {
            return msg;
        }

        // Step 9 : if message is empty, return name
        if (((String)msg).isEmpty()) {
            return name;
        }
        // Step 10 : return name + ": " + msg
        return name + ": " + msg;
    }

    private static MethodHandle findOwnMH(final String name, final Class<?> rtype, final Class<?>... types) {
        return MH.findStatic(MethodHandles.lookup(), NativeError.class, name, MH.type(rtype, types));
    }

    private static String getScriptStackString(final ScriptObject sobj, final Throwable exp) {
        return JSType.toString(sobj) + "\n" + NashornException.getScriptStackString(exp);
    }

    private static MethodHandle virtualHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findVirtual(NativeError.class, name,
                    MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(2);
            list.add(AccessorProperty.create("message", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$instMessage", Object.class),
                    virtualHandle("S$instMessage", void.class, Object.class)));
            list.add(AccessorProperty.create("nashornException", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$nashornException", Object.class),
                    virtualHandle("S$nashornException", void.class, ScriptFunction.class)));
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

    public void S$nashornException(final ScriptFunction function) {
        this.nashornException = function;
    }

    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(NativeError.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static final class Constructor extends ScriptFunctionImpl {
        private ScriptFunction captureStackTrace;
        private ScriptFunction dumpStack;
        private static final PropertyMap $nasgenmap$;

        public ScriptFunction G$captureStackTrace() {
            return this.captureStackTrace;
        }

        public void S$captureStackTrace(final ScriptFunction function) {
            this.captureStackTrace = function;
        }

        public ScriptFunction G$dumpStack() {
            return this.dumpStack;
        }

        public void S$dumpStack(final ScriptFunction function) {
            this.dumpStack = function;
        }

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(3);
            list.add(AccessorProperty.create("captureStackTrace", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$captureStackTrace", ScriptFunction.class),
                    virtualHandle("S$captureStackTrace", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("dumpStack", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$dumpStack", ScriptFunction.class),
                    virtualHandle("S$dumpStack", void.class, ScriptFunction.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Constructor() {
            super("Error", 
                    staticHandle("constructor", NativeError.class, boolean.class, Object.class, Object.class),
                    $nasgenmap$, null);
            captureStackTrace = ScriptFunctionImpl.makeFunction("captureStackTrace",
                    staticHandle("captureStackTrace", Object.class, Object.class, Object.class));
            dumpStack = ScriptFunctionImpl.makeFunction("dumpStack",
                    staticHandle("dumpStack", Object.class, Object.class));
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
        private ScriptFunction printStackTrace;
        private ScriptFunction getStackTrace;
        private ScriptFunction toString;
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

        public ScriptFunction G$printStackTrace() {
            return this.printStackTrace;
        }

        public void S$printStackTrace(final ScriptFunction function) {
            this.printStackTrace = function;
        }

        public ScriptFunction G$getStackTrace() {
            return this.getStackTrace;
        }

        public void S$getStackTrace(final ScriptFunction function) {
            this.getStackTrace = function;
        }

        public ScriptFunction G$toString() {
            return this.toString;
        }

        public void S$toString(final ScriptFunction function) {
            this.toString = function;
        }

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(6);
            list.add(AccessorProperty.create("name", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$name", Object.class),
                    virtualHandle("S$name", void.class, Object.class)));
            list.add(AccessorProperty.create("message", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$message", Object.class),
                    virtualHandle("S$message", void.class, Object.class)));
            list.add(AccessorProperty.create("printStackTrace", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$printStackTrace", ScriptFunction.class),
                    virtualHandle("S$printStackTrace", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getStackTrace", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getStackTrace", ScriptFunction.class),
                    virtualHandle("S$getStackTrace", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("toString", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$toString", ScriptFunction.class),
                    virtualHandle("S$toString", void.class, ScriptFunction.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Prototype() {
            super($nasgenmap$);
            printStackTrace = ScriptFunctionImpl.makeFunction("printStackTrace",
                    staticHandle("printStackTrace", Object.class, Object.class));
            getStackTrace = ScriptFunctionImpl.makeFunction("getStackTrace",
                    staticHandle("getStackTrace", Object.class, Object.class));
            toString = ScriptFunctionImpl.makeFunction("toString",
                    staticHandle("toString", Object.class, Object.class));
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
