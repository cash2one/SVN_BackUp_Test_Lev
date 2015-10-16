// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using JetBrains.Annotations;

namespace Microsoft.Data.Entity.ChangeTracking.Internal
{
    public interface IEntityEntryGraphIterator
    {
        void TraverseGraph<TNode>([NotNull] TNode node, [NotNull] Func<TNode, bool> handleNode)
            where TNode : EntityGraphNodeBase<TNode>;
    }
}
