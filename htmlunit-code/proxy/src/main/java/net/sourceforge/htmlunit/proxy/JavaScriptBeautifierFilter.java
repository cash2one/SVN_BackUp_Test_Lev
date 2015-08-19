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
package  net.sourceforge.htmlunit.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

import org.paw.server.PawMain;

import sunlabs.brazil.filter.Filter;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.http.MimeHeaders;

/**
 * Beautifier filter.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5595 $
 */
public class JavaScriptBeautifierFilter implements Filter {

    private static String LOCALHOST_ADDRESS_;
    private static final int SERVER_PORT_;
    private JavaScriptBeautifier beautifier_;

    static {
        if (PawMain.getServer() != null) {
            SERVER_PORT_ = PawMain.getServer().getPort();
        }
        else {
            SERVER_PORT_ = -1;
        }
        try {
            LOCALHOST_ADDRESS_ = InetAddress.getLocalHost().getHostAddress();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean init(final Server server, final String prefix) {
        final String className = server.props.getProperty(prefix + "beautifier");
        try {
            beautifier_ = (JavaScriptBeautifier) Class.forName(className).newInstance();
            beautifier_.setLoggingMethodName("window.top.__HtmlUnitLog");
            return true;
        }
        catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean respond(final Request request) throws IOException {
        if (request.url.endsWith("/__HtmlUnitLogger") && request.postData != null
                && beautifier_.getClass() != JavaScriptBeautifier.class) {
            final String log = new String(request.postData);
            WebApplUtils.addLog(log);
            request.sendResponse("");
            return true;
        }
        String urlString = request.url;
        if (urlString.startsWith("/")) {
            urlString = "http://localhost:" + SERVER_PORT_ + urlString;
        }
        final URL url = new URL(urlString);
        if (url.getPort() == SERVER_PORT_
                && (url.getHost().equals("localhost")
                || InetAddress.getByName(url.getHost()).getHostAddress().equals(LOCALHOST_ADDRESS_))) {
            try {
                WebApplUtils.respond(request);
                return true;
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean shouldFilter(final Request request, final MimeHeaders headers) {
        final String type = headers.get("Content-Type");
        return type != null
            && (type.equals("text/javascript") || type.equals("application/x-javascript")
                || type.equals("text/html"));
    }

    /**
     * {@inheritDoc}
     */
    public byte[] filter(final Request request, final MimeHeaders headers, final byte[] content) {
        final String type = headers.get("Content-Type");
        if (type.equals("text/html")) {
            return filterHtml(new String(content));
        }
        return filterJavaScript(new String(content), true);
    }

    private byte[] filterJavaScript(final String content, final boolean addLogMethods) {
        String beauty = beautifier_.beautify(content);
        if (addLogMethods && beautifier_.getClass() != JavaScriptBeautifier.class) {
            beauty = "\nif (!window.top.__HtmlUnitLog) {\n"
                + "  window.top.__HtmlUnitLogged = '';\n"
                + "  window.top.__HtmlUnitLogger = window.XMLHttpRequest "
                + "? new XMLHttpRequest() : new ActiveXObject('Microsoft.XMLHTTP');\n"
                + "  window.top.__HtmlUnitLog = function(data) {\n"
                + "    window.top.__HtmlUnitLogged += data + '\\n';\n"
                + "  }\n"
                + "  window.top.__HtmlUnitSendLog = function() {\n"
                + "    var req = window.top.__HtmlUnitLogger;\n"
                + "    req.open('POST', '/__HtmlUnitLogger', false);\n"
                + "    req.send(window.top.__HtmlUnitLogged);\n"
                + "    window.top.__HtmlUnitLogged = '';\n"
                + "  }\n"
                + "  window.top.__HtmlUnitLogging = false;\n"
                + "  window.top.setInterval(window.top.__HtmlUnitSendLog, 1000);\n"
                + "}\n"
                + beauty;
        }
        return beauty.getBytes();
    }

    private byte[] filterHtml(String content) {
        boolean firstJavaScript = true;
        int p0 = content.indexOf("<script>", 0);
        while (p0 != -1) {
            int p1 = content.indexOf("</script>", p0);
            p0 += 8;
            while (Character.isWhitespace(content.charAt(p0))) {
                p0++;
            }
            if (content.substring(p0, p0 + 4).equals("<!--")) {
                while (content.charAt(p0) != '\n') {
                    p0++;
                }
                p0++;
                while (content.charAt(p1) != '\n' && p1 > p0 + 1) {
                    p1--;
                }
            }
            final String js = content.substring(p0, p1);
            content = content.substring(0, p0)
                + new String(filterJavaScript(js, firstJavaScript)) + content.substring(p1);
            p0 = content.indexOf("<script>", p1);
            firstJavaScript = false;
        }
        return content.getBytes();
    }

}
