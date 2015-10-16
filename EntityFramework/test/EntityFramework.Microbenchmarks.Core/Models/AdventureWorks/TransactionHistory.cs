// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;

namespace EntityFramework.Microbenchmarks.Core.Models.AdventureWorks
{
    public class TransactionHistory
    {
        public int TransactionID { get; set; }
        public decimal ActualCost { get; set; }
        public DateTime ModifiedDate { get; set; }
        public int ProductID { get; set; }
        public int Quantity { get; set; }
        public int ReferenceOrderID { get; set; }
        public int ReferenceOrderLineID { get; set; }
        public DateTime TransactionDate { get; set; }
        public string TransactionType { get; set; }

        public virtual Product Product { get; set; }
    }
}
