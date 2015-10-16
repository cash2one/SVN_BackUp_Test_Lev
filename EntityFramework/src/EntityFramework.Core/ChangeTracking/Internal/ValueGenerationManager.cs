// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Diagnostics;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.ValueGeneration;
using Microsoft.Data.Entity.ValueGeneration.Internal;

namespace Microsoft.Data.Entity.ChangeTracking.Internal
{
    public class ValueGenerationManager : IValueGenerationManager
    {
        private readonly IValueGeneratorSelector _valueGeneratorSelector;
        private readonly IKeyPropagator _keyPropagator;

        public ValueGenerationManager(
            [NotNull] IValueGeneratorSelector valueGeneratorSelector,
            [NotNull] IKeyPropagator keyPropagator)
        {
            _valueGeneratorSelector = valueGeneratorSelector;
            _keyPropagator = keyPropagator;
        }

        public virtual void Generate(InternalEntityEntry entry)
        {
            foreach (var property in entry.EntityType.GetProperties())
            {
                var isForeignKey = property.IsForeignKey(entry.EntityType);

                if ((property.RequiresValueGenerator || isForeignKey)
                    && property.ClrType.IsDefaultValue(entry[property]))
                {
                    if (isForeignKey)
                    {
                        _keyPropagator.PropagateValue(entry, property);
                    }
                    else
                    {
                        var valueGenerator = _valueGeneratorSelector.Select(property, entry.EntityType);

                        Debug.Assert(valueGenerator != null);

                        SetGeneratedValue(entry, property, valueGenerator.Next(), valueGenerator.GeneratesTemporaryValues);
                    }
                }
            }
        }

        public virtual bool MayGetTemporaryValue(IProperty property, IEntityType entityType)
            => property.RequiresValueGenerator
               && _valueGeneratorSelector.Select(property, entityType).GeneratesTemporaryValues;

        private static void SetGeneratedValue(InternalEntityEntry entry, IProperty property, object generatedValue, bool isTemporary)
        {
            if (generatedValue != null)
            {
                entry[property] = generatedValue;

                if (isTemporary)
                {
                    entry.MarkAsTemporary(property);
                }
            }
        }
    }
}
