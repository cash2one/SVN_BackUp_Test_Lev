/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.visibility;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.visibility.AccessCanBeTightenedInspection;
import com.siyeh.ig.LightInspectionTestCase;

public class AccessCanBeTightenedInspectionTest extends LightInspectionTestCase {
  public void testSimple() {
    doTest("import java.util.*;\n" +
           "class C {\n" +
           "    final int /*Access can be private*/fd/**/ = 0;\n" +
           "    /*Access can be private*/public/**/ int fd2;\n" +
           "    /*Access can be packageLocal*/public/**/ int forSubClass;\n" +
           "    @Override\n" +
           "    public int hashCode() {\n" +
           "      return fd + fd2;\n" + // use field
           "    }\n" +
           "" +
           " public void fff() {\n" +
           "   class Local {\n" +
           "        int /*Access can be private*/fd/**/;\n" +
           "        void f(){}\n" + // unused, ignore
           "        void /*Access can be private*/fd/**/(){}\n" +
           "        @Override\n" +
           "        public int hashCode() {\n" +
           "          fd();\n" +
           "          return fd;\n" +
           "        }\n" +
           "        class CantbePrivate {}\n" +
           "   }\n" +
           " }\n" +
           "}\n" +
           "class Over extends C {" +
           "  int r = forSubClass;" +
           "  @Override " +
           "  public void fff() {}" +
           "}");
  }
  public void testUseInAnnotation() {
    doTest("import java.util.*;\n" +
           "@interface Ann{ String value(); }\n" +
           "@Ann(value = C.VAL\n)" +
           "class C {\n" +
           "    /*Access can be packageLocal*/public/**/ static final String VAL = \"xx\";\n" +
           "}");
  }

  public void testSameFile() {
    doTest("class C {\n" +
           "  private static class Err {\n" +
           "    /*Access can be private*/public/**/ boolean isVisible() { return true; }\n" +
           "  }\n"+
           "  boolean f = new Err().isVisible();\n" +
           "}");
  }

  public void testAccessFromSubclass() {
    myFixture.allowTreeAccessForAllFiles();
    myFixture.addFileToProject("x/Sub.java",
      "package x; " +
      "import y.C; " +
      "class Sub extends C {\n" +
      "  boolean f = new Err().isTVisible();\n" +
      "}\n" +
      "");
    myFixture.addFileToProject("y/C.java",
      "package y; public class C {\n" +
      "  public static class Err {\n" +
      "    public boolean isTVisible() { return true; }\n" +
      "  }\n"+
      "}");
    myFixture.configureByFiles("y/C.java","x/Sub.java");
    myFixture.checkHighlighting();
  }

  @Override
  protected LocalInspectionTool getInspection() {
    return new AccessCanBeTightenedInspection();
  }
}