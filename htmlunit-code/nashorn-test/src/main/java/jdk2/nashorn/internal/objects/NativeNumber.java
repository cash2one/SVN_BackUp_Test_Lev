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
import static jdk2.nashorn.internal.runtime.ECMAErrors.rangeError;
import static jdk2.nashorn.internal.runtime.ECMAErrors.typeError;
import static jdk2.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jdk2.internal.dynalink.linker.GuardedInvocation;
import jdk2.internal.dynalink.linker.LinkRequest;
import jdk2.nashorn.internal.objects.annotations.Attribute;
import jdk2.nashorn.internal.objects.annotations.Function;
import jdk2.nashorn.internal.objects.annotations.Property;
import jdk2.nashorn.internal.objects.annotations.ScriptClass;
import jdk2.nashorn.internal.objects.annotations.SpecializedFunction;
import jdk2.nashorn.internal.objects.annotations.Where;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.JSType;
import jdk2.nashorn.internal.runtime.PropertyMap;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.runtime.ScriptObject;
import jdk2.nashorn.internal.runtime.ScriptRuntime;
import jdk2.nashorn.internal.runtime.Specialization;
import jdk2.nashorn.internal.runtime.linker.PrimitiveLookup;

/**
 * ECMA 15.7 Number Objects.
 *
 */
@ScriptClass("Number")
public final class NativeNumber extends ScriptObject {

    /** Method handle to create an object wrapper for a primitive number. */
    static final MethodHandle WRAPFILTER = findOwnMH("wrapFilter", MH.type(NativeNumber.class, Object.class));
    /** Method handle to retrieve the Number prototype object. */
    private static final MethodHandle PROTOFILTER = findOwnMH("protoFilter", MH.type(Object.class, Object.class));

    /** ECMA 15.7.3.2 largest positive finite value */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double MAX_VALUE = Double.MAX_VALUE;

    /** ECMA 15.7.3.3 smallest positive finite value */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double MIN_VALUE = Double.MIN_VALUE;

    /** ECMA 15.7.3.4 NaN */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double NaN = Double.NaN;

    /** ECMA 15.7.3.5 negative infinity */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

    /** ECMA 15.7.3.5 positive infinity */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;

    private final double  value;

    // initialized by nasgen
    private static PropertyMap $nasgenmap$;

    private NativeNumber(final double value, final ScriptObject proto, final PropertyMap map) {
        super(proto, map);
        this.value = value;
    }

    NativeNumber(final double value, final Global global) {
        this(value, global.getNumberPrototype(), $nasgenmap$);
    }

    private NativeNumber(final double value) {
        this(value, Global.instance());
    }


    @Override
    public String safeToString() {
        return "[Number " + toString() + "]";
    }

    @Override
    public String toString() {
        return Double.toString(getValue());
    }

    /**
     * Get the value of this Number
     * @return a {@code double} representing the Number value
     */
    public double getValue() {
        return doubleValue();
    }

    /**
     * Get the value of this Number
     * @return a {@code double} representing the Number value
     */
    public double doubleValue() {
        return value;
    }

    @Override
    public String getClassName() {
        return "Number";
    }

    /**
     * ECMA 15.7.2 - The Number constructor
     *
     * @param newObj is this Number instantiated with the new operator
     * @param self   self reference
     * @param args   value of number
     * @return the Number instance (internally represented as a {@code NativeNumber})
     */
    @jdk2.nashorn.internal.objects.annotations.Constructor(arity = 1)
    public static Object constructor(final boolean newObj, final Object self, final Object... args) {
        final double num = (args.length > 0) ? JSType.toNumber(args[0]) : 0.0;

        return newObj? new NativeNumber(num) : num;
    }

