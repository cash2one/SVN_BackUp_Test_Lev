// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Linq.Expressions;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Query.Expressions;
using Remotion.Linq.Parsing;

namespace Microsoft.Data.Entity.Query.ExpressionVisitors
{
    public abstract class RelationalNullsExpressionVisitorBase : RelinqExpressionVisitor
    {
        protected virtual Expression BuildIsNullExpression([NotNull] Expression expression)
        {
            var isNullExpressionBuilder = new IsNullExpressionBuildingVisitor();

            isNullExpressionBuilder.Visit(expression);

            return isNullExpressionBuilder.ResultExpression;
        }

        protected override Expression VisitExtension(Expression expression)
        {
            return expression is NotNullableExpression
                ? expression
                : base.VisitExtension(expression);
        }
    }
}
