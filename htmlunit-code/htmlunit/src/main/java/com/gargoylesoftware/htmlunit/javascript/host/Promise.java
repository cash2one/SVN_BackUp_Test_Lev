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
package com.gargoylesoftware.htmlunit.javascript.host;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;

import com.gargoylesoftware.htmlunit.javascript.FunctionWrapper;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxStaticFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.JavaScriptException;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

/**
 * A JavaScript object for {@code Promise}.
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF) })
public class Promise extends SimpleScriptable {

    private Object value_;

    /**
     * Default constructor.
     */
    public Promise() {
    }

    /**
     * Constructor new promise with the given {@code object}.
     *
     * @param object the object
     */
    @JsxConstructor
    public Promise(final Object object) {
        if (object instanceof Promise) {
            value_ = ((Promise) object).value_;
        }
        else if (object instanceof NativeObject) {
            final NativeObject nativeObject = (NativeObject) object;
            value_ = nativeObject.get("then", nativeObject);
        }
        else {
            value_ = object;
        }
    }

    /**
     * Returns a {@link Promise} object that is resolved with the given value.
     *
     * @param context the context
     * @param thisObj this object
     * @param args the arguments
     * @param function the function
     * @return a {@link Promise}
     */
    @JsxStaticFunction
    public static Promise resolve(final Context context, final Scriptable thisObj, final Object[] args,
            final Function function) {
        final Promise promise = new Promise(args[0]);
        promise.setParentScope(thisObj.getParentScope());
        promise.setPrototype(getWindow(thisObj).getPrototype(promise.getClass()));
        return promise;
    }

    /**
     * It takes two arguments, both are callback functions for the success and failure cases of the Promise.
     *
     * @param onFulfilled success function
     * @param onRejected failure function
     * @return {@link Promise}
     */
    @JsxFunction
    public Promise then(final Function onFulfilled, final Function onRejected) {
        Object newValue = null;
        if (value_ instanceof Function) {
            final WasCalledFunctionWrapper wrapper = new WasCalledFunctionWrapper(onFulfilled);
            try {
                ((Function) value_).call(Context.getCurrentContext(), getParentScope(), this,
                    new Object[] {wrapper, onRejected});
                if (wrapper.wasCalled_) {
                    newValue = wrapper.value_;
                }
            }
            catch (final JavaScriptException e) {
                if (!wrapper.wasCalled_) {
                    newValue = onRejected.call(Context.getCurrentContext(), getParentScope(), this,
                            new Object[] {e.getValue()});
                }
            }
        }
        else {
            newValue = onFulfilled.call(Context.getCurrentContext(), getParentScope(), this,
                    new Object[] {value_});
        }
        final Promise promise = new Promise();
        promise.setParentScope(this.getParentScope());
        promise.setPrototype(getPrototype(promise.getClass()));
        promise.value_ = newValue;
        return promise;
    }

    private static class WasCalledFunctionWrapper extends FunctionWrapper {

        private boolean wasCalled_;
        private Object value_;

        WasCalledFunctionWrapper(final Function wrapped) {
            super(wrapped);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
            wasCalled_ = true;
            value_ = super.call(cx, scope, thisObj, args);
            return value_;
        }
    }
}
