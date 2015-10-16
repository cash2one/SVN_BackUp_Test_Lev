﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;
using JetBrains.Annotations;

namespace Microsoft.Data.Entity.Relational.Design.ReverseEngineering
{
    public class CSharpUniqueNamer<T> : CSharpNamer<T>
    {
        private readonly HashSet<string> _usedNames = new HashSet<string>();

        public CSharpUniqueNamer([NotNull] Func<T, string> nameGetter)
            : base(nameGetter)
        {
        }

        public override string GetName([NotNull] T item)
        {
            if (NameCache.ContainsKey(item))
            {
                return base.GetName(item);
            }

            var input = base.GetName(item);
            var name = input;
            var suffix = 1;

            while (_usedNames.Contains(name))
            {
                name = input + (suffix++);
            }

            _usedNames.Add(name);
            NameCache[item] = name;

            return name;
        }
    }
}
