package jdk2.nashorn.internal.tools.nasgen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk2.nashorn.internal.objects.ArrayBufferView;
import jdk2.nashorn.internal.objects.NativeArray;
import jdk2.nashorn.internal.objects.NativeArrayBuffer;
import jdk2.nashorn.internal.objects.NativeBoolean;
import jdk2.nashorn.internal.objects.NativeDataView;
import jdk2.nashorn.internal.objects.NativeDate;
import jdk2.nashorn.internal.objects.NativeError;
import jdk2.nashorn.internal.objects.NativeEvalError;
import jdk2.nashorn.internal.objects.NativeFloat32Array;
import jdk2.nashorn.internal.objects.NativeFloat64Array;
import jdk2.nashorn.internal.objects.NativeFunction;
import jdk2.nashorn.internal.objects.NativeInt16Array;
import jdk2.nashorn.internal.objects.NativeInt32Array;
import jdk2.nashorn.internal.objects.NativeInt8Array;
import jdk2.nashorn.internal.objects.NativeJSAdapter;
import jdk2.nashorn.internal.objects.NativeJSON;
import jdk2.nashorn.internal.objects.NativeJava;
import jdk2.nashorn.internal.objects.NativeJavaImporter;
import jdk2.nashorn.internal.objects.NativeMath;
import jdk2.nashorn.internal.objects.NativeNumber;
import jdk2.nashorn.internal.objects.NativeObject;
import jdk2.nashorn.internal.objects.NativeRangeError;
import jdk2.nashorn.internal.objects.NativeReferenceError;
import jdk2.nashorn.internal.objects.NativeRegExp;
import jdk2.nashorn.internal.objects.NativeString;
import jdk2.nashorn.internal.objects.NativeSyntaxError;
import jdk2.nashorn.internal.objects.NativeTypeError;
import jdk2.nashorn.internal.objects.NativeURIError;
import jdk2.nashorn.internal.objects.NativeUint16Array;
import jdk2.nashorn.internal.objects.NativeUint32Array;
import jdk2.nashorn.internal.objects.NativeUint8Array;
import jdk2.nashorn.internal.objects.NativeUint8ClampedArray;

public class JavaMain {

    public static void main(String[] args) throws Exception {
        List<Class<?>> list = Arrays.asList(NativeFunction.class, NativeObject.class, NativeArray.class,
                NativeBoolean.class, NativeDate.class,
                NativeJSON.class, NativeJSAdapter.class,
                NativeMath.class, NativeNumber.class, NativeRegExp.class, 
                NativeString.class, NativeError.class, NativeEvalError.class,
                NativeRangeError.class, NativeReferenceError.class,
                NativeSyntaxError.class, NativeTypeError.class, NativeURIError.class,
                NativeJavaImporter.class, NativeJava.class,
                NativeArrayBuffer.class, NativeDataView.class,
                NativeInt8Array.class, NativeUint8Array.class, NativeUint8ClampedArray.class,
                NativeInt16Array.class, NativeUint16Array.class,
                NativeInt32Array.class, NativeUint32Array.class,
                NativeFloat32Array.class, NativeFloat64Array.class,
                ArrayBufferView.class);
        String packageName = NativeArray.class.getName();
        packageName = packageName.substring(0, packageName.lastIndexOf('.'));
        File srcRoot = new File("src/main/java/");
        File srcFolder = new File(srcRoot, packageName.replace('.', '/'));
        File binRoot = new File("target/classes");
        for (File file : srcFolder.listFiles()) {
            if (file.isFile()) {
                String className = file.getName();
                className = className.substring(0, className.lastIndexOf('.'));
                Class<?> c = Class.forName(packageName + '.' + className);
                if (list.contains(c)) {
                    String fileName = binRoot.getAbsolutePath() + '/'
                            + c.getName().replace('.', '/') + ".class";
                    if (!new File(binRoot.getAbsolutePath() + '/'
                            + c.getName().replace('.', '/') + "$Constructor.class").exists()) {
                        final ScriptClassInfo sci = ClassJavaGenerator.getScriptClassInfo(fileName);
                        if (sci != null) {
                            File javaFile = new File(srcRoot, c.getName().replace('.', '/') + ".java");
                            List<String> lines = readLines(javaFile);
                            for (int i = lines.size() - 1; i >= 0; i--) {
                                String line = lines.get(i);
                                lines.remove(i);
                                if (line.trim().equals("}")) {
                                    break;
                                }
                            }
                            try (Writer writer = new BufferedWriter(new FileWriter(javaFile))) {
                                for (String line : lines) {
                                    writer.write(line);
                                    writer.write(System.lineSeparator());
                                }
                                writer.write(System.lineSeparator());
                                writer.write(ScriptClassJavaInstrumentor.getString(fileName));
                                writer.write(ConstructorJavaGenerator.getString(fileName));
                                writer.write(PrototypeJavaGenerator.getString(fileName));
                                writer.write("}" + System.lineSeparator());
                            }
                        }
                    }
                }
            }
        }
    }
    public static List<String> readLines(File file) throws IOException {
        final List<String> lines = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
