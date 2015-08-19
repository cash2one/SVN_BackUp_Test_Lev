/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import static jdk2.nashorn.internal.runtime.ECMAErrors.rangeError;
import static jdk2.nashorn.internal.runtime.ECMAErrors.typeError;
import static jdk2.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import jdk2.nashorn.internal.objects.annotations.Attribute;
import jdk2.nashorn.internal.objects.annotations.Function;
import jdk2.nashorn.internal.objects.annotations.Property;
import jdk2.nashorn.internal.objects.annotations.ScriptClass;
import jdk2.nashorn.internal.objects.annotations.SpecializedFunction;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.JSType;
import jdk2.nashorn.internal.runtime.PropertyMap;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.runtime.ScriptObject;
import jdk2.nashorn.internal.runtime.ScriptRuntime;
import jdk2.nashorn.internal.runtime.Specialization;

/**
 * <p>
 * DataView builtin constructor. Based on the specification here:
 * http://www.khronos.org/registry/typedarray/specs/latest/#8
 * </p>
 * <p>
 * An ArrayBuffer is a useful object for representing an arbitrary chunk of data.
 * In many cases, such data will be read from disk or from the network, and will
 * not follow the alignment restrictions that are imposed on the typed array views
 * described earlier. In addition, the data will often be heterogeneous in nature
 * and have a defined byte order. The DataView view provides a low-level interface
 * for reading such data from and writing it to an ArrayBuffer.
 * </p>
 * <p>
 * Regardless of the host computer's endianness, DataView reads or writes values
 * to or from main memory with a specified endianness: big or little.
 * </p>
 */
@ScriptClass("DataView")
public class NativeDataView extends ScriptObject {
    // initialized by nasgen
    private static PropertyMap $nasgenmap$;

    // inherited ArrayBufferView properties

