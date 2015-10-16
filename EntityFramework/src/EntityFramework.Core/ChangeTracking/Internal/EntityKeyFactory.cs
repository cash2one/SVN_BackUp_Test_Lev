// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections.Generic;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.Storage;

namespace Microsoft.Data.Entity.ChangeTracking.Internal
{
    public abstract class EntityKeyFactory
    {
        protected EntityKeyFactory([NotNull] IKey key)
        {
            Key = key;
        }

        public virtual IKey Key { get; }

        public abstract EntityKey Create(
            [NotNull] IReadOnlyList<IProperty> properties,
            ValueBuffer valueBuffer);

        public abstract EntityKey Create(
            [NotNull] IReadOnlyList<IProperty> properties,
            [NotNull] IPropertyAccessor propertyAccessor);
    }
}
