﻿using System;
using Fubu.Running;
using FubuCore;
using FubuMVC.Core;
using FubuMVC.Tests.TestSupport;
using Shouldly;
using NUnit.Framework;
using Rhino.Mocks;

namespace FubuMVC.IntegrationTesting.CommandLine.Running
{
    [TestFixture]
    public class RemoteFubuMvcBootstrapperTester : InteractionContext<RemoteFubuMvcBootstrapper>
    {
        private StartApplication startMessage;

        protected override void beforeEach()
        {
            startMessage = new StartApplication
            {
                PhysicalPath = Environment.CurrentDirectory,
                ApplicationName = null,
                PortNumber = 5500
            };
        }

        private void theFoundTypesAre(params Type[] types)
        {
            MockFor<IFubuRegistryFinder>().Stub(x => x.Find())
                                               .Return(types);
        }

        private void shouldHaveStartedApp<T>() where T : FubuRegistry
        {
            MockFor<IFubuMvcApplicationActivator>()
                .AssertWasCalled(x => x.Initialize(typeof(T), startMessage));
        }

        [Test]
        public void start_application_with_no_found_applications()
        {
            theFoundTypesAre();

            ClassUnderTest.Receive(startMessage);

            var message = lastMessage<InvalidApplication>();

            message.Message.ShouldBe("Could not find any instance of FubuRegistry in any assembly in this directory");
        }

        [Test]
        public void start_application_with_a_known_application()
        {
            startMessage.ApplicationName = typeof (App1).Name;

            theFoundTypesAre(typeof(App1), typeof(App2));

            ClassUnderTest.Receive(startMessage);

            shouldHaveStartedApp<App1>();
        }

        [Test]
        public void start_application_with_no_known_app_and_multiple_apps_to_choose_from()
        {
            theFoundTypesAre(typeof(App1), typeof(App2), typeof(App3));

            ClassUnderTest.Receive(startMessage);

            var message = lastMessage<InvalidApplication>();
            message.Message.ShouldBe("Unable to determine the FubuMVC Application");
            message.Applications.ShouldHaveTheSameElementsAs("App1", "App2", "App3");
        }

        [Test]
        public void start_application_with_no_known_app_but_there_is_only_one_type()
        {
            theFoundTypesAre(typeof(App3));

            startMessage.ApplicationName = null;

            ClassUnderTest.Receive(startMessage);

            shouldHaveStartedApp<App3>();
        }

        private T lastMessage<T>() where T : new()
        {
            var calls = MockFor<IMessaging>()
                .GetArgumentsForCallsMadeOn(x => x.Send(new T()), x => x.IgnoreArguments());

            return calls[0][0].As<T>();
        }

        [Test]
        public void no_application_types()
        {
            theFoundTypesAre();

            ClassUnderTest.Receive(startMessage);

            lastMessage<InvalidApplication>()
                .Message.ShouldBe("Could not find any instance of FubuRegistry in any assembly in this directory");
        }

        [Test]
        public void could_not_find_named_application()
        {
            theFoundTypesAre(typeof(App1), typeof(App2));

            startMessage.ApplicationName = "random";

            ClassUnderTest.Receive(startMessage);

            var invalidApplication = lastMessage<InvalidApplication>();
            invalidApplication
                .Message.ShouldBe("Could not find an application named 'random'");
        
            invalidApplication.Applications.ShouldHaveTheSameElementsAs("App1", "App2");
        }
    }

    public class App1 : FubuRegistry
    {

    }

    public class App2 : FubuRegistry
    {

    }

    public class App3 : FubuRegistry
    {

    }
}