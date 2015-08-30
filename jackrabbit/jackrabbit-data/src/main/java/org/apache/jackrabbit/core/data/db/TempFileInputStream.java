/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.data.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;

import org.apache.commons.io.input.ClosedInputStream;

/**
 * An input stream from a temporary file. The file is deleted when the stream is
 * closed or garbage collected.
 */
public class TempFileInputStream extends FilterInputStream {

    private final File file;

    public TempFileInputStream(File file) throws FileNotFoundException {
        this(new FileInputStream(file), file);
    }

    protected TempFileInputStream(FileInputStream in, File file) {
        super(in);
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        in.close();
        in = new ClosedInputStream();
        file.delete();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
