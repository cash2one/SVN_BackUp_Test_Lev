using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using FubuMVC.Core.Diagnostics.Packaging;
using FubuMVC.Core.Registration;
using FubuMVC.Core.Runtime.Files;
using FubuMVC.Core.View.Model;

namespace FubuMVC.Core.View
{
    /// <summary>
    /// Implement this contract to provide a service which allows to obatin
    /// <see cref="IViewToken"/>s based on a <see cref="TypePool"/> and the
    /// relevant <see cref="BehaviorGraph"/>
    /// </summary>
    public interface IViewFacility
    {
        void Fill(ViewEngineSettings settings, BehaviorGraph graph, IPerfTimer timer, IFubuApplicationFiles files);
        IEnumerable<IViewToken> AllViews();

        ITemplateFile FindInShared(string viewName);
        ViewEngineSettings Settings { get; set; }

        Type TemplateType { get; }

        void AttachViewModels(ViewTypePool types, ITemplateLogger logger);

        void ReadSharedNamespaces(CommonViewNamespaces namespaces);
        void AttachLayouts(ViewEngineSettings settings);
    }
}