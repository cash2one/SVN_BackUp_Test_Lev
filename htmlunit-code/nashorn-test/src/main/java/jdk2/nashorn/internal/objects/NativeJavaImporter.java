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

import static jdk2.nashorn.internal.runtime.ECMAErrors.typeError;
import static jdk2.nashorn.internal.runtime.UnwarrantedOptimismException.isValid;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk2.internal.dynalink.CallSiteDescriptor;
import jdk2.internal.dynalink.beans.StaticClass;
import jdk2.internal.dynalink.linker.GuardedInvocation;
import jdk2.internal.dynalink.linker.LinkRequest;
import jdk2.nashorn.internal.objects.annotations.Attribute;
import jdk2.nashorn.internal.objects.annotations.Function;
import jdk2.nashorn.internal.objects.annotations.ScriptClass;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.Context;
import jdk2.nashorn.internal.runtime.JSType;
import jdk2.nashorn.internal.runtime.NativeJavaPackage;
import jdk2.nashorn.internal.runtime.Property;
import jdk2.nashorn.internal.runtime.PropertyMap;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.runtime.ScriptObject;
import jdk2.nashorn.internal.runtime.ScriptRuntime;
import jdk2.nashorn.internal.runtime.UnwarrantedOptimismException;

/**
 * This is "JavaImporter" constructor. This constructor allows you to use Java types omitting explicit package names.
 * Objects of this constructor are used along with {@code "with"} statements and as such are not usable in ECMAScript
 * strict mode. Example:
 * <pre>
 *     var imports = new JavaImporter(java.util, java.io);
 *     with (imports) {
 *         var m = new HashMap(); // java.util.HashMap
 *         var f = new File("."); // java.io.File
 *         ...
 *     }
 * </pre>
 * Note however that the preferred way for accessing Java types in Nashorn is through the use of
 * {@link NativeJava#type(Object, Object) Java.type()} method.
 */
@ScriptClass("JavaImporter")
public final class NativeJavaImporter extends ScriptObject {
    private final Object[] args;

    // initialized by nasgen
    private static PropertyMap $nasgenmap$;

    private NativeJavaImporter(final Object[] args, final ScriptObject proto, final PropertyMap map) {
        super(proto, map);
        this.args = args;
    }

    private NativeJavaImporter(final Object[] args, final Global global) {
        this(args, global.getJavaImporterPrototype(), $nasgenmap$);
    }

    private NativeJavaImporter(final Object[] args) {
        this(args, Global.instance());
    }

    @Override
    public String getClassName() {
        return "JavaImporter";
    }

    /**
     * Constructor
     * @param isNew is the new operator used for instantiating this NativeJavaImporter
     * @param self self reference
     * @param args arguments
     * @return NativeJavaImporter instance
     */
    @jdk2.nashorn.internal.objects.annotations.Constructor(arity = 1)
    public static NativeJavaImporter constructor(final boolean isNew, final Object self, final Object... args) {
        return new NativeJavaImporter(args);
    }

    /**
     * "No such property" handler.
     *
     * @param self self reference
     * @param name property name
     * @return value of the missing property
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object __noSuchProperty__(final Object self, final Object name) {
        if (! (self instanceof NativeJavaImporter)) {
            throw typeError("not.a.java.importer", ScriptRuntime.safeToString(self));
        }
        return ((NativeJavaImporter)self).createProperty(JSType.toString(name));
    }

    /**
     * "No such method call" handler
     *
     * @param self self reference
     * @param args arguments to method
     * @return never returns always throw TypeError
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object __noSuchMethod__(final Object self, final Object... args) {
       throw typeError("not.a.function", ScriptRuntime.safeToString(args[0]));
    }

    @Override
    public GuardedInvocation noSuchProperty(final CallSiteDescriptor desc, final LinkRequest request) {
        return createAndSetProperty(desc) ? super.lookup(desc, request) : super.noSuchProperty(desc, request);
    }

    @Override
    public GuardedInvocation noSuchMethod(final CallSiteDescriptor desc, final LinkRequest request) {
        return createAndSetProperty(desc) ? super.lookup(desc, request) : super.noSuchMethod(desc, request);
    }

    @Override
    protected Object invokeNoSuchProperty(final String name, final int programPoint) {
        final Object retval = createProperty(name);
        if (isValid(programPoint)) {
            throw new UnwarrantedOptimismException(retval, programPoint);
        }
        return retval;
    }

    private boolean createAndSetProperty(final CallSiteDescriptor desc) {
        final String name = desc.getNameToken(CallSiteDescriptor.NAME_OPERAND);
        final Object value = createProperty(name);
        if(value != null) {
            set(name, value, 0);
            return true;
        }
        return false;
    }

    private Object createProperty(final String name) {
        final int len = args.length;

        for (int i = len - 1; i > -1; i--) {
            final Object obj = args[i];

            if (obj instanceof StaticClass) {
                if (((StaticClass)obj).getRepresentedClass().getSimpleName().equals(name)) {
                    return obj;
                }
            } else if (obj instanceof NativeJavaPackage) {
                final String pkgName  = ((NativeJavaPackage)obj).getName();
                final String fullName = pkgName.isEmpty() ? name : (pkgName + "." + name);
                final Context context = Global.instance().getContext();
                try {
                    return StaticClass.forClass(context.findClass(fullName));
                } catch (final ClassNotFoundException e) {
                    // IGNORE
                }
            }
        }
        return null;
    }

    static {
            final List<Property> list = Collections.emptyList();
            $nasgenmap$ = PropertyMap.newMap(list);
    }

    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(NativeJavaImporter.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static final class Constructor extends ScriptFunctionImpl {
        private static final PropertyMap $nasgenmap$;

        static {
            final List<Property> list = new ArrayList<>(1);
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Constructor() {
            super("JavaImporter", 
                    staticHandle("constructor", NativeJavaImporter.class, boolean.class, Object.class, Object[].class),
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
        private ScriptFunction __noSuchProperty__;
        private ScriptFunction __noSuchMethod__;
        private static final PropertyMap $nasgenmap$;

        public ScriptFunction G$__noSuchProperty__() {
            return this.__noSuchProperty__;
        }

        public void S$__noSuchProperty__(final ScriptFunction function) {
            this.__noSuchProperty__ = function;
        }

        public ScriptFunction G$__noSuchMethod__() {
            return this.__noSuchMethod__;
        }

        public void S$__noSuchMethod__(final ScriptFunction function) {
            this.__noSuchMethod__ = function;
        }

        static {
            final List<Property> list = new ArrayList<>(3);
            list.add(AccessorProperty.create("__noSuchProperty__", Property.NOT_ENUMERABLE, 
                    virtualHandle("G$__noSuchProperty__", ScriptFunction.class),
                    virtualHandle("S$__noSuchProperty__", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("__noSuchMethod__", Property.NOT_ENUMERABLE, 
                    virtualHandle("G$__noSuchMethod__", ScriptFunction.class),
                    virtualHandle("S$__noSuchMethod__", void.class, ScriptFunction.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Prototype() {
            super($nasgenmap$);
            __noSuchProperty__ = ScriptFunctionImpl.makeFunction("__noSuchProperty__",
                    staticHandle("__noSuchProperty__", Object.class, Object.class, Object.class));
            __noSuchMethod__ = ScriptFunctionImpl.makeFunction("__noSuchMethod__",
                    staticHandle("__noSuchMethod__", Object.class, Object.class, Object[].class));
        }

       public String getClassName() {
           return "JavaImporter";
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
