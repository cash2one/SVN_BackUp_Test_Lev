// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections.Generic;
using System.Diagnostics;
using JetBrains.Annotations;

namespace Microsoft.Data.Entity.Storage
{
    public struct ValueBuffer
    {
        public static readonly ValueBuffer Empty = new ValueBuffer();

        private readonly IList<object> _values;
        private readonly int _offset;

        public ValueBuffer([NotNull] IList<object> values)
            : this(values, 0)
        {
        }

        public ValueBuffer([NotNull] IList<object> values, int offset)
        {
            Debug.Assert(values != null);
            Debug.Assert(offset >= 0);

            _values = values;
            _offset = offset;
        }

        public object this[int index]
        {
            get { return _values[_offset + index]; }
            [param: CanBeNull] set { _values[_offset + index] = value; }
        }

        public int Count => _values.Count - _offset;

        public ValueBuffer WithOffset(int offset)
        {
            Debug.Assert(offset >= _offset);

            return offset > _offset
                ? new ValueBuffer(_values, offset)
                : this;
        }
    }
}
