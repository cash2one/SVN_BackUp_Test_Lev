﻿using System;
using System.Collections.Generic;
using System.Linq;

using BetterCms.Core.DataContracts.Enums;
using BetterCms.Core.Exceptions;
using BetterCms.Core.Exceptions.Mvc;
using BetterCms.Core.Security;

using BetterCms.Module.MediaManager.Models;

using BetterCms.Module.Pages.Content.Resources;
using BetterCms.Module.Pages.Models;
using BetterCms.Module.Pages.Models.Events;
using BetterCms.Module.Pages.Services;
using BetterCms.Module.Pages.ViewModels.Page;

using BetterCms.Module.Root;
using BetterCms.Module.Root.Models;
using BetterCms.Module.Root.Models.Extensions;
using BetterCms.Module.Root.Mvc;
using BetterCms.Module.Root.Mvc.Helpers;
using BetterCms.Module.Root.Services;

using BetterModules.Core.DataAccess;
using BetterModules.Core.DataAccess.DataContext;
using BetterModules.Core.DataAccess.DataContext.Fetching;
using BetterModules.Core.Web.Mvc.Commands;

using CategoryEntity = BetterCms.Module.Root.Models.Category;

namespace BetterCms.Module.Pages.Command.Page.SavePageProperties
{
    /// <summary>
    /// Page properties save command.
    /// </summary>
    public class SavePagePropertiesCommand : CommandBase, ICommand<EditPagePropertiesViewModel, SavePageResponse>
    {
        /// <summary>
        /// The page service
        /// </summary>
        private readonly IPageService pageService;

        /// <summary>
        /// The redirect service
        /// </summary>
        private readonly IRedirectService redirectService;

        /// <summary>
        /// The tag service
        /// </summary>
        private readonly ITagService tagService;

        /// <summary>
        /// The sitemap service.
        /// </summary>
        private readonly ISitemapService sitemapService;

        /// <summary>
        /// The url service
        /// </summary>
        private readonly IUrlService urlService;

        /// <summary>
        /// The options service
        /// </summary>
        private readonly IOptionService optionService;

        /// <summary>
        /// The CMS configuration
        /// </summary>
        private readonly ICmsConfiguration cmsConfiguration;

        /// <summary>
        /// The access control service
        /// </summary>
        private readonly IAccessControlService accessControlService;

        /// <summary>
        /// The content service
        /// </summary>
        private readonly IContentService contentService;

        /// <summary>
        /// The master page service
        /// </summary>
        private readonly IMasterPageService masterPageService;

        /// <summary>
        /// The category service
        /// </summary>
        private readonly ICategoryService categoryService;

        /// <summary>
        /// Initializes a new instance of the <see cref="SavePagePropertiesCommand" /> class.
        /// </summary>
        /// <param name="pageService">The page service.</param>
        /// <param name="redirectService">The redirect service.</param>
        /// <param name="tagService">The tag service.</param>
        /// <param name="sitemapService">The sitemap service.</param>
        /// <param name="urlService">The URL service.</param>
        /// <param name="optionService">The option service.</param>
        /// <param name="cmsConfiguration">The CMS configuration.</param>
        /// <param name="accessControlService">The access control service.</param>
        /// <param name="contentService">The content service.</param>
        /// <param name="masterPageService">The master page service.</param>
        /// <param name="categoryService">The category service.</param>
        public SavePagePropertiesCommand(
            IPageService pageService,
            IRedirectService redirectService,
            ITagService tagService,
            ISitemapService sitemapService,
            IUrlService urlService,
            IOptionService optionService,
            ICmsConfiguration cmsConfiguration,
            IAccessControlService accessControlService,
            IContentService contentService,
            IMasterPageService masterPageService,
            ICategoryService categoryService)
        {
            this.pageService = pageService;
            this.redirectService = redirectService;
            this.tagService = tagService;
            this.sitemapService = sitemapService;
            this.urlService = urlService;
            this.optionService = optionService;
            this.cmsConfiguration = cmsConfiguration;
            this.accessControlService = accessControlService;
            this.contentService = contentService;
            this.masterPageService = masterPageService;
            this.categoryService = categoryService;
        }

