// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

#if NET46

using System;
using System.IO;
using System.Linq;
using Microsoft.Data.Entity.Commands.TestUtilities;
using Xunit;

namespace Microsoft.Data.Entity.Design.Internal
{
    public class OperationExecutorTest
    {
        public class SimpleProjectTest : IClassFixture<SimpleProjectTest.SimpleProject>
        {
            private readonly SimpleProject _project;

            public SimpleProjectTest(SimpleProject project)
            {
                _project = project;
            }

            [Fact]
            public void GetContextType_works_cross_domain()
            {
                var contextTypeName = _project.Executor.GetContextType("SimpleContext");
                Assert.StartsWith("SimpleProject.SimpleContext, ", contextTypeName);
            }

            [Fact]
            public void AddMigration_works_cross_domain()
            {
                var artifacts = _project.Executor.AddMigration("EmptyMigration", "Migrationz", "SimpleContext");
                Assert.Equal(3, artifacts.Count());
                Assert.True(Directory.Exists(Path.Combine(_project.TargetDir, @"Migrationz")));
            }

            [Fact]
            public void ScriptMigration_works_cross_domain()
            {
                var sql = _project.Executor.ScriptMigration(null, "InitialCreate", false, "SimpleContext");
                Assert.NotEmpty(sql);
            }

            [Fact]
            public void GetContextTypes_works_cross_domain()
            {
                var contextTypes = _project.Executor.GetContextTypes();
                Assert.Equal(1, contextTypes.Count());
            }

            [Fact]
            public void GetMigrations_works_cross_domain()
            {
                var migrations = _project.Executor.GetMigrations("SimpleContext");
                Assert.Equal(1, migrations.Count());
            }

            public class SimpleProject : IDisposable
            {
                private readonly TempDirectory _directory = new TempDirectory();

                public SimpleProject()
                {
                    var source = new BuildSource
                    {
                        TargetDir = TargetDir,
                        References =
                                {
                                    BuildReference.ByName("System.Collections.Immutable", copyLocal: true),
                                    BuildReference.ByName("System.Interactive.Async", copyLocal: true),
                                    BuildReference.ByName("System.Data, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089"),
                                    BuildReference.ByName("EntityFramework.Core", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.Commands", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.Relational", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.Relational.Design", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.MicrosoftSqlServer", copyLocal: true),
                                    BuildReference.ByName("Microsoft.CodeAnalysis", copyLocal: true),
                                    BuildReference.ByName("Microsoft.CodeAnalysis.CSharp", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Caching.Abstractions", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Caching.Memory", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.DependencyInjection", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.DependencyInjection.Abstractions", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Logging", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Logging.Abstractions", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.OptionsModel", copyLocal: true),
                                    BuildReference.ByName("Remotion.Linq", copyLocal: true),
                                    BuildReference.ByName("System.Diagnostics.Tracing.Telemetry", copyLocal: true)
                                },
                        Sources = { @"
                            using Microsoft.Data.Entity;
                            using Microsoft.Data.Entity.Infrastructure;
                            using Microsoft.Data.Entity.Migrations;

                            namespace SimpleProject
                            {
                                internal class SimpleContext : DbContext
                                {
                                    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
                                    {
                                        optionsBuilder.UseSqlServer(""Data Source=(localdb)\\MSSQLLocalDB;Initial Catalog=SimpleProject.SimpleContext;Integrated Security=True"");
                                    }
                                }

                                namespace Migrations
                                {
                                    [DbContext(typeof(SimpleContext))]
                                    [Migration(""20141010222726_InitialCreate"")]
                                    public class InitialCreate : Migration
                                    {
                                        protected override void Up(MigrationBuilder migrationBuilder)
                                        {
                                        }
                                    }
                                }
                            }" }
                    };
                    var build = source.Build();
                    Executor = new OperationExecutorWrapper(TargetDir, build.TargetName, TargetDir, "SimpleProject");
                }

                public string TargetDir
                {
                    get { return _directory.Path; }
                }

                public OperationExecutorWrapper Executor { get; }

                public void Dispose()
                {
                    Executor.Dispose();
                    _directory.Dispose();
                }
            }
        }

