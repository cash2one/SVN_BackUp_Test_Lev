﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using Microsoft.Data.Entity.FunctionalTests;

namespace Microsoft.Data.Entity.Sqlite.FunctionalTests
{
    public class QueryNoClientEvalSqliteTest : QueryNoClientEvalTestBase<QueryNoClientEvalSqliteFixture>
    {
        public QueryNoClientEvalSqliteTest(QueryNoClientEvalSqliteFixture fixture)
            : base(fixture)
        {
        }
    }
}
