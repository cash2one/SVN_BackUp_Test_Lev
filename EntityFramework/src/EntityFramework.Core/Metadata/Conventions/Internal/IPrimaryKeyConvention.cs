// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using JetBrains.Annotations;
using Microsoft.Data.Entity.Metadata.Internal;

namespace Microsoft.Data.Entity.Metadata.Conventions.Internal
{
    public interface IPrimaryKeyConvention
    {
        bool Apply([NotNull] InternalKeyBuilder keyBuilder, [CanBeNull] Key previousPrimaryKey);
    }
}
