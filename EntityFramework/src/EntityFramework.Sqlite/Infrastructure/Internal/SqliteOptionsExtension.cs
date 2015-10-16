// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using JetBrains.Annotations;
using Microsoft.Data.Entity.Utilities;
using Microsoft.Extensions.DependencyInjection;

namespace Microsoft.Data.Entity.Infrastructure.Internal
{
    public class SqliteOptionsExtension : RelationalOptionsExtension
    {
        private bool _enforceForeignKeys = true;

        public SqliteOptionsExtension()
        {
        }

        // NB: When adding new options, make sure to update the copy ctor below.

        public SqliteOptionsExtension([NotNull] SqliteOptionsExtension copyFrom)
            : base(copyFrom)
        {
            _enforceForeignKeys = copyFrom._enforceForeignKeys;
        }

        public virtual bool EnforceForeignKeys
        {
            get { return _enforceForeignKeys; }
            set { _enforceForeignKeys = value; }
        }

        public override void ApplyServices(EntityFrameworkServicesBuilder builder)
        {
            Check.NotNull(builder, nameof(builder));

            builder.AddSqlite();
        }
    }
}
