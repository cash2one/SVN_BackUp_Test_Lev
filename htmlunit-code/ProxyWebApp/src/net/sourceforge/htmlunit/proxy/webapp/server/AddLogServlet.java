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
package net.sourceforge.htmlunit.proxy.webapp.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.htmlunit.proxy.webapp.shared.LogEntry;

/**
 * Servlet to add a log entry.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5525 $
 */
public class AddLogServlet extends HttpServlet {

    private static final long serialVersionUID = -704495463938248705L;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final Reader reader = req.getReader();
        final StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        reader.close();
        LogServiceImpl.addLog(new LogEntry(sb.toString()));
        resp.setStatus(HttpServletResponse.SC_OK);
        final Writer writer = resp.getWriter();
        writer.write("Ok!");
        writer.close();
    }

}
