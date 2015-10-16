// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.Metadata.Conventions;
using Microsoft.Data.Entity.Metadata.Conventions.Internal;
using Microsoft.Data.Entity.Utilities;

namespace Microsoft.Data.Entity.Infrastructure
{
    public abstract class ModelSource : IModelSource
    {
        private readonly ThreadSafeDictionaryCache<Type, IModel> _models = new ThreadSafeDictionaryCache<Type, IModel>();
        protected virtual IDbSetFinder SetFinder { get; }
        protected virtual ICoreConventionSetBuilder CoreConventionSetBuilder { get; }

        protected ModelSource(
            [NotNull] IDbSetFinder setFinder,
            [NotNull] ICoreConventionSetBuilder coreConventionSetBuilder)
        {
            Check.NotNull(setFinder, nameof(setFinder));
            Check.NotNull(coreConventionSetBuilder, nameof(coreConventionSetBuilder));

            SetFinder = setFinder;
            CoreConventionSetBuilder = coreConventionSetBuilder;
        }

        public virtual IModel GetModel(DbContext context, IConventionSetBuilder conventionSetBuilder, IModelValidator validator)
            => _models.GetOrAdd(context.GetType(), k => CreateModel(context, conventionSetBuilder, validator));

        protected virtual IModel CreateModel(
            [NotNull] DbContext context,
            [CanBeNull] IConventionSetBuilder conventionSetBuilder,
            [NotNull] IModelValidator validator)
        {
            Check.NotNull(context, nameof(context));
            Check.NotNull(validator, nameof(validator));

            var conventionSet = CreateConventionSet(conventionSetBuilder);
            var model = new Model();

            model.SetProductVersion(ProductInfo.GetVersion());

            var modelBuilder = new ModelBuilder(conventionSet, model);

            FindSets(modelBuilder, context);

            OnModelCreating(context, modelBuilder);

            modelBuilder.Validate();

            validator.Validate(model);

            return model;
        }

        protected virtual ConventionSet CreateConventionSet([CanBeNull] IConventionSetBuilder conventionSetBuilder)
        {
            var conventionSet = CoreConventionSetBuilder.CreateConventionSet();
            return conventionSetBuilder == null
                ? conventionSet
                : conventionSetBuilder.AddConventions(conventionSet);
        }

        protected virtual void FindSets([NotNull] ModelBuilder modelBuilder, [NotNull] DbContext context)
        {
            foreach (var setInfo in SetFinder.FindSets(context))
            {
                modelBuilder.Entity(setInfo.EntityType);
            }
        }

        public static void OnModelCreating([NotNull] DbContext context, [NotNull] ModelBuilder modelBuilder)
            => context.OnModelCreating(modelBuilder);
    }
}
