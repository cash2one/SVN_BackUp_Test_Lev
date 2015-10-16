// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Linq;
using System.Linq.Expressions;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.Query.Expressions;
using Microsoft.Data.Entity.Query.ExpressionTranslators;
using Microsoft.Data.Entity.Utilities;
using Remotion.Linq.Clauses;
using Remotion.Linq.Clauses.Expressions;
using Remotion.Linq.Clauses.ResultOperators;
using Remotion.Linq.Clauses.StreamedData;
using Remotion.Linq.Parsing;

// ReSharper disable AssignNullToNotNullAttribute

namespace Microsoft.Data.Entity.Query.ExpressionVisitors
{
    public class SqlTranslatingExpressionVisitor : ThrowingExpressionVisitor
    {
        private readonly IRelationalAnnotationProvider _relationalAnnotationProvider;
        private readonly IExpressionFragmentTranslator _compositeExpressionFragmentTranslator;
        private readonly IMethodCallTranslator _methodCallTranslator;
        private readonly IMemberTranslator _memberTranslator;
        private readonly RelationalQueryModelVisitor _queryModelVisitor;
        private readonly SelectExpression _targetSelectExpression;
        private readonly Expression _topLevelPredicate;

        private readonly bool _bindParentQueries;
        private readonly bool _inProjection;

        public SqlTranslatingExpressionVisitor(
            [NotNull] IRelationalAnnotationProvider relationalAnnotationProvider,
            [NotNull] IExpressionFragmentTranslator compositeExpressionFragmentTranslator,
            [NotNull] IMethodCallTranslator methodCallTranslator,
            [NotNull] IMemberTranslator memberTranslator,
            [NotNull] RelationalQueryModelVisitor queryModelVisitor,
            [CanBeNull] SelectExpression targetSelectExpression = null,
            [CanBeNull] Expression topLevelPredicate = null,
            bool bindParentQueries = false,
            bool inProjection = false)
        {
            Check.NotNull(relationalAnnotationProvider, nameof(relationalAnnotationProvider));
            Check.NotNull(compositeExpressionFragmentTranslator, nameof(compositeExpressionFragmentTranslator));
            Check.NotNull(methodCallTranslator, nameof(methodCallTranslator));
            Check.NotNull(memberTranslator, nameof(memberTranslator));
            Check.NotNull(queryModelVisitor, nameof(queryModelVisitor));

            _relationalAnnotationProvider = relationalAnnotationProvider;
            _compositeExpressionFragmentTranslator = compositeExpressionFragmentTranslator;
            _methodCallTranslator = methodCallTranslator;
            _memberTranslator = memberTranslator;
            _queryModelVisitor = queryModelVisitor;
            _targetSelectExpression = targetSelectExpression;
            _topLevelPredicate = topLevelPredicate;
            _bindParentQueries = bindParentQueries;
            _inProjection = inProjection;
        }

        public virtual Expression ClientEvalPredicate { get; private set; }

        public override Expression Visit(Expression expression)
        {
            var translatedExpression = _compositeExpressionFragmentTranslator.Translate(expression);

            if (translatedExpression != null && translatedExpression != expression)
            {
                return Visit(translatedExpression);
            }

            return base.Visit(expression);
        }

