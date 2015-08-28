/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.jsp.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.taskdefs.optional.jsp.JspMangler;

/**
 * The interface that all jsp compiler adapters must adher to.
 *
 * <p>A compiler adapter is an adapter that interprets the jspc's
 * parameters in preperation to be passed off to the compiler this
 * adapter represents.  As all the necessary values are stored in the
 * Jspc task itself, the only thing all adapters need is the jsp
 * task, the execute command and a parameterless constructor (for
 * reflection).</p>
 *
 */

public interface JspCompilerAdapter {

    /**
     * Sets the compiler attributes, which are stored in the Jspc task.
     * @param attributes the jsp compiler attributes
     */
    void setJspc(JspC attributes);

    /**
     * Executes the task.
     *
     * @return has the compilation been successful
     * @throws BuildException on error
     */
    boolean execute() throws BuildException;

    /**
     * @return an instance of the mangler this compiler uses
     */

    JspMangler createMangler();

    /**
     * ask if compiler can sort out its own dependencies
     * @return true if the compiler wants to do its own
     * depends
     */
    boolean implementsOwnDependencyChecking();
}
