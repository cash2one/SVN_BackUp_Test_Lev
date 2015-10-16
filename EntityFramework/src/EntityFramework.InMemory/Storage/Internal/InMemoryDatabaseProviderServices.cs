// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Reflection;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Data.Entity.Infrastructure.Internal;
using Microsoft.Data.Entity.Query;
using Microsoft.Data.Entity.Query.ExpressionVisitors;
using Microsoft.Data.Entity.Query.ExpressionVisitors.Internal;
using Microsoft.Data.Entity.Query.Internal;
using Microsoft.Data.Entity.ValueGeneration;
using Microsoft.Data.Entity.ValueGeneration.Internal;

namespace Microsoft.Data.Entity.Storage.Internal
{
    public class InMemoryDatabaseProviderServices : DatabaseProviderServices
    {
        public InMemoryDatabaseProviderServices([NotNull] IServiceProvider services)
            : base(services)
        {
        }

        public override string InvariantName => GetType().GetTypeInfo().Assembly.GetName().Name;
        public override IDatabase Database => GetService<IInMemoryDatabase>();
        public override IQueryContextFactory QueryContextFactory => GetService<InMemoryQueryContextFactory>();
        public override IDatabaseCreator Creator => GetService<InMemoryDatabaseCreator>();
        public override IValueGeneratorSelector ValueGeneratorSelector => GetService<InMemoryValueGeneratorSelector>();
        public override IModelSource ModelSource => GetService<InMemoryModelSource>();
        public override IValueGeneratorCache ValueGeneratorCache => GetService<InMemoryValueGeneratorCache>();
        public override IEntityQueryableExpressionVisitorFactory EntityQueryableExpressionVisitorFactory => GetService<InMemoryEntityQueryableExpressionVisitorFactory>();
        public override IEntityQueryModelVisitorFactory EntityQueryModelVisitorFactory => GetService<InMemoryQueryModelVisitorFactory>();
    }
}
