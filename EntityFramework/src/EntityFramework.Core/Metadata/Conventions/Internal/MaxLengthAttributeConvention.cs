// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.ComponentModel.DataAnnotations;
using System.Reflection;
using Microsoft.Data.Entity.Metadata.Internal;
using Microsoft.Data.Entity.Utilities;

namespace Microsoft.Data.Entity.Metadata.Conventions.Internal
{
    public class MaxLengthAttributeConvention : PropertyAttributeConvention<MaxLengthAttribute>
    {
        public override InternalPropertyBuilder Apply(InternalPropertyBuilder propertyBuilder, MaxLengthAttribute attribute, PropertyInfo clrProperty)
        {
            Check.NotNull(propertyBuilder, nameof(propertyBuilder));
            Check.NotNull(attribute, nameof(attribute));

            if (attribute.Length > 0)
            {
                propertyBuilder.HasMaxLength(attribute.Length, ConfigurationSource.DataAnnotation);
            }

            return propertyBuilder;
        }
    }
}
