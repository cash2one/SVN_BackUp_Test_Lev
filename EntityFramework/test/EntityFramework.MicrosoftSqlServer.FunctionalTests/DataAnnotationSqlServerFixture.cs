// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using Microsoft.Data.Entity.FunctionalTests;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace Microsoft.Data.Entity.SqlServer.FunctionalTests
{
    public class DataAnnotationSqlServerFixture : DataAnnotationFixtureBase<SqlServerTestStore>
    {
        public static readonly string DatabaseName = "DataAnnotations";

        private readonly IServiceProvider _serviceProvider;

        private readonly string _connectionString = SqlServerTestStore.CreateConnectionString(DatabaseName);

        public DataAnnotationSqlServerFixture()
        {
            _serviceProvider = new ServiceCollection()
                .AddEntityFramework()
                .AddSqlServer()
                .ServiceCollection()
                .AddSingleton(TestSqlServerModelSource.GetFactory(OnModelCreating))
                .AddInstance<ILoggerFactory>(new TestSqlLoggerFactory())
                .BuildServiceProvider();
        }

        public override SqlServerTestStore CreateTestStore()
        {
            return SqlServerTestStore.GetOrCreateShared(DatabaseName, () =>
            {
                var optionsBuilder = new DbContextOptionsBuilder();
                optionsBuilder.UseSqlServer(_connectionString);

                using (var context = new DataAnnotationContext(_serviceProvider, optionsBuilder.Options))
                {
                    // TODO: Delete DB if model changed
                    context.Database.EnsureDeleted();
                    if (context.Database.EnsureCreated())
                    {
                        DataAnnotationModelInitializer.Seed(context);
                    }

                    TestSqlLoggerFactory.SqlStatements.Clear();
                }
            });
        }

        public override DataAnnotationContext CreateContext(SqlServerTestStore testStore)
        {
            var optionsBuilder = new DbContextOptionsBuilder();
            optionsBuilder.UseSqlServer(testStore.Connection).LogSqlParameterValues();

            var context = new DataAnnotationContext(_serviceProvider, optionsBuilder.Options);
            context.Database.UseTransaction(testStore.Transaction);
            return context;
        }
    }
}
