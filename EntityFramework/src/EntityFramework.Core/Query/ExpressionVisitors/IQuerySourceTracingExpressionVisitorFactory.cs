﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using Microsoft.Data.Entity.Query.ExpressionVisitors.Internal;

namespace Microsoft.Data.Entity.Query.ExpressionVisitors
{
    public interface IQuerySourceTracingExpressionVisitorFactory
    {
        QuerySourceTracingExpressionVisitor Create();
    }
}
