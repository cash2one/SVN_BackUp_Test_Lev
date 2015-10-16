// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using JetBrains.Annotations;
using Microsoft.Data.Entity.Relational.Design.Model;
using Microsoft.Data.Entity.Relational.Design.ReverseEngineering.Internal;

namespace Microsoft.Data.Entity.Relational.Design.ReverseEngineering
{
    public interface IMetadataReader
    {
        SchemaInfo GetSchema([NotNull] string connectionString, [NotNull] TableSelectionSet tableSelectionSet);
    }
}