        /// <summary>
        /// Executes the specified request.
        /// </summary>
        /// <param name="request">The request.</param>
        /// <returns>Save response.</returns>
        /// <exception cref="CmsException">Failed to save page properties.</exception>
        public SavePageResponse Execute(EditPagePropertiesViewModel request)
        {
            var isMultilanguageEnabled = cmsConfiguration.EnableMultilanguage;

            ValidateRequest(request, isMultilanguageEnabled);

            var pageQuery =
                Repository.AsQueryable<PageProperties>(p => p.Id == request.Id)
                          .FetchMany(p => p.Options)
                          .Fetch(p => p.Layout)
                          .ThenFetchMany(l => l.LayoutOptions)
                          .FetchMany(p => p.MasterPages)
                          .AsQueryable();

            if (cmsConfiguration.Security.AccessControlEnabled)
            {
                pageQuery = pageQuery.FetchMany(f => f.AccessRules);
            }

            var page = pageQuery.ToList().FirstOne();
            var beforeChange = new UpdatingPagePropertiesModel(page);

            var roles = page.IsMasterPage
                            ? new[]
                                  {
                                      RootModuleConstants.UserRoles.EditContent, RootModuleConstants.UserRoles.PublishContent, RootModuleConstants.UserRoles.Administration
                                  }
                            : new[] { RootModuleConstants.UserRoles.EditContent, RootModuleConstants.UserRoles.PublishContent };

            if (cmsConfiguration.Security.AccessControlEnabled)
            {
                AccessControlService.DemandAccess(page, Context.Principal, AccessLevel.ReadWrite, roles);
            }
            else
            {
                AccessControlService.DemandAccess(Context.Principal, roles);
            }

            var canEdit = page.IsMasterPage
                              ? SecurityService.IsAuthorized(
                                  Context.Principal,
                                  RootModuleConstants.UserRoles.MultipleRoles(RootModuleConstants.UserRoles.EditContent, RootModuleConstants.UserRoles.Administration))
                              : SecurityService.IsAuthorized(Context.Principal, RootModuleConstants.UserRoles.EditContent);

            IList<PageProperties> translations = null;
            if (canEdit && isMultilanguageEnabled && !page.IsMasterPage)
            {
                translations = LoadAndValidateTranslations(page, request);
            }

            // Load master pages for updating page's master path and page's children master path
            IList<Guid> newMasterIds;
            IList<Guid> oldMasterIds;
            IList<Guid> childrenPageIds;
            IList<MasterPage> existingChildrenMasterPages;
            masterPageService.PrepareForUpdateChildrenMasterPages(page, request.MasterPageId, out newMasterIds, out oldMasterIds, out childrenPageIds, out existingChildrenMasterPages);

            IList<SitemapNode> updatedNodes = null;

            // Start transaction, only when everything is already loaded
            UnitOfWork.BeginTransaction();

            Models.Redirect redirectCreated = null;
            var initialSeoStatus = page.HasSEO;

            request.PageUrl = urlService.FixUrl(request.PageUrl);

            if (canEdit && !string.Equals(page.PageUrl, request.PageUrl))
            {
                pageService.ValidatePageUrl(request.PageUrl, request.Id);
                if (request.RedirectFromOldUrl)
                {
                    var redirect = redirectService.CreateRedirectEntity(page.PageUrl, request.PageUrl);
                    if (redirect != null)
                    {
                        Repository.Save(redirect);
                        redirectCreated = redirect;
                    }
                }

                if (request.UpdateSitemap)
                {
                    updatedNodes = sitemapService.ChangeUrlsInAllSitemapsNodes(page.PageUrl, request.PageUrl);
                }

                page.PageUrl = request.PageUrl;
            }

            List<PageProperties> updatePageTranslations = null;
            if (canEdit)
            {
                page.PageUrlHash = page.PageUrl.UrlHash();
                page.ForceAccessProtocol = request.ForceAccessProtocol;

                categoryService.CombineEntityCategories<PageProperties, PageCategory>(page, request.Categories);                

                page.Title = request.PageName;
                page.CustomCss = request.PageCSS;
                page.CustomJS = request.PageJavascript;

                masterPageService.SetMasterOrLayout(page, request.MasterPageId, request.TemplateId);

                if (isMultilanguageEnabled && !page.IsMasterPage)
                {
                    updatePageTranslations = UpdatePageTranslations(page, translations, request);
                }
            }

            var publishDraftContent = false;
            if (request.CanPublishPage && !page.IsMasterPage)
            {
                AccessControlService.DemandAccess(Context.Principal, RootModuleConstants.UserRoles.PublishContent);

                if (request.IsPagePublished)
                {
                    if (page.Status != PageStatus.Published)
                    {
                        page.Status = PageStatus.Published;
                        page.PublishedOn = DateTime.Now;
                        publishDraftContent = true;
                    }
                }
                else
                {
                    page.Status = PageStatus.Unpublished;
                }
            }

            IList<PageOption> pageOptions = page.Options.Distinct().ToList();
            if (canEdit)
            {
                if (!page.IsMasterPage)
                {
                    page.UseNoFollow = request.UseNoFollow;
                    page.UseNoIndex = request.UseNoIndex;
                    page.IsArchived = request.IsArchived;
                }

                page.UseCanonicalUrl = request.UseCanonicalUrl;
                page.Version = request.Version;

                page.Image = request.Image != null && request.Image.ImageId.HasValue ? Repository.AsProxy<MediaImage>(request.Image.ImageId.Value) : null;
                page.SecondaryImage = request.SecondaryImage != null && request.SecondaryImage.ImageId.HasValue
                                          ? Repository.AsProxy<MediaImage>(request.SecondaryImage.ImageId.Value)
                                          : null;
                page.FeaturedImage = request.FeaturedImage != null && request.FeaturedImage.ImageId.HasValue
                                         ? Repository.AsProxy<MediaImage>(request.FeaturedImage.ImageId.Value)
                                         : null;

                pageOptions = optionService.SaveOptionValues(request.OptionValues, pageOptions, () => new PageOption { Page = page });

                if (cmsConfiguration.Security.AccessControlEnabled)
                {
                    page.AccessRules.RemoveDuplicateEntities();

                    var accessRules = request.UserAccessList != null ? request.UserAccessList.Cast<IAccessRule>().ToList() : null;
                    accessControlService.UpdateAccessControl(page, accessRules);
                }
            }

            // Notify about page properties changing.
            var cancelEventArgs = Events.PageEvents.Instance.OnPagePropertiesChanging(beforeChange, new UpdatingPagePropertiesModel(page));
            if (cancelEventArgs.Cancel)
            {
                Context.Messages.AddError(cancelEventArgs.CancellationErrorMessages.ToArray());
                return null;
            }

            Repository.Save(page);

            IList<Tag> newTags = null;
            if (canEdit)
            {
                masterPageService.UpdateChildrenMasterPages(existingChildrenMasterPages, oldMasterIds, newMasterIds, childrenPageIds);
                tagService.SavePageTags(page, request.Tags, out newTags);
            }

            if (publishDraftContent)
            {
                contentService.PublishDraftContent(page.Id);
            }

            UnitOfWork.Commit();

            // Notify about page properties change.
            page.Options = pageOptions;
            Events.PageEvents.Instance.OnPagePropertiesChanged(page);

            // Notify about translation properties changed
            if (updatePageTranslations != null)
            {
                updatePageTranslations.ForEach(Events.PageEvents.Instance.OnPagePropertiesChanged);
            }

            // Notify about redirect creation.
            if (redirectCreated != null)
            {
                Events.PageEvents.Instance.OnRedirectCreated(redirectCreated);
            }

            // Notify about SEO status change.
            if (initialSeoStatus != page.HasSEO)
            {
                Events.PageEvents.Instance.OnPageSeoStatusChanged(page);
            }

            // Notify about new tags.
            Events.RootEvents.Instance.OnTagCreated(newTags);

            // Notify about updated sitemap nodes.
            if (updatedNodes != null)
            {
                var updatedSitemaps = new List<Models.Sitemap>();
                foreach (var node in updatedNodes)
                {
                    Events.SitemapEvents.Instance.OnSitemapNodeUpdated(node);
                    if (!updatedSitemaps.Contains(node.Sitemap))
                    {
                        updatedSitemaps.Add(node.Sitemap);
                    }
                }

                foreach (var updatedSitemap in updatedSitemaps)
                {
                    Events.SitemapEvents.Instance.OnSitemapUpdated(updatedSitemap);
                }
            }

            return new SavePageResponse(page);
        }

