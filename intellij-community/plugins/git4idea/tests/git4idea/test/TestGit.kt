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
package git4idea.test

import git4idea.branch.GitRebaseParams
import git4idea.commands.GitCommandResult
import git4idea.commands.GitImpl
import git4idea.commands.GitLineHandlerListener
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

/**
 * Any unknown error that could be returned by Git.
 */
public val UNKNOWN_ERROR_TEXT: String = "unknown error"

public class TestGitImpl : GitImpl() {

  private var myRebaseShouldFail: (GitRepository) -> Boolean = { false }
  private var myPushHandler: (GitRepository) -> GitCommandResult? = { null }

  override fun push(repository: GitRepository,
                    remote: GitRemote,
                    spec: String,
                    force: Boolean,
                    updateTracking: Boolean,
                    tagMode: String?,
                    vararg listeners: GitLineHandlerListener): GitCommandResult {
    return myPushHandler(repository) ?:
        super.push(repository, remote, spec, force, updateTracking, tagMode, *listeners)
  }

  override fun rebase(repository: GitRepository, params: GitRebaseParams, vararg listeners: GitLineHandlerListener): GitCommandResult {
    return if (myRebaseShouldFail(repository))
      GitCommandResult(false, 128, listOf("fatal: error: $UNKNOWN_ERROR_TEXT"), emptyList<String>(), null)
    else
      super.rebase(repository, params, *listeners)
  }

  public fun setShouldRebaseFail(shouldFail: (GitRepository) -> Boolean) {
    myRebaseShouldFail = shouldFail
  }

  fun onPush(pushHandler: (GitRepository) -> GitCommandResult?) {
    myPushHandler = pushHandler;
  }

  fun reset() {
    myRebaseShouldFail = { false }
    myPushHandler = { null }
  }
}


