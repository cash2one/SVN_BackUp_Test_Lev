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

package jdk2.nashorn.internal.tools.nasgen;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_FINAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKEVIRTUAL;
import static jdk2.nashorn.internal.tools.nasgen.StringConstants.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import jdk2.nashorn.internal.objects.NativeObject;
import jdk2.nashorn.internal.runtime.AccessorProperty;
import jdk2.nashorn.internal.runtime.Property;
import jdk2.nashorn.internal.runtime.ScriptFunction;
import jdk2.nashorn.internal.tools.nasgen.MemberInfo.Kind;

/**
 * Base class for class generator classes.
 *
 */
public class ClassJavaGenerator {
    /** ASM class writer used to output bytecode for this class */
    protected final ClassWriter cw;
    protected static final StringBuilder builder = new StringBuilder();

    /**
     * Constructor
     */
    protected ClassJavaGenerator() {
        this.cw = makeClassWriter();
    }

    MethodGenerator makeStaticInitializer() {
        return makeStaticInitializer(cw);
    }

    MethodGenerator makeConstructor() {
        return makeConstructor(cw);
    }

    MethodGenerator makeMethod(final int access, final String name, final String desc) {
        return makeMethod(cw, access, name, desc);
    }

    void addMapField() {
        addMapField(cw);
    }

    void addField(final String name, final String desc) {
        addField(cw, name, desc);
    }

    void addFunctionField(final String name) {
        addFunctionField(cw, name);
    }

    void addGetter(final String owner, final MemberInfo memInfo) {
        addGetter(cw, owner, memInfo);
    }

    void addSetter(final String owner, final MemberInfo memInfo) {
        addSetter(cw, owner, memInfo);
    }

    void emitGetClassName(final String name) {
        final MethodGenerator mi = makeMethod(ACC_PUBLIC, GET_CLASS_NAME, GET_CLASS_NAME_DESC);
        builder.append("       public String getClassName() {" + System.lineSeparator());
        builder.append("           return \"" + name+ "\";" + System.lineSeparator());
        builder.append("       }" + System.lineSeparator());
        builder.append(System.lineSeparator());
        mi.loadLiteral(name);
        mi.returnValue();
        mi.computeMaxs();
        mi.visitEnd();
    }

    public static String getCode(final String name) {
        builder.insert(0, "    static {" + System.lineSeparator());
        builder.append("    }" + System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {" + System.lineSeparator());
        builder.append("        try {" + System.lineSeparator());
        builder.append("            return MethodHandles.lookup().findStatic(" + name + ".class," + System.lineSeparator());
        builder.append("                    name, MethodType.methodType(rtype, ptypes));" + System.lineSeparator());
        builder.append("        }" + System.lineSeparator());
        builder.append("        catch (final ReflectiveOperationException e) {" + System.lineSeparator());
        builder.append("            throw new IllegalStateException(e);" + System.lineSeparator());
        builder.append("        }" + System.lineSeparator());
        builder.append("    }" + System.lineSeparator());
        
        return builder.toString();
    }

    static ClassWriter makeClassWriter() {
        return new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (final RuntimeException | LinkageError e) {
                    return StringConstants.OBJECT_TYPE;
                }
            }
        };
    }

    static MethodGenerator makeStaticInitializer(final ClassVisitor cv) {
        return makeStaticInitializer(cv, CLINIT);
    }

    static MethodGenerator makeStaticInitializer(final ClassVisitor cv, final String name) {
        final int access =  ACC_PUBLIC | ACC_STATIC;
        final String desc = DEFAULT_INIT_DESC;
        final MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
        return new MethodGenerator(mv, access, name, desc);
    }

    static MethodGenerator makeConstructor(final ClassVisitor cv) {
        final int access = 0;
        final String name = INIT;
        final String desc = DEFAULT_INIT_DESC;
        final MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
        return new MethodGenerator(mv, access, name, desc);
    }