    /**
     * ECMA 15.7.4.5 Number.prototype.toFixed (fractionDigits)
     *
     * @param self           self reference
     * @param fractionDigits how many digits should be after the decimal point, 0 if undefined
     *
     * @return number in decimal fixed point notation
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static String toFixed(final Object self, final Object fractionDigits) {
        return toFixed(self, JSType.toInteger(fractionDigits));
    }

    /**
     * ECMA 15.7.4.5 Number.prototype.toFixed (fractionDigits) specialized for int fractionDigits
     *
     * @param self           self reference
     * @param fractionDigits how many digits should be after the decimal point, 0 if undefined
     *
     * @return number in decimal fixed point notation
     */
    @SpecializedFunction
    public static String toFixed(final Object self, final int fractionDigits) {
        if (fractionDigits < 0 || fractionDigits > 20) {
            throw rangeError("invalid.fraction.digits", "toFixed");
        }

        final double x = getNumberValue(self);
        if (Double.isNaN(x)) {
            return "NaN";
        }

        if (Math.abs(x) >= 1e21) {
            return JSType.toString(x);
        }

        final NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        format.setGroupingUsed(false);

        return format.format(x);
    }

    /**
     * ECMA 15.7.4.6 Number.prototype.toExponential (fractionDigits)
     *
     * @param self           self reference
     * @param fractionDigits how many digital should be after the significand's decimal point. If undefined, use as many as necessary to uniquely specify number.
     *
     * @return number in decimal exponential notation
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static String toExponential(final Object self, final Object fractionDigits) {
        final double  x         = getNumberValue(self);
        final boolean trimZeros = fractionDigits == UNDEFINED;
        final int     f         = trimZeros ? 16 : JSType.toInteger(fractionDigits);

        if (Double.isNaN(x)) {
            return "NaN";
        } else if (Double.isInfinite(x)) {
            return x > 0? "Infinity" : "-Infinity";
        }

        if (fractionDigits != UNDEFINED && (f < 0 || f > 20)) {
            throw rangeError("invalid.fraction.digits", "toExponential");
        }

        final String res = String.format(Locale.US, "%1." + f + "e", x);
        return fixExponent(res, trimZeros);
    }

    /**
     * ECMA 15.7.4.7 Number.prototype.toPrecision (precision)
     *
     * @param self      self reference
     * @param precision use {@code precision - 1} digits after the significand's decimal point or call {@link JSType#toString} if undefined
     *
     * @return number in decimal exponentiation notation or decimal fixed notation depending on {@code precision}
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static String toPrecision(final Object self, final Object precision) {
        final double x = getNumberValue(self);
        if (precision == UNDEFINED) {
            return JSType.toString(x);
        }
        return (toPrecision(x, JSType.toInteger(precision)));
    }

    /**
     * ECMA 15.7.4.7 Number.prototype.toPrecision (precision) specialized f
     *
     * @param self      self reference
     * @param precision use {@code precision - 1} digits after the significand's decimal point.
     *
     * @return number in decimal exponentiation notation or decimal fixed notation depending on {@code precision}
     */
    @SpecializedFunction
    public static String toPrecision(final Object self, final int precision) {
        return toPrecision(getNumberValue(self), precision);
    }

    private static String toPrecision(final double x, final int p) {
        if (Double.isNaN(x)) {
            return "NaN";
        } else if (Double.isInfinite(x)) {
            return x > 0? "Infinity" : "-Infinity";
        }

        if (p < 1 || p > 21) {
            throw rangeError("invalid.precision");
        }

        // workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6469160
        if (x == 0.0 && p <= 1) {
            return "0";
        }

        return fixExponent(String.format(Locale.US, "%." + p + "g", x), false);
    }