        protected override Expression VisitBinary(BinaryExpression binaryExpression)
        {
            Check.NotNull(binaryExpression, nameof(binaryExpression));

            switch (binaryExpression.NodeType)
            {
                case ExpressionType.Coalesce:
                {
                    var left = Visit(binaryExpression.Left);
                    var right = Visit(binaryExpression.Right);

                    return new AliasExpression(binaryExpression.Update(left, binaryExpression.Conversion, right));
                }

                case ExpressionType.Equal:
                case ExpressionType.NotEqual:
                {
                    var structuralComparisonExpression
                        = UnfoldStructuralComparison(
                            binaryExpression.NodeType,
                            ProcessComparisonExpression(binaryExpression));

                    return structuralComparisonExpression;
                }

                case ExpressionType.GreaterThan:
                case ExpressionType.GreaterThanOrEqual:
                case ExpressionType.LessThan:
                case ExpressionType.LessThanOrEqual:
                {
                    return ProcessComparisonExpression(binaryExpression);
                }

                case ExpressionType.AndAlso:
                {
                    var left = Visit(binaryExpression.Left);
                    var right = Visit(binaryExpression.Right);

                    if (binaryExpression == _topLevelPredicate)
                    {
                        if (left != null
                            && right != null)
                        {
                            return Expression.AndAlso(left, right);
                        }

                        if (left != null)
                        {
                            ClientEvalPredicate = binaryExpression.Right;
                            return left;
                        }

                        if (right != null)
                        {
                            ClientEvalPredicate = binaryExpression.Left;
                            return right;
                        }

                        return null;
                    }

                    return left != null && right != null
                        ? Expression.AndAlso(left, right)
                        : null;
                }

                case ExpressionType.OrElse:
                case ExpressionType.Add:
                case ExpressionType.Subtract:
                case ExpressionType.Multiply:
                case ExpressionType.Divide:
                case ExpressionType.Modulo:
                {
                    var leftExpression = Visit(binaryExpression.Left);
                    var rightExpression = Visit(binaryExpression.Right);

                    return leftExpression != null
                           && rightExpression != null
                        ? Expression.MakeBinary(
                            binaryExpression.NodeType, 
                            leftExpression, 
                            rightExpression, 
                            binaryExpression.IsLiftedToNull, 
                            binaryExpression.Method)
                        : null;
                }
            }

            return null;
        }

        protected override Expression VisitConditional(ConditionalExpression conditionalExpression)
        {
            Check.NotNull(conditionalExpression, nameof(conditionalExpression));

            var test = Visit(conditionalExpression.Test);
            var ifTrue = Visit(conditionalExpression.IfTrue);
            var ifFalse = Visit(conditionalExpression.IfFalse);

            if (test != null
                && ifTrue != null
                && ifFalse != null)
            {
                return conditionalExpression.Update(test, ifTrue, ifFalse);
            }

            return null;
        }

        private static Expression UnfoldStructuralComparison(ExpressionType expressionType, Expression expression)
        {
            var binaryExpression = expression as BinaryExpression;
            var leftConstantExpression = binaryExpression?.Left as ConstantExpression;
            var leftExpressions = leftConstantExpression?.Value as Expression[];
            var rightConstantExpression = binaryExpression?.Right as ConstantExpression;
            var rightExpressions = rightConstantExpression?.Value as Expression[];

            if (leftExpressions != null
                && (rightConstantExpression != null
                    && rightConstantExpression.Value == null))
            {
                rightExpressions
                    = Enumerable
                        .Repeat<Expression>(rightConstantExpression, leftExpressions.Length)
                        .ToArray();
            }

            if (rightExpressions != null
                && (leftConstantExpression != null
                    && leftConstantExpression.Value == null))
            {
                leftExpressions
                    = Enumerable
                        .Repeat<Expression>(leftConstantExpression, rightExpressions.Length)
                        .ToArray();
            }

            if (leftExpressions != null
                && rightExpressions != null
                && leftExpressions.Length == rightExpressions.Length)
            {
                return leftExpressions
                    .Zip(rightExpressions, (l, r) =>
                        TransformNullComparison(l, r, binaryExpression.NodeType)
                        ?? Expression.MakeBinary(expressionType, l, r))
                    .Aggregate((e1, e2) =>
                        expressionType == ExpressionType.Equal
                            ? Expression.AndAlso(e1, e2)
                            : Expression.OrElse(e1, e2));
            }

            return expression;
        }

        private Expression ProcessComparisonExpression(BinaryExpression binaryExpression)
        {
            var leftExpression = Visit(binaryExpression.Left);
            var rightExpression = Visit(binaryExpression.Right);

            if (leftExpression == null
                || rightExpression == null)
            {
                return null;
            }

            var nullExpression
                = TransformNullComparison(leftExpression, rightExpression, binaryExpression.NodeType);

            return nullExpression
                   ?? Expression.MakeBinary(binaryExpression.NodeType, leftExpression, rightExpression);
        }

