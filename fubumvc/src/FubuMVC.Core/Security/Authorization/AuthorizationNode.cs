using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using FubuCore;
using FubuMVC.Core.Registration.Nodes;
using FubuMVC.Core.Runtime;
using StructureMap.Pipeline;

namespace FubuMVC.Core.Security.Authorization
{
    [Description("Authorization checks for this endpoint")]
    public class AuthorizationNode : BehaviorNode, IAuthorizationNode
    {
        private readonly IList<IAuthorizationPolicy> _policies = new List<IAuthorizationPolicy>();
        private Instance _failure;

        public override BehaviorCategory Category
        {
            get { return BehaviorCategory.Authorization; }
        }

        protected override IConfiguredInstance buildInstance()
        {
            var instance = new SmartInstance<AuthorizationBehavior>();
            instance.Ctor<IAuthorizationNode>().Is(this);

            if (_failure != null)
            {
                instance.Ctor<IAuthorizationFailureHandler>().Is(_failure);
            }

            return instance;
        }

        public void FailureHandler<T>() where T : IAuthorizationFailureHandler
        {
            _failure = new SmartInstance<T>();
        }

        public void FailureHandler(IAuthorizationFailureHandler handler)
        {
            _failure = new ObjectInstance(handler);
        }

        public Instance FailureHandler()
        {
            return _failure;
        }

        public void FailureHandler(Type handlerType)
        {
            _failure = new ConfiguredInstance(handlerType);
        }

        public AuthorizationRight IsAuthorized(IFubuRequestContext context)
        {
            if (!_policies.Any()) return AuthorizationRight.Allow;

            return AuthorizationRight.Combine(_policies.Select(x => x.RightsFor(context)));
        }

        public IEnumerable<IAuthorizationPolicy> Policies
        {
            get { return _policies; }
        }

        public void AddPolicies(IEnumerable<IAuthorizationPolicy> authorizationPolicies)
        {
            _policies.AddRange(authorizationPolicies);
        }

        /// <summary>
        /// Adds an authorization rule based on membership in a given role
        /// on the principal
        /// </summary>
        /// <param name="roleName"></param>
        /// <returns></returns>
        public AllowRole AddRole(string roleName)
        {
            if (AllowedRoles().Contains(roleName)) return null;

            var allow = new AllowRole(roleName);

            _policies.Add(allow);

            return allow;
        }

        /// <summary>
        /// Adds an authorization policy to this behavior chain
        /// </summary>
        /// <param name="policy"></param>
        public void AddPolicy(IAuthorizationPolicy policy)
        {
            _policies.Add(policy);
        }

        /// <summary>
        /// Add either an IAuthorizationPolicy or IAuthorizationCheck to 
        /// this chain by concrete type
        /// </summary>
        /// <param name="type"></param>
        public void Add(Type type)
    {
        if (type.CanBeCastTo<IAuthorizationPolicy>() && type.IsConcreteWithDefaultCtor())
        {
            var policy = Activator.CreateInstance(type).As<IAuthorizationPolicy>();
            AddPolicy(policy);
        }
        else if (type.CanBeCastTo<IAuthorizationCheck>())
        {
            var policyType = typeof (AuthorizationCheckPolicy<>).MakeGenericType(type);
            Add(policyType);
        }
        else
        {
            throw new ArgumentOutOfRangeException("Type {0} is not a concrete type of {1} with a default ctor or a type of {2}".ToFormat(type.FullName, typeof(IAuthorizationPolicy).FullName, typeof(IAuthorizationCheck)));
            
        }

            
    }

        /// <summary>
        /// List of all roles that have privileges to this BehaviorChain endpoint
        /// </summary>
        /// <returns></returns>
        public IEnumerable<string> AllowedRoles()
        {
            return _policies.OfType<AllowRole>().Select(x => x.Role);
        }

        /// <summary>
        /// Simple boolean test of whether or not this BehaviorChain has any
        /// authorization rules
        /// </summary>
        /// <returns></returns>
        public bool HasRules()
        {
            return _policies.Any();
        }


    }
}