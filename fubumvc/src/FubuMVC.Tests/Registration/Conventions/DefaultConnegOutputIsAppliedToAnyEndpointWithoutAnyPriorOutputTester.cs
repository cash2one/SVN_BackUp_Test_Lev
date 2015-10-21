using FubuMVC.Core;
using FubuMVC.Core.Json;
using FubuMVC.Core.Registration;
using FubuMVC.Core.Runtime;
using FubuMVC.Core.Runtime.Formatters;
using Shouldly;
using NUnit.Framework;

namespace FubuMVC.Tests.Registration.Conventions
{
    [TestFixture]
    public class DefaultConnegOutputIsAppliedToAnyEndpointWithoutAnyPriorOutputTester
    {
        public class SomeInput
        {
        }

        public class SomeResource
        {
        }

        public class SomeController
        {
            public SomeResource Go(SomeInput input)
            {
                return new SomeResource();
            }
        }

        [Test]
        public void applies_conneg()
        {
            var registry = new FubuRegistry();
            registry.Actions.IncludeType<SomeController>();

            var chain = BehaviorGraph.BuildFrom(registry).ChainFor<SomeController>(x => x.Go(null));

            chain.Output.Add(new NewtonsoftJsonFormatter());
            chain.Output.Add(new XmlFormatter());

            chain.Input.CanRead(MimeType.HttpFormMimetype).ShouldBeTrue();
            chain.Input.CanRead(MimeType.Json).ShouldBeTrue();
            chain.Input.CanRead(MimeType.Xml).ShouldBeTrue();
        }
    }
}