        private static Expression TransformNullComparison(
            Expression left, Expression right, ExpressionType expressionType)
        {
            if (expressionType == ExpressionType.Equal
                || expressionType == ExpressionType.NotEqual)
            {
                var constantExpression
                    = right as ConstantExpression
                      ?? left as ConstantExpression;

                if (constantExpression != null
                    && constantExpression.Value == null)
                {
                    var columnExpression
                        = left.RemoveConvert().TryGetColumnExpression()
                          ?? right.RemoveConvert().TryGetColumnExpression();

                    if (columnExpression != null)
                    {
                        return expressionType == ExpressionType.Equal
                            ? (Expression)new IsNullExpression(columnExpression)
                            : Expression.Not(new IsNullExpression(columnExpression));
                    }
                }
            }

            return null;
        }

        protected override Expression VisitMethodCall(MethodCallExpression methodCallExpression)
        {
            Check.NotNull(methodCallExpression, nameof(methodCallExpression));

            var operand = Visit(methodCallExpression.Object);

            if (operand != null
                || methodCallExpression.Object == null)
            {
                var arguments
                    = methodCallExpression.Arguments
                        .Where(e => !(e is QuerySourceReferenceExpression)
                                    && !(e is SubQueryExpression))
                        .Select(Visit)
                        .Where(e => e != null)
                        .ToArray();

                if (arguments.Length == methodCallExpression.Arguments.Count)
                {
                    var boundExpression
                        = operand != null
                            ? Expression.Call(operand, methodCallExpression.Method, arguments)
                            : Expression.Call(methodCallExpression.Method, arguments);

                    var translatedExpression =
                        _methodCallTranslator.Translate(boundExpression);

                    if (translatedExpression != null)
                    {
                        return translatedExpression;
                    }
                }
            }

            var aliasExpression
                = _queryModelVisitor
                    .BindMethodCallExpression(methodCallExpression, CreateAliasedColumnExpression);

            if (aliasExpression == null
                && _bindParentQueries)
            {
                aliasExpression
                    = _queryModelVisitor?.ParentQueryModelVisitor
                        .BindMethodCallExpression(methodCallExpression, CreateAliasedColumnExpressionCore);
            }

            return aliasExpression;
        }

        protected override Expression VisitMember(MemberExpression memberExpression)
        {
            Check.NotNull(memberExpression, nameof(memberExpression));

            if (!(memberExpression.Expression is QuerySourceReferenceExpression)
                && !(memberExpression.Expression is SubQueryExpression))
            {
                var newExpression = Visit(memberExpression.Expression);

                if (newExpression != null
                    || memberExpression.Expression == null)
                {
                    var newMemberExpression
                        = newExpression != memberExpression.Expression
                            ? Expression.Property(newExpression, memberExpression.Member.Name)
                            : memberExpression;

                    var translatedExpression = _memberTranslator.Translate(newMemberExpression);

                    if (translatedExpression != null)
                    {
                        return translatedExpression;
                    }
                }
            }

            var aliasExpression
                = _queryModelVisitor
                    .BindMemberExpression(memberExpression, CreateAliasedColumnExpression);

            if (aliasExpression == null
                && _bindParentQueries)
            {
                aliasExpression
                    = _queryModelVisitor?.ParentQueryModelVisitor
                        .BindMemberExpression(memberExpression, CreateAliasedColumnExpressionCore);
            }

            if (aliasExpression == null)
            {
                var querySourceReferenceExpression
                    = memberExpression.Expression as QuerySourceReferenceExpression;

                if (querySourceReferenceExpression != null)
                {
                    var selectExpression
                        = _queryModelVisitor.TryGetQuery(querySourceReferenceExpression.ReferencedQuerySource);

                    if (selectExpression != null)
                    {
                        aliasExpression
                            = selectExpression.Projection
                                .OfType<AliasExpression>()
                                .SingleOrDefault(ae => ae.SourceMember == memberExpression.Member);
                    }
                }
            }

            return aliasExpression;
        }

