﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using JetBrains.Annotations;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Extensions.Logging;

namespace Microsoft.Data.Entity.Infrastructure.Internal
{
    public class SqliteModelValidator : RelationalModelValidator
    {
        public SqliteModelValidator([NotNull] ILogger<RelationalModelValidator> loggerFactory, [NotNull] IRelationalAnnotationProvider relationalExtensions)
            : base(loggerFactory, relationalExtensions)
        {
        }
    }
}
