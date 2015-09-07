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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Base container to group rules. Can be extended so that the contained rules
 * will only be applied under certain conditions
 */
public class RuleContainer extends Rule
{
    private static final Logger LOG = Log.getLogger(RuleContainer.class);

    protected Rule[] _rules;
    
    protected String _originalPathAttribute;
    protected boolean _rewriteRequestURI=true;
    protected boolean _rewritePathInfo=true;
     
    /* ------------------------------------------------------------ */
    /**
     * Returns the list of rules.
     * @return an array of {@link Rule}.
     */
    public Rule[] getRules()
    {
        return _rules;
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the rules to process.
     * @param rules an array of {@link Rule}. 
     */
    public void setRules(Rule[] rules)
    {
        _rules = rules;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add a Rule
     * @param rule The rule to add to the end of the rules array
     */
    public void addRule(Rule rule)
    {
        _rules = ArrayUtil.addToArray(_rules,rule,Rule.class);
    }
   

    /* ------------------------------------------------------------ */
    /**
     * @return the rewriteRequestURI If true, this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public boolean isRewriteRequestURI()
    {
        return _rewriteRequestURI;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewriteRequestURI true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public void setRewriteRequestURI(boolean rewriteRequestURI)
    {
        _rewriteRequestURI=rewriteRequestURI;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public boolean isRewritePathInfo()
    {
        return _rewritePathInfo;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewritePathInfo true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public void setRewritePathInfo(boolean rewritePathInfo)
    {
        _rewritePathInfo=rewritePathInfo;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the originalPathAttribte. If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public String getOriginalPathAttribute()
    {
        return _originalPathAttribute;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param originalPathAttribte If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public void setOriginalPathAttribute(String originalPathAttribte)
    {
        _originalPathAttribute=originalPathAttribte;
    }
    
    /**
     * Process the contained rules
     * @param target target field to pass on to the contained rules
     * @param request request object to pass on to the contained rules
     * @param response response object to pass on to the contained rules
     */
    @Override
    public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return apply(target, request, response);
    }

    /**
     * Process the contained rules (called by matchAndApply) 
     * @param target target field to pass on to the contained rules
     * @param request request object to pass on to the contained rules
     * @param response response object to pass on to the contained rules
     * @return the target
     * @throws IOException if unable to apply the rule
     */
    protected String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        boolean original_set=_originalPathAttribute==null;
                
        for (Rule rule : _rules)
        {
            String applied=rule.matchAndApply(target,request, response);
            if (applied!=null)
            {       
                LOG.debug("applied {}",rule);
                LOG.debug("rewrote {} to {}",target,applied);
                if (!original_set)
                {
                    original_set=true;
                    request.setAttribute(_originalPathAttribute, target);
                }     

                if (_rewriteRequestURI)
                {
                    String encoded=URIUtil.encodePath(applied);
                    if (rule instanceof Rule.ApplyURI)
                        ((Rule.ApplyURI)rule).applyURI((Request)request,((Request)request).getRequestURI(), encoded);
                    else
                        ((Request)request).setURIPathQuery(encoded);
                }

                if (_rewritePathInfo)
                    ((Request)request).setPathInfo(applied);

                target=applied;
                
                if (rule.isHandling())
                {
                    LOG.debug("handling {}",rule);
                    Request.getBaseRequest(request).setHandled(true);
                }

                if (rule.isTerminating())
                {
                    LOG.debug("terminating {}",rule);
                    break;
                }
            }
        }

        return target;
    }
}
