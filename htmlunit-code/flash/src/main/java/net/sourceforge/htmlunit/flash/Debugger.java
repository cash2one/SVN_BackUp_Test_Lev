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
import adobe.abc.Block;
import adobe.abc.Expr;
import adobe.abc.GlobalOptimizer.InputAbc;
import adobe.abc.Method;
import adobe.abc.Type;

public class Debugger {

    public static void debug(InputAbc ia) {
        for (final Method m : ia.methods) {
            System.out.println("-------------------------------------");
            System.out.println(m.getName().format());
            debug(m.entry.to);
        }
    }

    private static void debug(Block b) {
        for (final Expr e : b.exprs) {
            switch (e.op) {
            case 0:
                System.out.println(e.ref);
                break;

            case ActionBlockConstants.OP_pushscope:
                ensure(e.args.length == 1);
                System.out.println(Util.getOpcodeName(e.op) + ' ' + e.args[0].ref);
                break;

            case ActionBlockConstants.OP_getproperty:
                ensure(e.args.length == 1);
                System.out.println(Util.getOpcodeName(e.op) + ' '+ e.args[0].ref + ' ' + '.' + e.ref);
                break;

            case ActionBlockConstants.OP_callpropvoid:
                if (e.args.length == 2) {
                    System.out.println(Util.getOpcodeName(e.op) + ' ' + e.args[0].ref + "." +  e.ref + '(' + e.args[1].ref + ')');
                }
                else if (e.args.length == 3) {
                    System.out.println(Util.getOpcodeName(e.op) + ' ' + e.args[0].ref + "." + e.ref + '(' + e.args[1].ref + ", " + e.args[2].ref + ')');
                }
                else {
                    throw new AssertionError();
                }
                break;

            case ActionBlockConstants.OP_findpropstrict:
                ensure(e.args.length == 0);
                System.out.println(Util.getOpcodeName(e.op) + ' ' + e.ref.format());
                break;

            case ActionBlockConstants.OP_jump:
                ensure(e.succ.length == 1);
                System.out.println("JUMP");
                debug(e.succ[0].to);
                break;

            default:
                System.out.println(Util.getOpcodeName(e.op) + " " + e.args.length);
            }
        }
    }

    public static void ensure(final boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
