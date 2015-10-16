﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Diagnostics;

namespace EntityFramework.Microbenchmarks.Core
{
#if !DNXCORE50
    public partial class NullMetricCollector : MarshalByRefObject
    {
        private partial class Scope : MarshalByRefObject
        {
        }
    }
#endif

    public partial class NullMetricCollector : IMetricCollector
    {
        private readonly Scope _scope;

        public NullMetricCollector()
        {
            _scope = new Scope();
        }

        public IDisposable StartCollection()
        {
            return _scope;
        }

        public void StopCollection()
        { }

        public void Reset()
        { }

        public long TimeElapsed => 0;

        public long MemoryDelta => 0;

        private partial class Scope : IDisposable
        {
            public Scope()
            { }

            public void Dispose()
            { }
        }

    }
}
