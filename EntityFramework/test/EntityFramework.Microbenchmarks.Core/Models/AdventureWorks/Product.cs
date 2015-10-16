// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;

namespace EntityFramework.Microbenchmarks.Core.Models.AdventureWorks
{
    public class Product
    {
        public Product()
        {
            BillOfMaterials = new HashSet<BillOfMaterials>();
            BillOfMaterialsNavigation = new HashSet<BillOfMaterials>();
            ProductCostHistory = new HashSet<ProductCostHistory>();
            ProductDocument = new HashSet<ProductDocument>();
            ProductInventory = new HashSet<ProductInventory>();
            ProductListPriceHistory = new HashSet<ProductListPriceHistory>();
            ProductProductPhoto = new HashSet<ProductProductPhoto>();
            ProductReview = new HashSet<ProductReview>();
            ProductVendor = new HashSet<ProductVendor>();
            PurchaseOrderDetail = new HashSet<PurchaseOrderDetail>();
            ShoppingCartItem = new HashSet<ShoppingCartItem>();
            SpecialOfferProduct = new HashSet<SpecialOfferProduct>();
            TransactionHistory = new HashSet<TransactionHistory>();
            WorkOrder = new HashSet<WorkOrder>();
        }

        public int ProductID { get; set; }
        public string Class { get; set; }
        public string Color { get; set; }
        public int DaysToManufacture { get; set; }
        public DateTime? DiscontinuedDate { get; set; }
        public bool FinishedGoodsFlag { get; set; }
        public decimal ListPrice { get; set; }
        public bool MakeFlag { get; set; }
        public DateTime ModifiedDate { get; set; }
        public string Name { get; set; }
        public string ProductLine { get; set; }
        public int? ProductModelID { get; set; }
        public string ProductNumber { get; set; }
        public int? ProductSubcategoryID { get; set; }
        public short ReorderPoint { get; set; }
        public Guid rowguid { get; set; }
        public short SafetyStockLevel { get; set; }
        public DateTime? SellEndDate { get; set; }
        public DateTime SellStartDate { get; set; }
        public string Size { get; set; }
        public string SizeUnitMeasureCode { get; set; }
        public decimal StandardCost { get; set; }
        public string Style { get; set; }
        public decimal? Weight { get; set; }
        public string WeightUnitMeasureCode { get; set; }

        public virtual ICollection<BillOfMaterials> BillOfMaterials { get; set; }
        public virtual ICollection<BillOfMaterials> BillOfMaterialsNavigation { get; set; }
        public virtual ICollection<ProductCostHistory> ProductCostHistory { get; set; }
        public virtual ICollection<ProductDocument> ProductDocument { get; set; }
        public virtual ICollection<ProductInventory> ProductInventory { get; set; }
        public virtual ICollection<ProductListPriceHistory> ProductListPriceHistory { get; set; }
        public virtual ICollection<ProductProductPhoto> ProductProductPhoto { get; set; }
        public virtual ICollection<ProductReview> ProductReview { get; set; }
        public virtual ICollection<ProductVendor> ProductVendor { get; set; }
        public virtual ICollection<PurchaseOrderDetail> PurchaseOrderDetail { get; set; }
        public virtual ICollection<ShoppingCartItem> ShoppingCartItem { get; set; }
        public virtual ICollection<SpecialOfferProduct> SpecialOfferProduct { get; set; }
        public virtual ICollection<TransactionHistory> TransactionHistory { get; set; }
        public virtual ICollection<WorkOrder> WorkOrder { get; set; }
        public virtual ProductModel ProductModel { get; set; }
        public virtual ProductSubcategory ProductSubcategory { get; set; }
        public virtual UnitMeasure SizeUnitMeasureCodeNavigation { get; set; }
        public virtual UnitMeasure WeightUnitMeasureCodeNavigation { get; set; }
    }
}
