// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using System.Reflection;
using System.Threading;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Utilities;
using Remotion.Linq;
using Remotion.Linq.Clauses;
using Remotion.Linq.Clauses.ResultOperators;

namespace Microsoft.Data.Entity.Query
{
    using ResultHandler = Func<EntityQueryModelVisitor, ResultOperatorBase, QueryModel, Expression>;

    public class ResultOperatorHandler : IResultOperatorHandler
    {
        private static readonly Dictionary<Type, ResultHandler> _handlers
            = new Dictionary<Type, ResultHandler>
            {
                { typeof(AllResultOperator), (v, r, q) => HandleAll(v, (AllResultOperator)r, q) },
                { typeof(AnyResultOperator), (v, _, __) => HandleAny(v) },
                { typeof(AverageResultOperator), (v, _, __) => HandleAverage(v) },
                { typeof(CastResultOperator), (v, r, __) => HandleCast(v, (CastResultOperator)r) },
                { typeof(CountResultOperator), (v, _, __) => HandleCount(v) },
                { typeof(ContainsResultOperator), (v, r, q) => HandleContains(v, (ContainsResultOperator)r, q) },
                { typeof(DefaultIfEmptyResultOperator), (v, r, q) => HandleDefaultIfEmpty(v, (DefaultIfEmptyResultOperator)r, q) },
                { typeof(DistinctResultOperator), (v, _, __) => HandleDistinct(v) },
                { typeof(FirstResultOperator), (v, r, __) => HandleFirst(v, (ChoiceResultOperatorBase)r) },
                { typeof(GroupResultOperator), (v, r, q) => HandleGroup(v, (GroupResultOperator)r, q) },
                { typeof(LastResultOperator), (v, r, __) => HandleLast(v, (ChoiceResultOperatorBase)r) },
                { typeof(LongCountResultOperator), (v, _, __) => HandleLongCount(v) },
                { typeof(MinResultOperator), (v, _, __) => HandleMin(v) },
                { typeof(MaxResultOperator), (v, _, __) => HandleMax(v) },
                { typeof(OfTypeResultOperator), (v, r, q) => HandleOfType(v, (OfTypeResultOperator)r) },
                { typeof(SingleResultOperator), (v, r, __) => HandleSingle(v, (ChoiceResultOperatorBase)r) },
                { typeof(SkipResultOperator), (v, r, __) => HandleSkip(v, (SkipResultOperator)r) },
                { typeof(SumResultOperator), (v, _, __) => HandleSum(v) },
                { typeof(TakeResultOperator), (v, r, __) => HandleTake(v, (TakeResultOperator)r) }
            };

        public virtual Expression HandleResultOperator(
            EntityQueryModelVisitor entityQueryModelVisitor,
            ResultOperatorBase resultOperator,
            QueryModel queryModel)
        {
            Check.NotNull(entityQueryModelVisitor, nameof(entityQueryModelVisitor));
            Check.NotNull(resultOperator, nameof(resultOperator));
            Check.NotNull(queryModel, nameof(queryModel));

            ResultHandler handler;
            if (!_handlers.TryGetValue(resultOperator.GetType(), out handler))
            {
                throw new NotImplementedException(resultOperator.GetType().ToString());
            }

            return handler(entityQueryModelVisitor, resultOperator, queryModel);
        }

        private static Expression HandleAll(
            EntityQueryModelVisitor entityQueryModelVisitor,
            AllResultOperator allResultOperator,
            QueryModel queryModel)
        {
            var predicate
                = entityQueryModelVisitor
                    .ReplaceClauseReferences(
                        allResultOperator.Predicate,
                        queryModel.MainFromClause);

            return CallWithPossibleCancellationToken(
                entityQueryModelVisitor.LinqOperatorProvider.All
                    .MakeGenericMethod(typeof(QueryResultScope)),
                entityQueryModelVisitor.CreateScope(
                    entityQueryModelVisitor.Expression,
                    entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType,
                    queryModel.MainFromClause),
                Expression.Lambda(predicate, EntityQueryModelVisitor.QueryResultScopeParameter));
        }