        /// <summary>
        /// Validates the request.
        /// </summary>
        /// <param name="request">The request.</param>
        /// <param name="isMultilanguageEnabled">if set to <c>true</c> multilanguage is enabled.</param>
        private void ValidateRequest(EditPagePropertiesViewModel request, bool isMultilanguageEnabled)
        {
            if (!request.MasterPageId.HasValue && !request.TemplateId.HasValue)
            {
                var logMessage = string.Format("Template or master page should be selected for page {0}.", request.Id);
                throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_NoLayoutOrMasterSelected_Message, logMessage);
            }

            if (request.MasterPageId.HasValue && request.TemplateId.HasValue)
            {
                var logMessage = string.Format("Only one of master page and layout can be selected for page {0}.", request.Id);
                throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_LayoutAndMasterIsSelected_Message, logMessage);
            }

            if (request.MasterPageId.HasValue)
            {
                if (request.Id == request.MasterPageId.Value)
                {
                    var logMessage = string.Format("Selected master page is the current page {0}.", request.Id);
                    throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_SelectedMasterIsCurrentPage_Message, logMessage);
                }

                if (Repository.AsQueryable<MasterPage>().Where(m => m.Page.Id == request.MasterPageId.Value).Any(m => m.Master.Id == request.Id))
                {
                    var logMessage = string.Format("Selected master page {0} is a child of the current page {1}.", request.MasterPageId.Value, request.Id);
                    throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_SelectedMasterIsChildPage_Message, logMessage);
                }
            }

            if (isMultilanguageEnabled)
            {
                var pageId = request.Id.HasDefaultValue() ? (Guid?)null : request.Id;
                var pageLanguageId = request.LanguageId.HasValue && !request.LanguageId.Value.HasDefaultValue() ? request.LanguageId : null;
                
                if (request.Translations != null)
                {
                    if (pageId.HasValue && request.Translations.Any(t => t.Id == pageId))
                    {
                        var logMessage = string.Format("Page cannot be added itself to translations list. Id: {0}, ", request.Id);
                        throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_PageHasTranslationsWithItsId_Message, logMessage);
                    }

                    if (request.Translations.Any(t => t.LanguageId == pageLanguageId))
                    {
                        var logMessage = string.Format("Page cannot contain translations with the same language as itself. Id: {0}, LanguageId: {1}", request.Id, request.LanguageId);
                        throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_PageHasTranslationsWithItsLanguage_Message, logMessage);
                    }

                    if (request.Translations.GroupBy(t => t.LanguageId).Any(g => g.Count() > 1))
                    {
                        var logMessage = string.Format("Page cannot contain two or more translations with the same language. Id: {0}, LanguageId: {1}", request.Id, request.LanguageId);
                        throw new ValidationException(() => PagesGlobalization.SavePagePropertiesCommand_PageHasTranslationsWithSameLanguage_Message, logMessage);
                    }
                }
            }
        }

        /// <summary>
        /// Loads the list of translations and validates them.
        /// </summary>
        /// <param name="page">The page.</param>
        /// <param name="request">The request.</param>
        /// <returns>List of old and new page translations</returns>
        private IList<PageProperties> LoadAndValidateTranslations(PageProperties page, EditPagePropertiesViewModel request)
        {
            // Load translations
            var predicateBuilder = PredicateBuilder.False<PageProperties>();
            if (page.LanguageGroupIdentifier.HasValue)
            {
                predicateBuilder = predicateBuilder.Or(p => p.LanguageGroupIdentifier == page.LanguageGroupIdentifier.Value);
            }

            if (request.Translations != null)
            {
                request.Translations.Select(t => t.Id).ToList().ForEach(t => { predicateBuilder = predicateBuilder.Or(p => p.Id == t); });
            }

            var translations = Repository.AsQueryable<PageProperties>().Where(predicateBuilder).ToList();

            // Validate translations
            if (request.Translations != null)
            {
                request.Translations.ForEach(rt =>
                    {
                        var translation = translations.FirstOrDefault(t => t.Id == rt.Id && t.LanguageGroupIdentifier != null && t.LanguageGroupIdentifier != page.LanguageGroupIdentifier);
                        if (translation != null)
                        {
                            var logMessage = string.Format("Page cannot be assigned as translation, because it's assigned with another page. PageId: {0}, TranslationId: {1}", request.Id, translation.Id);
                            var message = string.Format(PagesGlobalization.SavePagePropertiesCommand_PageTranslationsIsAlreadyAssigned_Message, translation.Title);
                            throw new ValidationException(() => message, logMessage);
                        }

                        translation = translations.FirstOrDefault(t => t.Id == rt.Id && (t.Language != null && t.Language.Id != rt.LanguageId));
                        if (translation != null)
                        {
                            var logMessage = string.Format("Page cannot be assigned with language {0}, because it has assigned another language {1}. PageId: {2}, TranslationId: {1}", 
                                rt.LanguageId,
                                translation.Language != null ? translation.Language.Id : (Guid?)null, request.Id);
                            var message = string.Format(PagesGlobalization.SavePagePropertiesCommand_PageTranslationsHasDifferentLanguage_Message, translation.Title);
                            throw new ValidationException(() => message, logMessage);
                        }

                        translation = translations.FirstOrDefault(t => t.Id == rt.Id && t.IsMasterPage);
                        if (translation != null)
                        {
                            var logMessage = string.Format("Master pages cannot be assigned as translation. PageId: {0}, TranslationId: {1}", request.Id, translation.Id);
                            var message = string.Format(PagesGlobalization.SavePagePropertiesCommand_PageTranslationsIsMasterPage_Message, translation.Title);
                            throw new ValidationException(() => message, logMessage);
                        }
                    });
            }

            return translations;
        }

        /// <summary>
        /// Updates the page translations.
        /// </summary>
        /// <param name="page">The page.</param>
        /// <param name="translations">The translations.</param>
        /// <param name="request">The request.</param>
        private List<PageProperties> UpdatePageTranslations(PageProperties page, IList<PageProperties> translations, EditPagePropertiesViewModel request)
        {
            var updatedPages = new List<PageProperties>();

            // Update page language
            var oldLanguageId = page.Language != null ? page.Language.Id : (Guid?)null;
            var newLanguageId = request.LanguageId;
            if (oldLanguageId != newLanguageId)
            {
                page.Language = request.LanguageId.HasValue ? Repository.AsProxy<Language>(request.LanguageId.Value) : null;
            }

            // Change language group
            var requestTranslations = request.Translations ?? new List<PageTranslationViewModel>();
            var oldLanguageGroupId = page.LanguageGroupIdentifier;

            // Old group's translations, not included to current group, goes to independent groups
            if (oldLanguageGroupId.HasValue)
            {
                translations
                    .Where(t => t.LanguageGroupIdentifier == oldLanguageGroupId
                        && requestTranslations.All(rt => rt.Id != t.Id)
                        && t.Id != page.Id)
                    .ToList()
                    .ForEach(translation =>
                            {
                                translation.LanguageGroupIdentifier = null;

                                updatedPages.Add(translation);
                                Repository.Save(translation);
                            });
            }

            if (requestTranslations.Count == 0)
            {
                page.LanguageGroupIdentifier = null;
            }
            else if (!page.LanguageGroupIdentifier.HasValue)
            {
                page.LanguageGroupIdentifier = Guid.NewGuid();
            }

            // Save current page translations
            foreach (var translationViewModel in requestTranslations)
            {
                var translation = translations.Where(t => t.Id == translationViewModel.Id).FirstOne();
                var changeLanguage = translation.Language == null && translationViewModel.LanguageId.HasValue;
                if (translation.LanguageGroupIdentifier != page.LanguageGroupIdentifier || changeLanguage)
                {
                    translation.LanguageGroupIdentifier = page.LanguageGroupIdentifier;
                    if (changeLanguage)
                    {
                        translation.Language = Repository.AsProxy<Language>(translationViewModel.LanguageId.Value);
                    }

                    updatedPages.Add(translation);
                    Repository.Save(translation);
                }
            }

            return updatedPages;
        }
    }
}