    /**
     * ECMA 15.7.4.2 Number.prototype.toString ( [ radix ] )
     *
     * @param self  self reference
     * @param radix radix to use for string conversion
     * @return string representation of this Number in the given radix
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static String toString(final Object self, final Object radix) {
        if (radix != UNDEFINED) {
            final int intRadix = JSType.toInteger(radix);
            if (intRadix != 10) {
                if (intRadix < 2 || intRadix > 36) {
                    throw rangeError("invalid.radix");
                }
                return JSType.toString(getNumberValue(self), intRadix);
            }
        }

        return JSType.toString(getNumberValue(self));
    }

    /**
     * ECMA 15.7.4.3 Number.prototype.toLocaleString()
     *
     * @param self self reference
     * @return localized string for this Number
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static String toLocaleString(final Object self) {
        return JSType.toString(getNumberValue(self));
    }


    /**
     * ECMA 15.7.4.4 Number.prototype.valueOf ( )
     *
     * @param self self reference
     * @return number value for this Number
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static double valueOf(final Object self) {
        return getNumberValue(self);
    }

    /**
     * Lookup the appropriate method for an invoke dynamic call.
     * @param request  The link request
     * @param receiver receiver of call
     * @return Link to be invoked at call site.
     */
    public static GuardedInvocation lookupPrimitive(final LinkRequest request, final Object receiver) {
        return PrimitiveLookup.lookupPrimitive(request, Number.class, new NativeNumber(((Number)receiver).doubleValue()), WRAPFILTER, PROTOFILTER);
    }

    @SuppressWarnings("unused")
    private static NativeNumber wrapFilter(final Object receiver) {
        return new NativeNumber(((Number)receiver).doubleValue());
    }

    @SuppressWarnings("unused")
    private static Object protoFilter(final Object object) {
        return Global.instance().getNumberPrototype();
    }

    private static double getNumberValue(final Object self) {
        if (self instanceof Number) {
            return ((Number)self).doubleValue();
        } else if (self instanceof NativeNumber) {
            return ((NativeNumber)self).getValue();
        } else if (self != null && self == Global.instance().getNumberPrototype()) {
            return 0.0;
        } else {
            throw typeError("not.a.number", ScriptRuntime.safeToString(self));
        }
    }

    // Exponent of Java "e" or "E" formatter is always 2 digits and zero
    // padded if needed (e+01, e+00, e+12 etc.) JS expects exponent to contain
    // exact number of digits e+1, e+0, e+12 etc. Fix the exponent here.
    //
    // Additionally, if trimZeros is true, this cuts trailing zeros in the
    // fraction part for calls to toExponential() with undefined fractionDigits
    // argument.
    private static String fixExponent(final String str, final boolean trimZeros) {
        final int index = str.indexOf('e');
        if (index < 1) {
            // no exponent, do nothing..
            return str;
        }

        // check if character after e+ or e- is 0
        final int expPadding = str.charAt(index + 2) == '0' ? 3 : 2;
        // check if there are any trailing zeroes we should remove

        int fractionOffset = index;
        if (trimZeros) {
            assert fractionOffset > 0;
            char c = str.charAt(fractionOffset - 1);
            while (fractionOffset > 1 && (c == '0' || c == '.')) {
                c = str.charAt(--fractionOffset - 1);
            }

        }
        // if anything needs to be done compose a new string
        if (fractionOffset < index || expPadding == 3) {
            return str.substring(0, fractionOffset)
                    + str.substring(index, index + 2)
                    + str.substring(index + expPadding);
        }
        return str;
    }

    private static MethodHandle findOwnMH(final String name, final MethodType type) {
        return MH.findStatic(MethodHandles.lookup(), NativeNumber.class, name, type);
    }

