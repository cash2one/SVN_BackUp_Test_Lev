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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk2.nashorn.internal.objects.annotations.Attribute;
import jdk2.nashorn.internal.objects.annotations.Function;
import jdk2.nashorn.internal.objects.annotations.Property;
import jdk2.nashorn.internal.objects.annotations.ScriptClass;
import jdk2.nashorn.internal.objects.annotations.SpecializedFunction;
import jdk2.nashorn.internal.objects.annotations.Where;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.JSType;
//import jdk2.nashorn.internal.runtime.Property;
import jdk2.nashorn.internal.runtime.PropertyMap;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.runtime.ScriptObject;
import jdk2.nashorn.internal.runtime.Specialization;

/**
 * ECMA 15.8 The Math Object
 *
 */
@ScriptClass("Math")
public final class NativeMath extends ScriptObject {

    // initialized by nasgen
    @SuppressWarnings("unused")
    private static PropertyMap $nasgenmap$;

    private NativeMath() {
        // don't create me!
        throw new UnsupportedOperationException();
    }

    /** ECMA 15.8.1.1 - E, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double E = Math.E;

    /** ECMA 15.8.1.2 - LN10, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double LN10 = 2.302585092994046;

    /** ECMA 15.8.1.3 - LN2, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double LN2 = 0.6931471805599453;

    /** ECMA 15.8.1.4 - LOG2E, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double LOG2E = 1.4426950408889634;

    /** ECMA 15.8.1.5 - LOG10E, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double LOG10E = 0.4342944819032518;

    /** ECMA 15.8.1.6 - PI, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double PI = Math.PI;

    /** ECMA 15.8.1.7 - SQRT1_2, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double SQRT1_2 = 0.7071067811865476;

    /** ECMA 15.8.1.8 - SQRT2, always a double constant. Not writable or configurable */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT, where = Where.CONSTRUCTOR)
    public static final double SQRT2 = 1.4142135623730951;

    /**
     * ECMA 15.8.2.1 abs(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return abs of value
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double abs(final Object self, final Object x) {
        return Math.abs(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.1 abs(x) - specialization for int values
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return abs of argument
     */
    @SpecializedFunction
    public static int abs(final Object self, final int x) {
        return Math.abs(x);
    }

    /**
     * ECMA 15.8.2.1 abs(x) - specialization for long values
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return abs of argument
     */
    @SpecializedFunction
    public static long abs(final Object self, final long x) {
        return Math.abs(x);
    }

    /**
     * ECMA 15.8.2.1 abs(x) - specialization for double values
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return abs of argument
     */
    @SpecializedFunction
    public static double abs(final Object self, final double x) {
        return Math.abs(x);
    }

    /**
     * ECMA 15.8.2.2 acos(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return acos of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double acos(final Object self, final Object x) {
        return Math.acos(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.2 acos(x) - specialization for double values
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return acos of argument
     */
    @SpecializedFunction
    public static double acos(final Object self, final double x) {
        return Math.acos(x);
    }

    /**
     * ECMA 15.8.2.3 asin(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return asin of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double asin(final Object self, final Object x) {
        return Math.asin(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.3 asin(x) - specialization for double values
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return asin of argument
     */
    @SpecializedFunction
    public static double asin(final Object self, final double x) {
        return Math.asin(x);
    }

    /**
     * ECMA 15.8.2.4 atan(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return atan of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double atan(final Object self, final Object x) {
        return Math.atan(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.4 atan(x) - specialization for double values
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return atan of argument
     */
    @SpecializedFunction
    public static double atan(final Object self, final double x) {
        return Math.atan(x);
    }

    /**
     * ECMA 15.8.2.5 atan2(x,y)
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return atan2 of x and y
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double atan2(final Object self, final Object y, final Object x) {
        return Math.atan2(JSType.toNumber(y), JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.5 atan2(x,y) - specialization for double values
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return atan2 of x and y
     */
    @SpecializedFunction
    public static double atan2(final Object self, final double y, final double x) {
        return Math.atan2(y,x);
    }

    /**
     * ECMA 15.8.2.6 ceil(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return ceil of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double ceil(final Object self, final Object x) {
        return Math.ceil(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.6 ceil(x) - specialized version for ints
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return ceil of argument
     */
    @SpecializedFunction
    public static int ceil(final Object self, final int x) {
        return x;
    }

    /**
     * ECMA 15.8.2.6 ceil(x) - specialized version for longs
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return ceil of argument
     */
    @SpecializedFunction
    public static long ceil(final Object self, final long x) {
        return x;
    }

    /**
     * ECMA 15.8.2.6 ceil(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return ceil of argument
     */
    @SpecializedFunction
    public static double ceil(final Object self, final double x) {
        return Math.ceil(x);
    }

    /**
     * ECMA 15.8.2.7 cos(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return cos of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double cos(final Object self, final Object x) {
        return Math.cos(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.7 cos(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return cos of argument
     */
    @SpecializedFunction
    public static double cos(final Object self, final double x) {
        return Math.cos(x);
    }

    /**
     * ECMA 15.8.2.8 exp(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return exp of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double exp(final Object self, final Object x) {
        return Math.exp(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.9 floor(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return floor of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double floor(final Object self, final Object x) {
        return Math.floor(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.9 floor(x) - specialized version for ints
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return floor of argument
     */
    @SpecializedFunction
    public static int floor(final Object self, final int x) {
        return x;
    }

    /**
     * ECMA 15.8.2.9 floor(x) - specialized version for longs
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return floor of argument
     */
    @SpecializedFunction
    public static long floor(final Object self, final long x) {
        return x;
    }

    /**
     * ECMA 15.8.2.9 floor(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return floor of argument
     */
    @SpecializedFunction
    public static double floor(final Object self, final double x) {
        return Math.floor(x);
    }

    /**
     * ECMA 15.8.2.10 log(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return log of argument
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double log(final Object self, final Object x) {
        return Math.log(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.10 log(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return log of argument
     */
    @SpecializedFunction
    public static double log(final Object self, final double x) {
        return Math.log(x);
    }

    /**
     * ECMA 15.8.2.11 max(x)
     *
     * @param self  self reference
     * @param args  arguments
     *
     * @return the largest of the arguments, {@link Double#NEGATIVE_INFINITY} if no args given, or identity if one arg is given
     */
    @Function(arity = 2, attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double max(final Object self, final Object... args) {
        switch (args.length) {
        case 0:
            return Double.NEGATIVE_INFINITY;
        case 1:
            return JSType.toNumber(args[0]);
        default:
            double res = JSType.toNumber(args[0]);
            for (int i = 1; i < args.length; i++) {
                res = Math.max(res, JSType.toNumber(args[i]));
            }
            return res;
        }
    }

    /**
     * ECMA 15.8.2.11 max(x) - specialized no args version
     *
     * @param self  self reference
     *
     * @return {@link Double#NEGATIVE_INFINITY}
     */
    @SpecializedFunction
    public static double max(final Object self) {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * ECMA 15.8.2.11 max(x) - specialized version for ints
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return largest value of x and y
     */
    @SpecializedFunction
    public static int max(final Object self, final int x, final int y) {
        return Math.max(x, y);
    }

    /**
     * ECMA 15.8.2.11 max(x) - specialized version for longs
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return largest value of x and y
     */
    @SpecializedFunction
    public static long max(final Object self, final long x, final long y) {
        return Math.max(x, y);
    }

    /**
     * ECMA 15.8.2.11 max(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return largest value of x and y
     */
    @SpecializedFunction
    public static double max(final Object self, final double x, final double y) {
        return Math.max(x, y);
    }

    /**
     * ECMA 15.8.2.11 max(x) - specialized version for two Object args
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return largest value of x and y
     */
    @SpecializedFunction
    public static double max(final Object self, final Object x, final Object y) {
        return Math.max(JSType.toNumber(x), JSType.toNumber(y));
    }

    /**
     * ECMA 15.8.2.12 min(x)
     *
     * @param self  self reference
     * @param args  arguments
     *
     * @return the smallest of the arguments, {@link Double#NEGATIVE_INFINITY} if no args given, or identity if one arg is given
     */
    @Function(arity = 2, attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double min(final Object self, final Object... args) {
        switch (args.length) {
        case 0:
            return Double.POSITIVE_INFINITY;
        case 1:
            return JSType.toNumber(args[0]);
        default:
            double res = JSType.toNumber(args[0]);
            for (int i = 1; i < args.length; i++) {
                res = Math.min(res, JSType.toNumber(args[i]));
            }
            return res;
        }
    }

    /**
     * ECMA 15.8.2.11 min(x) - specialized no args version
     *
     * @param self  self reference
     *
     * @return {@link Double#POSITIVE_INFINITY}
     */
    @SpecializedFunction
    public static double min(final Object self) {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * ECMA 15.8.2.12 min(x) - specialized version for ints
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return smallest value of x and y
     */
    @SpecializedFunction
    public static int min(final Object self, final int x, final int y) {
        return Math.min(x, y);
    }

    /**
     * ECMA 15.8.2.12 min(x) - specialized version for longs
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return smallest value of x and y
     */
    @SpecializedFunction
    public static long min(final Object self, final long x, final long y) {
        return Math.min(x, y);
    }

    /**
     * ECMA 15.8.2.12 min(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return smallest value of x and y
     */
    @SpecializedFunction
    public static double min(final Object self, final double x, final double y) {
        return Math.min(x, y);
    }

    /**
     * ECMA 15.8.2.12 min(x) - specialized version for two Object args
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return smallest value of x and y
     */
    @SpecializedFunction
    public static double min(final Object self, final Object x, final Object y) {
        return Math.min(JSType.toNumber(x), JSType.toNumber(y));
    }

    /**
     * ECMA 15.8.2.13 pow(x,y)
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return x raised to the power of y
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double pow(final Object self, final Object x, final Object y) {
        return Math.pow(JSType.toNumber(x), JSType.toNumber(y));
    }

    /**
     * ECMA 15.8.2.13 pow(x,y) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     first argument
     * @param y     second argument
     *
     * @return x raised to the power of y
     */
    @SpecializedFunction
    public static double pow(final Object self, final double x, final double y) {
        return Math.pow(x, y);
    }

    /**
     * ECMA 15.8.2.14 random()
     *
     * @param self  self reference
     *
     * @return random number in the range [0..1)
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double random(final Object self) {
        return Math.random();
    }

    /**
     * ECMA 15.8.2.15 round(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return x rounded
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double round(final Object self, final Object x) {
        final double d = JSType.toNumber(x);
        if (Math.getExponent(d) >= 52) {
            return d;
        }
        return Math.copySign(Math.floor(d + 0.5), d);
    }

    /**
     * ECMA 15.8.2.16 sin(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return sin of x
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double sin(final Object self, final Object x) {
        return Math.sin(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.16 sin(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return sin of x
     */
    @SpecializedFunction
    public static double sin(final Object self, final double x) {
        return Math.sin(x);
    }

    /**
     * ECMA 15.8.2.17 sqrt(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return sqrt of x
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static double sqrt(final Object self, final Object x) {
        return Math.sqrt(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.17 sqrt(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return sqrt of x
     */
    @SpecializedFunction
    public static double sqrt(final Object self, final double x) {
        return Math.sqrt(x);
    }

    /**
     * ECMA 15.8.2.18 tan(x)
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return tan of x
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where=Where.CONSTRUCTOR)
    public static double tan(final Object self, final Object x) {
        return Math.tan(JSType.toNumber(x));
    }

    /**
     * ECMA 15.8.2.18 tan(x) - specialized version for doubles
     *
     * @param self  self reference
     * @param x     argument
     *
     * @return tan of x
     */
    @SpecializedFunction
    public static double tan(final Object self, final double x) {
        return Math.tan(x);
    }

    static {
            final List<jdk2.nashorn.internal.runtime.Property> list = Collections.emptyList();
            $nasgenmap$ = PropertyMap.newMap(list);
    }

    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(NativeMath.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static final class Constructor extends ScriptObject {
        private ScriptFunction abs;
        private ScriptFunction acos;
        private ScriptFunction asin;
        private ScriptFunction atan;
        private ScriptFunction atan2;
        private ScriptFunction ceil;
        private ScriptFunction cos;
        private ScriptFunction exp;
        private ScriptFunction floor;
        private ScriptFunction log;
        private ScriptFunction max;
        private ScriptFunction min;
        private ScriptFunction pow;
        private ScriptFunction random;
        private ScriptFunction round;
        private ScriptFunction sin;
        private ScriptFunction sqrt;
        private ScriptFunction tan;
        private static final PropertyMap $nasgenmap$;

        public ScriptFunction G$abs() {
            return this.abs;
        }

        public void S$abs(final ScriptFunction function) {
            this.abs = function;
        }

        public ScriptFunction G$acos() {
            return this.acos;
        }

        public void S$acos(final ScriptFunction function) {
            this.acos = function;
        }

        public ScriptFunction G$asin() {
            return this.asin;
        }

        public void S$asin(final ScriptFunction function) {
            this.asin = function;
        }

        public ScriptFunction G$atan() {
            return this.atan;
        }

        public void S$atan(final ScriptFunction function) {
            this.atan = function;
        }

        public ScriptFunction G$atan2() {
            return this.atan2;
        }

        public void S$atan2(final ScriptFunction function) {
            this.atan2 = function;
        }

        public ScriptFunction G$ceil() {
            return this.ceil;
        }

        public void S$ceil(final ScriptFunction function) {
            this.ceil = function;
        }

        public ScriptFunction G$cos() {
            return this.cos;
        }

        public void S$cos(final ScriptFunction function) {
            this.cos = function;
        }

        public ScriptFunction G$exp() {
            return this.exp;
        }

        public void S$exp(final ScriptFunction function) {
            this.exp = function;
        }

        public ScriptFunction G$floor() {
            return this.floor;
        }

        public void S$floor(final ScriptFunction function) {
            this.floor = function;
        }

        public ScriptFunction G$log() {
            return this.log;
        }

        public void S$log(final ScriptFunction function) {
            this.log = function;
        }

        public ScriptFunction G$max() {
            return this.max;
        }

        public void S$max(final ScriptFunction function) {
            this.max = function;
        }

        public ScriptFunction G$min() {
            return this.min;
        }

        public void S$min(final ScriptFunction function) {
            this.min = function;
        }

        public ScriptFunction G$pow() {
            return this.pow;
        }

        public void S$pow(final ScriptFunction function) {
            this.pow = function;
        }

        public ScriptFunction G$random() {
            return this.random;
        }

        public void S$random(final ScriptFunction function) {
            this.random = function;
        }

        public ScriptFunction G$round() {
            return this.round;
        }

        public void S$round(final ScriptFunction function) {
            this.round = function;
        }

        public ScriptFunction G$sin() {
            return this.sin;
        }

        public void S$sin(final ScriptFunction function) {
            this.sin = function;
        }

        public ScriptFunction G$sqrt() {
            return this.sqrt;
        }

        public void S$sqrt(final ScriptFunction function) {
            this.sqrt = function;
        }

        public ScriptFunction G$tan() {
            return this.tan;
        }

        public void S$tan(final ScriptFunction function) {
            this.tan = function;
        }

        public double G$E() {
            return NativeMath.E;
         }

         public double G$LN10() {
            return NativeMath.LN10;
         }

         public double G$LN2() {
            return NativeMath.LN2;
         }

         public double G$LOG2E() {
            return NativeMath.LOG2E;
         }

         public double G$LOG10E() {
            return NativeMath.LOG10E;
         }

         public double G$PI() {
            return NativeMath.PI;
         }

         public double G$SQRT1_2() {
            return NativeMath.SQRT1_2;
         }

         public double G$SQRT2() {
            return NativeMath.SQRT2;
         }

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(26);
            list.add(AccessorProperty.create("E", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$E", double.class),
null));
            list.add(AccessorProperty.create("LN10", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$LN10", double.class),
null));
            list.add(AccessorProperty.create("LN2", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$LN2", double.class),
null));
            list.add(AccessorProperty.create("LOG2E", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$LOG2E", double.class),
null));
            list.add(AccessorProperty.create("LOG10E", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$LOG10E", double.class),
null));
            list.add(AccessorProperty.create("PI", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$PI", double.class),
null));
            list.add(AccessorProperty.create("SQRT1_2", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$SQRT1_2", double.class),
null));
            list.add(AccessorProperty.create("SQRT2", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$SQRT2", double.class),
null));
            list.add(AccessorProperty.create("abs", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$abs", ScriptFunction.class),
                    virtualHandle("S$abs", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("acos", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$acos", ScriptFunction.class),
                    virtualHandle("S$acos", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("asin", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$asin", ScriptFunction.class),
                    virtualHandle("S$asin", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("atan", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$atan", ScriptFunction.class),
                    virtualHandle("S$atan", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("atan2", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$atan2", ScriptFunction.class),
                    virtualHandle("S$atan2", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("ceil", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$ceil", ScriptFunction.class),
                    virtualHandle("S$ceil", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("cos", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$cos", ScriptFunction.class),
                    virtualHandle("S$cos", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("exp", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$exp", ScriptFunction.class),
                    virtualHandle("S$exp", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("floor", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$floor", ScriptFunction.class),
                    virtualHandle("S$floor", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("log", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$log", ScriptFunction.class),
                    virtualHandle("S$log", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("max", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$max", ScriptFunction.class),
                    virtualHandle("S$max", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("min", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$min", ScriptFunction.class),
                    virtualHandle("S$min", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("pow", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$pow", ScriptFunction.class),
                    virtualHandle("S$pow", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("random", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$random", ScriptFunction.class),
                    virtualHandle("S$random", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("round", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$round", ScriptFunction.class),
                    virtualHandle("S$round", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("sin", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$sin", ScriptFunction.class),
                    virtualHandle("S$sin", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("sqrt", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$sqrt", ScriptFunction.class),
                    virtualHandle("S$sqrt", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("tan", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$tan", ScriptFunction.class),
                    virtualHandle("S$tan", void.class, ScriptFunction.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Constructor() {
            super($nasgenmap$);
            abs = ScriptFunctionImpl.makeFunction("abs",
                    staticHandle("abs", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("abs", int.class, Object.class, int.class), false),
                        new Specialization(staticHandle("abs", long.class, Object.class, long.class), false),
                        new Specialization(staticHandle("abs", double.class, Object.class, double.class), false)
                    });
            acos = ScriptFunctionImpl.makeFunction("acos",
                    staticHandle("acos", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("acos", double.class, Object.class, double.class), false)
                    });
            asin = ScriptFunctionImpl.makeFunction("asin",
                    staticHandle("asin", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("asin", double.class, Object.class, double.class), false)
                    });
            atan = ScriptFunctionImpl.makeFunction("atan",
                    staticHandle("atan", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("atan", double.class, Object.class, double.class), false)
                    });
            atan2 = ScriptFunctionImpl.makeFunction("atan2",
                    staticHandle("atan2", double.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("atan2", double.class, Object.class, double.class, double.class), false)
                    });
            ceil = ScriptFunctionImpl.makeFunction("ceil",
                    staticHandle("ceil", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("ceil", int.class, Object.class, int.class), false),
                        new Specialization(staticHandle("ceil", long.class, Object.class, long.class), false),
                        new Specialization(staticHandle("ceil", double.class, Object.class, double.class), false)
                    });
            cos = ScriptFunctionImpl.makeFunction("cos",
                    staticHandle("cos", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("cos", double.class, Object.class, double.class), false)
                    });
            exp = ScriptFunctionImpl.makeFunction("exp",
                    staticHandle("exp", double.class, Object.class, Object.class));
            floor = ScriptFunctionImpl.makeFunction("floor",
                    staticHandle("floor", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("floor", int.class, Object.class, int.class), false),
                        new Specialization(staticHandle("floor", long.class, Object.class, long.class), false),
                        new Specialization(staticHandle("floor", double.class, Object.class, double.class), false)
                    });
            log = ScriptFunctionImpl.makeFunction("log",
                    staticHandle("log", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("log", double.class, Object.class, double.class), false)
                    });
            max = ScriptFunctionImpl.makeFunction("max",
                    staticHandle("max", double.class, Object.class, Object[].class), new Specialization[] {
                        new Specialization(staticHandle("max", double.class, Object.class), false),
                        new Specialization(staticHandle("max", int.class, Object.class, int.class, int.class), false),
                        new Specialization(staticHandle("max", long.class, Object.class, long.class, long.class), false),
                        new Specialization(staticHandle("max", double.class, Object.class, double.class, double.class), false),
                        new Specialization(staticHandle("max", double.class, Object.class, Object.class, Object.class), false)
                    });
            max.setArity(2);
            min = ScriptFunctionImpl.makeFunction("min",
                    staticHandle("min", double.class, Object.class, Object[].class), new Specialization[] {
                        new Specialization(staticHandle("min", double.class, Object.class), false),
                        new Specialization(staticHandle("min", int.class, Object.class, int.class, int.class), false),
                        new Specialization(staticHandle("min", long.class, Object.class, long.class, long.class), false),
                        new Specialization(staticHandle("min", double.class, Object.class, double.class, double.class), false),
                        new Specialization(staticHandle("min", double.class, Object.class, Object.class, Object.class), false)
                    });
            min.setArity(2);
            pow = ScriptFunctionImpl.makeFunction("pow",
                    staticHandle("pow", double.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("pow", double.class, Object.class, double.class, double.class), false)
                    });
            random = ScriptFunctionImpl.makeFunction("random",
                    staticHandle("random", double.class, Object.class));
            round = ScriptFunctionImpl.makeFunction("round",
                    staticHandle("round", double.class, Object.class, Object.class));
            sin = ScriptFunctionImpl.makeFunction("sin",
                    staticHandle("sin", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("sin", double.class, Object.class, double.class), false)
                    });
            sqrt = ScriptFunctionImpl.makeFunction("sqrt",
                    staticHandle("sqrt", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("sqrt", double.class, Object.class, double.class), false)
                    });
            tan = ScriptFunctionImpl.makeFunction("tan",
                    staticHandle("tan", double.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("tan", double.class, Object.class, double.class), false)
                    });
        }

       public String getClassName() {
           return "Math";
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
        Prototype() {
        }

       public String getClassName() {
           return "Math";
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