        private static Expression HandleAny(EntityQueryModelVisitor entityQueryModelVisitor)
            => CallWithPossibleCancellationToken(
                entityQueryModelVisitor.LinqOperatorProvider.Any
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleAverage(EntityQueryModelVisitor entityQueryModelVisitor)
            => HandleAggregate(entityQueryModelVisitor, "Average");

        private static Expression HandleCast(
            EntityQueryModelVisitor entityQueryModelVisitor, CastResultOperator castResultOperator)
        {
            var resultItemTypeInfo
                = entityQueryModelVisitor.Expression.Type
                    .GetSequenceType().GetTypeInfo();

            if (castResultOperator.CastItemType.GetTypeInfo()
                .IsAssignableFrom(resultItemTypeInfo))
            {
                return entityQueryModelVisitor.Expression;
            }

            return Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider
                    .Cast.MakeGenericMethod(castResultOperator.CastItemType),
                entityQueryModelVisitor.Expression);
        }

        private static Expression HandleCount(EntityQueryModelVisitor entityQueryModelVisitor)
            => CallWithPossibleCancellationToken(
                entityQueryModelVisitor.LinqOperatorProvider
                    .Count.MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleContains(
            EntityQueryModelVisitor entityQueryModelVisitor,
            ContainsResultOperator containsResultOperator,
            QueryModel queryModel)
        {
            var item
                = entityQueryModelVisitor
                    .ReplaceClauseReferences(
                        containsResultOperator.Item,
                        queryModel.MainFromClause);

            return CallWithPossibleCancellationToken(
                entityQueryModelVisitor.LinqOperatorProvider.Contains
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression,
                item);
        }

        private static Expression HandleDefaultIfEmpty(
            EntityQueryModelVisitor entityQueryModelVisitor,
            DefaultIfEmptyResultOperator defaultIfEmptyResultOperator,
            QueryModel queryModel)
        {
            if (defaultIfEmptyResultOperator.OptionalDefaultValue == null)
            {
                return Expression.Call(
                    entityQueryModelVisitor.LinqOperatorProvider.DefaultIfEmpty
                        .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                    entityQueryModelVisitor.Expression);
            }

            var optionalDefaultValue
                = entityQueryModelVisitor
                    .ReplaceClauseReferences(
                        defaultIfEmptyResultOperator.OptionalDefaultValue,
                        queryModel.MainFromClause);

            return Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider.DefaultIfEmptyArg
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression,
                optionalDefaultValue);
        }

        private static Expression HandleDistinct(EntityQueryModelVisitor entityQueryModelVisitor)
            => Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider.Distinct
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleFirst(
            EntityQueryModelVisitor entityQueryModelVisitor, ChoiceResultOperatorBase choiceResultOperator)
            => CallWithPossibleCancellationToken(
                (choiceResultOperator.ReturnDefaultWhenEmpty
                    ? entityQueryModelVisitor.LinqOperatorProvider.FirstOrDefault
                    : entityQueryModelVisitor.LinqOperatorProvider.First)
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleGroup(
            EntityQueryModelVisitor entityQueryModelVisitor,
            GroupResultOperator groupResultOperator,
            QueryModel queryModel)
        {
            var keySelector
                = entityQueryModelVisitor
                    .ReplaceClauseReferences(
                        groupResultOperator.KeySelector,
                        queryModel.MainFromClause);

            var elementSelector
                = entityQueryModelVisitor
                    .ReplaceClauseReferences(
                        groupResultOperator.ElementSelector,
                        queryModel.MainFromClause);

            return Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider.GroupBy
                    .MakeGenericMethod(
                        typeof(QueryResultScope),
                        keySelector.Type,
                        elementSelector.Type),
                entityQueryModelVisitor.CreateScope(
                    entityQueryModelVisitor.Expression,
                    entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType,
                    queryModel.MainFromClause),
                Expression.Lambda(keySelector, EntityQueryModelVisitor.QueryResultScopeParameter),
                Expression.Lambda(elementSelector, EntityQueryModelVisitor.QueryResultScopeParameter));
        }

