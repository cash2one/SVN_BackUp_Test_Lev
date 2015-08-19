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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import macromedia.asc.embedding.avmplus.ActionBlockConstants;

/**
 * Utility class.
 *
 * @version $Revision: 7359 $
 * @author Ahmed Ashour
 */
public final class Util {

    private Util() {
    }

    public static String getOpcodeName(final int op) {
        try {
            for (final Field f : ActionBlockConstants.class.getFields()) {
                if (f.getName().startsWith("OP_") && (Integer) f.get(null) == op) {
                    return f.getName().substring(3);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void processDirectory(File directory, String pkgname, List<String> classes) {
        String[] files = directory.list();
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            String className = null;
            if (fileName.endsWith(".class")) {
                className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
            }
            if (className != null) {
                classes.add(className);
            }
            final File subdir = new File(directory, fileName);
            if (subdir.isDirectory()) {
                processDirectory(subdir, pkgname + '.' + fileName, classes);
            }
        }
    }

    private static void processJar(URL url, String packageName, List<String> classes) {
        String relPath = packageName.replace('.', '/');
        String resPath = url.getPath();
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarPath);         
        }
        catch (IOException e) {
            throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }
        for(Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
            final JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String className = null;
            if(entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
            }
            if (className != null) {
                classes.add(className);
            }
        }
    }
    
    public static List<String> getClassNamesForPackage(final Package p) {
        final List<String> classes = new ArrayList<String>();
        
        final String name = p.getName();
        final String path = name.replace('.', '/');
    
        final URL resource = ClassLoader.getSystemClassLoader().getResource(path);

        if(resource.toString().startsWith("jar:")) {
            processJar(resource, name, classes);
        }
        else {
            processDirectory(new File(resource.getPath()), name, classes);
        }

        return classes;
    }
}
