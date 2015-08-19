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

import macromedia.asc.embedding.avmplus.ActionBlockConstants;
import net.sourceforge.htmlunit.flash.actionscript.Function;
import net.sourceforge.htmlunit.flash.actionscript.flash.display.DisplayObject;
import adobe.abc.Binding;
import adobe.abc.Block;
import adobe.abc.Expr;
import adobe.abc.GlobalOptimizer.InputAbc;
import adobe.abc.Method;
import adobe.abc.Type;
import event.Example1;

/**
 * The engine.
 *
 * @version $Revision: 7462 $
 * @author Ahmed Ashour
 */
public final class ActionScriptEngine {

    private static final boolean DEBUG = false;
    private Flash flash_;
    
    public static void main(String[] args) throws Exception {
        new Example1().test();
    }

    private ActionScriptEngine(Flash flash) {
        flash_ = flash;
    }

    public static void execute(final InputAbc ia, final Flash flash) {
        new ActionScriptEngine(flash).execute(ia);
    }
    private void execute(final InputAbc ia) {
        for (Type t : ia.classes) {
            final String thisClass = t.itype.getName().toString();
            final String superClassName = t.itype.base.getName().toString();
            RuntimeScriptObject runtimeObject = new RuntimeScriptObject(thisClass, superClassName);
            ActionScriptConfiguration.setPrototype(thisClass, runtimeObject);
            for (final Binding b : t.itype.defs.values()) {
                runtimeObject.putProperty(b.getName().toString(), b.method);
            }
        }
        final Type initType = ia.scripts[ia.scripts.length - 1].ref.getType();
        for (final Method method : ia.methods) {
            if (method.getName().equals(initType.getName())) {
                execute(method, null);
            }
        }
    }

    private void execute(final Method method, final ScriptObject thisObj) {
        execute(method.entry.to, thisObj);
    }

    private void execute(final Block block, final ScriptObject thisObj) {
        for (final Expr e : block.exprs) {
            execute(e, thisObj);
        }
    }

    private Object execute(final Expr e, final ScriptObject thisObj) {
        switch (e.op) {
        case 0:
            if (DEBUG) {
                System.out.println(e.ref);
            }
            if (e.ref.format().equals("public::this")) {
                return thisObj;
            }
            break;

        case ActionBlockConstants.OP_pushscope:
            Debugger.ensure(e.args.length == 1);
            if (DEBUG) {
                System.out.println(Util.getOpcodeName(e.op) + ' ' + e.args[0].ref);
            }
            break;

        case ActionBlockConstants.OP_getproperty:
            Debugger.ensure(e.args.length == 1);
            if (DEBUG) {
                System.out.println(Util.getOpcodeName(e.op) + ' '+ e.args[0].ref + ' ' + '.' + e.ref);
            }
            Object o = execute(e.args[0], thisObj);
            if (o instanceof ScriptObject) {
                return ((ScriptObject) o).getProperty(getNative(((ScriptObject) o)), e.ref.toString());
            }
            else if (o instanceof java.lang.reflect.Method) {
                return call((java.lang.reflect.Method) o, getNative(thisObj), new Object[0]);
            }
            return o;

        case ActionBlockConstants.OP_callpropvoid:
            if (e.args.length == 2) {
                if (DEBUG) {
                    System.out.println(Util.getOpcodeName(e.op) + ' ' + e.args[0].ref + "." +  e.ref + '(' + e.args[1].ref + ')');
                }
            }
            else if (e.args.length == 3) {
                Object function = execute(e.args[0], thisObj);
                Object[] args = new Object[e.args.length - 1];
                for (int i = 0; i < args.length; i++) {
                    args[i] = execute(e.args[i + 1], thisObj);
                }
                if (function instanceof java.lang.reflect.Method) {
                    try {
//                        ((MovieClip) getNative(thisObj)).addFrameScript(0, (Function)args[1]);
                        ((java.lang.reflect.Method) function).invoke(getNative(thisObj), args);
                    }
                    catch(final Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            else {
                throw new AssertionError();
            }
            break;

        case ActionBlockConstants.OP_findpropstrict:
            Debugger.ensure(e.args.length == 0);
            final String property = e.ref.toString();
            for (Expr scope : e.scopes) {
                if (scope.op == ActionBlockConstants.OP_pushscope) {
                    if (scope.args[0].op == 0 && scope.args[0].ref.toString().equals("this")) {
                        if (thisObj != null) {
                            if (e.ref.isQname() && !"public".equals(e.ref.nsset(0).toString())) {
                                final String className = e.ref.nsset(0).toString() + '.' + e.ref.toString();
                                return ActionScriptConfiguration.getPrototypeOf(className);
                            }
                            return thisObj.getProperty(getNative(thisObj), property);
                        }
                    }
                    else {
                        if (DEBUG) {
                            System.out.println("OP_findpropstrict " + scope.args[0].ref);
                        }
                    }
                }
                else {
                    throw new AssertionError();
                }
            }
            break;

        case ActionBlockConstants.OP_jump:
            Debugger.ensure(e.succ.length == 1);
            if (DEBUG) {
                System.out.println("JUMP");
            }
            execute(e.succ[0].to, thisObj);
            break;

        case ActionBlockConstants.OP_newclass:
            Debugger.ensure(e.args.length == 1);
            execute(e.c.init, null);
            final String thisClass = e.c.itype.getName().toString();
            ScriptObject prototype = ActionScriptConfiguration.getPrototypeOf(thisClass);
            final ScriptObject scriptObject;
            if (prototype instanceof RuntimeScriptObject) {
                scriptObject = new RuntimeScriptObject(flash_, (RuntimeScriptObject) prototype);
            }
            else {
                scriptObject = new NativeScriptObject((NativeScriptObject) prototype);
            }
            Object nativeObject = getNative(scriptObject);
            if (nativeObject instanceof DisplayObject) {
                ((DisplayObject) nativeObject).setStage(flash_.getStage());
            }
            execute(e.c.itype.init, scriptObject);
            break;

        case ActionBlockConstants.OP_constructsuper:
            Debugger.ensure(e.args.length == 1);
            if (DEBUG) {
                System.out.println(Util.getOpcodeName(e.op) + ' '+ e.args[0].ref + ' ' + '.' + e.ref);
            }
            break;

        case ActionBlockConstants.OP_pushbyte:
            return e.value;

        case ActionBlockConstants.OP_popscope:
        case ActionBlockConstants.OP_returnvoid:
            //nothing for now
            break;
        default:
            if (DEBUG) {
                System.out.println(Util.getOpcodeName(e.op) + " " + e.args.length);
            }
        }
        return null;
    }

    public static Object call(final Function function, final Object[] arguments) {
        final Object impl = function.getImplementation();
        if (impl instanceof java.lang.reflect.Method) {
            try {
                return call((java.lang.reflect.Method) impl, function.getThisObj(), arguments);
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }
        else {
            new ActionScriptEngine(function.getFlash()).execute((Method) impl, function.getThisObj());
        }
        return null;
    }

    public static Object call(final java.lang.reflect.Method method, final Object object, final Object[] arguments) {
        try {
            return method.invoke(object, arguments);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getNative(final ScriptObject thisObj) {
        Object nativeObject;
        if (thisObj instanceof NativeScriptObject) {
            nativeObject = ((NativeScriptObject) thisObj).getObject();
        }
        else {
            nativeObject = ((RuntimeScriptObject) thisObj).getNativeObject().getObject();
        }
        return nativeObject;
    }
}
