﻿using System;

using BetterCms.Module.Pages.ViewModels.Content;

namespace BetterCms.Module.Pages.ViewModels.Widgets
{
    /// <summary>
    /// A widget view model.
    /// </summary>
    public class EditHtmlContentWidgetViewModel : HtmlContentWidgetViewModel, IDraftDestroy
    {
        /// <summary>
        /// Gets or sets the page content id to preview this widget.
        /// </summary>
        /// <value>
        /// The page content id to preview this widget.
        /// </value>
        public Guid? PreviewOnPageContentId { get; set; }

        /// <summary>
        /// Gets or sets a value indicating whether content editor must be opened in source mode.
        /// </summary>
        /// <value>
        ///   <c>true</c> if content editor must be opened in source mode; otherwise, <c>false</c>.
        /// </value>
        public bool EditInSourceMode { get; set; }

        /// <summary>
        /// Gets or sets a value indicating whether user can destroy draft.
        /// </summary>
        /// <value>
        ///   <c>true</c> if user can destroy draft; otherwise, <c>false</c>.
        /// </value>
        public bool CanDestroyDraft { get; set; }

        /// <summary>
        /// Gets or sets the last dynamic region number.
        /// </summary>
        /// <value>
        /// The last dynamic region number.
        /// </value>
        public int LastDynamicRegionNumber { get; set; }

        /// <summary>
        /// Determines, if child regions should be included to the results.
        /// </summary>
        /// <value>
        ///   <c>true</c> if child regions should be included to the results; otherwise, <c>false</c>.
        /// </value>
        public bool IncludeChildRegions { get; set; }

        /// <summary>
        /// Gets or sets the categories filter key.
        /// </summary>
        /// <value>
        /// The categories filter key.
        /// </value>
        public string CategoriesFilterKey { get; set; }

        /// <summary>
        /// Returns a <see cref="System.String" /> that represents this instance.
        /// </summary>
        /// <returns>
        /// A <see cref="System.String" /> that represents this instance.
        /// </returns>
        public override string ToString()
        {
            return string.Format("{0}", base.ToString());
        }
    }
}