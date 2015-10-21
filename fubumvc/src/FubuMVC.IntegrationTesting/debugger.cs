using System;
using System.Collections.Generic;
using System.Reflection;
using FubuCore.Dates;
using FubuMVC.Core;
using Shouldly;
using NUnit.Framework;

namespace FubuMVC.IntegrationTesting
{
    [TestFixture, Explicit]
    public class debugger
    {
        [Test]
        public void try_to_load_asset_fixture()
        {
            var assembly = Assembly.Load("WebDriver");
            assembly.GetExportedTypes().Each(x => Console.WriteLine(x.FullName));
        }

        [Test]
        public void try_to_load_the_serenity_assembly()
        {
            var assembly = Assembly.Load("Serenity");
            assembly.GetExportedTypes().Each(x => Console.WriteLine(x.FullName));
        }

        [Test]
        public void try_to_create_ISystemTime()
        {
            using (var runtime = FubuRuntime.Basic())
            {
                runtime.Get<ISystemTime>().ShouldBeOfType<SystemTime>();
            }
        }
    }
}