        private AliasExpression CreateAliasedColumnExpression(
            IProperty property, IQuerySource querySource, SelectExpression selectExpression)
        {
            if (_targetSelectExpression != null
                && selectExpression != _targetSelectExpression)
            {
                selectExpression?.AddToProjection(
                    _relationalAnnotationProvider.For(property).ColumnName,
                    property,
                    querySource);

                return null;
            }

            return CreateAliasedColumnExpressionCore(property, querySource, selectExpression);
        }

        private AliasExpression CreateAliasedColumnExpressionCore(
            IProperty property, IQuerySource querySource, SelectExpression selectExpression)
            => new AliasExpression(
                new ColumnExpression(
                    _relationalAnnotationProvider.For(property).ColumnName,
                    property,
                    selectExpression.GetTableForQuerySource(querySource)));

        protected override Expression VisitUnary(UnaryExpression unaryExpression)
        {
            Check.NotNull(unaryExpression, nameof(unaryExpression));

            switch (unaryExpression.NodeType)
            {
                case ExpressionType.Not:
                {
                    var operand = Visit(unaryExpression.Operand);
                    if (operand != null)
                    {
                        return Expression.Not(operand);
                    }

                    break;
                }
                case ExpressionType.Convert:
                {
                    var operand = Visit(unaryExpression.Operand);
                    if (operand != null)
                    {
                        return Expression.Convert(operand, unaryExpression.Type);
                    }

                    break;
                }
            }

            return null;
        }

        protected override Expression VisitNew(NewExpression newExpression)
        {
            Check.NotNull(newExpression, nameof(newExpression));

            if (newExpression.Members != null
                && newExpression.Arguments.Any()
                && newExpression.Arguments.Count == newExpression.Members.Count)
            {
                var memberBindings
                    = newExpression.Arguments
                        .Select(Visit)
                        .Where(e => e != null)
                        .ToArray();

                if (memberBindings.Length == newExpression.Arguments.Count)
                {
                    return Expression.Constant(memberBindings);
                }
            }
            else if (NavigationRewritingExpressionVisitor.IsCompositeKey(newExpression.Type))
            {
                var propertyCallExpressions
                    = ((NewArrayExpression)newExpression.Arguments.Single()).Expressions;

                var memberBindings
                    = propertyCallExpressions
                        .Select(Visit)
                        .Where(e => e != null)
                        .ToArray();

                if (memberBindings.Length == propertyCallExpressions.Count)
                {
                    return Expression.Constant(memberBindings);
                }
            }

            return null;
        }

