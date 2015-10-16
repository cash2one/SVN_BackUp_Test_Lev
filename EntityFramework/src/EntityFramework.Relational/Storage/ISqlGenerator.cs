﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using JetBrains.Annotations;

namespace Microsoft.Data.Entity.Storage
{
    public interface ISqlGenerator
    {
        string BatchCommandSeparator { get; }

        string BatchSeparator { get; }

        string GenerateParameterName([NotNull] string name);

        string GenerateLiteral([CanBeNull] object value);

        string EscapeLiteral([NotNull] string literal);

        string EscapeIdentifier([NotNull] string identifier);

        string DelimitIdentifier([NotNull] string identifier);

        string DelimitIdentifier([NotNull] string name, [CanBeNull] string schema);
    }
}
