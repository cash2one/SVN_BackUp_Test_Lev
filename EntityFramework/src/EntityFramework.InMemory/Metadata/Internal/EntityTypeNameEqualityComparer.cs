// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;

namespace Microsoft.Data.Entity.Metadata.Internal
{
    public class EntityTypeNameEqualityComparer : IEqualityComparer<IEntityType>
    {
        public virtual bool Equals(IEntityType x, IEntityType y)
            => StringComparer.Ordinal.Equals(x.Name, y.Name);

        public virtual int GetHashCode(IEntityType obj)
            => StringComparer.Ordinal.GetHashCode(obj.Name);
    }
}