        protected override Expression VisitSubQuery(SubQueryExpression subQueryExpression)
        {
            Check.NotNull(subQueryExpression, nameof(subQueryExpression));

            var subQueryModel = subQueryExpression.QueryModel;

            if (subQueryModel.IsIdentityQuery()
                && subQueryModel.ResultOperators.Count == 1)
            {
                var contains = subQueryModel.ResultOperators.First() as ContainsResultOperator;

                if (contains != null)
                {
                    var parameter = subQueryModel.MainFromClause.FromExpression as ParameterExpression;
                    var memberItem = contains.Item as MemberExpression;

                    if (parameter != null
                        && memberItem != null)
                    {
                        var aliasExpression = (AliasExpression)VisitMember(memberItem);

                        return new InExpression(aliasExpression, new[] { parameter });
                    }
                }
            }
            else if (!(subQueryModel.GetOutputDataInfo() is StreamedSequenceInfo))
            {
                if (_inProjection
                    && !(subQueryModel.GetOutputDataInfo() is StreamedScalarValueInfo))
                {
                    return null;
                }

                var querySourceReferenceExpression
                    = subQueryModel.SelectClause.Selector as QuerySourceReferenceExpression;

                if (querySourceReferenceExpression == null
                    || (_inProjection 
                        || !_queryModelVisitor.QueryCompilationContext
                        .QuerySourceRequiresMaterialization(querySourceReferenceExpression.ReferencedQuerySource)))
                {
                    var queryModelVisitor
                        = (RelationalQueryModelVisitor)_queryModelVisitor.QueryCompilationContext
                            .CreateQueryModelVisitor(_queryModelVisitor);

                    queryModelVisitor.VisitSubQueryModel(subQueryModel);

                    if (queryModelVisitor.Queries.Count == 1
                        && !queryModelVisitor.RequiresClientFilter
                        && !queryModelVisitor.RequiresClientProjection
                        && !queryModelVisitor.RequiresClientResultOperator)
                    {
                        var selectExpression = queryModelVisitor.Queries.First();

                        selectExpression.Alias = string.Empty; // anonymous

                        var containsResultOperator
                            = subQueryModel.ResultOperators.LastOrDefault() as ContainsResultOperator;

                        if (containsResultOperator != null)
                        {
                            var itemExpression = Visit(containsResultOperator.Item) as AliasExpression;

                            if (itemExpression != null)
                            {
                                return new InExpression(itemExpression, selectExpression);
                            }
                        }

                        return selectExpression;
                    }
                }
            }

            return null;
        }

        private static readonly Type[] _supportedConstantTypes =
            {
                typeof(bool),
                typeof(byte),
                typeof(byte[]),
                typeof(char),
                typeof(decimal),
                typeof(DateTime),
                typeof(DateTimeOffset),
                typeof(double),
                typeof(float),
                typeof(Guid),
                typeof(int),
                typeof(long),
                typeof(sbyte),
                typeof(short),
                typeof(string),
                typeof(uint),
                typeof(ulong),
                typeof(ushort)
            };

        protected override Expression VisitConstant(ConstantExpression constantExpression)
        {
            Check.NotNull(constantExpression, nameof(constantExpression));

            if (constantExpression.Value == null)
            {
                return constantExpression;
            }

            var underlyingType = constantExpression.Type.UnwrapNullableType().UnwrapEnumType();

            return _supportedConstantTypes.Contains(underlyingType)
                ? constantExpression
                : null;
        }

        protected override Expression VisitParameter(ParameterExpression parameterExpression)
        {
            Check.NotNull(parameterExpression, nameof(parameterExpression));

            var underlyingType = parameterExpression.Type.UnwrapNullableType().UnwrapEnumType();

            return _supportedConstantTypes.Contains(underlyingType)
                ? parameterExpression
                : null;
        }

        protected override Expression VisitExtension(Expression expression)
        {
            var stringCompare = expression as StringCompareExpression;
            if (stringCompare != null)
            {
                var newLeft = Visit(stringCompare.Left);
                var newRight = Visit(stringCompare.Right);

                if (newLeft == null || newRight == null)
                {
                    return null;
                }

                return newLeft != stringCompare.Left || newRight != stringCompare.Right
                    ? new StringCompareExpression(stringCompare.Operator, newLeft, newRight)
                    : expression;
            }

            return base.VisitExtension(expression);
        }

        protected override Expression VisitQuerySourceReference(QuerySourceReferenceExpression querySourceReferenceExpression)
        {
            Check.NotNull(querySourceReferenceExpression, nameof(querySourceReferenceExpression));

            var selector
                = ((querySourceReferenceExpression.ReferencedQuerySource as FromClauseBase)
                    ?.FromExpression as SubQueryExpression)
                    ?.QueryModel.SelectClause.Selector;

            return selector != null ? Visit(selector) : null;
        }

        protected override TResult VisitUnhandledItem<TItem, TResult>(
            TItem unhandledItem, string visitMethod, Func<TItem, TResult> baseBehavior)
            => default(TResult);

        protected override Exception CreateUnhandledItemException<T>(T unhandledItem, string visitMethod)
            => null; // Never called
    }
}
