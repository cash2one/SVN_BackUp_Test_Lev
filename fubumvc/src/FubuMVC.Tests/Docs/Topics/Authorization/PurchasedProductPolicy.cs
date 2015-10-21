﻿using System.Collections.Generic;
using System.Linq;
using FubuMVC.Core;
using FubuMVC.Core.Runtime;
using FubuMVC.Core.Security;
using FubuMVC.Core.Security.Authorization;

namespace FubuMVC.Tests.Docs.Topics.Authorization
{
    // SAMPLE: authorization-policy
    public class PurchasedProductPolicy : IAuthorizationPolicy
    {
        public AuthorizationRight RightsFor(IFubuRequestContext request)
        {
            var customerId = request.Models.Get<Customer>().Id;
            var productId = request.Models.Get<Product>().Id;

            var hasPurchasedProduct = request.Service<IRepository>().Get<IPurchaseHistory>(customerId)
                .Any(x => x.ContainsProduct(productId));

            return !hasPurchasedProduct ? AuthorizationRight.Deny : AuthorizationRight.Allow;
        }
    }
    // ENDSAMPLE

    public class Customer
    {
        public object Id { get; set; }
    }

    public class Product
    {
        public object Id { get; set; }
    }

    public interface IRepository
    {
        IEnumerable<IPurchaseHistory> Get<T>(object customerId);
    }

    public interface IPurchaseHistory
    {
        bool ContainsProduct(object productId);
    }
}