    static MethodGenerator makeMethod(final ClassVisitor cv, final int access, final String name, final String desc) {
        final MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
        return new MethodGenerator(mv, access, name, desc);
    }

    static void emitStaticInitPrefix(final MethodGenerator mi, final String className, final int memberCount) {
        mi.visitCode();
        if (memberCount > 0) {
            // new ArrayList(int)
            mi.newObject(ARRAYLIST_TYPE);
            mi.dup();
            mi.push(memberCount);
            mi.invokeSpecial(ARRAYLIST_TYPE, INIT, ARRAYLIST_INIT_DESC);
            builder.append("            final List<Property> list = new ArrayList<>("
                    + memberCount + ");" + System.lineSeparator());
            // stack: ArrayList
        } else {
            // java.util.Collections.EMPTY_LIST
            builder.append("            final List<Property> list = Collections.emptyList();" + System.lineSeparator());
            mi.getStatic(COLLECTIONS_TYPE, COLLECTIONS_EMPTY_LIST, LIST_DESC);
            // stack List
        }
    }

    static void emitStaticInitSuffix(final MethodGenerator mi, final String className) {
        // stack: Collection
        // pmap = PropertyMap.newMap(Collection<Property>);
        mi.invokeStatic(PROPERTYMAP_TYPE, PROPERTYMAP_NEWMAP, PROPERTYMAP_NEWMAP_DESC);
        // $nasgenmap$ = pmap;
        mi.putStatic(className, PROPERTYMAP_FIELD_NAME, PROPERTYMAP_DESC);
        builder.append("            " + PROPERTYMAP_FIELD_NAME + " = PropertyMap.newMap(list);" + System.lineSeparator());
        mi.returnVoid();
        mi.computeMaxs();
        mi.visitEnd();
    }

    @SuppressWarnings("fallthrough")
    private static Type memInfoType(final MemberInfo memInfo) {
        switch (memInfo.getJavaDesc().charAt(0)) {
            case 'I': return Type.INT_TYPE;
            case 'J': return Type.LONG_TYPE;
            case 'D': return Type.DOUBLE_TYPE;
            default:  assert false : memInfo.getJavaDesc();
            case 'L': return TYPE_OBJECT;
        }
    }

    @SuppressWarnings("fallthrough")
    private static Type memInfoTypeScriptFuntion(final MemberInfo memInfo) {
        switch (memInfo.getJavaDesc().charAt(0)) {
            case 'I': return Type.INT_TYPE;
            case 'J': return Type.LONG_TYPE;
            case 'D': return Type.DOUBLE_TYPE;
            default:
                assert false : memInfo.getJavaDesc();
            case 'L': return TYPE_SCRIPTFUNCTION;
        }
    }

    private static String getterDesc(final MemberInfo memInfo) {
        return Type.getMethodDescriptor(memInfoType(memInfo));
    }

    private static String setterDesc(final MemberInfo memInfo) {
        return Type.getMethodDescriptor(Type.VOID_TYPE, memInfoType(memInfo));
    }

    static void addGetter(final ClassVisitor cv, final String owner, final MemberInfo memInfo) {
        final int access = ACC_PUBLIC;
        final String name = GETTER_PREFIX + memInfo.getJavaName();
        final String desc = getterDesc(memInfo);
        final MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
        final MethodGenerator mi = new MethodGenerator(mv, access, name, desc);
        mi.visitCode();
        if (memInfo.isStatic() && memInfo.getKind() == Kind.PROPERTY) {
            mi.getStatic(owner, memInfo.getJavaName(), memInfo.getJavaDesc());
        } else {
            mi.loadLocal(0);
            mi.getField(owner, memInfo.getJavaName(), memInfo.getJavaDesc());
            builder.append("        public ScriptFunction " + name + "() {" + System.lineSeparator());
            builder.append("            return this." + memInfo.getJavaName() + ";" + System.lineSeparator());
            builder.append("        }" + System.lineSeparator());
            builder.append(System.lineSeparator());
        }
        mi.returnValue();
        mi.computeMaxs();
        mi.visitEnd();
    }

