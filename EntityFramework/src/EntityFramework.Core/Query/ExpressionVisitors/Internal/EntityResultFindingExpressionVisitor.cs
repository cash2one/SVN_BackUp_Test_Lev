// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections.Generic;
using System.Linq.Expressions;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Metadata;
using Remotion.Linq.Clauses.Expressions;

namespace Microsoft.Data.Entity.Query.ExpressionVisitors.Internal
{
    public class EntityResultFindingExpressionVisitor : ExpressionVisitorBase
    {
        private readonly IModel _model;
        private readonly IEntityTrackingInfoFactory _entityTrackingInfoFactory;
        private readonly QueryCompilationContext _queryCompilationContext;

        private List<EntityTrackingInfo> _entityTrackingInfos;

        public EntityResultFindingExpressionVisitor(
            [NotNull] IModel model,
            [NotNull] IEntityTrackingInfoFactory entityTrackingInfoFactory,
            [NotNull] QueryCompilationContext queryCompilationContext)
        {
            _model = model;
            _entityTrackingInfoFactory = entityTrackingInfoFactory;
            _queryCompilationContext = queryCompilationContext;
        }

        public virtual IReadOnlyCollection<EntityTrackingInfo> FindEntitiesInResult([NotNull] Expression expression)
        {
            _entityTrackingInfos = new List<EntityTrackingInfo>();

            Visit(expression);

            return _entityTrackingInfos;
        }

        protected override Expression VisitQuerySourceReference(
            QuerySourceReferenceExpression querySourceReferenceExpression)
        {
            var entityType = _model.FindEntityType(querySourceReferenceExpression.Type);

            if (entityType != null)
            {
                _entityTrackingInfos.Add(
                    _entityTrackingInfoFactory
                        .Create(_queryCompilationContext, querySourceReferenceExpression, entityType));
            }

            return querySourceReferenceExpression;
        }

        // Prune these nodes...

        protected override Expression VisitSubQuery(SubQueryExpression expression) => expression;

        protected override Expression VisitMember(MemberExpression expression) => expression;

        protected override Expression VisitMethodCall(MethodCallExpression expression) => expression;

        protected override Expression VisitConditional(ConditionalExpression expression) => expression;

        protected override Expression VisitBinary(BinaryExpression expression) => expression;

        protected override Expression VisitTypeBinary(TypeBinaryExpression expression) => expression;

        protected override Expression VisitLambda<T>(Expression<T> expression) => expression;

        protected override Expression VisitInvocation(InvocationExpression expression) => expression;
    }
}