    /**
     * Underlying ArrayBuffer storage object
     */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT)
    public final Object buffer;

    /**
     * The offset in bytes from the start of the ArrayBuffer
     */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT)
    public final int byteOffset;

    /**
     * The number of bytes from the offset that this DataView will reference
     */
    @Property(attributes = Attribute.NON_ENUMERABLE_CONSTANT)
    public final int byteLength;

    // underlying ByteBuffer
    private final ByteBuffer buf;

    private NativeDataView(final NativeArrayBuffer arrBuf) {
        this(arrBuf, arrBuf.getBuffer(), 0);
    }

    private NativeDataView(final NativeArrayBuffer arrBuf, final int offset) {
        this(arrBuf, bufferFrom(arrBuf, offset), offset);
    }

    private NativeDataView(final NativeArrayBuffer arrBuf, final int offset, final int length) {
        this(arrBuf, bufferFrom(arrBuf, offset, length), offset, length);
    }

    private NativeDataView(final NativeArrayBuffer arrBuf, final ByteBuffer buf, final int offset) {
       this(arrBuf, buf, offset, buf.capacity() - offset);
    }

    private NativeDataView(final NativeArrayBuffer arrBuf, final ByteBuffer buf, final int offset, final int length) {
        super(Global.instance().getDataViewPrototype(), $nasgenmap$);
        this.buffer     = arrBuf;
        this.byteOffset = offset;
        this.byteLength = length;
        this.buf        = buf;
    }

    /**
     * Create a new DataView object using the passed ArrayBuffer for its
     * storage. Optional byteOffset and byteLength can be used to limit the
     * section of the buffer referenced. The byteOffset indicates the offset in
     * bytes from the start of the ArrayBuffer, and the byteLength is the number
     * of bytes from the offset that this DataView will reference. If both
     * byteOffset and byteLength are omitted, the DataView spans the entire
     * ArrayBuffer range. If the byteLength is omitted, the DataView extends from
     * the given byteOffset until the end of the ArrayBuffer.
     *
     * If the given byteOffset and byteLength references an area beyond the end
     * of the ArrayBuffer an exception is raised.

     * @param newObj if this constructor was invoked with 'new' or not
     * @param self   constructor function object
     * @param args   arguments to the constructor
     * @return newly constructed DataView object
     */
    @jdk2.nashorn.internal.objects.annotations.Constructor(arity = 1)
    public static NativeDataView constructor(final boolean newObj, final Object self, final Object... args) {
        if (args.length == 0 || !(args[0] instanceof NativeArrayBuffer)) {
            throw typeError("not.an.arraybuffer.in.dataview");
        }

        final NativeArrayBuffer arrBuf = (NativeArrayBuffer)args[0];
        switch (args.length) {
        case 1:
            return new NativeDataView(arrBuf);
        case 2:
            return new NativeDataView(arrBuf, JSType.toInt32(args[1]));
        default:
            return new NativeDataView(arrBuf, JSType.toInt32(args[1]), JSType.toInt32(args[2]));
        }
    }

    /**
     * Specialized version of DataView constructor
     *
     * @param newObj if this constructor was invoked with 'new' or not
     * @param self   constructor function object
     * @param arrBuf underlying ArrayBuffer storage object
     * @param offset offset in bytes from the start of the ArrayBuffer
     * @return newly constructed DataView object
     */
    @SpecializedFunction(isConstructor=true)
    public static NativeDataView constructor(final boolean newObj, final Object self, final Object arrBuf, final int offset) {
        if (!(arrBuf instanceof NativeArrayBuffer)) {
            throw typeError("not.an.arraybuffer.in.dataview");
        }
        return new NativeDataView((NativeArrayBuffer) arrBuf, offset);
    }

    /**
     * Specialized version of DataView constructor
     *
     * @param newObj if this constructor was invoked with 'new' or not
     * @param self   constructor function object
     * @param arrBuf underlying ArrayBuffer storage object
     * @param offset in bytes from the start of the ArrayBuffer
     * @param length is the number of bytes from the offset that this DataView will reference
     * @return newly constructed DataView object
     */
    @SpecializedFunction(isConstructor=true)
    public static NativeDataView constructor(final boolean newObj, final Object self, final Object arrBuf, final int offset, final int length) {
        if (!(arrBuf instanceof NativeArrayBuffer)) {
            throw typeError("not.an.arraybuffer.in.dataview");
        }
        return new NativeDataView((NativeArrayBuffer) arrBuf, offset, length);
    }

    // Gets the value of the given type at the specified byte offset
    // from the start of the view. There is no alignment constraint;
    // multi-byte values may be fetched from any offset.
    //
    // For multi-byte values, the optional littleEndian argument
    // indicates whether a big-endian or little-endian value should be
    // read. If false or undefined, a big-endian value is read.
    //
    // These methods raise an exception if they would read
    // beyond the end of the view.

    /**
     * Get 8-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 8-bit signed int value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static int getInt8(final Object self, final Object byteOffset) {
        try {
            return getBuffer(self).get(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 8-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 8-bit signed int value at the byteOffset
     */
    @SpecializedFunction
    public static int getInt8(final Object self, final int byteOffset) {
        try {
            return getBuffer(self).get(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 8-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 8-bit unsigned int value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static int getUint8(final Object self, final Object byteOffset) {
        try {
            return 0xFF & getBuffer(self).get(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 8-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 8-bit unsigned int value at the byteOffset
     */
    @SpecializedFunction
    public static int getUint8(final Object self, final int byteOffset) {
        try {
            return 0xFF & getBuffer(self).get(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 16-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 16-bit signed int value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static int getInt16(final Object self, final Object byteOffset, final Object littleEndian) {
        try {
            return getBuffer(self, littleEndian).getShort(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 16-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 16-bit signed int value at the byteOffset
     */
    @SpecializedFunction
    public static int getInt16(final Object self, final int byteOffset) {
        try {
            return getBuffer(self, false).getShort(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 16-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 16-bit signed int value at the byteOffset
     */
    @SpecializedFunction
    public static int getInt16(final Object self, final int byteOffset, final boolean littleEndian) {
        try {
            return getBuffer(self, littleEndian).getShort(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 16-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 16-bit unsigned int value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static int getUint16(final Object self, final Object byteOffset, final Object littleEndian) {
        try {
            return 0xFFFF & getBuffer(self, littleEndian).getShort(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 16-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 16-bit unsigned int value at the byteOffset
     */
    @SpecializedFunction
    public static int getUint16(final Object self, final int byteOffset) {
        try {
            return 0xFFFF & getBuffer(self, false).getShort(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 16-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 16-bit unsigned int value at the byteOffset
     */
    @SpecializedFunction
    public static int getUint16(final Object self, final int byteOffset, final boolean littleEndian) {
        try {
            return 0xFFFF & getBuffer(self, littleEndian).getShort(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 32-bit signed int value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static int getInt32(final Object self, final Object byteOffset, final Object littleEndian) {
        try {
            return getBuffer(self, littleEndian).getInt(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 32-bit signed int value at the byteOffset
     */
    @SpecializedFunction
    public static int getInt32(final Object self, final int byteOffset) {
        try {
            return getBuffer(self, false).getInt(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit signed int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 32-bit signed int value at the byteOffset
     */
    @SpecializedFunction
    public static int getInt32(final Object self, final int byteOffset, final boolean littleEndian) {
        try {
            return getBuffer(self, littleEndian).getInt(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 32-bit unsigned int value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static long getUint32(final Object self, final Object byteOffset, final Object littleEndian) {
        try {
            return 0xFFFFFFFFL & getBuffer(self, littleEndian).getInt(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 32-bit unsigned int value at the byteOffset
     */
    @SpecializedFunction
    public static long getUint32(final Object self, final int byteOffset) {
        try {
            return JSType.toUint32(getBuffer(self, false).getInt(JSType.toInt32(byteOffset)));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit unsigned int from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 32-bit unsigned int value at the byteOffset
     */
    @SpecializedFunction
    public static long getUint32(final Object self, final int byteOffset, final boolean littleEndian) {
        try {
            return JSType.toUint32(getBuffer(self, littleEndian).getInt(JSType.toInt32(byteOffset)));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit float value from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 32-bit float value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static double getFloat32(final Object self, final Object byteOffset, final Object littleEndian) {
        try {
            return getBuffer(self, littleEndian).getFloat(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit float value from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 32-bit float value at the byteOffset
     */
    @SpecializedFunction
    public static double getFloat32(final Object self, final int byteOffset) {
        try {
            return getBuffer(self, false).getFloat(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 32-bit float value from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 32-bit float value at the byteOffset
     */
    @SpecializedFunction
    public static double getFloat32(final Object self, final int byteOffset, final boolean littleEndian) {
        try {
            return getBuffer(self, littleEndian).getFloat(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 64-bit float value from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 64-bit float value at the byteOffset
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static double getFloat64(final Object self, final Object byteOffset, final Object littleEndian) {
        try {
            return getBuffer(self, littleEndian).getDouble(JSType.toInt32(byteOffset));
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 64-bit float value from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @return 64-bit float value at the byteOffset
     */
    @SpecializedFunction
    public static double getFloat64(final Object self, final int byteOffset) {
        try {
            return getBuffer(self, false).getDouble(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Get 64-bit float value from given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param littleEndian (optional) flag indicating whether to read in little endian order
     * @return 64-bit float value at the byteOffset
     */
    @SpecializedFunction
    public static double getFloat64(final Object self, final int byteOffset, final boolean littleEndian) {
        try {
            return getBuffer(self, littleEndian).getDouble(byteOffset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    // Stores a value of the given type at the specified byte offset
    // from the start of the view. There is no alignment constraint;
    // multi-byte values may be stored at any offset.
    //
    // For multi-byte values, the optional littleEndian argument
    // indicates whether the value should be stored in big-endian or
    // little-endian byte order. If false or undefined, the value is
    // stored in big-endian byte order.
    //
    // These methods raise an exception if they would write
    // beyond the end of the view.

    /**
     * Set 8-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param value byte value to set
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setInt8(final Object self, final Object byteOffset, final Object value) {
        try {
            getBuffer(self).put(JSType.toInt32(byteOffset), (byte)JSType.toInt32(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 8-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to read from
     * @param value byte value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setInt8(final Object self, final int byteOffset, final int value) {
        try {
            getBuffer(self).put(byteOffset, (byte)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 8-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value byte value to set
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setUint8(final Object self, final Object byteOffset, final Object value) {
        try {
            getBuffer(self).put(JSType.toInt32(byteOffset), (byte)JSType.toInt32(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 8-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value byte value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setUint8(final Object self, final int byteOffset, final int value) {
        try {
            getBuffer(self).put(byteOffset, (byte)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 16-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value short value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setInt16(final Object self, final Object byteOffset, final Object value, final Object littleEndian) {
        try {
            getBuffer(self, littleEndian).putShort(JSType.toInt32(byteOffset), (short)JSType.toInt32(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 16-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value short value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setInt16(final Object self, final int byteOffset, final int value) {
        try {
            getBuffer(self, false).putShort(byteOffset, (short)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 16-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value short value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @SpecializedFunction
    public static Object setInt16(final Object self, final int byteOffset, final int value, final boolean littleEndian) {
        try {
            getBuffer(self, littleEndian).putShort(byteOffset, (short)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 16-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value short value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setUint16(final Object self, final Object byteOffset, final Object value, final Object littleEndian) {
        try {
            getBuffer(self, littleEndian).putShort(JSType.toInt32(byteOffset), (short)JSType.toInt32(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 16-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value short value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setUint16(final Object self, final int byteOffset, final int value) {
        try {
            getBuffer(self, false).putShort(byteOffset, (short)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 16-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value short value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @SpecializedFunction
    public static Object setUint16(final Object self, final int byteOffset, final int value, final boolean littleEndian) {
        try {
            getBuffer(self, littleEndian).putShort(byteOffset, (short)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value int value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setInt32(final Object self, final Object byteOffset, final Object value, final Object littleEndian) {
        try {
            getBuffer(self, littleEndian).putInt(JSType.toInt32(byteOffset), JSType.toInt32(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value int value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setInt32(final Object self, final int byteOffset, final int value) {
        try {
            getBuffer(self, false).putInt(byteOffset, value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit signed int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value int value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @SpecializedFunction
    public static Object setInt32(final Object self, final int byteOffset, final int value, final boolean littleEndian) {
        try {
            getBuffer(self, littleEndian).putInt(byteOffset, value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value int value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setUint32(final Object self, final Object byteOffset, final Object value, final Object littleEndian) {
        try {
            getBuffer(self, littleEndian).putInt(JSType.toInt32(byteOffset), (int)JSType.toUint32(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value int value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setUint32(final Object self, final int byteOffset, final long value) {
        try {
            getBuffer(self, false).putInt(byteOffset, (int)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit unsigned int at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value int value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @SpecializedFunction
    public static Object setUint32(final Object self, final int byteOffset, final long value, final boolean littleEndian) {
        try {
            getBuffer(self, littleEndian).putInt(byteOffset, (int)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit float at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value float value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setFloat32(final Object self, final Object byteOffset, final Object value, final Object littleEndian) {
        try {
            getBuffer(self, littleEndian).putFloat((int)JSType.toUint32(byteOffset), (float)JSType.toNumber(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit float at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value float value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setFloat32(final Object self, final int byteOffset, final double value) {
        try {
            getBuffer(self, false).putFloat(byteOffset, (float)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 32-bit float at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value float value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @SpecializedFunction
    public static Object setFloat32(final Object self, final int byteOffset, final double value, final boolean littleEndian) {
        try {
            getBuffer(self, littleEndian).putFloat(byteOffset, (float)value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 64-bit float at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value double value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 2)
    public static Object setFloat64(final Object self, final Object byteOffset, final Object value, final Object littleEndian) {
        try {
            getBuffer(self, littleEndian).putDouble((int)JSType.toUint32(byteOffset), JSType.toNumber(value));
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 64-bit float at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value double value to set
     * @return undefined
     */
    @SpecializedFunction
    public static Object setFloat64(final Object self, final int byteOffset, final double value) {
        try {
            getBuffer(self, false).putDouble(byteOffset, value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    /**
     * Set 64-bit float at the given byteOffset
     *
     * @param self DataView object
     * @param byteOffset byte offset to write at
     * @param value double value to set
     * @param littleEndian (optional) flag indicating whether to write in little endian order
     * @return undefined
     */
    @SpecializedFunction
    public static Object setFloat64(final Object self, final int byteOffset, final double value, final boolean littleEndian) {
        try {
            getBuffer(self, littleEndian).putDouble(byteOffset, value);
            return UNDEFINED;
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.offset");
        }
    }

    // internals only below this point
    private static ByteBuffer bufferFrom(final NativeArrayBuffer nab, final int offset) {
        try {
            return nab.getBuffer(offset);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.constructor.offset");
        }
    }

    private static ByteBuffer bufferFrom(final NativeArrayBuffer nab, final int offset, final int length) {
        try {
            return nab.getBuffer(offset, length);
        } catch (final IllegalArgumentException iae) {
            throw rangeError(iae, "dataview.constructor.offset");
        }
    }

    private static NativeDataView checkSelf(final Object self) {
        if (!(self instanceof NativeDataView)) {
            throw typeError("not.an.arraybuffer.in.dataview", ScriptRuntime.safeToString(self));
        }
        return (NativeDataView)self;
    }

    private static ByteBuffer getBuffer(final Object self) {
        return checkSelf(self).buf;
    }

    private static ByteBuffer getBuffer(final Object self, final Object littleEndian) {
        return getBuffer(self, JSType.toBoolean(littleEndian));
    }

    private static ByteBuffer getBuffer(final Object self, final boolean littleEndian) {
        return getBuffer(self).order(littleEndian? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(3);
            list.add(AccessorProperty.create("buffer", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$buffer", Object.class),
null));
            list.add(AccessorProperty.create("byteOffset", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$byteOffset", int.class),
null));
            list.add(AccessorProperty.create("byteLength", jdk2.nashorn.internal.runtime.Property.NOT_CONFIGURABLE, 
                    virtualHandle("G$byteLength", int.class),
null));
            $nasgenmap$ = PropertyMap.newMap(list);
    }
    public Object G$buffer() {
        return this.buffer;
    }

    public int G$byteOffset() {
        return this.byteOffset;
    }

    public int G$byteLength() {
        return this.byteLength;
    }


    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(NativeDataView.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    private static MethodHandle virtualHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findVirtual(NativeDataView.class, name,
                    MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    static final class Constructor extends ScriptFunctionImpl {
        private static final PropertyMap $nasgenmap$;

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(3);
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Constructor() {
            super("DataView", 
                    staticHandle("constructor", NativeDataView.class, boolean.class, Object.class, Object[].class),
                    $nasgenmap$, new Specialization[] {
                        new Specialization(staticHandle("constructor", NativeDataView.class, boolean.class, Object.class, Object.class, int.class), false),
                        new Specialization(staticHandle("constructor", NativeDataView.class, boolean.class, Object.class, Object.class, int.class, int.class), false)
            });
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
        private ScriptFunction getInt8;
        private ScriptFunction getUint8;
        private ScriptFunction getInt16;
        private ScriptFunction getUint16;
        private ScriptFunction getInt32;
        private ScriptFunction getUint32;
        private ScriptFunction getFloat32;
        private ScriptFunction getFloat64;
        private ScriptFunction setInt8;
        private ScriptFunction setUint8;
        private ScriptFunction setInt16;
        private ScriptFunction setUint16;
        private ScriptFunction setInt32;
        private ScriptFunction setUint32;
        private ScriptFunction setFloat32;
        private ScriptFunction setFloat64;
        private static final PropertyMap $nasgenmap$;

        public ScriptFunction G$getInt8() {
            return this.getInt8;
        }

        public void S$getInt8(final ScriptFunction function) {
            this.getInt8 = function;
        }

        public ScriptFunction G$getUint8() {
            return this.getUint8;
        }

        public void S$getUint8(final ScriptFunction function) {
            this.getUint8 = function;
        }

        public ScriptFunction G$getInt16() {
            return this.getInt16;
        }

        public void S$getInt16(final ScriptFunction function) {
            this.getInt16 = function;
        }

        public ScriptFunction G$getUint16() {
            return this.getUint16;
        }

        public void S$getUint16(final ScriptFunction function) {
            this.getUint16 = function;
        }

        public ScriptFunction G$getInt32() {
            return this.getInt32;
        }

        public void S$getInt32(final ScriptFunction function) {
            this.getInt32 = function;
        }

        public ScriptFunction G$getUint32() {
            return this.getUint32;
        }

        public void S$getUint32(final ScriptFunction function) {
            this.getUint32 = function;
        }

        public ScriptFunction G$getFloat32() {
            return this.getFloat32;
        }

        public void S$getFloat32(final ScriptFunction function) {
            this.getFloat32 = function;
        }

        public ScriptFunction G$getFloat64() {
            return this.getFloat64;
        }

        public void S$getFloat64(final ScriptFunction function) {
            this.getFloat64 = function;
        }

        public ScriptFunction G$setInt8() {
            return this.setInt8;
        }

        public void S$setInt8(final ScriptFunction function) {
            this.setInt8 = function;
        }

        public ScriptFunction G$setUint8() {
            return this.setUint8;
        }

        public void S$setUint8(final ScriptFunction function) {
            this.setUint8 = function;
        }

        public ScriptFunction G$setInt16() {
            return this.setInt16;
        }

        public void S$setInt16(final ScriptFunction function) {
            this.setInt16 = function;
        }

        public ScriptFunction G$setUint16() {
            return this.setUint16;
        }

        public void S$setUint16(final ScriptFunction function) {
            this.setUint16 = function;
        }

        public ScriptFunction G$setInt32() {
            return this.setInt32;
        }

        public void S$setInt32(final ScriptFunction function) {
            this.setInt32 = function;
        }

        public ScriptFunction G$setUint32() {
            return this.setUint32;
        }

        public void S$setUint32(final ScriptFunction function) {
            this.setUint32 = function;
        }

        public ScriptFunction G$setFloat32() {
            return this.setFloat32;
        }

        public void S$setFloat32(final ScriptFunction function) {
            this.setFloat32 = function;
        }

        public ScriptFunction G$setFloat64() {
            return this.setFloat64;
        }

        public void S$setFloat64(final ScriptFunction function) {
            this.setFloat64 = function;
        }

        static {
            final List<jdk2.nashorn.internal.runtime.Property> list = new ArrayList<>(17);
            list.add(AccessorProperty.create("getInt8", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getInt8", ScriptFunction.class),
                    virtualHandle("S$getInt8", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getUint8", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getUint8", ScriptFunction.class),
                    virtualHandle("S$getUint8", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getInt16", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getInt16", ScriptFunction.class),
                    virtualHandle("S$getInt16", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getUint16", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getUint16", ScriptFunction.class),
                    virtualHandle("S$getUint16", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getInt32", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getInt32", ScriptFunction.class),
                    virtualHandle("S$getInt32", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getUint32", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getUint32", ScriptFunction.class),
                    virtualHandle("S$getUint32", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getFloat32", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getFloat32", ScriptFunction.class),
                    virtualHandle("S$getFloat32", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("getFloat64", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$getFloat64", ScriptFunction.class),
                    virtualHandle("S$getFloat64", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setInt8", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setInt8", ScriptFunction.class),
                    virtualHandle("S$setInt8", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setUint8", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setUint8", ScriptFunction.class),
                    virtualHandle("S$setUint8", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setInt16", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setInt16", ScriptFunction.class),
                    virtualHandle("S$setInt16", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setUint16", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setUint16", ScriptFunction.class),
                    virtualHandle("S$setUint16", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setInt32", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setInt32", ScriptFunction.class),
                    virtualHandle("S$setInt32", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setUint32", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setUint32", ScriptFunction.class),
                    virtualHandle("S$setUint32", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setFloat32", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setFloat32", ScriptFunction.class),
                    virtualHandle("S$setFloat32", void.class, ScriptFunction.class)));
            list.add(AccessorProperty.create("setFloat64", jdk2.nashorn.internal.runtime.Property.NOT_ENUMERABLE, 
                    virtualHandle("G$setFloat64", ScriptFunction.class),
                    virtualHandle("S$setFloat64", void.class, ScriptFunction.class)));
            $nasgenmap$ = PropertyMap.newMap(list);
        }

        Prototype() {
            super($nasgenmap$);
            getInt8 = ScriptFunctionImpl.makeFunction("getInt8",
                    staticHandle("getInt8", int.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getInt8", int.class, Object.class, int.class), false)
                    });
            getUint8 = ScriptFunctionImpl.makeFunction("getUint8",
                    staticHandle("getUint8", int.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getUint8", int.class, Object.class, int.class), false)
                    });
            getInt16 = ScriptFunctionImpl.makeFunction("getInt16",
                    staticHandle("getInt16", int.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getInt16", int.class, Object.class, int.class), false),
                        new Specialization(staticHandle("getInt16", int.class, Object.class, int.class, boolean.class), false)
                    });
            getInt16.setArity(1);
            getUint16 = ScriptFunctionImpl.makeFunction("getUint16",
                    staticHandle("getUint16", int.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getUint16", int.class, Object.class, int.class), false),
                        new Specialization(staticHandle("getUint16", int.class, Object.class, int.class, boolean.class), false)
                    });
            getUint16.setArity(1);
            getInt32 = ScriptFunctionImpl.makeFunction("getInt32",
                    staticHandle("getInt32", int.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getInt32", int.class, Object.class, int.class), false),
                        new Specialization(staticHandle("getInt32", int.class, Object.class, int.class, boolean.class), false)
                    });
            getInt32.setArity(1);
            getUint32 = ScriptFunctionImpl.makeFunction("getUint32",
                    staticHandle("getUint32", long.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getUint32", long.class, Object.class, int.class), false),
                        new Specialization(staticHandle("getUint32", long.class, Object.class, int.class, boolean.class), false)
                    });
            getUint32.setArity(1);
            getFloat32 = ScriptFunctionImpl.makeFunction("getFloat32",
                    staticHandle("getFloat32", double.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getFloat32", double.class, Object.class, int.class), false),
                        new Specialization(staticHandle("getFloat32", double.class, Object.class, int.class, boolean.class), false)
                    });
            getFloat32.setArity(1);
            getFloat64 = ScriptFunctionImpl.makeFunction("getFloat64",
                    staticHandle("getFloat64", double.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("getFloat64", double.class, Object.class, int.class), false),
                        new Specialization(staticHandle("getFloat64", double.class, Object.class, int.class, boolean.class), false)
                    });
            getFloat64.setArity(1);
            setInt8 = ScriptFunctionImpl.makeFunction("setInt8",
                    staticHandle("setInt8", Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setInt8", Object.class, Object.class, int.class, int.class), false)
                    });
            setInt8.setArity(2);
            setUint8 = ScriptFunctionImpl.makeFunction("setUint8",
                    staticHandle("setUint8", Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setUint8", Object.class, Object.class, int.class, int.class), false)
                    });
            setUint8.setArity(2);
            setInt16 = ScriptFunctionImpl.makeFunction("setInt16",
                    staticHandle("setInt16", Object.class, Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setInt16", Object.class, Object.class, int.class, int.class), false),
                        new Specialization(staticHandle("setInt16", Object.class, Object.class, int.class, int.class, boolean.class), false)
                    });
            setInt16.setArity(2);
            setUint16 = ScriptFunctionImpl.makeFunction("setUint16",
                    staticHandle("setUint16", Object.class, Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setUint16", Object.class, Object.class, int.class, int.class), false),
                        new Specialization(staticHandle("setUint16", Object.class, Object.class, int.class, int.class, boolean.class), false)
                    });
            setUint16.setArity(2);
            setInt32 = ScriptFunctionImpl.makeFunction("setInt32",
                    staticHandle("setInt32", Object.class, Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setInt32", Object.class, Object.class, int.class, int.class), false),
                        new Specialization(staticHandle("setInt32", Object.class, Object.class, int.class, int.class, boolean.class), false)
                    });
            setInt32.setArity(2);
            setUint32 = ScriptFunctionImpl.makeFunction("setUint32",
                    staticHandle("setUint32", Object.class, Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setUint32", Object.class, Object.class, int.class, long.class), false),
                        new Specialization(staticHandle("setUint32", Object.class, Object.class, int.class, long.class, boolean.class), false)
                    });
            setUint32.setArity(2);
            setFloat32 = ScriptFunctionImpl.makeFunction("setFloat32",
                    staticHandle("setFloat32", Object.class, Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setFloat32", Object.class, Object.class, int.class, double.class), false),
                        new Specialization(staticHandle("setFloat32", Object.class, Object.class, int.class, double.class, boolean.class), false)
                    });
            setFloat32.setArity(2);
            setFloat64 = ScriptFunctionImpl.makeFunction("setFloat64",
                    staticHandle("setFloat64", Object.class, Object.class, Object.class, Object.class, Object.class), new Specialization[] {
                        new Specialization(staticHandle("setFloat64", Object.class, Object.class, int.class, double.class), false),
                        new Specialization(staticHandle("setFloat64", Object.class, Object.class, int.class, double.class, boolean.class), false)
                    });
            setFloat64.setArity(2);
        }

       public String getClassName() {
           return "DataView";
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