    static void addSetter(final ClassVisitor cv, final String owner, final MemberInfo memInfo) {
        final int access = ACC_PUBLIC;
        final String name = SETTER_PREFIX + memInfo.getJavaName();
        final String desc = setterDesc(memInfo);
        final MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
        final MethodGenerator mi = new MethodGenerator(mv, access, name, desc);
        mi.visitCode();
        if (memInfo.isStatic() && memInfo.getKind() == Kind.PROPERTY) {
            mi.loadLocal(1);
            builder.append("addSetter() NOT SUPPORTED" + System.lineSeparator());
            mi.putStatic(owner, memInfo.getJavaName(), memInfo.getJavaDesc());
        } else {
            mi.loadLocal(0);
            mi.loadLocal(1);
            builder.append("        public void " + name + "(final ScriptFunction function) {" + System.lineSeparator());
            builder.append("            this." + memInfo.getJavaName() + " = function;" + System.lineSeparator());
            builder.append("        }" + System.lineSeparator());
            builder.append(System.lineSeparator());
            mi.putField(owner, memInfo.getJavaName(), memInfo.getJavaDesc());
        }
        mi.returnVoid();
        mi.computeMaxs();
        mi.visitEnd();
    }

    static void addMapField(final ClassVisitor cv) {
        // add a PropertyMap static field
        final FieldVisitor fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
            PROPERTYMAP_FIELD_NAME, PROPERTYMAP_DESC, null, null);
        String descJava = descToJava(PROPERTYMAP_DESC);
        builder.append("        private static final " + descJava.substring(0, descJava.length() - ".class".length())
                + " " + PROPERTYMAP_FIELD_NAME + ';' + System.lineSeparator());
        if (fv != null) {
            fv.visitEnd();
        }
    }

    static void addField(final ClassVisitor cv, final String name, final String desc) {
        final FieldVisitor fv = cv.visitField(ACC_PRIVATE, name, desc, null, null);
        String descJava = descToJava(desc);
        builder.append("        private " + descJava.substring(0, descJava.length() - ".class".length())
                + " " + name + ";" + System.lineSeparator());
        if (fv != null) {
            fv.visitEnd();
        }
    }

    protected static String descToJava(String name) {
        String java = "";
        if (name.isEmpty()) {
            return java;
        }
        while (name.startsWith("[")) {
            java += "[]";
            name = name.substring(1);
        }
        if (name.startsWith("L")) {
            java = name.substring(name.lastIndexOf('/') + 1, name.length() - 1) + java + ".class";
        }
        else if (name.startsWith("Z")) {
            java = "boolean" + java + ".class";
            String remaining = descToJava(name.substring(1));
            if (!remaining.isEmpty()) {
                java += ", " + remaining;
            }
        }
        else if (name.startsWith("V")) {
            java = "void" + java + ".class";
            String remaining = descToJava(name.substring(1));
            if (!remaining.isEmpty()) {
                java += ", " + remaining;
            }
        }
        else if (name.startsWith("J")) {
            java = "long" + java + ".class";
            String remaining = descToJava(name.substring(1));
            if (!remaining.isEmpty()) {
                java += ", " + remaining;
            }
        }
        else if (name.startsWith("D")) {
            java = "double" + java + ".class";
            String remaining = descToJava(name.substring(1));
            if (!remaining.isEmpty()) {
                java += ", " + remaining;
            }
        }
        else if (name.startsWith("I")) {
            java = "int" + java + ".class";
            String remaining = descToJava(name.substring(1));
            if (!remaining.isEmpty()) {
                java += ", " + remaining;
            }
        }
        else if (name.startsWith("(")) {
            String rv = name.substring(name.lastIndexOf(')') + 1);
            java = descToJava(rv);
            for (String s : name.substring(1, name.lastIndexOf(')')).split(";")) {
                if (!s.isEmpty()) {
                    java += ", " + descToJava(s + ';');
                }
            }
        }
        return java;
    }

    static void addFunctionField(final ClassVisitor cv, final String name) {
        addField(cv, name, OBJECT_DESC);
    }

    static void newFunction(final MethodGenerator mi, final String className, final MemberInfo memInfo, final List<MemberInfo> specs) {
        final boolean arityFound = (memInfo.getArity() != MemberInfo.DEFAULT_ARITY);

        mi.loadLiteral(memInfo.getName());
        mi.visitLdcInsn(new Handle(H_INVOKESTATIC, className, memInfo.getJavaName(), memInfo.getJavaDesc()));

        assert specs != null;
        if (!specs.isEmpty()) {
            mi.memberInfoArray(className, specs);
            mi.invokeStatic(SCRIPTFUNCTIONIMPL_TYPE, SCRIPTFUNCTIONIMPL_MAKEFUNCTION, SCRIPTFUNCTIONIMPL_MAKEFUNCTION_SPECS_DESC);
            builder.append("ScriptFunctionImpl.makeFunction(\"" + memInfo.getName() + "\"," + System.lineSeparator());
            builder.append("                    staticHandle(\"" + memInfo.getJavaName()
                    + "\", " + descToJava(memInfo.getJavaDesc()) + ")");
            builder.append(", new Specialization[] {" + System.lineSeparator());
            for (int i = 0; i < specs.size(); i++) {
                final MemberInfo m = specs.get(i);
                builder.append("                        new Specialization(staticHandle(\"" + m.getJavaName()
                        + "\", " + descToJava(m.getJavaDesc()) + "), " + m.isOptimistic() + ")");
                if (i < specs.size() - 1) {
                    builder.append(',');
                }
                builder.append(System.lineSeparator());
            }
            builder.append("                    });" + System.lineSeparator());
        } else {
            builder.append("ScriptFunctionImpl.makeFunction(\"" + memInfo.getName() + "\"," + System.lineSeparator());
            builder.append("                    staticHandle(\"" + memInfo.getJavaName()
                    + "\", " + descToJava(memInfo.getJavaDesc()) + "));" + System.lineSeparator());
            mi.invokeStatic(SCRIPTFUNCTIONIMPL_TYPE, SCRIPTFUNCTIONIMPL_MAKEFUNCTION, SCRIPTFUNCTIONIMPL_MAKEFUNCTION_DESC);
        }

        if (arityFound) {
            mi.dup();
            mi.push(memInfo.getArity());
            mi.invokeVirtual(SCRIPTFUNCTION_TYPE, SCRIPTFUNCTION_SETARITY, SCRIPTFUNCTION_SETARITY_DESC);
            builder.append("            " + memInfo.getJavaName() + ".setArity(" + memInfo.getArity() + ");"
                    + System.lineSeparator());
        }
    }

    static void linkerAddGetterSetter(final MethodGenerator mi, final String className, final MemberInfo memInfo) {
        final String propertyName = memInfo.getName();
        // stack: Collection
        // dup of Collection instance
        mi.dup();

        // property = AccessorProperty.create(key, flags, getter, setter);
        mi.loadLiteral(propertyName);
        // setup flags
        mi.push(memInfo.getAttributes());
        // setup getter method handle
        String javaName = GETTER_PREFIX + memInfo.getJavaName();
        mi.visitLdcInsn(new Handle(H_INVOKEVIRTUAL, className, javaName, getterDesc(memInfo)));
        builder.append("            list.add(AccessorProperty.create(\"" + propertyName + "\", "
                + attributesToJava(memInfo.getAttributes()) + ", " + System.lineSeparator()
                + "                    virtualHandle(\"" + javaName + "\", " +
                descToJava(Type.getMethodDescriptor(memInfoTypeScriptFuntion(memInfo)))
                + ")," + System.lineSeparator());
        
        // setup setter method handle
        if (memInfo.isFinal()) {
            mi.pushNull();
            builder.append("null");
        } else {
            javaName = SETTER_PREFIX + memInfo.getJavaName();
            mi.visitLdcInsn(new Handle(H_INVOKEVIRTUAL, className, javaName, setterDesc(memInfo)));
            builder.append("                    virtualHandle(\"" + javaName + "\", " +
                    descToJava(Type.getMethodDescriptor(Type.VOID_TYPE, memInfoTypeScriptFuntion(memInfo)))
                    + ")");
        }
        builder.append("));" + System.lineSeparator());
        mi.invokeStatic(ACCESSORPROPERTY_TYPE, ACCESSORPROPERTY_CREATE, ACCESSORPROPERTY_CREATE_DESC);
        // boolean Collection.add(property)
        mi.invokeInterface(COLLECTION_TYPE, COLLECTION_ADD, COLLECTION_ADD_DESC);
        // pop return value of Collection.add
        mi.pop();
        // stack: Collection
    }

    private static String attributesToJava(int attributes) {
        String java = "";
        if ((attributes & Property.NOT_WRITABLE) != 0) {
            java = "Property.NOT_WRITABLE";
        }
        if ((attributes & Property.NOT_ENUMERABLE) != 0) {
            if (!java.isEmpty()) {
                java += " | ";
            }
            java += "Property.NOT_ENUMERABLE";
        }
        if ((attributes & Property.NOT_CONFIGURABLE) != 0) {
            if (!java.isEmpty()) {
                java += " | ";
            }
            java += "Property.NOT_CONFIGURABLE";
        }
        return java;
    }

    static void linkerAddGetterSetter(final MethodGenerator mi, final String className, final MemberInfo getter, final MemberInfo setter) {
        final String propertyName = getter.getName();
        // stack: Collection
        // dup of Collection instance
        mi.dup();

        // property = AccessorProperty.create(key, flags, getter, setter);
        mi.loadLiteral(propertyName);
        // setup flags
        mi.push(getter.getAttributes());
        // setup getter method handle
        mi.visitLdcInsn(new Handle(H_INVOKESTATIC, className, getter.getJavaName(), getter.getJavaDesc()));
        builder.append("            list.add(AccessorProperty.create(\"" + propertyName + "\", "
                + attributesToJava(getter.getAttributes()) + ", " + System.lineSeparator()
                + "                    staticHandle(\"" + getter.getJavaName() + "\", " +
                descToJava(getter.getJavaDesc())
                + ")," + System.lineSeparator());

        // setup setter method handle
        if (setter == null) {
            mi.pushNull();
            builder.append("null");
        } else {
            mi.visitLdcInsn(new Handle(H_INVOKESTATIC, className, setter.getJavaName(), setter.getJavaDesc()));
            builder.append("                    staticHandle(\"" + setter.getJavaName() + "\", " +
                    descToJava(setter.getJavaDesc())
                    + ")");
        }
        builder.append("));" + System.lineSeparator());
        mi.invokeStatic(ACCESSORPROPERTY_TYPE, ACCESSORPROPERTY_CREATE, ACCESSORPROPERTY_CREATE_DESC);
        // boolean Collection.add(property)
        mi.invokeInterface(COLLECTION_TYPE, COLLECTION_ADD, COLLECTION_ADD_DESC);
        // pop return value of Collection.add
        mi.pop();
        // stack: Collection
    }

    static ScriptClassInfo getScriptClassInfo(final String fileName) throws IOException {
        builder.setLength(0);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName))) {
            return getScriptClassInfo(new ClassReader(bis));
        }
    }

    static ScriptClassInfo getScriptClassInfo(final byte[] classBuf) {
        return getScriptClassInfo(new ClassReader(classBuf));
    }

    private static ScriptClassInfo getScriptClassInfo(final ClassReader reader) {
        final ScriptClassInfoCollector scic = new ScriptClassInfoCollector();
        reader.accept(scic, 0);
        return scic.getScriptClassInfo();
    }
}
