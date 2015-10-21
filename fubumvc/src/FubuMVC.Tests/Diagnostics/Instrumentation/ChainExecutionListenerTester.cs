using FubuCore.Logging;
using FubuMVC.Core.Diagnostics.Instrumentation;
using FubuMVC.Tests.TestSupport;
using NUnit.Framework;
using Rhino.Mocks;
using Shouldly;

namespace FubuMVC.Tests.Diagnostics.Instrumentation
{
    [TestFixture]
    public class ChainExecutionListenerTester : InteractionContext<ChainExecutionListener>
    {
        private void assertMessageWasLogged(object message)
        {
            MockFor<IChainExecutionLog>().AssertWasCalled(x => x.Log(message));
        }

        [Test]
        public void debugging_is_enabled()
        {
            ClassUnderTest.IsDebugEnabled.ShouldBeTrue();
        }

        [Test]
        public void info_is_enabled()
        {
            ClassUnderTest.IsInfoEnabled.ShouldBeTrue();
        }

        [Test]
        public void debug_message_delegates()
        {
            var message = new object();

            ClassUnderTest.DebugMessage(message);

            assertMessageWasLogged(message);
        }

        [Test]
        public void debug_string_delegates()
        {
            ClassUnderTest.Debug("some stuff");

            assertMessageWasLogged(new StringMessage("some stuff"));
        }

        [Test]
        public void info_message_delegates()
        {
            var message = new object();

            ClassUnderTest.InfoMessage(message);

            assertMessageWasLogged(message);
        }

        [Test]
        public void info_string_delegates()
        {
            ClassUnderTest.Info("some stuff");

            assertMessageWasLogged(new StringMessage("some stuff"));
        }


    }
}