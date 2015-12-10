/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.impl.cache.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author max
 */
public class SourceRootAddedAsLibraryRootTest extends PsiTestCase {
  private VirtualFile myDir;
  private VirtualFile myVFile;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final File root = createTempDirectory();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          VirtualFile rootVFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(root.getAbsolutePath().replace(File.separatorChar, '/'));

          myDir = rootVFile.createChildDirectory(null, "contentAndLibrary");

          PsiTestUtil.addSourceRoot(myModule, myDir);
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    });
  }

  private void changeRoots() {
    ModuleRootModificationUtil.addModuleLibrary(myModule, myDir.getUrl());
  }

  public void testBug() throws Exception {
    touchFileSync();
    PsiFile psiFile = myPsiManager.findFile(myVFile);
    psiFile.getText();
    changeRoots();
  }

  private void touchFileSync() throws IOException {
    myVFile = myDir.createChildData(null, "A.java");
    VfsUtil.saveText(myVFile, "package p; public class A{ public void foo(); }");
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
  }
}
