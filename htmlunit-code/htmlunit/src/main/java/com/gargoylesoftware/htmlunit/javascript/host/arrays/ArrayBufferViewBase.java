/*
 * Copyright (c) 2002-2015 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit.javascript.host.arrays;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.NativeArray;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

/**
 * The parent class of all typed arrays, {@link DataView} is not included.
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Frank Danek
 */
@JsxClass(isJSObject = false,
        browsers = { @WebBrowser(FF), @WebBrowser(CHROME), @WebBrowser(value = IE, minVersion = 11) })
public class ArrayBufferViewBase extends ArrayBufferView {

    /**
     * The constructor.
     *
     * @param object the object
     * @param byteOffset optional byteOffset
     * @param length optional length
     */
    public void constructor(final Object object, final Object byteOffset, final Object length) {
        if (object instanceof Number) {
            constructor(((Number) object).intValue());
        }
        else if (object instanceof NativeArray) {
            constructor((NativeArray) object);
        }
        else if (object instanceof ArrayBufferViewBase) {
            constructor((ArrayBufferViewBase) object);
        }
        else if (object instanceof ArrayBuffer) {
            final ArrayBuffer array = (ArrayBuffer) object;

            double dbByteOffset = Context.toNumber(byteOffset);
            if (dbByteOffset != dbByteOffset) { // test if == NaN
                dbByteOffset = 0;
            }

            double dbLength = Context.toNumber(length);
            if (dbLength != dbLength) { // test if == NaN
                dbLength = array.getByteLength();
            }
            super.constructor(array, (int) dbByteOffset, (int) dbLength);
        }
        else {
            Context.reportRuntimeError("Invalid type " + object.getClass().getName());
        }
    }

    private void constructor(final int length) {
        final int byteLength = length * getBytesPerElement();
        setByteLength(byteLength);
        initBuffer(byteLength);
    }

    private void constructor(final NativeArray array) {
        final int byteLength = (int) array.getLength() * getBytesPerElement();
        setByteLength(byteLength);
        initBuffer(byteLength);
        set(array, 0);
    }

    private void constructor(final ArrayBufferViewBase array) {
        final int byteLength = array.getLength() * getBytesPerElement();
        setByteLength(byteLength);
        initBuffer(byteLength);
        set(array, 0);
    }

    private void initBuffer(final int lengthInBytes) {
        final ArrayBuffer buffer = new ArrayBuffer();
        buffer.constructor(lengthInBytes);
        buffer.setPrototype(getPrototype(buffer.getClass()));
        buffer.setParentScope(getParentScope());
        setBuffer(buffer);
    }

    /**
     * Returns the number of entries in the array.
     * @return the number of entries
     */
    @JsxGetter
    public int getLength() {
        return getByteLength() / getBytesPerElement();
    }

    /**
     * Sets multiple values in the typed array, reading input values from a specified array.
     * @param sourceArray the source array
     * @param offset the offset into the target array at which to begin writing values from the source one
     */
    @JsxFunction
    public void set(final ScriptableObject sourceArray, final int offset) {
        final int length = ((Number) ScriptableObject.getProperty(sourceArray, "length")).intValue();
        for (int i = 0; i < length; i++) {
            put(i + offset, this, sourceArray.get(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start) {
        final int offset = index * getBytesPerElement() + getByteOffset();
        final ArrayBuffer buffer = getBuffer();
        if (buffer == null) {
            return Scriptable.NOT_FOUND;
        }
        return fromArray(buffer.getBytes(), offset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final int index, final Scriptable start, final Object value) {
        getBuffer().setBytes(index * getBytesPerElement() + getByteOffset() , toArray((Number) value));
    }

    /**
     * Converts the provided number to byte array.
     * @param number the number
     * @return the byte array
     */
    protected byte[] toArray(final Number number) {
        return null;
    }

    /**
     * Converts the provided byte array to number.
     * @param array the array
     * @param offset the offset
     * @return the byte array
     */
    protected Object fromArray(final byte[] array, final int offset) {
        return null;
    }

    /**
     * Returns a new view on the ArrayBuffer store for this object.
     * @param begin the offset to the first element in the array to be referenced by the new object
     * @param end the end offset (exclusive), optional to return at the end.
     * @return the newly created array
     */
    @JsxFunction
    public ArrayBufferView subarray(final int begin, Object end) {
        if (end == Undefined.instance) {
            end = getLength();
        }
        try {
            final ArrayBufferView object = getClass().newInstance();
            object.setPrototype(getPrototype());
            object.setParentScope(getParentScope());
            object.constructor(getBuffer(), begin, ((Number) end).intValue() - begin);
            return object;
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the size in bytes of an item in this array.
     * @return the size of bytes of an item
     */
    protected int getBytesPerElement() {
        return 1;
    }

}
