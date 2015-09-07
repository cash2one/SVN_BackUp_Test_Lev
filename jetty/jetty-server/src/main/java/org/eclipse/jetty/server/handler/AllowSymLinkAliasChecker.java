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

package org.eclipse.jetty.server.handler;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;


/* ------------------------------------------------------------ */
/** Symbolic Link AliasChecker.
 * <p>An instance of this class can be registered with {@link ContextHandler#addAliasCheck(AliasCheck)}
 * to check resources that are aliased to other locations.   The checker uses the 
 * Java {@link Files#readSymbolicLink(Path)} and {@link Path#toRealPath(java.nio.file.LinkOption...)}
 * APIs to check if a file is aliased with symbolic links.</p>
 */
public class AllowSymLinkAliasChecker implements AliasCheck
{
    private static final Logger LOG = Log.getLogger(AllowSymLinkAliasChecker.class);
    
    @Override
    public boolean check(String uri, Resource resource)
    {
        // Only support PathResource alias checking
        if (!(resource instanceof PathResource))
            return false;
        
        PathResource pathResource = (PathResource)resource;

        try
        {
            Path path = pathResource.getPath();
            Path alias = pathResource.getAliasPath();
            
            // is the file itself a symlink?
            if (Files.isSymbolicLink(path))
            {        
                alias = path.getParent().resolve(alias);
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("path ={}",path);
                    LOG.debug("alias={}",alias);
                }
                if (Files.isSameFile(path,alias))
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Allow symlink {} --> {}",resource,pathResource.getAliasPath());
                    return true;
                }
            }
            
            // No, so let's check each element ourselves
            Path d = path.getRoot();
            for (Path e:path)
            {
                d=d.resolve(e);
                
                while (Files.exists(d) && Files.isSymbolicLink(d))
                {
                    Path link=Files.readSymbolicLink(d);                    
                    if (!link.isAbsolute())
                        link=d.resolve(link);
                    d=link;
                }
            }
            if (pathResource.getAliasPath().equals(d))
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Allow path symlink {} --> {}",resource,d);
                return true;
            }
        }
        catch(Exception e)
        {
            LOG.ignore(e);
        }
        
        return false;
    }

}
