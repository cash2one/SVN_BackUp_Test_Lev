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
package net.sourceforge.htmlunit.proxy.webapp.client;

import net.sourceforge.htmlunit.proxy.webapp.shared.LogEntry;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5525 $
 */
@RemoteServiceRelativePath("getLog")
public interface LogService extends RemoteService {

    /**
     * Returns the logs.
     * @param index the starting index
     * @return the logs
     */
    LogEntry[] getLog(final int index);

}
