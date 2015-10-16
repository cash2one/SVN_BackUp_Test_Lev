﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using Microsoft.Data.Entity.Storage.Internal;
using Xunit;

namespace Microsoft.Data.Entity.Storage
{
    public class SqliteSqlGeneratorTest : SqlGeneratorTestBase
    {
        public override void GenerateLiteral_returns_DateTime_literal()
        {
            var value = new DateTime(2015, 3, 12, 13, 36, 37, 371);
            var literal = CreateSqlGenerator().GenerateLiteral(value);
            Assert.Equal("'2015-03-12 13:36:37.371'", literal);
        }

        public override void GenerateLiteral_returns_DateTimeOffset_literal()
        {
            var value = new DateTimeOffset(2015, 3, 12, 13, 36, 37, 371, new TimeSpan(-7, 0, 0));
            var literal = CreateSqlGenerator().GenerateLiteral(value);
            Assert.Equal("'2015-03-12 13:36:37.371-07:00'", literal);
        }

        protected override ISqlGenerator CreateSqlGenerator()
            => new SqliteSqlGenerator();
    }
}