        private static Expression HandleLast(
            EntityQueryModelVisitor entityQueryModelVisitor, ChoiceResultOperatorBase choiceResultOperator)
            => CallWithPossibleCancellationToken(
                (choiceResultOperator.ReturnDefaultWhenEmpty
                    ? entityQueryModelVisitor.LinqOperatorProvider.LastOrDefault
                    : entityQueryModelVisitor.LinqOperatorProvider.Last)
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleLongCount(EntityQueryModelVisitor entityQueryModelVisitor)
            => CallWithPossibleCancellationToken(
                entityQueryModelVisitor.LinqOperatorProvider.LongCount
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleMin(EntityQueryModelVisitor entityQueryModelVisitor)
            => HandleAggregate(entityQueryModelVisitor, "Min");

        private static Expression HandleMax(EntityQueryModelVisitor entityQueryModelVisitor)
            => HandleAggregate(entityQueryModelVisitor, "Max");

        private static Expression HandleOfType(
            EntityQueryModelVisitor entityQueryModelVisitor,
            OfTypeResultOperator ofTypeResultOperator)
            => Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider.OfType
                    .MakeGenericMethod(ofTypeResultOperator.SearchedItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleSingle(
            EntityQueryModelVisitor entityQueryModelVisitor, ChoiceResultOperatorBase choiceResultOperator)
            => CallWithPossibleCancellationToken(
                (choiceResultOperator.ReturnDefaultWhenEmpty
                    ? entityQueryModelVisitor.LinqOperatorProvider.SingleOrDefault
                    : entityQueryModelVisitor.LinqOperatorProvider.Single)
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static Expression HandleSkip(
            EntityQueryModelVisitor entityQueryModelVisitor, SkipResultOperator skipResultOperator)
            => Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider.Skip
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression, skipResultOperator.Count);

        private static Expression HandleSum(EntityQueryModelVisitor entityQueryModelVisitor)
            => HandleAggregate(entityQueryModelVisitor, "Sum");

        private static Expression HandleTake(
            EntityQueryModelVisitor entityQueryModelVisitor, TakeResultOperator takeResultOperator)
            => Expression.Call(
                entityQueryModelVisitor.LinqOperatorProvider.Take
                    .MakeGenericMethod(entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression, takeResultOperator.Count);

        private static Expression HandleAggregate(EntityQueryModelVisitor entityQueryModelVisitor, string methodName)
            => CallWithPossibleCancellationToken(
                entityQueryModelVisitor.LinqOperatorProvider.GetAggregateMethod(
                    methodName,
                    entityQueryModelVisitor.StreamedSequenceInfo.ResultItemType),
                entityQueryModelVisitor.Expression);

        private static readonly PropertyInfo _cancellationTokenProperty
            = typeof(QueryContext).GetTypeInfo()
                .GetDeclaredProperty("CancellationToken");

        public static Expression CallWithPossibleCancellationToken(
            [NotNull] MethodInfo methodInfo, [CanBeNull] params Expression[] arguments)
        {
            Check.NotNull(methodInfo, nameof(methodInfo));

            if (methodInfo.GetParameters().Last().ParameterType == typeof(CancellationToken))
            {
                return Expression.Call(
                    methodInfo,
                    arguments
                        .AsEnumerable()
                        .Concat(new[]
                        {
                            Expression.Property(
                                EntityQueryModelVisitor.QueryContextParameter,
                                _cancellationTokenProperty)
                        }));
            }

            return Expression.Call(methodInfo, arguments);
        }
    }
}
