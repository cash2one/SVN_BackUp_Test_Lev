// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using Microsoft.Data.Entity.FunctionalTests;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Extensions.DependencyInjection;

namespace Microsoft.Data.Entity.SqlServer.FunctionalTests
{
    public class NullKeysSqlServerTest : NullKeysTestBase<NullKeysSqlServerTest.NullKeysSqlServerFixture>
    {
        public NullKeysSqlServerTest(NullKeysSqlServerFixture fixture)
            : base(fixture)
        {
        }

        public class NullKeysSqlServerFixture : NullKeysFixtureBase
        {
            private readonly IServiceProvider _serviceProvider;
            private readonly DbContextOptions _options;

            public NullKeysSqlServerFixture()
            {
                _serviceProvider = new ServiceCollection()
                    .AddEntityFramework()
                    .AddSqlServer()
                    .ServiceCollection()
                    .AddSingleton(TestSqlServerModelSource.GetFactory(OnModelCreating))
                    .BuildServiceProvider();

                var optionsBuilder = new DbContextOptionsBuilder();
                optionsBuilder.UseSqlServer(SqlServerTestStore.CreateConnectionString("StringsContext"));
                _options = optionsBuilder.Options;

                EnsureCreated();
            }

            public override DbContext CreateContext()
            {
                return new DbContext(_serviceProvider, _options);
            }
        }
    }
}