        [Fact]
        public void GetMigrations_filters_by_context_name()
        {
            using (var directory = new TempDirectory())
            {
                var targetDir = directory.Path;
                var source = new BuildSource
                {
                    TargetDir = targetDir,
                    References =
                            {
                                BuildReference.ByName("System.Collections.Immutable", copyLocal: true),
                                BuildReference.ByName("System.Interactive.Async", copyLocal: true),
                                BuildReference.ByName("System.Data, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089"),
                                BuildReference.ByName("EntityFramework.Core", copyLocal: true),
                                BuildReference.ByName("EntityFramework.Commands", copyLocal: true),
                                BuildReference.ByName("EntityFramework.Relational", copyLocal: true),
                                BuildReference.ByName("EntityFramework.Relational.Design", copyLocal: true),
                                BuildReference.ByName("EntityFramework.MicrosoftSqlServer", copyLocal: true),
                                BuildReference.ByName("Microsoft.CodeAnalysis", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.Caching.Abstractions", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.Caching.Memory", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.DependencyInjection", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.DependencyInjection.Abstractions", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.Logging", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.Logging.Abstractions", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.OptionsModel", copyLocal: true),
                                BuildReference.ByName("Remotion.Linq", copyLocal: true),
                                BuildReference.ByName("System.Diagnostics.Tracing.Telemetry", copyLocal: true)
                            },
                    Sources = { @"
                        using Microsoft.Data.Entity;
                        using Microsoft.Data.Entity.Infrastructure;
                        using Microsoft.Data.Entity.Migrations;

                        namespace MyProject
                        {
                            internal class Context1 : DbContext
                            {
                                protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
                                {
                                    optionsBuilder.UseSqlServer(""Data Source=(localdb)\\MSSQLLocalDB;Initial Catalog=SimpleProject.SimpleContext;Integrated Security=True"");
                                }
                            }

                            internal class Context2 : DbContext
                            {
                            }

                            namespace Migrations
                            {
                                namespace Context1Migrations
                                {
                                    [DbContext(typeof(Context1))]
                                    [Migration(""000000000000000_Context1Migration"")]
                                    public class Context1Migration : Migration
                                    {
                                        protected override void Up(MigrationBuilder migrationBuilder)
                                        {
                                        }
                                    }
                                }

                                namespace Context2Migrations
                                {
                                    [DbContext(typeof(Context2))]
                                    [Migration(""000000000000000_Context2Migration"")]
                                    public class Context2Migration : Migration
                                    {
                                        protected override void Up(MigrationBuilder migrationBuilder)
                                        {
                                        }
                                    }
                                }
                            }
                        }" }
                };
                var build = source.Build();
                using (var executor = new OperationExecutorWrapper(targetDir, build.TargetName, targetDir, "MyProject"))
                {
                    var migrations = executor.GetMigrations("Context1");

                    Assert.Equal(1, migrations.Count());
                }
            }
        }

        [Fact]
        public void GetContextType_works_with_multiple_assemblies()
        {
            using (var directory = new TempDirectory())
            {
                var targetDir = directory.Path;
                var contextsSource = new BuildSource
                {
                    TargetDir = targetDir,
                    References =
                            {
                                BuildReference.ByName("EntityFramework.Core", copyLocal: true),
                                BuildReference.ByName("EntityFramework.Commands", copyLocal: true)
                            },
                    Sources = { @"
                        using Microsoft.Data.Entity;

                        namespace MyProject
                        {
                            public class Context1 : DbContext
                            {
                            }

                            public class Context2 : DbContext
                            {
                            }
                        }" }
                };
                var contextsBuild = contextsSource.Build();
                var migrationsSource = new BuildSource
                {
                    TargetDir = targetDir,
                    References =
                            {
                                BuildReference.ByName("System.Collections.Immutable", copyLocal: true),
                                BuildReference.ByName("System.Reflection.Metadata", copyLocal: true),
                                BuildReference.ByName("EntityFramework.Core"),
                                BuildReference.ByName("EntityFramework.Relational", copyLocal: true),
                                BuildReference.ByName("EntityFramework.Relational.Design", copyLocal: true),
                                BuildReference.ByName("Microsoft.CodeAnalysis", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.DependencyInjection", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.DependencyInjection.Abstractions", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.Logging", copyLocal: true),
                                BuildReference.ByName("Microsoft.Extensions.Logging.Abstractions", copyLocal: true),
                                BuildReference.ByPath(contextsBuild.TargetPath)
                            },
                    Sources = { @"
                        using Microsoft.Data.Entity;
                        using Microsoft.Data.Entity.Infrastructure;
                        using Microsoft.Data.Entity.Migrations;

                        namespace MyProject
                        {
                            internal class Context3 : DbContext
                            {
                            }

                            namespace Migrations
                            {
                                namespace Context1Migrations
                                {
                                    [DbContext(typeof(Context1))]
                                    [Migration(""000000000000000_Context1Migration"")]
                                    public class Context1Migration : Migration
                                    {
                                        protected override void Up(MigrationBuilder migrationBuilder)
                                        {
                                        }
                                    }
                                }

                                namespace Context2Migrations
                                {
                                    [DbContext(typeof(Context2))]
                                    [Migration(""000000000000000_Context2Migration"")]
                                    public class Context2Migration : Migration
                                    {
                                        protected override void Up(MigrationBuilder migrationBuilder)
                                        {
                                        }
                                    }
                                }
                            }
                        }" }
                };
                var migrationsBuild = migrationsSource.Build();
                using (var executor = new OperationExecutorWrapper(targetDir, migrationsBuild.TargetName, targetDir, "MyProject"))
                {
                    var contextTypes = executor.GetContextTypes();

                    Assert.Equal(3, contextTypes.Count());
                }
            }
        }

        [Fact]
        public void AddMigration_begins_new_namespace_when_foreign_migrations()
        {
            using (var directory = new TempDirectory())
            {
                var targetDir = directory.Path;
                var source = new BuildSource
                {
                    TargetDir = targetDir,
                    References =
                                {
                                    BuildReference.ByName("System.Collections.Immutable", copyLocal: true),
                                    BuildReference.ByName("System.Interactive.Async", copyLocal: true),
                                    BuildReference.ByName("System.Data, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089"),
                                    BuildReference.ByName("EntityFramework.Core", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.Commands", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.Relational", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.Relational.Design", copyLocal: true),
                                    BuildReference.ByName("EntityFramework.MicrosoftSqlServer", copyLocal: true),
                                    BuildReference.ByName("Microsoft.CodeAnalysis", copyLocal: true),
                                    BuildReference.ByName("Microsoft.CodeAnalysis.CSharp", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Caching.Abstractions", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Caching.Memory", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.DependencyInjection", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.DependencyInjection.Abstractions", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Logging", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.Logging.Abstractions", copyLocal: true),
                                    BuildReference.ByName("Microsoft.Extensions.OptionsModel", copyLocal: true),
                                    BuildReference.ByName("Remotion.Linq", copyLocal: true),
                                    BuildReference.ByName("System.Diagnostics.Tracing.Telemetry", copyLocal: true)
                                },
                    Sources = { @"
                            using Microsoft.Data.Entity;
                            using Microsoft.Data.Entity.Infrastructure;
                            using Microsoft.Data.Entity.Migrations;

                            namespace MyProject
                            {
                                internal class MyFirstContext : DbContext
                                {
                                    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
                                    {
                                        optionsBuilder.UseSqlServer(""Data Source=(localdb)\\MSSQLLocalDB;Initial Catalog=MyProject.MyFirstContext"");
                                    }
                                }

                                internal class MySecondContext : DbContext
                                {
                                    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
                                    {
                                        optionsBuilder.UseSqlServer(""Data Source=(localdb)\\MSSQLLocalDB;Initial Catalog=MyProject.MySecondContext"");
                                    }
                                }

                                namespace Migrations
                                {
                                    [DbContext(typeof(MyFirstContext))]
                                    [Migration(""20151006140723_InitialCreate"")]
                                    public class InitialCreate : Migration
                                    {
                                        protected override void Up(MigrationBuilder migrationBuilder)
                                        {
                                        }
                                    }
                                }
                            }" }
                };
                var build = source.Build();
                using (var executor = new OperationExecutorWrapper(targetDir, build.TargetName, targetDir, "MyProject"))
                {
                    var artifacts = executor.AddMigration("MyMigration", /*outputDir:*/ null, "MySecondContext");
                    Assert.Equal(3, artifacts.Count());
                    Assert.True(Directory.Exists(Path.Combine(targetDir, @"Migrations\MySecond")));
                }
            }
        }
    }
}

#endif
