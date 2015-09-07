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

package org.eclipse.jetty.start.builders;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.start.BaseBuilder;
import org.eclipse.jetty.start.BaseHome;
import org.eclipse.jetty.start.Module;
import org.eclipse.jetty.start.Props;
import org.eclipse.jetty.start.StartLog;
import org.eclipse.jetty.start.graph.OnlyTransitivePredicate;

/**
 * Management of the <code>${jetty.base}/start.ini</code> based configuration.
 * <p>
 * Implementation of the <code>--add-to-start=[name]</code> command line behavior
 */
public class StartIniBuilder implements BaseBuilder.Config
{
    private final BaseHome baseHome;
    private final Path startIni;

    /* List of modules already present in start.ini */
    private Set<String> modulesPresent = new HashSet<>();

    /* List of properties (keys only) already present in start.ini */
    private Set<String> propsPresent = new HashSet<>();

    public StartIniBuilder(BaseBuilder baseBuilder) throws IOException
    {
        this.baseHome = baseBuilder.getBaseHome();
        this.startIni = baseHome.getBasePath("start.ini");

        if (Files.exists(startIni))
        {
            parseIni();
        }
    }

    private void parseIni() throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(startIni,StandardCharsets.UTF_8))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.startsWith("--module="))
                {
                    List<String> moduleNames = Props.getValues(line);
                    this.modulesPresent.addAll(moduleNames);
                }
                else if (!line.startsWith("-") && line.contains("="))
                {
                    String key = line.substring(0,line.indexOf('='));
                    this.propsPresent.add(key);
                }
            }
        }
    }

    @Override
    public boolean addModule(Module module) throws IOException
    {
        if (modulesPresent.contains(module.getName()))
        {
            StartLog.info("%-15s already initialised in %s",module.getName(),baseHome.toShortForm(startIni));
            // skip, already present
            return false;
        }

        if (module.isDynamic())
        {
            if (module.hasIniTemplate())
            {
                // warn
                StartLog.warn("%-15s not adding [ini-template] from dynamic module",module.getName());
            }
            return false;
        }

        String mode = "";
        boolean isTransitive = module.matches(OnlyTransitivePredicate.INSTANCE);
        if (isTransitive)
        {
            mode = "(transitively) ";
        }

        if (module.hasIniTemplate() || !isTransitive)
        {
            StartLog.info("%-15s initialised %sin %s",module.getName(),mode,baseHome.toShortForm(startIni));

            // Append to start.ini
            try (BufferedWriter writer = Files.newBufferedWriter(startIni,StandardCharsets.UTF_8,StandardOpenOption.APPEND,StandardOpenOption.CREATE))
            {
                writeModuleSection(writer,module);
            }
            return true;
        }

        return false;
    }

    protected void writeModuleSection(BufferedWriter writer, Module module)
    {
        PrintWriter out = new PrintWriter(writer);

        out.println("# --------------------------------------- ");
        out.println("# Module: " + module.getName());
        out.println("--module=" + module.getName());
        out.println();

        for (String line : module.getIniTemplate())
        {
            out.println(line);
        }

        out.println();
        out.flush();
    }
}
