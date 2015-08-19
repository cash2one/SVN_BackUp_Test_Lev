/*
 * Copyright (c) 2010 HtmlUnit team.
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
package net.sourceforge.htmlunit.proxy.webapp.shared;

import java.io.Serializable;
import java.util.Date;

/**
 * A log Entry.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5525 $
 */
public class LogEntry implements Serializable {

    private static final long serialVersionUID = -659843620880309549L;

    private String value_;
    private String time_;

    /**
     * Default constructor, used for serialization, don't call.
     * @deprecated
     */
    @Deprecated
    public LogEntry() {
    }

    /**
     * Constructs a new log entry.
     * @param value the log value
     */
    public LogEntry(final String value) {
        value_ = value;
        time_ = new Date().toString();
    }

    /**
     * Returns the value.
     * @return the value
     */
    public String getValue() {
        return value_;
    }

    /**
     * The creation time of this entry.
     * @return the time as a string
     */
    public String getTime() {
        return time_;
    }
}
