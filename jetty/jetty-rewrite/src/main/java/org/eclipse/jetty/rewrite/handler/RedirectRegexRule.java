//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.rewrite.handler;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

/**
 * Issues a (3xx) Redirect response whenever the rule finds a match via regular expression.
 * <p>
 * The replacement string may use $n" to replace the nth capture group.
 * <p>
 * All redirects are part of the <a href="http://tools.ietf.org/html/rfc7231#section-6.4"><code>3xx Redirection</code> status code set</a>.
 * <p>
 * Defaults to <a href="http://tools.ietf.org/html/rfc7231#section-6.4.3"><code>302 Found</code></a>
 */
public class RedirectRegexRule extends RegexRule
{
    protected String _replacement;
    private int _statusCode = HttpStatus.FOUND_302;
    
    public RedirectRegexRule()
    {
        _handling = true;
        _terminating = true;
    }

    /**
     * Whenever a match is found, it replaces with this value.
     * 
     * @param replacement the replacement string.
     */
    public void setReplacement(String replacement)
    {
        _replacement = replacement;
    }
    
    /**
     * Sets the redirect status code.
     * 
     * @param statusCode the 3xx redirect status code
     */
    public void setStatusCode(int statusCode)
    {
        if ((300 <= statusCode) || (statusCode >= 399))
        {
            _statusCode = statusCode;
        }
        else
        {
            throw new IllegalArgumentException("Invalid redirect status code " + statusCode + " (must be a value between 300 and 399)");
        }
    }
    
    @Override
    protected String apply(String target, HttpServletRequest request, HttpServletResponse response, Matcher matcher)
            throws IOException
    {
        target=_replacement;
        for (int g=1;g<=matcher.groupCount();g++)
        {
            String group = matcher.group(g);
            target=target.replaceAll("\\$"+g,group);
        }
        
        target = response.encodeRedirectURL(target);
        response.setHeader("Location",RedirectUtil.toRedirectURL(request,target));
        response.setStatus(_statusCode);
        response.getOutputStream().flush(); // no output / content
        response.getOutputStream().close();
        return target;
    }
    
    /**
     * Returns the redirect status code and replacement.
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append(super.toString());
        str.append('[').append(_statusCode);
        str.append('>').append(_replacement);
        str.append(']');
        return str.toString();
    }

}
