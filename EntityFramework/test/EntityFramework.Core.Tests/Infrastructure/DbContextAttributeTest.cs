﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using Xunit;

namespace Microsoft.Data.Entity.Infrastructure
{
    public class DbContextAttributeTest
    {
        [Fact]
        public void Create_attribute()
        {
            var attribute = new DbContextAttribute(typeof(MyContext));

            Assert.Same(typeof(MyContext), attribute.ContextType);
        }

        public class MyContext : DbContext
        {
        }
    }
}
