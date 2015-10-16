// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;

namespace Microsoft.Data.Entity.Commands.TestUtilities
{
    public class BuildSource
    {
        public ICollection<BuildReference> References
        { get; }
        = new List<BuildReference>
            {
#if !DNXCORE50
            BuildReference.ByName("mscorlib")
#else
            BuildReference.ByName("System.Runtime")
#endif
        };

        public string TargetDir { get; set; }
        public ICollection<string> Sources { get; } = new List<string>();

        public BuildFileResult Build()
        {
            var projectName = Path.GetRandomFileName();
            var references = new List<MetadataReference>();

            foreach (var reference in References)
            {
                if (reference.CopyLocal)
                {
                    File.Copy(reference.Path, Path.Combine(TargetDir, Path.GetFileName(reference.Path)));
                }

                references.Add(reference.Reference);
            }

            var compilation = CSharpCompilation.Create(
                projectName,
                Sources.Select(s => SyntaxFactory.ParseSyntaxTree(s)),
                references,
                new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));

            var targetPath = Path.Combine(TargetDir, projectName + ".dll");

            using (var stream = File.OpenWrite(targetPath))
            {
                var result = compilation.Emit(stream);
                if (!result.Success)
                {
                    throw new InvalidOperationException(
                        $"Build failed. Diagnostics: {string.Join(Environment.NewLine, result.Diagnostics)}");
                }
            }

            return new BuildFileResult(targetPath);
        }

        public Assembly BuildInMemory()
        {
            var projectName = Path.GetRandomFileName();
            var references = new List<MetadataReference>();

            foreach (var reference in References)
            {
                if (reference.CopyLocal)
                {
                    throw new InvalidOperationException("Assemblies cannot be copied locally for in-memory builds.");
                }

                references.Add(reference.Reference);
            }

            var compilation = CSharpCompilation.Create(
                projectName,
                Sources.Select(s => SyntaxFactory.ParseSyntaxTree(s)),
                references,
                new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));

            Assembly assembly;
            using (var stream = new MemoryStream())
            {
                var result = compilation.Emit(stream);
                if (!result.Success)
                {
                    throw new InvalidOperationException(
                        $"Build failed. Diagnostics: {string.Join(Environment.NewLine, result.Diagnostics)}");
                }

#if !DNXCORE50
                assembly = Assembly.Load(stream.ToArray());
#else
                assembly = (Assembly)typeof(Assembly).GetTypeInfo().GetDeclaredMethods("Load")
                    .First(
                        m =>
                        {
                            var parameters = m.GetParameters();

                            return parameters.Length == 1 && parameters[0].ParameterType == typeof(byte[]);
                        })
                    .Invoke(null, new[] { stream.ToArray() });
#endif
            }

            return assembly;
        }
    }
}
