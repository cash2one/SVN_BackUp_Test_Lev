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
package com.intellij.configurationStore

import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.module.Module
import java.io.File

private open class ModuleStoreImpl(module: Module, private val pathMacroManager: PathMacroManager) : ComponentStoreImpl() {
  override val project = module.project

  override val storageManager = ModuleStateStorageManager(pathMacroManager.createTrackingSubstitutor(), module)

  override fun setPath(path: String) {
    if (!storageManager.addMacro(StoragePathMacros.MODULE_FILE, path)) {
      storageManager.getCachedFileStorages(listOf(StoragePathMacros.MODULE_FILE)).firstOrNull()?.setFile(null, File(path))
    }
  }

  override final fun getPathMacroManagerForDefaults() = pathMacroManager

  private class TestModuleStore(module: Module, pathMacroManager: PathMacroManager) : ModuleStoreImpl(module, pathMacroManager) {
    private var moduleComponentLoadPolicy: StateLoadPolicy? = null

    override fun setPath(path: String) {
      super.setPath(path)

      if (File(path).exists()) {
        moduleComponentLoadPolicy = StateLoadPolicy.LOAD
      }
    }

    override val loadPolicy: StateLoadPolicy
      get() = moduleComponentLoadPolicy ?: (project.stateStore as ComponentStoreImpl).loadPolicy
  }
}