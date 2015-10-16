// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using JetBrains.Annotations;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.Utilities;

namespace Microsoft.Data.Entity.Relational.Design.ReverseEngineering.Configuration
{
    public class RelationshipConfiguration
    {
        public RelationshipConfiguration(
            [NotNull] EntityConfiguration entityConfiguration,
            [NotNull] IForeignKey foreignKey,
            [NotNull] string dependentEndNavigationPropertyName,
            [NotNull] string principalEndNavigationPropertyName)
        {
            Check.NotNull(entityConfiguration, nameof(entityConfiguration));
            Check.NotNull(foreignKey, nameof(foreignKey));
            Check.NotEmpty(dependentEndNavigationPropertyName, nameof(dependentEndNavigationPropertyName));
            Check.NotEmpty(principalEndNavigationPropertyName, nameof(principalEndNavigationPropertyName));

            EntityConfiguration = entityConfiguration;
            ForeignKey = foreignKey;
            DependentEndNavigationPropertyName = dependentEndNavigationPropertyName;
            PrincipalEndNavigationPropertyName = principalEndNavigationPropertyName;
        }

        public virtual bool HasAttributeEquivalent { get; set; } = true;
        public virtual EntityConfiguration EntityConfiguration { get; }
        public virtual IForeignKey ForeignKey { get; }
        public virtual string DependentEndNavigationPropertyName { get; }
        public virtual string PrincipalEndNavigationPropertyName { get; }
    }
}
