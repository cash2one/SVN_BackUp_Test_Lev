// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using System.Reflection;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Utilities;

namespace Microsoft.Data.Entity.Query.Annotations
{
    public class IncludeQueryAnnotation : QueryAnnotationBase
    {
        private readonly List<PropertyInfo> _chainedNavigationProperties;

        public virtual MemberExpression NavigationPropertyPath { get; }

        public virtual IReadOnlyList<PropertyInfo> ChainedNavigationProperties => _chainedNavigationProperties;

        public IncludeQueryAnnotation([NotNull] MemberExpression navigationPropertyPath)
        {
            Check.NotNull(navigationPropertyPath, nameof(navigationPropertyPath));

            NavigationPropertyPath = navigationPropertyPath;
            _chainedNavigationProperties = new List<PropertyInfo>();
        }

        public virtual void AppendToNavigationPath([NotNull] IReadOnlyList<PropertyInfo> propertyInfos)
        {
            Check.NotNull(propertyInfos, nameof(propertyInfos));

            _chainedNavigationProperties.AddRange(propertyInfos);
        }

        public override string ToString()
            => "Include("
               + NavigationPropertyPath
               + (_chainedNavigationProperties.Count > 0
                   ? _chainedNavigationProperties.Select(p => p.Name).Join(".")
                   : string.Empty)
               + ")";
    }
}
