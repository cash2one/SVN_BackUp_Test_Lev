// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;

namespace Microsoft.Data.Entity.Metadata
{
    public interface IProperty : IPropertyBase
    {
        Type ClrType { get; }
        bool IsNullable { get; }
        bool IsReadOnlyBeforeSave { get; }
        bool IsReadOnlyAfterSave { get; }
        bool StoreGeneratedAlways { get; }
        ValueGenerated ValueGenerated { get; }
        bool RequiresValueGenerator { get; }
        int Index { get; }
        bool IsShadowProperty { get; }
        bool IsConcurrencyToken { get; }
    }
}
