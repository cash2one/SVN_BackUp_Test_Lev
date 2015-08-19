package net.sourceforge.htmlunit.nashorn;

import java.lang.reflect.Field;

import javax.script.ScriptEngine;

import org.junit.Test;

import jdk2.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk2.nashorn.api.scripting.ScriptObjectMirror;
import jdk2.nashorn.internal.runtime.ScriptObject;

/**
 * Simple test.
 */
public class AppTest {

    @Test
    public void test() throws Exception {
        final ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.eval("print('Hello, World!');");
    }

    @Test
    public void int8Array() throws Exception {
        final ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.eval("print(new Int8Array().length);");
    }

    @Test
    public void int8Array2() throws Exception {
        final ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
        final ScriptObjectMirror mirror = (ScriptObjectMirror) engine.eval("new Int8Array()");
        final ScriptObject sobj = get(mirror, "sobj");
        for (final Object o : sobj.getMap().getProperties()) {
            System.out.println(o);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(final Object o, final String fieldName) throws Exception {
        final Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(o);
    }
}
