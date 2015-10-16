// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections;
using System.Collections.Generic;
#if NET45 || DNX451
using System.ComponentModel;
#endif
using System.Linq;
using System.Linq.Expressions;
using JetBrains.Annotations;
using Microsoft.Data.Entity.ChangeTracking;
using Microsoft.Data.Entity.Infrastructure;
#if NET45 || DNX451
using Microsoft.Data.Entity.Internal;
#endif
using Microsoft.Data.Entity.Query;

namespace Microsoft.Data.Entity
{
    /// <summary>
    ///     <para>
    ///         A DbSet allows operations to be performed for a given entity type. LINQ queries against
    ///         <see cref="DbSet{TEntity}" /> will be translated into queries against the database.
    ///     </para>
    ///     <para>
    ///         The results of a LINQ query against a <see cref="DbSet{TEntity}" /> will contain the results
    ///         returned from the database and may not reflect changes made in the context that have not
    ///         been persisted to the database. For example, the results will not contain newly added entities
    ///         and may still contain entities that are marked for deletion.
    ///     </para>
    ///     <para>
    ///         <see cref="DbSet{TEntity}" /> objects are usually obtained from a <see cref="DbSet{TEntity}" />
    ///         property on a derived <see cref="DbContext" /> or from the <see cref="DbContext.Set{TEntity}" />
    ///         method.
    ///     </para>
    /// </summary>
    /// <typeparam name="TEntity"> The type of entity being operated on by this set. </typeparam>
    public abstract partial class DbSet<TEntity>
        : IOrderedQueryable<TEntity>, IAsyncEnumerableAccessor<TEntity>, IAccessor<IServiceProvider>
        where TEntity : class
    {
        /// <summary>
        ///     Begins tracking the given entity in the <see cref="EntityState.Added" /> state such that it will
        ///     be inserted into the database when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <param name="entity"> The entity to add. </param>
        /// <param name="behavior">
        ///     Determines whether the context will bring in only the given entity or also other related entities.
        /// </param>
        /// <returns>
        ///     The <see cref="EntityEntry{TEntity}" /> for the entity. This entry provides access to
        ///     information the context is tracking for the entity and the ability to perform
        ///     actions on the entity.
        /// </returns>
        public virtual EntityEntry<TEntity> Add(
            [NotNull] TEntity entity, 
            GraphBehavior behavior = GraphBehavior.IncludeDependents)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entity in the <see cref="EntityState.Unchanged" /> state such that no
        ///     operation will be performed when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <param name="entity"> The entity to attach. </param>
        /// <param name="behavior">
        ///     Determines whether the context will bring in only the given entity or also other related entities.
        /// </param>
        /// <returns>
        ///     The <see cref="EntityEntry" /> for the entity. This entry provides access to
        ///     information the context is tracking for the entity and the ability to perform
        ///     actions on the entity.
        /// </returns>
        public virtual EntityEntry<TEntity> Attach(
            [NotNull] TEntity entity, 
            GraphBehavior behavior = GraphBehavior.IncludeDependents)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entity in the <see cref="EntityState.Deleted" /> state such that it will
        ///     be removed from the database when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <remarks>
        ///     If the entity is already tracked in the <see cref="EntityState.Added" /> state then the context will
        ///     stop tracking the entity (rather than marking it as <see cref="EntityState.Deleted" />) since the
        ///     entity was previously added to the context and does not exist in the database.
        /// </remarks>
        /// <param name="entity"> The entity to remove. </param>
        /// <returns>
        ///     The <see cref="EntityEntry" /> for the entity. This entry provides access to
        ///     information the context is tracking for the entity and the ability to perform
        ///     actions on the entity.
        /// </returns>
        public virtual EntityEntry<TEntity> Remove([NotNull] TEntity entity)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     <para>
        ///         Begins tracking the given entity in the <see cref="EntityState.Modified" /> state such that it will
        ///         be updated in the database when <see cref="DbContext.SaveChanges()" /> is called.
        ///     </para>
        ///     <para>
        ///         All properties of the entity will be marked as modified. To mark only some properties as modified, use
        ///         <see cref="Attach(TEntity, GraphBehavior)" /> to begin tracking the entity in the <see cref="EntityState.Unchanged" />
        ///         state and then use the returned <see cref="EntityEntry" /> to mark the desired properties as modified.
        ///     </para>
        /// </summary>
        /// <param name="entity"> The entity to update. </param>
        /// <param name="behavior">
        ///     Determines whether the context will bring in only the given entity or also other related entities.
        /// </param>
        /// <returns>
        ///     The <see cref="EntityEntry" /> for the entity. This entry provides access to
        ///     information the context is tracking for the entity and the ability to perform
        ///     actions on the entity.
        /// </returns>
        public virtual EntityEntry<TEntity> Update(
            [NotNull] TEntity entity, 
            GraphBehavior behavior = GraphBehavior.IncludeDependents)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entities in the <see cref="EntityState.Added" /> state such that they will
        ///     be inserted into the database when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <param name="entities"> The entities to add. </param>
        public virtual void AddRange([NotNull] params TEntity[] entities)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entities in the <see cref="EntityState.Unchanged" /> state such that no
        ///     operation will be performed when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <param name="entities"> The entities to attach. </param>
        public virtual void AttachRange([NotNull] params TEntity[] entities)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entities in the <see cref="EntityState.Deleted" /> state such that they will
        ///     be removed from the database when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <remarks>
        ///     If any of the entities are already tracked in the <see cref="EntityState.Added" /> state then the context will
        ///     stop tracking those entities (rather than marking them as <see cref="EntityState.Deleted" />) since those
        ///     entities were previously added to the context and do not exist in the database.
        /// </remarks>
        /// <param name="entities"> The entities to remove. </param>
        public virtual void RemoveRange([NotNull] params TEntity[] entities)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     <para>
        ///         Begins tracking the given entities in the <see cref="EntityState.Modified" /> state such that they will
        ///         be updated in the database when <see cref="DbContext.SaveChanges()" /> is called.
        ///     </para>
        ///     <para>
        ///         All properties of the entities will be marked as modified. To mark only some properties as modified, use
        ///         <see cref="Attach(TEntity, GraphBehavior)" /> to begin tracking each entity in the <see cref="EntityState.Unchanged" />
        ///         state and then use the returned <see cref="EntityEntry" /> to mark the desired properties as modified.
        ///     </para>
        /// </summary>
        /// <param name="entities"> The entities to update. </param>
        public virtual void UpdateRange([NotNull] params TEntity[] entities)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entities in the <see cref="EntityState.Added" /> state such that they will
        ///     be inserted into the database when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <param name="entities"> The entities to add. </param>
        /// <param name="behavior">
        ///     Determines whether the context will bring in only the given entities or also other related entities.
        /// </param>
        public virtual void AddRange(
            [NotNull] IEnumerable<TEntity> entities, 
            GraphBehavior behavior = GraphBehavior.IncludeDependents)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entities in the <see cref="EntityState.Unchanged" /> state such that no
        ///     operation will be performed when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <param name="entities"> The entities to attach. </param>
        /// <param name="behavior">
        ///     Determines whether the context will bring in only the given entities or also other related entities.
        /// </param>
        public virtual void AttachRange(
            [NotNull] IEnumerable<TEntity> entities, 
            GraphBehavior behavior = GraphBehavior.IncludeDependents)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Begins tracking the given entities in the <see cref="EntityState.Deleted" /> state such that they will
        ///     be removed from the database when <see cref="DbContext.SaveChanges()" /> is called.
        /// </summary>
        /// <remarks>
        ///     If any of the entities are already tracked in the <see cref="EntityState.Added" /> state then the context will
        ///     stop tracking those entities (rather than marking them as <see cref="EntityState.Deleted" />) since those
        ///     entities were previously added to the context and do not exist in the database.
        /// </remarks>
        /// <param name="entities"> The entities to remove. </param>
        public virtual void RemoveRange([NotNull] IEnumerable<TEntity> entities)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     <para>
        ///         Begins tracking the given entities in the <see cref="EntityState.Modified" /> state such that they will
        ///         be updated in the database when <see cref="DbContext.SaveChanges()" /> is called.
        ///     </para>
        ///     <para>
        ///         All properties of the entities will be marked as modified. To mark only some properties as modified, use
        ///         <see cref="Attach(TEntity, GraphBehavior)" /> to begin tracking each entity in the <see cref="EntityState.Unchanged" />
        ///         state and then use the returned <see cref="EntityEntry" /> to mark the desired properties as modified.
        ///     </para>
        /// </summary>
        /// <param name="entities"> The entities to update. </param>
        /// <param name="behavior">
        ///     Determines whether the context will bring in only the given entities or also other related entities.
        /// </param>
        public virtual void UpdateRange(
            [NotNull] IEnumerable<TEntity> entities, 
            GraphBehavior behavior = GraphBehavior.IncludeDependents)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Returns an <see cref="IEnumerator{T}" /> which when enumerated will execute the query against the database.
        /// </summary>
        /// <returns> The query results. </returns>
        IEnumerator<TEntity> IEnumerable<TEntity>.GetEnumerator()
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Returns an <see cref="IEnumerator" /> which when enumerated will execute the query against the database.
        /// </summary>
        /// <returns> The query results. </returns>
        IEnumerator IEnumerable.GetEnumerator()
        {
            throw new NotImplementedException();
        }

        /// <summary>
        ///     Returns an <see cref="IAsyncEnumerable{T}" /> which when enumerated will asynchronously execute the query against
        ///     the database.
        /// </summary>
        /// <returns> The query results. </returns>
        IAsyncEnumerable<TEntity> IAsyncEnumerableAccessor<TEntity>.AsyncEnumerable
        {
            get { throw new NotImplementedException(); }
        }

        /// <summary>
        ///     Gets the IQueryable element type.
        /// </summary>
        Type IQueryable.ElementType
        {
            get { throw new NotImplementedException(); }
        }

        /// <summary>
        ///     Gets the IQueryable LINQ Expression.
        /// </summary>
        Expression IQueryable.Expression
        {
            get { throw new NotImplementedException(); }
        }

        /// <summary>
        ///     Gets the IQueryable provider.
        /// </summary>
        IQueryProvider IQueryable.Provider
        {
            get { throw new NotImplementedException(); }
        }

        /// <summary>
        ///     <para>
        ///         Gets the scoped <see cref="IServiceProvider" /> being used to resolve services.
        ///     </para>
        ///     <para>
        ///         This property is intended for use by extension methods that need to make use of services
        ///         not directly exposed in the public API surface.
        ///     </para>
        /// </summary>
        IServiceProvider IAccessor<IServiceProvider>.Service
        {
            get { throw new NotImplementedException(); }
        }
    }

#if NET45 || DNX451

    public abstract partial class DbSet<TEntity> : IListSource
        where TEntity : class
    {
        IList IListSource.GetList()
        {
            throw new NotSupportedException(CoreStrings.DataBindingWithIListSource);
        }

        bool IListSource.ContainsListCollection => false;
    }
#endif
}
