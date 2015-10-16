// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections.Generic;
using System.Data;
using JetBrains.Annotations;
// ReSharper disable once CheckNamespace

namespace Microsoft.Data.Entity.Storage
{
    public class DbCommandLogData
    {
        public DbCommandLogData(
            [NotNull] string commandText,
            CommandType commandType,
            int commandTimeout,
            [NotNull] IReadOnlyDictionary<string, object> parameters)
        {
            CommandText = commandText;
            CommandType = commandType;
            CommandTimeout = commandTimeout;
            Parameters = parameters;
        }

        public virtual string CommandText { get; }
        public virtual CommandType CommandType { get; }
        public virtual int CommandTimeout { get; }
        public virtual IReadOnlyDictionary<string, object> Parameters { get; }
    }
}
