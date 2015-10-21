using System.Collections.Generic;
using System.Linq;
using FubuMVC.Core.Registration;
using FubuMVC.Core.Registration.Nodes;
using FubuMVC.Core.Urls;

namespace FubuMVC.Core.Diagnostics.Endpoints
{
    public class EndpointExplorerFubuDiagnostics
    {
        private readonly BehaviorGraph _graph;
        private readonly IUrlRegistry _urls;

        public EndpointExplorerFubuDiagnostics(BehaviorGraph graph, IUrlRegistry urls)
        {
            _graph = graph;
            _urls = urls;
        }

        public EndpointList get_endpoints()
        {
            return new EndpointList
            {
                endpoints = _graph.Routes
                    .Where(IsNotDiagnosticRoute)
                    .Select(EndpointReport.ForChain)
                    .OrderBy(x => x.Title)
                    .Select(x => x.ToDictionary())
                    .ToList()
            };
        }

        public EndpointList get_partials()
        {
            return new EndpointList
            {
                endpoints = _graph.Chains.Where(x => x.IsPartialOnly && x.GetType() == typeof(BehaviorChain))
                    .Where(IsNotDiagnosticRoute)
                    .Select(EndpointReport.ForChain)
                    .OrderBy(x => x.Title)
                    .Select(x => x.ToDictionary())
                    .ToList()
            };
        }

        public static bool IsNotDiagnosticRoute(BehaviorChain chain)
        {
            if (chain is DiagnosticChain) return false;
            if (chain.Tags.Contains("Diagnostics")) return false;

            return true;
        }
    }

    public class EndpointList
    {
        public IList<IDictionary<string, object>> endpoints;
    }
}