    static {
            final List<jdk2.nashorn.internal.runtime.Property> list = Collections.emptyList();
            $nasgenmap$ = PropertyMap.newMap(list);
    }

    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(NativeNumber.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static final class Constructor extends ScriptFunctionImpl {
        private static final PropertyMap $nasgenmap$;

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(6);
            list.add(AccessorProperty.create("MAX_VALUE", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$MAX_VALUE", double.class),
null));
            list.add(AccessorProperty.create("MIN_VALUE", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$MIN_VALUE", double.class),
null));
            list.add(AccessorProperty.create("NaN", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$NaN", double.class),
null));
            list.add(AccessorProperty.create("NEGATIVE_INFINITY", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$NEGATIVE_INFINITY", double.class),
null));
            list.add(AccessorProperty.create("POSITIVE_INFINITY", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$POSITIVE_INFINITY", double.class),
null));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Constructor() {
            super("Number", 
                    staticHandle("constructor", Object.class, boolean.class, Object.class, Object[].class),
                    $nasgenmap$, null);
            final Prototype prototype = new Prototype();
            PrototypeObject.setConstructor(prototype, this);
            setPrototype(prototype);
        }

        public double G$MAX_VALUE() {
            return NativeNumber.MAX_VALUE;
         }

         public double G$MIN_VALUE() {
            return NativeNumber.MIN_VALUE;
         }

         public double G$NaN() {
            return NativeNumber.NaN;
         }

         public double G$NEGATIVE_INFINITY() {
            return NativeNumber.NEGATIVE_INFINITY;
         }

         public double G$POSITIVE_INFINITY() {
            return NativeNumber.POSITIVE_INFINITY;
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
        private ScriptFunction toFixed;
        private ScriptFunction toExponential;
        private ScriptFunction toPrecision;
        private ScriptFunction toString;
        private ScriptFunction toLocaleString;
        private ScriptFunction valueOf;
        private static final PropertyMap $nasgenmap$;

        public ScriptFunction G$toFixed() {
            return this.toFixed;
        }

        public void S$toFixed(final ScriptFunction function) {
            this.toFixed = function;
        }

        public ScriptFunction G$toExponential() {
            return this.toExponential;
        }

        public void S$toExponential(final ScriptFunction function) {
            this.toExponential = function;
        }

        public ScriptFunction G$toPrecision() {
            return this.toPrecision;
        }

        public void S$toPrecision(final ScriptFunction function) {
            this.toPrecision = function;
        }

        public ScriptFunction G$toString() {
            return this.toString;
        }

        public void S$toString(final ScriptFunction function) {
            this.toString = function;
        }

        public ScriptFunction G$toLocaleString() {
            return this.toLocaleString;
        }

        public void S$toLocaleString(final ScriptFunction function) {
            this.toLocaleString = function;
        }

        public ScriptFunction G$valueOf() {
            return this.valueOf;
        }

        public void S$valueOf(final ScriptFunction function) {
            this.valueOf = function;
        }

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(7);
            list.add(AccessorProperty.create("toFixed", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$toFixed", ScriptFunction.class),
                    virtualHandle("S$toFixed", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("toExponential", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$toExponential", ScriptFunction.class),
                    virtualHandle("S$toExponential", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("toPrecision", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$toPrecision", ScriptFunction.class),
                    virtualHandle("S$toPrecision", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("toString", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$toString", ScriptFunction.class),
                    virtualHandle("S$toString", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("toLocaleString", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$toLocaleString", ScriptFunction.class),
                    virtualHandle("S$toLocaleString", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("valueOf", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$valueOf", ScriptFunction.class),
                    virtualHandle("S$valueOf", void.class, ScriptFunction.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Prototype() {
            super($nasgenmap$);
            toFixed = ScriptFunctionImpl.makeFunction("toFixed",
                    staticHandle("toFixed", String.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("toFixed", String.class, Object.class, int.class), false)
                    });
            toExponential = ScriptFunctionImpl.makeFunction("toExponential",
                    staticHandle("toExponential", String.class, Object.class, Object.class));
            toPrecision = ScriptFunctionImpl.makeFunction("toPrecision",
                    staticHandle("toPrecision", String.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("toPrecision", String.class, Object.class, int.class), false)
                    });
            toString = ScriptFunctionImpl.makeFunction("toString",
                    staticHandle("toString", String.class, Object.class, Object.class));
            toLocaleString = ScriptFunctionImpl.makeFunction("toLocaleString",
                    staticHandle("toLocaleString", String.class, Object.class));
            valueOf = ScriptFunctionImpl.makeFunction("valueOf",
                    staticHandle("valueOf", double.class, Object.class));
        }

       public String getClassName() {
           return "Number";
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
