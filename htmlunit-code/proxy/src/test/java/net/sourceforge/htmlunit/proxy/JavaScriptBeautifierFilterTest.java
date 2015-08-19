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
package net.sourceforge.htmlunit.proxy;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import sunlabs.brazil.filter.Filter;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.http.MimeHeaders;

/**
 * Test for {@link JavaScriptBeautifierFilter}.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5568 $
 */
public class JavaScriptBeautifierFilterTest {

    /**
     * Test HTML filtering.
     */
    @Test
    public void html() {
        final String source = "<html>\n"
            + "<head>\n"
            + "  <script>\n"
            + "  alert(1);"
            + "  </script>\n"
            + "  <script><!--\n"
            + "  alert(2);\n"
            + "  --></script>\n"
            + "</head>\n"
            + "<body>\n"
            + "</body>\n"
            + "</html>";

        final MimeHeaders headers = new MimeHeaders();
        headers.add("Content-Type", "text/html");
        final Filter filter = new JavaScriptBeautifierFilter();
        final Server server = new Server();
        server.props = new Properties();
        server.props.put("JavaScriptBeautifierFilter.beautifier", JavaScriptFunctionLogger.class.getName());
        filter.init(server, "JavaScriptBeautifierFilter.");

        final String filtered = new String(filter.filter(null, headers, source.getBytes()));
        assertTrue(filtered.replaceAll("\\s", "")
                .contains("setInterval(window.top.__HtmlUnitSendLog,1000);}alert(1);"));
        assertTrue(filtered.replaceAll("\\s", "").contains("<script><!--alert(2);--></script>"));
    }

}
