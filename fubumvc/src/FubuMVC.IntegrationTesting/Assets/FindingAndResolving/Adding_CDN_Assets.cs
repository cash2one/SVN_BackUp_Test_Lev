﻿using FubuMVC.Core;
using FubuMVC.Core.Assets;
using Shouldly;
using NUnit.Framework;

namespace FubuMVC.IntegrationTesting.Assets.FindingAndResolving
{
    [TestFixture, Explicit("Flaky on CI. Betting it's file system crap")]
    public class Adding_CDN_Assets_to_AssetGraph : AssetIntegrationContext
    {
        public Adding_CDN_Assets_to_AssetGraph()
        {
            File("foo.js");
            File("bar.js");
            File("fubar.1.2.js");
        }

        public class MyRegistry : FubuRegistry
        {
            public MyRegistry()
            {
                AlterSettings<AssetSettings>(x => { x.CdnAssets.Add(new CdnAsset {Url = "http://server.com/bar.js"}); });
            }
        }

        [Test]
        public void cdn_registration_with_file_name()
        {
            var cdn = new CdnAsset {File = "fubar.1.2.js", Url = "http://server/fubar.js"};
            var asset = AllAssets.RegisterCdnAsset(cdn);

            asset.Filename.ShouldBe("fubar.1.2.js");
            asset.File.ShouldNotBeNull(); // just seeing that we match up on existing
            asset.CdnUrl.ShouldBe("http://server/fubar.js");
            asset.Url.ShouldBe("fubar.1.2.js");
        }

        [Test]
        public void cdn_registration_flows_from_settings_to_graph()
        {
            var asset = Assets.FindAsset("bar.js");
            asset.CdnUrl.ShouldBe("http://server.com/bar.js");
        }

        [Test]
        public void add_cdn_that_does_not_match()
        {
            var asset = AllAssets.RegisterCdnAsset(new CdnAsset {Url = "http://cdn.server.com/jquery.js"});
            asset.CdnUrl.ShouldBe("http://cdn.server.com/jquery.js");
            asset.FallbackTest.ShouldBeNull();
            asset.File.ShouldBeNull();
        }

        [Test]
        public void add_cdn_that_does_match()
        {
            var cdn = new CdnAsset
            {
                Url = "http://cdn.server.com/foo.js",
                Fallback = "something == null"
            };

            var asset = AllAssets.RegisterCdnAsset(cdn);
            asset.File.ShouldNotBeNull();
            asset.FallbackTest.ShouldBe("something == null");
            asset.CdnUrl.ShouldBe("http://cdn.server.com/foo.js");
        }
    }
}