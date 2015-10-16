// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections.Generic;
using System.Diagnostics;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Data.Entity.Metadata.Internal;
using Microsoft.Data.Entity.Utilities;

namespace Microsoft.Data.Entity.Metadata
{
    [DebuggerDisplay("{DebuggerDisplay,nq}")]
    public class Key : Annotatable, IKey
    {
        public Key([NotNull] IReadOnlyList<Property> properties)
        {
            Check.NotEmpty(properties, nameof(properties));
            Check.HasNoNulls(properties, nameof(properties));
            MetadataHelper.CheckSameEntityType(properties, "properties");

            Properties = properties;
        }

        public virtual IReadOnlyList<Property> Properties { get; }

        public virtual EntityType DeclaringEntityType => Properties[0].DeclaringEntityType;

        IReadOnlyList<IProperty> IKey.Properties => Properties;

        IEntityType IKey.EntityType => DeclaringEntityType;

        [UsedImplicitly]
        private string DebuggerDisplay => Property.Format(Properties);
    }
}
