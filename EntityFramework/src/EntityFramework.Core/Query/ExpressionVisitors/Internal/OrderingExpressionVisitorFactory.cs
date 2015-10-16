﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Linq.Expressions;

namespace Microsoft.Data.Entity.Query.ExpressionVisitors.Internal
{
    public class OrderingExpressionVisitorFactory : IOrderingExpressionVisitorFactory
    {
        public virtual ExpressionVisitor Create(EntityQueryModelVisitor queryModelVisitor)
            => new DefaultQueryExpressionVisitor(queryModelVisitor);
    }
}
