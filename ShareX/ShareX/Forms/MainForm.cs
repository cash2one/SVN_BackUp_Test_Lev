﻿#region License Information (GPL v3)

/*
    ShareX - A program that allows you to take screenshots and share any file type
    Copyright (c) 2007-2015 ShareX Team

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

    Optionally you can also view the license at <http://www.gnu.org/licenses/>.
*/

#endregion License Information (GPL v3)

using ShareX.HelpersLib;
using ShareX.HistoryLib;
using ShareX.Properties;
using ShareX.ScreenCaptureLib;
using ShareX.UploadersLib;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace ShareX
{
    public partial class MainForm : HotkeyForm
    {
        public bool IsReady { get; private set; }

        private bool forceClose, firstUpdateCheck = true;
        private UploadInfoManager uim;
        private ToolStripDropDownItem tsmiImageFileUploaders, tsmiTrayImageFileUploaders, tsmiTextFileUploaders, tsmiTrayTextFileUploaders;
        private System.Threading.Timer updateTimer;
        private static readonly object updateTimerLock = new object();

        public MainForm()
        {
            InitControls();
            HandleCreated += MainForm_HandleCreated;
        }

        private void MainForm_HandleCreated(object sender, EventArgs e)
        {
            LoadSettings();
            InitHotkeys();
            ConfigureAutoUpdate();

            IsReady = true;

            DebugHelper.WriteLine("Startup time: {0} ms", Program.StartTimer.ElapsedMilliseconds);

            UseCommandLineArgs(Program.CLI.Commands);
        }

        private void AfterShownJobs()
        {
            if (Program.IsFirstTimeConfig)
            {
                using (FirstTimeConfigForm firstTimeConfigForm = new FirstTimeConfigForm())
                {
                    firstTimeConfigForm.ShowDialog();
                }
            }
            else
            {
                this.ShowActivate();
            }

            if (Program.Settings != null && Program.Settings.ShowTrayLeftClickTip && niTray.Visible && Program.Settings.TrayLeftClickAction == HotkeyType.RectangleRegion)
            {
                niTray.ShowBalloonTip(5000, "ShareX", Resources.MainForm_AfterShownJobs_You_can_single_left_click_the_ShareX_tray_icon_to_start_region_capture_, ToolTipIcon.Info);
                Program.Settings.ShowTrayLeftClickTip = false;
            }
        }

        private void InitControls()
        {
            InitializeComponent();

            Text = Program.Title;

            tsddbWorkflows.HideImageMargin();
            tsmiTrayWorkflows.HideImageMargin();
            tsmiMonitor.HideImageMargin();
            tsmiTrayMonitor.HideImageMargin();
            tsmiOpen.HideImageMargin();
            tsmiCopy.HideImageMargin();
            tsmiShortenSelectedURL.HideImageMargin();
            tsmiShareSelectedURL.HideImageMargin();
            tsmiTrayRecentItems.HideImageMargin();

            AddMultiEnumItems<AfterCaptureTasks>(x => Program.DefaultTaskSettings.AfterCaptureJob = Program.DefaultTaskSettings.AfterCaptureJob.Swap(x),
                tsddbAfterCaptureTasks, tsmiTrayAfterCaptureTasks);
            AddMultiEnumItems<AfterUploadTasks>(x => Program.DefaultTaskSettings.AfterUploadJob = Program.DefaultTaskSettings.AfterUploadJob.Swap(x),
                tsddbAfterUploadTasks, tsmiTrayAfterUploadTasks);
            // Destinations -> Image uploader
            AddEnumItems<ImageDestination>(x =>
            {
                Program.DefaultTaskSettings.ImageDestination = x;
                // if click on "folder" with file destinations then set ImageFileDestination and check it
                if (x == ImageDestination.FileUploader)
                {
                    SetEnumChecked(Program.DefaultTaskSettings.ImageFileDestination, tsmiImageFileUploaders, tsmiTrayImageFileUploaders);
                }
                else // if click not on "folder" with destinations then uncheck file destinations
                {
                    Uncheck(tsmiImageFileUploaders, tsmiTrayImageFileUploaders);
                }
            }, tsmiImageUploaders, tsmiTrayImageUploaders);
            tsmiImageFileUploaders = (ToolStripDropDownItem)tsmiImageUploaders.DropDownItems[tsmiImageUploaders.DropDownItems.Count - 1];
            tsmiTrayImageFileUploaders = (ToolStripDropDownItem)tsmiTrayImageUploaders.DropDownItems[tsmiTrayImageUploaders.DropDownItems.Count - 1];
            AddEnumItems<FileDestination>(x =>
            {
                Program.DefaultTaskSettings.ImageFileDestination = x;
                tsmiImageFileUploaders.PerformClick();
                tsmiTrayImageFileUploaders.PerformClick();
            }, tsmiImageFileUploaders, tsmiTrayImageFileUploaders);
            // Destinations -> Text uploader
            AddEnumItems<TextDestination>(x =>
            {
                Program.DefaultTaskSettings.TextDestination = x;
                // if click on "folder" with file destinations then set TextFileDestination and check it
                if (x == TextDestination.FileUploader)
                {
                    SetEnumChecked(Program.DefaultTaskSettings.TextFileDestination, tsmiTextFileUploaders, tsmiTrayTextFileUploaders);
                }
                else // if click not on "folder" with destinations then uncheck file destinations
                {
                    Uncheck(tsmiTextFileUploaders, tsmiTrayTextFileUploaders);
                }
            }, tsmiTextUploaders, tsmiTrayTextUploaders);
            tsmiTextFileUploaders = (ToolStripDropDownItem)tsmiTextUploaders.DropDownItems[tsmiTextUploaders.DropDownItems.Count - 1];
            tsmiTrayTextFileUploaders = (ToolStripDropDownItem)tsmiTrayTextUploaders.DropDownItems[tsmiTrayTextUploaders.DropDownItems.Count - 1];
            AddEnumItems<FileDestination>(x =>
            {
                Program.DefaultTaskSettings.TextFileDestination = x;
                tsmiTextFileUploaders.PerformClick();
                tsmiTrayTextFileUploaders.PerformClick();
            }, tsmiTextFileUploaders, tsmiTrayTextFileUploaders);
            // Destinations -> File uploader
            AddEnumItems<FileDestination>(x => Program.DefaultTaskSettings.FileDestination = x, tsmiFileUploaders, tsmiTrayFileUploaders);
            AddEnumItems<UrlShortenerType>(x => Program.DefaultTaskSettings.URLShortenerDestination = x, tsmiURLShorteners, tsmiTrayURLShorteners);
            AddEnumItems<URLSharingServices>(x => Program.DefaultTaskSettings.URLSharingServiceDestination = x, tsmiURLSharingServices, tsmiTrayURLSharingServices);

            foreach (UrlShortenerType urlShortener in Helpers.GetEnums<UrlShortenerType>())
            {
                ToolStripMenuItem tsmi = new ToolStripMenuItem(urlShortener.GetLocalizedDescription());
                tsmi.Click += (sender, e) => uim.ShortenURL(urlShortener);
                tsmiShortenSelectedURL.DropDownItems.Add(tsmi);
            }

            foreach (URLSharingServices urlSharingService in Helpers.GetEnums<URLSharingServices>())
            {
                ToolStripMenuItem tsmi = new ToolStripMenuItem(urlSharingService.GetLocalizedDescription());
                tsmi.Click += (sender, e) => uim.ShareURL(urlSharingService);
                tsmiShareSelectedURL.DropDownItems.Add(tsmi);
            }

            ImageList il = new ImageList();
            il.ColorDepth = ColorDepth.Depth32Bit;
            il.Images.Add(Resources.navigation_090_button);
            il.Images.Add(Resources.cross_button);
            il.Images.Add(Resources.tick_button);
            il.Images.Add(Resources.navigation_000_button);
            lvUploads.SmallImageList = il;

            TaskManager.ListViewControl = lvUploads;
            uim = new UploadInfoManager(lvUploads);

            ExportImportControl.UploadRequested += json => UploadManager.UploadText(json);
        }

        private void UpdateWorkflowsMenu()
        {
            tsddbWorkflows.DropDownItems.Clear();
            tsmiTrayWorkflows.DropDownItems.Clear();

            foreach (HotkeySettings hotkeySetting in Program.HotkeysConfig.Hotkeys)
            {
                if (hotkeySetting.TaskSettings.Job != HotkeyType.None && (!Program.Settings.WorkflowsOnlyShowEdited || !hotkeySetting.TaskSettings.IsUsingDefaultSettings))
                {
                    tsddbWorkflows.DropDownItems.Add(WorkflowMenuItem(hotkeySetting));
                    tsmiTrayWorkflows.DropDownItems.Add(WorkflowMenuItem(hotkeySetting));
                }
            }

            if (tsddbWorkflows.DropDownItems.Count > 0)
            {
                ToolStripSeparator tss = new ToolStripSeparator();
                tsddbWorkflows.DropDownItems.Add(tss);
            }

            ToolStripMenuItem tsmi = new ToolStripMenuItem(Resources.MainForm_UpdateWorkflowsMenu_You_can_add_workflows_from_hotkey_settings___);
            tsmi.Click += tsbHotkeySettings_Click;
            tsddbWorkflows.DropDownItems.Add(tsmi);

            tsmiTrayWorkflows.Visible = tsmiTrayWorkflows.DropDownItems.Count > 0;

            UpdateMainFormTip();
        }

        private void UpdateMainFormTip()
        {
            TaskManager.UpdateMainFormTip();

            StringBuilder sb = new StringBuilder(Resources.MainForm_UpdateMainFormTip_You_can_drag_and_drop_files_to_this_window_);

            List<HotkeySettings> hotkeys = Program.HotkeysConfig.Hotkeys.Where(x => x.HotkeyInfo.IsValidHotkey).ToList();

            if (hotkeys.Count > 0)
            {
                sb.AppendLine();
                sb.AppendLine();
                sb.AppendLine(Resources.MainForm_UpdateMainFormTip_Currently_configured_hotkeys_);

                foreach (HotkeySettings hotkey in hotkeys)
                {
                    sb.AppendFormat("{0}  |  {1}\r\n", hotkey.HotkeyInfo, hotkey.TaskSettings);
                }
            }

            lblMainFormTip.Text = sb.ToString().Trim();
        }

        private ToolStripMenuItem WorkflowMenuItem(HotkeySettings hotkeySetting)
        {
            ToolStripMenuItem tsmi = new ToolStripMenuItem(hotkeySetting.TaskSettings.ToString().Replace("&", "&&"));
            if (hotkeySetting.HotkeyInfo.IsValidHotkey)
            {
                tsmi.ShortcutKeyDisplayString = "  " + hotkeySetting.HotkeyInfo;
            }
            if (!hotkeySetting.TaskSettings.IsUsingDefaultSettings)
            {
                tsmi.Font = new Font(tsmi.Font, FontStyle.Bold);
            }
            tsmi.Click += (sender, e) => ExecuteJob(hotkeySetting.TaskSettings);
            return tsmi;
        }

        private void UpdateDestinationStates()
        {
            if (Program.UploadersConfig != null)
            {
                EnableDisableToolStripMenuItems<ImageDestination>(tsmiImageUploaders, tsmiTrayImageUploaders);
                EnableDisableToolStripMenuItems<FileDestination>(tsmiImageFileUploaders, tsmiTrayImageFileUploaders);
                EnableDisableToolStripMenuItems<TextDestination>(tsmiTextUploaders, tsmiTrayTextUploaders);
                EnableDisableToolStripMenuItems<FileDestination>(tsmiTextFileUploaders, tsmiTrayTextFileUploaders);
                EnableDisableToolStripMenuItems<FileDestination>(tsmiFileUploaders, tsmiTrayFileUploaders);
                EnableDisableToolStripMenuItems<UrlShortenerType>(tsmiURLShorteners, tsmiTrayURLShorteners);
                EnableDisableToolStripMenuItems<URLSharingServices>(tsmiURLSharingServices, tsmiTrayURLSharingServices);
            }
        }

        private void AddEnumItems<T>(Action<T> selectedEnum, params ToolStripDropDownItem[] parents)
        {
            string[] enums = Helpers.GetLocalizedEnumDescriptions<T>();

            foreach (ToolStripDropDownItem parent in parents)
            {
                for (int i = 0; i < enums.Length; i++)
                {
                    ToolStripMenuItem tsmi = new ToolStripMenuItem(enums[i]);

                    int index = i;

                    tsmi.Click += (sender, e) =>
                    {
                        foreach (ToolStripDropDownItem parent2 in parents)
                        {
                            for (int i2 = 0; i2 < enums.Length; i2++)
                            {
                                ToolStripMenuItem tsmi2 = (ToolStripMenuItem)parent2.DropDownItems[i2];
                                tsmi2.Checked = index == i2;
                            }
                        }

                        selectedEnum((T)Enum.ToObject(typeof(T), index));

                        UpdateUploaderMenuNames();
                    };

                    parent.DropDownItems.Add(tsmi);
                }
            }
        }

        public static void Uncheck(params ToolStripDropDownItem[] lists)
        {
            foreach (ToolStripDropDownItem parent in lists)
            {
                foreach (var dropDownItem in parent.DropDownItems)
                {
                    ((ToolStripMenuItem)dropDownItem).Checked = false;
                }
            }
        }

        /// <summary>
        /// Finds dropDowonItem corresponding to the enum value and checks it.
        /// </summary>
        /// <param name="value">Enum item</param>
        /// <param name="parents">DropDowns where enum-th item must be checked.</param>
        private static void SetEnumChecked(Enum value, params ToolStripDropDownItem[] parents)
        {
            if (value == null)
            {
                return;
            }

            int index = value.GetIndex();

            foreach (ToolStripDropDownItem parent in parents)
            {
                ((ToolStripMenuItem)parent.DropDownItems[index]).Checked = true;
            }
        }

        private void AddMultiEnumItems<T>(Action<T> selectedEnum, params ToolStripDropDownItem[] parents)
        {
            string[] enums = Helpers.GetLocalizedEnumDescriptions<T>().Skip(1).ToArray();

            foreach (ToolStripDropDownItem parent in parents)
            {
                for (int i = 0; i < enums.Length; i++)
                {
                    ToolStripMenuItem tsmi = new ToolStripMenuItem(enums[i]);

                    int index = i;

                    tsmi.Click += (sender, e) =>
                    {
                        foreach (ToolStripDropDownItem parent2 in parents)
                        {
                            ToolStripMenuItem tsmi2 = (ToolStripMenuItem)parent2.DropDownItems[index];
                            tsmi2.Checked = !tsmi2.Checked;
                        }

                        selectedEnum((T)Enum.ToObject(typeof(T), 1 << index));

                        UpdateUploaderMenuNames();
                    };

                    parent.DropDownItems.Add(tsmi);
                }
            }
        }

        private void SetMultiEnumChecked(Enum value, params ToolStripDropDownItem[] parents)
        {
            for (int i = 0; i < parents[0].DropDownItems.Count; i++)
            {
                foreach (ToolStripDropDownItem parent in parents)
                {
                    ToolStripMenuItem tsmi = (ToolStripMenuItem)parent.DropDownItems[i];
                    tsmi.Checked = value.HasFlag(1 << i);
                }
            }
        }

        private void EnableDisableToolStripMenuItems<T>(params ToolStripDropDownItem[] parents)
        {
            foreach (ToolStripDropDownItem parent in parents)
            {
                for (int i = 0; i < parent.DropDownItems.Count; i++)
                {
                    parent.DropDownItems[i].Enabled = Program.UploadersConfig.IsValid<T>(i);
                }
            }
        }

        private void UpdateControls()
        {
            cmsTaskInfo.SuspendLayout();

            tsmiStopUpload.Visible = tsmiOpen.Visible = tsmiCopy.Visible = tsmiShowErrors.Visible = tsmiShowResponse.Visible = tsmiShowQRCode.Visible = tsmiUploadSelectedFile.Visible =
                 tsmiEditSelectedFile.Visible = tsmiDeleteSelectedFile.Visible = tsmiShortenSelectedURL.Visible = tsmiShareSelectedURL.Visible = tsmiClearList.Visible = tssUploadInfo1.Visible = false;
            pbPreview.Reset();
            uim.RefreshSelectedItems();

            switch (Program.Settings.ImagePreview)
            {
                case ImagePreviewVisibility.Show:
                    scMain.Panel2Collapsed = false;
                    break;
                case ImagePreviewVisibility.Hide:
                    scMain.Panel2Collapsed = true;
                    break;
                case ImagePreviewVisibility.Automatic:
                    scMain.Panel2Collapsed = !uim.IsItemSelected || (!uim.SelectedItem.IsImageFile && !uim.SelectedItem.IsImageURL);
                    break;
            }

            if (uim.IsItemSelected)
            {
                // Open
                tsmiOpen.Visible = true;

                tsmiOpenURL.Enabled = uim.SelectedItem.IsURLExist;
                tsmiOpenShortenedURL.Enabled = uim.SelectedItem.IsShortenedURLExist;
                tsmiOpenThumbnailURL.Enabled = uim.SelectedItem.IsThumbnailURLExist;
                tsmiOpenDeletionURL.Enabled = uim.SelectedItem.IsDeletionURLExist;

                tsmiOpenFile.Enabled = uim.SelectedItem.IsFileExist;
                tsmiOpenFolder.Enabled = uim.SelectedItem.IsFileExist;
                tsmiOpenThumbnailFile.Enabled = uim.SelectedItem.IsThumbnailFileExist;

                if (GetCurrentTasks().Any(x => x.IsWorking))
                {
                    tsmiStopUpload.Visible = true;
                }
                else
                {
                    tsmiShowErrors.Visible = uim.SelectedItem.Info.Result.IsError;

                    // Copy
                    tsmiCopy.Visible = true;

                    tsmiCopyURL.Enabled = uim.SelectedItems.Any(x => x.IsURLExist);
                    tsmiCopyShortenedURL.Enabled = uim.SelectedItems.Any(x => x.IsShortenedURLExist);
                    tsmiCopyThumbnailURL.Enabled = uim.SelectedItems.Any(x => x.IsThumbnailURLExist);
                    tsmiCopyDeletionURL.Enabled = uim.SelectedItems.Any(x => x.IsDeletionURLExist);

                    tsmiCopyFile.Enabled = uim.SelectedItem.IsFileExist;
                    tsmiCopyImage.Enabled = uim.SelectedItem.IsImageFile;
                    tsmiCopyText.Enabled = uim.SelectedItem.IsTextFile;
                    tsmiCopyThumbnailFile.Enabled = uim.SelectedItem.IsThumbnailFileExist;
                    tsmiCopyThumbnailImage.Enabled = uim.SelectedItem.IsThumbnailFileExist;

                    tsmiCopyHTMLLink.Enabled = uim.SelectedItems.Any(x => x.IsURLExist);
                    tsmiCopyHTMLImage.Enabled = uim.SelectedItems.Any(x => x.IsImageURL);
                    tsmiCopyHTMLLinkedImage.Enabled = uim.SelectedItems.Any(x => x.IsImageURL && x.IsThumbnailURLExist);

                    tsmiCopyForumLink.Enabled = uim.SelectedItems.Any(x => x.IsURLExist);
                    tsmiCopyForumImage.Enabled = uim.SelectedItems.Any(x => x.IsImageURL && x.IsURLExist);
                    tsmiCopyForumLinkedImage.Enabled = uim.SelectedItems.Any(x => x.IsImageURL && x.IsThumbnailURLExist);

                    tsmiCopyFilePath.Enabled = uim.SelectedItems.Any(x => x.IsFilePathValid);
                    tsmiCopyFileName.Enabled = uim.SelectedItems.Any(x => x.IsFilePathValid);
                    tsmiCopyFileNameWithExtension.Enabled = uim.SelectedItems.Any(x => x.IsFilePathValid);
                    tsmiCopyFolder.Enabled = uim.SelectedItems.Any(x => x.IsFilePathValid);

                    CleanCustomClipboardFormats();

                    if (Program.Settings.ClipboardContentFormats != null && Program.Settings.ClipboardContentFormats.Count > 0)
                    {
                        tssCopy5.Visible = true;

                        foreach (ClipboardFormat cf in Program.Settings.ClipboardContentFormats)
                        {
                            ToolStripMenuItem tsmiClipboardFormat = new ToolStripMenuItem(cf.Description);
                            tsmiClipboardFormat.Tag = cf;
                            tsmiClipboardFormat.Click += tsmiClipboardFormat_Click;
                            tsmiCopy.DropDownItems.Add(tsmiClipboardFormat);
                        }
                    }

                    tsmiUploadSelectedFile.Visible = uim.SelectedItem.IsFileExist;
                    tsmiEditSelectedFile.Visible = uim.SelectedItem.IsImageFile;
                    tsmiDeleteSelectedFile.Visible = uim.SelectedItem.IsFileExist;
                    tsmiShortenSelectedURL.Visible = uim.SelectedItem.IsURLExist;
                    tsmiShareSelectedURL.Visible = uim.SelectedItem.IsURLExist;
                    tsmiShowQRCode.Visible = uim.SelectedItem.IsURLExist;
                    tsmiShowResponse.Visible = !string.IsNullOrEmpty(uim.SelectedItem.Info.Result.Response);
                }

                if (!scMain.Panel2Collapsed)
                {
                    if (uim.SelectedItem.IsImageFile)
                    {
                        pbPreview.LoadImageFromFileAsync(uim.SelectedItem.Info.FilePath);
                    }
                    else if (uim.SelectedItem.IsImageURL)
                    {
                        pbPreview.LoadImageFromURLAsync(uim.SelectedItem.Info.Result.URL);
                    }
                }
            }

            tsmiClearList.Visible = tssUploadInfo1.Visible = lvUploads.Items.Count > 0;

            cmsTaskInfo.ResumeLayout();
            Refresh();
        }

        private void CleanCustomClipboardFormats()
        {
            tssCopy5.Visible = false;

            int tssCopy5Index = tsmiCopy.DropDownItems.IndexOf(tssCopy5);

            while (tssCopy5Index < tsmiCopy.DropDownItems.Count - 1)
            {
                using (ToolStripItem tsi = tsmiCopy.DropDownItems[tsmiCopy.DropDownItems.Count - 1])
                {
                    tsmiCopy.DropDownItems.Remove(tsi);
                }
            }
        }

        private void LoadSettings()
        {
            niTray.Icon = ShareXResources.Icon;
            niTray.Visible = Program.Settings.ShowTray;

            if (Program.Settings.RecentLinksRemember)
            {
                TaskManager.RecentManager.UpdateItems(Program.Settings.RecentLinks);
            }

            bool isPositionChanged = false;

            if (Program.Settings.RememberMainFormPosition && !Program.Settings.MainFormPosition.IsEmpty &&
                CaptureHelpers.GetScreenBounds().IntersectsWith(new Rectangle(Program.Settings.MainFormPosition, Program.Settings.MainFormSize)))
            {
                StartPosition = FormStartPosition.Manual;
                Location = Program.Settings.MainFormPosition;
                isPositionChanged = true;
            }

            // Adjust the menu width to the items
            tsMain.Width = tsMain.PreferredSize.Width;

            // Calculate the required height to view the whole menu
            int height = Size.Height + tsMain.PreferredSize.Height - tsMain.Height;

            // Set the minimum size of the form to prevent menu items from hidding
            MinimumSize = new Size(MinimumSize.Width, height);

            if (Program.Settings.RememberMainFormSize && !Program.Settings.MainFormSize.IsEmpty)
            {
                Size = Program.Settings.MainFormSize;

                if (!isPositionChanged)
                {
                    StartPosition = FormStartPosition.Manual;
                    Rectangle activeScreen = CaptureHelpers.GetActiveScreenBounds();
                    Location = new Point(activeScreen.Width / 2 - Size.Width / 2, activeScreen.Height / 2 - Size.Height / 2);
                }
            }
            else
            {
                // Adjust the size to the minimum if not loaded
                Size = new Size(Size.Width, height);
            }

            switch (Program.Settings.ImagePreview)
            {
                case ImagePreviewVisibility.Show:
                    tsmiImagePreviewShow.Check();
                    break;
                case ImagePreviewVisibility.Hide:
                    tsmiImagePreviewHide.Check();
                    break;
                case ImagePreviewVisibility.Automatic:
                    tsmiImagePreviewAutomatic.Check();
                    break;
            }

            UpdateMainFormSettings();
            UpdateMenu();
            UpdateUploaderMenuNames();
            RegisterMenuClosing();

            AfterSettingsJobs();

            if (Program.Settings.PreviewSplitterDistance > 0)
            {
                scMain.SplitterDistance = Program.Settings.PreviewSplitterDistance;
            }

            UpdateControls();
            UpdateToggleHotkeyButton();

            TaskbarManager.Enabled = Program.Settings.TaskbarProgressEnabled;

#if !STEAM
            btnOpenSteam.Visible = btnHideSteam.Visible = Program.Settings.ShowSteamButtons;
#endif
        }

        private void RegisterMenuClosing()
        {
            foreach (ToolStripDropDownItem dropDownItem in new ToolStripDropDownItem[]
            {
                tsddbAfterCaptureTasks, tsddbAfterUploadTasks, tsmiImageUploaders, tsmiImageFileUploaders, tsmiTextUploaders, tsmiTextFileUploaders, tsmiFileUploaders,
                tsmiURLShorteners, tsmiURLSharingServices, tsmiTrayAfterCaptureTasks, tsmiTrayAfterUploadTasks, tsmiTrayImageUploaders, tsmiTrayImageFileUploaders,
                tsmiTrayTextUploaders, tsmiTrayTextFileUploaders, tsmiTrayFileUploaders, tsmiTrayURLShorteners, tsmiTrayURLSharingServices
            })
            {
                dropDownItem.DropDown.Closing += (sender, e) => e.Cancel = (e.CloseReason == ToolStripDropDownCloseReason.ItemClicked);
            }
        }

        private void AfterSettingsJobs()
        {
            HelpersOptions.CurrentProxy = Program.Settings.ProxySettings;
            HelpersOptions.UseAlternativeCopyImage = Program.Settings.UseAlternativeClipboardCopyImage;
            HelpersOptions.BrowserPath = Program.Settings.BrowserPath;
            TaskManager.RecentManager.MaxCount = Program.Settings.RecentLinksMaxCount;
        }

        public void UpdateMainFormSettings()
        {
            SetMultiEnumChecked(Program.DefaultTaskSettings.AfterCaptureJob, tsddbAfterCaptureTasks, tsmiTrayAfterCaptureTasks);
            SetMultiEnumChecked(Program.DefaultTaskSettings.AfterUploadJob, tsddbAfterUploadTasks, tsmiTrayAfterUploadTasks);
            SetEnumChecked(Program.DefaultTaskSettings.ImageDestination, tsmiImageUploaders, tsmiTrayImageUploaders);
            SetImageFileDestinationChecked(Program.DefaultTaskSettings.ImageDestination,
                Program.DefaultTaskSettings.ImageFileDestination, tsmiImageFileUploaders, tsmiTrayImageFileUploaders);
            SetEnumChecked(Program.DefaultTaskSettings.TextDestination, tsmiTextUploaders, tsmiTrayTextUploaders);
            SetTextFileDestinationChecked(Program.DefaultTaskSettings.TextDestination,
                Program.DefaultTaskSettings.TextFileDestination, tsmiTextFileUploaders, tsmiTrayTextFileUploaders);
            SetEnumChecked(Program.DefaultTaskSettings.FileDestination, tsmiFileUploaders, tsmiTrayFileUploaders);
            SetEnumChecked(Program.DefaultTaskSettings.URLShortenerDestination, tsmiURLShorteners, tsmiTrayURLShorteners);
            SetEnumChecked(Program.DefaultTaskSettings.URLSharingServiceDestination, tsmiURLSharingServices, tsmiTrayURLSharingServices);
        }

        /// <summary>
        /// Sets necessary menu item checked in Text Uploader->File Uploader.
        /// </summary>
        /// <param name="textDestination">Currently checked menu item inside Text Uploader</param>
        /// <param name="textFileDestination">Currently checked menu item inside Text Uploader->File Uploader</param>
        /// <param name="lists">List of menu items to be analysed for being checked.</param>
        public static void SetTextFileDestinationChecked(TextDestination textDestination,
            FileDestination textFileDestination, params ToolStripDropDownItem[] lists)
        {
            if (textDestination == TextDestination.FileUploader)
            {
                SetEnumChecked(textFileDestination, lists);
            }
            else
            {
                Uncheck(lists);
            }
        }

        /// <summary>
        /// Sets necessary menu item checked in Image Uploader->File Uploader.
        /// </summary>
        /// <param name="imageDestination">Currently checked menu item inside Image Uploader</param>
        /// <param name="imageFileDestination">Currently checked menu item inside Image Uploader->File Uploader</param>
        /// <param name="lists">List of menu items to be analysed for being checked.</param>
        public static void SetImageFileDestinationChecked(ImageDestination imageDestination,
            FileDestination imageFileDestination, params ToolStripDropDownItem[] lists)
        {
            if (imageDestination == ImageDestination.FileUploader)
            {
                SetEnumChecked(imageFileDestination, lists);
            }
            else
            {
                Uncheck(lists);
            }
        }

        private void UpdateUploaderMenuNames()
        {
            string imageUploader = Program.DefaultTaskSettings.ImageDestination == ImageDestination.FileUploader ?
                Program.DefaultTaskSettings.ImageFileDestination.GetLocalizedDescription() : Program.DefaultTaskSettings.ImageDestination.GetLocalizedDescription();
            tsmiImageUploaders.Text = tsmiTrayImageUploaders.Text = string.Format(Resources.TaskSettingsForm_UpdateUploaderMenuNames_Image_uploader___0_, imageUploader);

            string textUploader = Program.DefaultTaskSettings.TextDestination == TextDestination.FileUploader ?
                Program.DefaultTaskSettings.TextFileDestination.GetLocalizedDescription() : Program.DefaultTaskSettings.TextDestination.GetLocalizedDescription();
            tsmiTextUploaders.Text = tsmiTrayTextUploaders.Text = string.Format(Resources.TaskSettingsForm_UpdateUploaderMenuNames_Text_uploader___0_, textUploader);

            tsmiFileUploaders.Text = tsmiTrayFileUploaders.Text = string.Format(Resources.TaskSettingsForm_UpdateUploaderMenuNames_File_uploader___0_,
                Program.DefaultTaskSettings.FileDestination.GetLocalizedDescription());

            tsmiURLShorteners.Text = tsmiTrayURLShorteners.Text = string.Format(Resources.TaskSettingsForm_UpdateUploaderMenuNames_URL_shortener___0_,
                Program.DefaultTaskSettings.URLShortenerDestination.GetLocalizedDescription());

            tsmiURLSharingServices.Text = tsmiTrayURLSharingServices.Text = string.Format(Resources.TaskSettingsForm_UpdateUploaderMenuNames_URL_sharing_service___0_,
                Program.DefaultTaskSettings.URLSharingServiceDestination.GetLocalizedDescription());
        }

        private void ConfigureAutoUpdate()
        {
#if RELEASE
            lock (updateTimerLock)
            {
                if (Program.Settings.AutoCheckUpdate)
                {
                    if (updateTimer == null)
                    {
                        updateTimer = new System.Threading.Timer(state => CheckUpdate(), null, 0, 1000 * 60 * 60);
                    }
                }
                else if (updateTimer != null)
                {
                    updateTimer.Dispose();
                    updateTimer = null;
                }
            }
#endif
        }

        private void CheckUpdate()
        {
            if (!UpdateMessageBox.DontShow && !UpdateMessageBox.IsOpen)
            {
                UpdateChecker updateChecker = TaskHelpers.CheckUpdate();
                UpdateMessageBox.Start(updateChecker, firstUpdateCheck);
                firstUpdateCheck = false;
            }
        }

        private void ForceClose()
        {
            forceClose = true;
            Close();
        }

        public void UseCommandLineArgs(List<CLICommand> commands)
        {
            TaskSettings taskSettings = FindCLITask(commands);

            foreach (CLICommand command in commands)
            {
                DebugHelper.WriteLine("CommandLine: " + command.Command);

                if (command.IsCommand && (CheckCLIHotkey(command) || CheckCLIWorkflow(command)))
                {
                    continue;
                }

                if (URLHelpers.IsValidURLRegex(command.Command))
                {
                    UploadManager.DownloadAndUploadFile(command.Command, taskSettings);
                }
                else
                {
                    UploadManager.UploadFile(command.Command, taskSettings);
                }
            }
        }

        private bool CheckCLIHotkey(CLICommand command)
        {
            foreach (HotkeyType job in Helpers.GetEnums<HotkeyType>())
            {
                if (command.CheckCommand(job.ToString()))
                {
                    ExecuteJob(job);
                    return true;
                }
            }

            return false;
        }

        private bool CheckCLIWorkflow(CLICommand command)
        {
            if (Program.HotkeysConfig != null && command.CheckCommand("workflow") && !string.IsNullOrEmpty(command.Parameter))
            {
                foreach (HotkeySettings hotkeySetting in Program.HotkeysConfig.Hotkeys)
                {
                    if (hotkeySetting.TaskSettings.Job != HotkeyType.None)
                    {
                        if (command.Parameter == hotkeySetting.TaskSettings.ToString())
                        {
                            ExecuteJob(hotkeySetting.TaskSettings);
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private TaskSettings FindCLITask(List<CLICommand> commands)
        {
            if (Program.HotkeysConfig != null)
            {
                CLICommand command = commands.FirstOrDefault(x => x.CheckCommand("task") && !string.IsNullOrEmpty(x.Parameter));

                if (command != null)
                {
                    foreach (HotkeySettings hotkeySetting in Program.HotkeysConfig.Hotkeys)
                    {
                        if (command.Parameter == hotkeySetting.TaskSettings.ToString())
                        {
                            return hotkeySetting.TaskSettings;
                        }
                    }
                }
            }

            return null;
        }

        private WorkerTask[] GetCurrentTasks()
        {
            if (lvUploads.SelectedItems.Count > 0)
            {
                return lvUploads.SelectedItems.Cast<ListViewItem>().Select(x => x.Tag as WorkerTask).Where(x => x != null).ToArray();
            }

            return null;
        }

        private TaskInfo GetCurrentUploadInfo()
        {
            TaskInfo info = null;
            WorkerTask[] tasks = GetCurrentTasks();

            if (tasks != null && tasks.Length > 0)
            {
                info = tasks[0].Info;
            }

            return info;
        }

        private void RemoveSelectedItems()
        {
            lvUploads.SelectedItems.Cast<ListViewItem>().Select(x => x.Tag as WorkerTask).Where(x => x != null && !x.IsWorking).ForEach(TaskManager.Remove);
        }

        private void RemoveAllItems()
        {
            lvUploads.Items.Cast<ListViewItem>().Select(x => x.Tag as WorkerTask).Where(x => x != null && !x.IsWorking).ForEach(TaskManager.Remove);
        }

        private void UpdateMenu()
        {
            if (Program.Settings.ShowMenu)
            {
                tsmiHideMenu.Text = Resources.MainForm_UpdateMenu_Hide_menu;
            }
            else
            {
                tsmiHideMenu.Text = Resources.MainForm_UpdateMenu_Show_menu;
            }

            tsMain.Visible = lblSplitter.Visible = Program.Settings.ShowMenu;
            Refresh();
        }

        public void UpdateToggleHotkeyButton()
        {
            if (Program.Settings.DisableHotkeys)
            {
                tsmiTrayToggleHotkeys.Text = Resources.MainForm_UpdateToggleHotkeyButton_Enable_hotkeys;
                tsmiTrayToggleHotkeys.Image = Resources.keyboard__plus;
            }
            else
            {
                tsmiTrayToggleHotkeys.Text = Resources.MainForm_UpdateToggleHotkeyButton_Disable_hotkeys;
                tsmiTrayToggleHotkeys.Image = Resources.keyboard__minus;
            }
        }

        #region Form events

        protected override void SetVisibleCore(bool value)
        {
            if (value && !IsHandleCreated && (Program.IsSilentRun || Program.Settings.SilentRun) && Program.Settings.ShowTray)
            {
                CreateHandle();
                value = false;
            }

            base.SetVisibleCore(value);
        }

        private void MainForm_Shown(object sender, EventArgs e)
        {
            AfterShownJobs();
        }

        private void MainForm_VisibleChanged(object sender, EventArgs e)
        {
            if (Visible)
            {
                tsmiDonate.StartAnimation();
            }
            else
            {
                tsmiDonate.StopAnimation();
            }
        }

        private void MainForm_Resize(object sender, EventArgs e)
        {
            Refresh();
        }

        private void MainForm_LocationChanged(object sender, EventArgs e)
        {
            if (IsReady && WindowState == FormWindowState.Normal)
            {
                Program.Settings.MainFormPosition = Location;
            }
        }

        private void MainForm_SizeChanged(object sender, EventArgs e)
        {
            if (IsReady && WindowState == FormWindowState.Normal)
            {
                Program.Settings.MainFormSize = Size;
            }
        }

        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            if (e.CloseReason == CloseReason.UserClosing && Program.Settings.ShowTray && !forceClose)
            {
                e.Cancel = true;
                Hide();
                Program.SaveSettingsAsync();
            }
        }

        private void MainForm_FormClosed(object sender, FormClosedEventArgs e)
        {
            TaskManager.StopAllTasks();
        }

        private void MainForm_DragEnter(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop, false) ||
                e.Data.GetDataPresent(DataFormats.Bitmap, false) ||
                e.Data.GetDataPresent(DataFormats.Text, false))
            {
                e.Effect = DragDropEffects.Copy;
            }
            else
            {
                e.Effect = DragDropEffects.None;
            }
        }

        private void MainForm_DragDrop(object sender, DragEventArgs e)
        {
            UploadManager.DragDropUpload(e.Data);
        }

        private void tsbFileUpload_Click(object sender, EventArgs e)
        {
            UploadManager.UploadFile();
        }

        private void tsmiUploadFolder_Click(object sender, EventArgs e)
        {
            UploadManager.UploadFolder();
        }

        private void tsbClipboardUpload_Click(object sender, EventArgs e)
        {
            UploadManager.ClipboardUploadMainWindow();
        }

        private void tsmiUploadURL_Click(object sender, EventArgs e)
        {
            UploadManager.UploadURL();
        }

        private void tsbDragDropUpload_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenDropWindow();
        }

        private void tsmiColorPicker_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenColorPicker();
        }

        private void tsmiScreenColorPicker_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenScreenColorPicker();
        }

        private void tsmiImageEditor_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenImageEditor();
        }

        private void tsmiImageEffects_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenImageEffects();
        }

        private void tsmiHashCheck_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenHashCheck();
        }

        private void tsmiDNSChanger_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenDNSChanger();
        }

        private void tsmiQRCode_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenQRCode();
        }

        private void tsmiRuler_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenRuler();
        }

        private void tsmiAutomate_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenAutomate();
        }

        private void tsmiIndexFolder_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenIndexFolder();
        }

        private void tsmiImageCombiner_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenImageCombiner();
        }

        private void tsmiVideoThumbnailer_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenVideoThumbnailer();
        }

        private void tsmiFTPClient_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenFTPClient();
        }

        private void tsmiIRCClient_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenIRCClient();
        }

        private void tsmiTweetMessage_Click(object sender, EventArgs e)
        {
            TaskHelpers.TweetMessage();
        }

        private void tsmiMonitorTest_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenMonitorTest();
        }

        private void tsddbDestinations_DropDownOpened(object sender, EventArgs e)
        {
            UpdateDestinationStates();
        }

        private void tsmiShowDebugLog_Click(object sender, EventArgs e)
        {
            new DebugForm(DebugHelper.Logger).Show();
        }

        private void tsmiTestImageUpload_Click(object sender, EventArgs e)
        {
            UploadManager.UploadImage(ShareXResources.Logo);
        }

        private void tsmiTestTextUpload_Click(object sender, EventArgs e)
        {
            UploadManager.UploadText(Resources.MainForm_tsmiTestTextUpload_Click_Text_upload_test);
        }

        private void tsmiTestFileUpload_Click(object sender, EventArgs e)
        {
            UploadManager.UploadImage(ShareXResources.Logo, ImageDestination.FileUploader, Program.DefaultTaskSettings.FileDestination);
        }

        private void tsmiTestURLShortener_Click(object sender, EventArgs e)
        {
            UploadManager.ShortenURL(Links.URL_WEBSITE);
        }

        private void tsmiTestURLSharing_Click(object sender, EventArgs e)
        {
            UploadManager.ShareURL(Links.URL_WEBSITE);
        }

        private void tsmiScreenRecordingFFmpeg_Click(object sender, EventArgs e)
        {
            TaskHelpers.StartScreenRecording(ScreenRecordOutput.FFmpeg, ScreenRecordStartMethod.Region);
        }

        private void tsmiScreenRecordingGIF_Click(object sender, EventArgs e)
        {
            TaskHelpers.StartScreenRecording(ScreenRecordOutput.GIF, ScreenRecordStartMethod.Region);
        }

        private void tsmiScrollingCapture_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenScrollingCapture();
        }

        private void tsmiAutoCapture_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenAutoCapture();
        }

        private void tsmiWebpageCapture_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenWebpageCapture();
        }

        private void tsbApplicationSettings_Click(object sender, EventArgs e)
        {
            using (ApplicationSettingsForm settingsForm = new ApplicationSettingsForm())
            {
                settingsForm.ShowDialog();
            }

            AfterSettingsJobs();
            UpdateWorkflowsMenu();
            Program.Settings.SaveAsync(Program.ApplicationConfigFilePath);
            Program.ConfigureUploadersConfigWatcher();
            ConfigureAutoUpdate();
        }

        private void tsbTaskSettings_Click(object sender, EventArgs e)
        {
            using (TaskSettingsForm taskSettingsForm = new TaskSettingsForm(Program.DefaultTaskSettings, true))
            {
                taskSettingsForm.ShowDialog();
            }

            Program.Settings.SaveAsync(Program.ApplicationConfigFilePath);
        }

        private void tsbHotkeySettings_Click(object sender, EventArgs e)
        {
            if (Program.HotkeysConfig == null)
            {
                Program.HotkeySettingsResetEvent.WaitOne();
            }

            using (HotkeySettingsForm hotkeySettingsForm = new HotkeySettingsForm())
            {
                hotkeySettingsForm.ShowDialog();
            }

            UpdateWorkflowsMenu();
            Program.HotkeysConfig.SaveAsync(Program.HotkeysConfigFilePath);
        }

        private void tsmiTrayToggleHotkeys_Click(object sender, EventArgs e)
        {
            TaskHelpers.ToggleHotkeys();
        }

        private void tsbDestinationSettings_Click(object sender, EventArgs e)
        {
            if (Program.UploadersConfig == null)
            {
                Program.UploaderSettingsResetEvent.WaitOne();
            }

            using (UploadersConfigForm uploadersConfigForm = new UploadersConfigForm(Program.UploadersConfig))
            {
                uploadersConfigForm.ShowDialog();
            }

            Program.UploadersConfigSaveAsync();
        }

        private void tsbScreenshotsFolder_Click(object sender, EventArgs e)
        {
            TaskHelpers.OpenScreenshotsFolder();
        }

        private void tsbHistory_Click(object sender, EventArgs e)
        {
            HistoryForm historyForm = new HistoryForm(Program.HistoryFilePath);
            Program.Settings.HistoryWindowState.AutoHandleFormState(historyForm);
            historyForm.Show();
        }

        private void tsbImageHistory_Click(object sender, EventArgs e)
        {
            ImageHistoryForm imageHistoryForm = new ImageHistoryForm(Program.HistoryFilePath, Program.Settings.ImageHistoryViewMode,
                Program.Settings.ImageHistoryThumbnailSize, Program.Settings.ImageHistoryMaxItemCount);
            Program.Settings.ImageHistoryWindowState.AutoHandleFormState(imageHistoryForm);
            imageHistoryForm.FormClosed += imageHistoryForm_FormClosed;
            imageHistoryForm.Show();
        }

        private void imageHistoryForm_FormClosed(object sender, FormClosedEventArgs e)
        {
            ImageHistoryForm imageHistoryForm = sender as ImageHistoryForm;
            Program.Settings.ImageHistoryViewMode = imageHistoryForm.ViewMode;
            Program.Settings.ImageHistoryThumbnailSize = imageHistoryForm.ThumbnailSize;
            Program.Settings.ImageHistoryMaxItemCount = imageHistoryForm.MaxItemCount;
        }

        private void tsbAbout_Click(object sender, EventArgs e)
        {
            using (AboutForm aboutForm = new AboutForm())
            {
                aboutForm.ShowDialog();
            }
        }

        private void tsbDonate_Click(object sender, EventArgs e)
        {
#if STEAM
            URLHelpers.OpenURL(Links.URL_STEAM_DONATE);
#else
            URLHelpers.OpenURL(Links.URL_DONATE);
#endif
        }

        private void lblDragAndDropTip_MouseUp(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                lvUploads.Focus();
            }
            else if (e.Button == MouseButtons.Right)
            {
                UpdateControls();
                cmsTaskInfo.Show((Control)sender, e.X + 1, e.Y + 1);
            }
        }

        private void lvUploads_SelectedIndexChanged(object sender, EventArgs e)
        {
            UpdateControls();
        }

        private void lvUploads_MouseUp(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Right)
            {
                UpdateControls();
                cmsTaskInfo.Show(lvUploads, e.X + 1, e.Y + 1);
            }
        }

        private void lvUploads_MouseDoubleClick(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                uim.TryOpen();
            }
        }

        private void scMain_SplitterMoved(object sender, SplitterEventArgs e)
        {
            Program.Settings.PreviewSplitterDistance = scMain.SplitterDistance;
        }

        private void lvUploads_KeyDown(object sender, KeyEventArgs e)
        {
            switch (e.KeyData)
            {
                default:
                    return;
                case Keys.Enter:
                    uim.TryOpen();
                    break;
                case Keys.Control | Keys.Enter:
                    uim.OpenFile();
                    break;
                case Keys.Control | Keys.X:
                    uim.TryCopy();
                    RemoveSelectedItems();
                    break;
                case Keys.Control | Keys.C:
                    uim.TryCopy();
                    break;
                case Keys.Control | Keys.Shift | Keys.C:
                    uim.CopyFilePath();
                    break;
                case Keys.Control | Keys.V:
                    UploadManager.ClipboardUploadMainWindow();
                    break;
                case Keys.Delete:
                    RemoveSelectedItems();
                    break;
                case Keys.Shift | Keys.Delete:
                    uim.DeleteFiles();
                    RemoveSelectedItems();
                    break;
            }

            e.Handled = true;
        }

        private void btnOpenSteam_MouseClick(object sender, MouseEventArgs e)
        {
            URLHelpers.OpenURL(Links.URL_STEAM);
        }

        private void btnHideSteam_MouseClick(object sender, MouseEventArgs e)
        {
            Program.Settings.ShowSteamButtons = false;
            btnOpenSteam.Visible = btnHideSteam.Visible = false;
        }

        #region Tray events

        private void timerTraySingleClick_Tick(object sender, EventArgs e)
        {
            timerTraySingleClick.Stop();
            ExecuteJob(Program.Settings.TrayLeftClickAction);
        }

        private void niTray_MouseClick(object sender, MouseEventArgs e)
        {
            switch (e.Button)
            {
                case MouseButtons.Left:
                    timerTraySingleClick.Interval = (int)(SystemInformation.DoubleClickTime * 1.1);
                    timerTraySingleClick.Start();
                    break;
                case MouseButtons.Middle:
                    ExecuteJob(Program.Settings.TrayMiddleClickAction);
                    break;
            }
        }

        private void niTray_MouseDoubleClick(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                timerTraySingleClick.Stop();
                this.ShowActivate();
            }
        }

        private void niTray_BalloonTipClicked(object sender, EventArgs e)
        {
            string url = niTray.Tag as string;

            if (!string.IsNullOrEmpty(url))
            {
                URLHelpers.OpenURL(url);
            }
        }

        private void tsmiTrayShow_Click(object sender, EventArgs e)
        {
            this.ShowActivate();
        }

        private void tsmiTrayExit_Click(object sender, EventArgs e)
        {
            ForceClose();
        }

        #endregion Tray events

        #region UploadInfoMenu events

        private void tsmiShowErrors_Click(object sender, EventArgs e)
        {
            uim.ShowErrors();
        }

        private void tsmiStopUpload_Click(object sender, EventArgs e)
        {
            if (lvUploads.SelectedItems.Count > 0)
            {
                foreach (WorkerTask task in GetCurrentTasks())
                {
                    task.Stop();
                }
            }
        }

        private void tsmiOpenURL_Click(object sender, EventArgs e)
        {
            uim.OpenURL();
        }

        private void tsmiOpenShortenedURL_Click(object sender, EventArgs e)
        {
            uim.OpenShortenedURL();
        }

        private void tsmiOpenThumbnailURL_Click(object sender, EventArgs e)
        {
            uim.OpenThumbnailURL();
        }

        private void tsmiOpenDeletionURL_Click(object sender, EventArgs e)
        {
            uim.OpenDeletionURL();
        }

        private void tsmiOpenFile_Click(object sender, EventArgs e)
        {
            uim.OpenFile();
        }

        private void tsmiOpenThumbnailFile_Click(object sender, EventArgs e)
        {
            uim.OpenThumbnailFile();
        }

        private void tsmiOpenFolder_Click(object sender, EventArgs e)
        {
            uim.OpenFolder();
        }

        private void tsmiCopyURL_Click(object sender, EventArgs e)
        {
            uim.CopyURL();
        }

        private void tsmiCopyShortenedURL_Click(object sender, EventArgs e)
        {
            uim.CopyShortenedURL();
        }

        private void tsmiCopyThumbnailURL_Click(object sender, EventArgs e)
        {
            uim.CopyThumbnailURL();
        }

        private void tsmiCopyDeletionURL_Click(object sender, EventArgs e)
        {
            uim.CopyDeletionURL();
        }

        private void tsmiCopyFile_Click(object sender, EventArgs e)
        {
            uim.CopyFile();
        }

        private void tsmiCopyImage_Click(object sender, EventArgs e)
        {
            uim.CopyImage();
        }

        private void tsmiCopyText_Click(object sender, EventArgs e)
        {
            uim.CopyText();
        }

        private void tsmiCopyThumbnailFile_Click(object sender, EventArgs e)
        {
            uim.CopyThumbnailFile();
        }

        private void tsmiCopyThumbnailImage_Click(object sender, EventArgs e)
        {
            uim.CopyThumbnailImage();
        }

        private void tsmiCopyHTMLLink_Click(object sender, EventArgs e)
        {
            uim.CopyHTMLLink();
        }

        private void tsmiCopyHTMLImage_Click(object sender, EventArgs e)
        {
            uim.CopyHTMLImage();
        }

        private void tsmiCopyHTMLLinkedImage_Click(object sender, EventArgs e)
        {
            uim.CopyHTMLLinkedImage();
        }

        private void tsmiCopyForumLink_Click(object sender, EventArgs e)
        {
            uim.CopyForumLink();
        }

        private void tsmiCopyForumImage_Click(object sender, EventArgs e)
        {
            uim.CopyForumImage();
        }

        private void tsmiCopyForumLinkedImage_Click(object sender, EventArgs e)
        {
            uim.CopyForumLinkedImage();
        }

        private void tsmiCopyFilePath_Click(object sender, EventArgs e)
        {
            uim.CopyFilePath();
        }

        private void tsmiCopyFileName_Click(object sender, EventArgs e)
        {
            uim.CopyFileName();
        }

        private void tsmiCopyFileNameWithExtension_Click(object sender, EventArgs e)
        {
            uim.CopyFileNameWithExtension();
        }

        private void tsmiCopyFolder_Click(object sender, EventArgs e)
        {
            uim.CopyFolder();
        }

        private void tsmiClipboardFormat_Click(object sender, EventArgs e)
        {
            ToolStripMenuItem tsmiClipboardFormat = sender as ToolStripMenuItem;
            ClipboardFormat cf = tsmiClipboardFormat.Tag as ClipboardFormat;
            uim.CopyCustomFormat(cf.Format);
        }

        private void tsmiUploadSelectedFile_Click(object sender, EventArgs e)
        {
            uim.Upload();
        }

        private void tsmiDeleteSelectedFile_Click(object sender, EventArgs e)
        {
            if (MessageBox.Show(Resources.MainForm_tsmiDeleteSelectedFile_Click_Do_you_really_want_to_delete_this_file_,
                "ShareX - " + Resources.MainForm_tsmiDeleteSelectedFile_Click_File_delete_confirmation, MessageBoxButtons.YesNo) == DialogResult.Yes)
            {
                uim.DeleteFiles();
                RemoveSelectedItems();
            }
        }

        private void tsmiEditSelectedFile_Click(object sender, EventArgs e)
        {
            uim.EditImage();
        }

        private void tsmiShowQRCode_Click(object sender, EventArgs e)
        {
            uim.ShowQRCode();
        }

        private void tsmiShowResponse_Click(object sender, EventArgs e)
        {
            uim.ShowResponse();
        }

        private void tsmiClearList_Click(object sender, EventArgs e)
        {
            RemoveAllItems();
        }

        private void tsmiHideMenu_Click(object sender, EventArgs e)
        {
            Program.Settings.ShowMenu = !Program.Settings.ShowMenu;
            UpdateMenu();
        }

        private void tsmiImagePreviewShow_Click(object sender, EventArgs e)
        {
            Program.Settings.ImagePreview = ImagePreviewVisibility.Show;
            tsmiImagePreviewShow.Check();
            UpdateControls();
        }

        private void tsmiImagePreviewHide_Click(object sender, EventArgs e)
        {
            Program.Settings.ImagePreview = ImagePreviewVisibility.Hide;
            tsmiImagePreviewHide.Check();
            UpdateControls();
        }

        private void tsmiImagePreviewAutomatic_Click(object sender, EventArgs e)
        {
            Program.Settings.ImagePreview = ImagePreviewVisibility.Automatic;
            tsmiImagePreviewAutomatic.Check();
            UpdateControls();
        }

        #endregion UploadInfoMenu events

        #endregion Form events

        #region Hotkey/Capture codes and form events

        private delegate Image ScreenCaptureDelegate();

        private enum LastRegionCaptureType { Surface, Light, Transparent, Annotate }

        private LastRegionCaptureType lastRegionCaptureType = LastRegionCaptureType.Surface;

        private void InitHotkeys()
        {
            TaskEx.Run(() =>
            {
                if (Program.HotkeysConfig == null)
                {
                    Program.HotkeySettingsResetEvent.WaitOne();
                }
            },
            () =>
            {
                Program.HotkeyManager = new HotkeyManager(this, Program.HotkeysConfig.Hotkeys, !Program.NoHotkeys);
                Program.HotkeyManager.HotkeyTrigger += HandleHotkeys;
                DebugHelper.WriteLine("HotkeyManager started");

                Program.WatchFolderManager = new WatchFolderManager();
                DebugHelper.WriteLine("WatchFolderManager started");

                UpdateWorkflowsMenu();
            });
        }

        private void HandleHotkeys(HotkeySettings hotkeySetting)
        {
            DebugHelper.WriteLine("Hotkey triggered: " + hotkeySetting);

            if (hotkeySetting.TaskSettings.Job != HotkeyType.None)
            {
                if (hotkeySetting.TaskSettings.Job == HotkeyType.DisableHotkeys)
                {
                    TaskHelpers.ToggleHotkeys();
                }

                if (!Program.Settings.DisableHotkeys)
                {
                    ExecuteJob(hotkeySetting.TaskSettings);
                }
            }
        }

        private void ExecuteJob(HotkeyType job)
        {
            ExecuteJob(Program.DefaultTaskSettings, job);
        }

        private void ExecuteJob(TaskSettings taskSettings)
        {
            ExecuteJob(taskSettings, taskSettings.Job);
        }

        private void ExecuteJob(TaskSettings taskSettings, HotkeyType job)
        {
            TaskSettings safeTaskSettings = TaskSettings.GetSafeTaskSettings(taskSettings);

            switch (job)
            {
                // Upload
                case HotkeyType.FileUpload:
                    UploadManager.UploadFile(safeTaskSettings);
                    break;
                case HotkeyType.FolderUpload:
                    UploadManager.UploadFolder(safeTaskSettings);
                    break;
                case HotkeyType.ClipboardUpload:
                    UploadManager.ClipboardUpload(safeTaskSettings);
                    break;
                case HotkeyType.ClipboardUploadWithContentViewer:
                    UploadManager.ClipboardUploadWithContentViewer(safeTaskSettings);
                    break;
                case HotkeyType.UploadURL:
                    UploadManager.UploadURL(safeTaskSettings);
                    break;
                case HotkeyType.DragDropUpload:
                    TaskHelpers.OpenDropWindow(safeTaskSettings);
                    break;
                case HotkeyType.StopUploads:
                    TaskManager.StopAllTasks();
                    break;
                // Screen capture
                case HotkeyType.PrintScreen:
                    CaptureScreenshot(CaptureType.Screen, safeTaskSettings, false);
                    break;
                case HotkeyType.ActiveWindow:
                    CaptureScreenshot(CaptureType.ActiveWindow, safeTaskSettings, false);
                    break;
                case HotkeyType.ActiveMonitor:
                    CaptureScreenshot(CaptureType.ActiveMonitor, safeTaskSettings, false);
                    break;
                case HotkeyType.RectangleRegion:
                    CaptureScreenshot(CaptureType.Rectangle, safeTaskSettings, false);
                    break;
                case HotkeyType.WindowRectangle:
                    CaptureScreenshot(CaptureType.RectangleWindow, safeTaskSettings, false);
                    break;
                case HotkeyType.RectangleAnnotate:
                    CaptureRectangleAnnotate(safeTaskSettings, false);
                    break;
                case HotkeyType.RectangleLight:
                    CaptureRectangleLight(safeTaskSettings, false);
                    break;
                case HotkeyType.RectangleTransparent:
                    CaptureRectangleTransparent(safeTaskSettings, false);
                    break;
                case HotkeyType.PolygonRegion:
                    CaptureScreenshot(CaptureType.Polygon, safeTaskSettings, false);
                    break;
                case HotkeyType.FreeHandRegion:
                    CaptureScreenshot(CaptureType.Freehand, safeTaskSettings, false);
                    break;
                case HotkeyType.CustomRegion:
                    CaptureScreenshot(CaptureType.CustomRegion, safeTaskSettings, false);
                    break;
                case HotkeyType.LastRegion:
                    CaptureScreenshot(CaptureType.LastRegion, safeTaskSettings, false);
                    break;
                case HotkeyType.ScrollingCapture:
                    TaskHelpers.OpenScrollingCapture(safeTaskSettings, true);
                    break;
                case HotkeyType.CaptureWebpage:
                    TaskHelpers.OpenWebpageCapture(safeTaskSettings);
                    break;
                case HotkeyType.AutoCapture:
                    TaskHelpers.OpenAutoCapture();
                    break;
                case HotkeyType.StartAutoCapture:
                    TaskHelpers.StartAutoCapture();
                    break;
                // Screen record
                case HotkeyType.ScreenRecorder:
                    TaskHelpers.StartScreenRecording(ScreenRecordOutput.FFmpeg, ScreenRecordStartMethod.Region, safeTaskSettings);
                    break;
                case HotkeyType.ScreenRecorderActiveWindow:
                    TaskHelpers.StartScreenRecording(ScreenRecordOutput.FFmpeg, ScreenRecordStartMethod.ActiveWindow, safeTaskSettings);
                    break;
                case HotkeyType.StartScreenRecorder:
                    TaskHelpers.StartScreenRecording(ScreenRecordOutput.FFmpeg, ScreenRecordStartMethod.LastRegion, safeTaskSettings);
                    break;
                case HotkeyType.ScreenRecorderGIF:
                    TaskHelpers.StartScreenRecording(ScreenRecordOutput.GIF, ScreenRecordStartMethod.Region, safeTaskSettings);
                    break;
                case HotkeyType.ScreenRecorderGIFActiveWindow:
                    TaskHelpers.StartScreenRecording(ScreenRecordOutput.GIF, ScreenRecordStartMethod.ActiveWindow, safeTaskSettings);
                    break;
                case HotkeyType.StartScreenRecorderGIF:
                    TaskHelpers.StartScreenRecording(ScreenRecordOutput.GIF, ScreenRecordStartMethod.LastRegion, safeTaskSettings);
                    break;
                // Tools
                case HotkeyType.ColorPicker:
                    TaskHelpers.OpenColorPicker();
                    break;
                case HotkeyType.ScreenColorPicker:
                    TaskHelpers.OpenScreenColorPicker(safeTaskSettings);
                    break;
                case HotkeyType.ImageEditor:
                    TaskHelpers.OpenImageEditor();
                    break;
                case HotkeyType.ImageEffects:
                    TaskHelpers.OpenImageEffects();
                    break;
                case HotkeyType.HashCheck:
                    TaskHelpers.OpenHashCheck();
                    break;
                case HotkeyType.IRCClient:
                    TaskHelpers.OpenIRCClient(safeTaskSettings);
                    break;
                case HotkeyType.DNSChanger:
                    TaskHelpers.OpenDNSChanger();
                    break;
                case HotkeyType.QRCode:
                    TaskHelpers.OpenQRCode();
                    break;
                case HotkeyType.Ruler:
                    TaskHelpers.OpenRuler();
                    break;
                case HotkeyType.Automate:
                    TaskHelpers.StartAutomate();
                    break;
                case HotkeyType.IndexFolder:
                    TaskHelpers.OpenIndexFolder();
                    break;
                case HotkeyType.ImageCombiner:
                    TaskHelpers.OpenImageCombiner(safeTaskSettings);
                    break;
                case HotkeyType.VideoThumbnailer:
                    TaskHelpers.OpenVideoThumbnailer(safeTaskSettings);
                    break;
                case HotkeyType.FTPClient:
                    TaskHelpers.OpenFTPClient();
                    break;
                case HotkeyType.TweetMessage:
                    TaskHelpers.TweetMessage();
                    break;
                case HotkeyType.MonitorTest:
                    TaskHelpers.OpenMonitorTest();
                    break;
                // Other
                case HotkeyType.OpenScreenshotsFolder:
                    TaskHelpers.OpenScreenshotsFolder();
                    break;
            }
        }

        public void CaptureScreenshot(CaptureType captureType, TaskSettings taskSettings = null, bool autoHideForm = true)
        {
            if (taskSettings == null) taskSettings = TaskSettings.GetDefaultTaskSettings();

            switch (captureType)
            {
                case CaptureType.Screen:
                    DoCapture(Screenshot.CaptureFullscreen, CaptureType.Screen, taskSettings, autoHideForm);
                    break;
                case CaptureType.ActiveWindow:
                    CaptureActiveWindow(taskSettings, autoHideForm);
                    break;
                case CaptureType.ActiveMonitor:
                    DoCapture(Screenshot.CaptureActiveMonitor, CaptureType.ActiveMonitor, taskSettings, autoHideForm);
                    break;
                case CaptureType.Rectangle:
                case CaptureType.RectangleWindow:
                case CaptureType.Polygon:
                case CaptureType.Freehand:
                    CaptureRegion(captureType, taskSettings, autoHideForm);
                    break;
                case CaptureType.CustomRegion:
                    CaptureCustomRegion(taskSettings, autoHideForm);
                    break;
                case CaptureType.LastRegion:
                    CaptureLastRegion(taskSettings, autoHideForm);
                    break;
            }
        }

        private void DoCapture(ScreenCaptureDelegate capture, CaptureType captureType, TaskSettings taskSettings = null, bool autoHideForm = true)
        {
            if (taskSettings == null) taskSettings = TaskSettings.GetDefaultTaskSettings();

            if (taskSettings.CaptureSettings.IsDelayScreenshot && taskSettings.CaptureSettings.DelayScreenshot > 0)
            {
                TaskEx.Run(() =>
                {
                    int sleep = (int)(taskSettings.CaptureSettings.DelayScreenshot * 1000);
                    Thread.Sleep(sleep);
                },
                () =>
                {
                    DoCaptureWork(capture, captureType, taskSettings, autoHideForm);
                });
            }
            else
            {
                DoCaptureWork(capture, captureType, taskSettings, autoHideForm);
            }
        }

        private void DoCaptureWork(ScreenCaptureDelegate capture, CaptureType captureType, TaskSettings taskSettings, bool autoHideForm = true)
        {
            if (autoHideForm)
            {
                Hide();
                Thread.Sleep(250);
            }

            Image img = null;

            try
            {
                Screenshot.CaptureCursor = taskSettings.CaptureSettings.ShowCursor;
                Screenshot.CaptureShadow = taskSettings.CaptureSettings.CaptureShadow;
                Screenshot.ShadowOffset = taskSettings.CaptureSettings.CaptureShadowOffset;
                Screenshot.CaptureClientArea = taskSettings.CaptureSettings.CaptureClientArea;
                Screenshot.AutoHideTaskbar = taskSettings.CaptureSettings.CaptureAutoHideTaskbar;

                img = capture();
            }
            catch (Exception ex)
            {
                DebugHelper.WriteException(ex);
            }
            finally
            {
                if (autoHideForm)
                {
                    this.ShowActivate();
                }

                AfterCapture(img, captureType, taskSettings);
            }
        }

        private void AfterCapture(Image img, CaptureType captureType, TaskSettings taskSettings)
        {
            if (img != null)
            {
                if (taskSettings.GeneralSettings.PlaySoundAfterCapture)
                {
                    TaskHelpers.PlayCaptureSound(taskSettings);
                }

                if (taskSettings.ImageSettings.ImageEffectOnlyRegionCapture && !IsRegionCapture(captureType))
                {
                    taskSettings.AfterCaptureJob = taskSettings.AfterCaptureJob.Remove(AfterCaptureTasks.AddImageEffects);
                }

                if (TaskHelpers.ShowAfterCaptureForm(taskSettings, img))
                {
                    UploadManager.RunImageTask(img, taskSettings);
                }
            }
        }

        private bool IsRegionCapture(CaptureType captureType)
        {
            return captureType.HasFlagAny(CaptureType.RectangleWindow, CaptureType.Rectangle, CaptureType.Polygon, CaptureType.Freehand, CaptureType.LastRegion);
        }

        private void CaptureActiveWindow(TaskSettings taskSettings, bool autoHideForm = true)
        {
            DoCapture(() =>
            {
                Image img;
                string activeWindowTitle = NativeMethods.GetForegroundWindowText();
                string activeProcessName = null;

                using (Process process = NativeMethods.GetForegroundWindowProcess())
                {
                    if (process != null)
                    {
                        activeProcessName = process.ProcessName;
                    }
                }

                if (taskSettings.CaptureSettings.CaptureTransparent && !taskSettings.CaptureSettings.CaptureClientArea)
                {
                    img = Screenshot.CaptureActiveWindowTransparent();
                }
                else
                {
                    img = Screenshot.CaptureActiveWindow();
                }

                img.Tag = new ImageTag
                {
                    ActiveWindowTitle = activeWindowTitle,
                    ActiveProcessName = activeProcessName
                };

                return img;
            }, CaptureType.ActiveWindow, taskSettings, autoHideForm);
        }

        private void CaptureCustomRegion(TaskSettings taskSettings, bool autoHideForm)
        {
            DoCapture(() =>
            {
                Rectangle regionBounds = taskSettings.CaptureSettings.CaptureCustomRegion;
                Image img = Screenshot.CaptureRectangle(regionBounds);

                return img;
            }, CaptureType.CustomRegion, taskSettings, autoHideForm);
        }

        private void CaptureWindow(IntPtr handle, TaskSettings taskSettings = null, bool autoHideForm = true)
        {
            if (taskSettings == null) taskSettings = TaskSettings.GetDefaultTaskSettings();

            autoHideForm = autoHideForm && handle != Handle;

            DoCapture(() =>
            {
                if (NativeMethods.IsIconic(handle))
                {
                    NativeMethods.RestoreWindow(handle);
                }

                NativeMethods.SetForegroundWindow(handle);
                Thread.Sleep(250);

                if (taskSettings.CaptureSettings.CaptureTransparent && !taskSettings.CaptureSettings.CaptureClientArea)
                {
                    return Screenshot.CaptureWindowTransparent(handle);
                }

                return Screenshot.CaptureWindow(handle);
            }, CaptureType.Window, taskSettings, autoHideForm);
        }

        private void CaptureRegion(CaptureType captureType, TaskSettings taskSettings, bool autoHideForm = true)
        {
            Surface surface;

            switch (captureType)
            {
                default:
                case CaptureType.Rectangle:
                    surface = new RectangleRegion();
                    break;
                case CaptureType.RectangleWindow:
                    RectangleRegion rectangleRegion = new RectangleRegion();
                    rectangleRegion.AreaManager.WindowCaptureMode = true;
                    rectangleRegion.AreaManager.IncludeControls = true;
                    surface = rectangleRegion;
                    break;
                case CaptureType.Polygon:
                    surface = new PolygonRegion();
                    break;
                case CaptureType.Freehand:
                    surface = new FreeHandRegion();
                    break;
            }

            DoCapture(() =>
            {
                Image img = null;
                Image screenshot = Screenshot.CaptureFullscreen();

                try
                {
                    surface.Config = taskSettings.CaptureSettingsReference.SurfaceOptions;
                    surface.SurfaceImage = screenshot;
                    surface.Prepare();
                    surface.ShowDialog();

                    if (surface.Result == SurfaceResult.Region)
                    {
                        using (screenshot)
                        {
                            img = surface.GetRegionImage();
                        }
                    }
                    else if (surface.Result == SurfaceResult.Fullscreen)
                    {
                        img = screenshot;
                    }
                    else if (surface.Result == SurfaceResult.Monitor)
                    {
                        Screen[] screens = Screen.AllScreens;

                        if (surface.MonitorIndex < screens.Length)
                        {
                            Screen screen = screens[surface.MonitorIndex];
                            Rectangle screenRect = CaptureHelpers.ScreenToClient(screen.Bounds);

                            using (screenshot)
                            {
                                img = ImageHelpers.CropImage(screenshot, screenRect);
                            }
                        }
                    }
                    else if (surface.Result == SurfaceResult.ActiveMonitor)
                    {
                        Rectangle activeScreenRect = CaptureHelpers.GetActiveScreenBounds0Based();

                        using (screenshot)
                        {
                            img = ImageHelpers.CropImage(screenshot, activeScreenRect);
                        }
                    }

                    if (img != null)
                    {
                        lastRegionCaptureType = LastRegionCaptureType.Surface;
                    }
                }
                finally
                {
                    surface.Dispose();
                }

                return img;
            }, captureType, taskSettings, autoHideForm);
        }

        private void CaptureRectangleAnnotate(TaskSettings taskSettings = null, bool autoHideForm = true)
        {
            if (taskSettings == null) taskSettings = TaskSettings.GetDefaultTaskSettings();

            DoCapture(() =>
            {
                Image img = null;

                using (RectangleAnnotate rectangleAnnotate = new RectangleAnnotate(taskSettings.CaptureSettingsReference.RectangleAnnotateOptions))
                {
                    if (rectangleAnnotate.ShowDialog() == DialogResult.OK)
                    {
                        img = rectangleAnnotate.GetAreaImage();

                        if (img != null)
                        {
                            lastRegionCaptureType = LastRegionCaptureType.Annotate;
                        }
                    }
                }

                return img;
            }, CaptureType.Rectangle, taskSettings, autoHideForm);
        }

        private void CaptureRectangleLight(TaskSettings taskSettings = null, bool autoHideForm = true)
        {
            if (taskSettings == null) taskSettings = TaskSettings.GetDefaultTaskSettings();

            DoCapture(() =>
            {
                Image img = null;

                using (RectangleLight rectangleLight = new RectangleLight())
                {
                    if (rectangleLight.ShowDialog() == DialogResult.OK)
                    {
                        img = rectangleLight.GetAreaImage();

                        if (img != null)
                        {
                            lastRegionCaptureType = LastRegionCaptureType.Light;
                        }
                    }
                }

                return img;
            }, CaptureType.Rectangle, taskSettings, autoHideForm);
        }

        private void CaptureRectangleTransparent(TaskSettings taskSettings = null, bool autoHideForm = true)
        {
            if (taskSettings == null) taskSettings = TaskSettings.GetDefaultTaskSettings();

            DoCapture(() =>
            {
                Image img = null;

                using (RectangleTransparent rectangleTransparent = new RectangleTransparent())
                {
                    if (rectangleTransparent.ShowDialog() == DialogResult.OK)
                    {
                        img = rectangleTransparent.GetAreaImage();

                        if (img != null)
                        {
                            lastRegionCaptureType = LastRegionCaptureType.Transparent;
                        }
                    }
                }

                return img;
            }, CaptureType.Rectangle, taskSettings, autoHideForm);
        }

        private void CaptureLastRegion(TaskSettings taskSettings, bool autoHideForm = true)
        {
            switch (lastRegionCaptureType)
            {
                case LastRegionCaptureType.Surface:
                    if (Surface.LastRegionFillPath != null)
                    {
                        DoCapture(() =>
                        {
                            using (Image screenshot = Screenshot.CaptureFullscreen())
                            {
                                return ShapeCaptureHelpers.GetRegionImage(screenshot, Surface.LastRegionFillPath, Surface.LastRegionDrawPath, taskSettings.CaptureSettings.SurfaceOptions);
                            }
                        }, CaptureType.LastRegion, taskSettings, autoHideForm);
                    }
                    else
                    {
                        CaptureRegion(CaptureType.Rectangle, taskSettings, autoHideForm);
                    }
                    break;
                case LastRegionCaptureType.Light:
                    if (!RectangleLight.LastSelectionRectangle0Based.IsEmpty)
                    {
                        DoCapture(() =>
                        {
                            using (Image screenshot = Screenshot.CaptureFullscreen())
                            {
                                return ImageHelpers.CropImage(screenshot, RectangleLight.LastSelectionRectangle0Based);
                            }
                        }, CaptureType.LastRegion, taskSettings, autoHideForm);
                    }
                    else
                    {
                        CaptureRectangleLight(taskSettings, autoHideForm);
                    }
                    break;
                case LastRegionCaptureType.Transparent:
                    if (!RectangleTransparent.LastSelectionRectangle0Based.IsEmpty)
                    {
                        DoCapture(() =>
                        {
                            using (Image screenshot = Screenshot.CaptureFullscreen())
                            {
                                return ImageHelpers.CropImage(screenshot, RectangleTransparent.LastSelectionRectangle0Based);
                            }
                        }, CaptureType.LastRegion, taskSettings, autoHideForm);
                    }
                    else
                    {
                        CaptureRectangleTransparent(taskSettings, autoHideForm);
                    }
                    break;
                case LastRegionCaptureType.Annotate:
                    if (!RectangleAnnotate.LastSelectionRectangle0Based.IsEmpty)
                    {
                        DoCapture(() =>
                        {
                            using (Image screenshot = Screenshot.CaptureFullscreen())
                            {
                                return ImageHelpers.CropImage(screenshot, RectangleAnnotate.LastSelectionRectangle0Based);
                            }
                        }, CaptureType.LastRegion, taskSettings, autoHideForm);
                    }
                    else
                    {
                        CaptureRectangleAnnotate(taskSettings, autoHideForm);
                    }
                    break;
            }
        }

        private void PrepareCaptureMenuAsync(ToolStripMenuItem tsmiWindow, EventHandler handlerWindow, ToolStripMenuItem tsmiMonitor, EventHandler handlerMonitor)
        {
            tsmiWindow.DropDownItems.Clear();

            WindowsList windowsList = new WindowsList();
            List<WindowInfo> windows = null;

            TaskEx.Run(() =>
            {
                windows = windowsList.GetVisibleWindowsList();
            },
            () =>
            {
                if (windows != null)
                {
                    foreach (WindowInfo window in windows)
                    {
                        try
                        {
                            string title = window.Text.Truncate(50, "...");
                            ToolStripItem tsi = tsmiWindow.DropDownItems.Add(title);
                            tsi.Tag = window;
                            tsi.Click += handlerWindow;

                            using (Icon icon = window.Icon)
                            {
                                if (icon != null && icon.Width > 0 && icon.Height > 0)
                                {
                                    tsi.Image = icon.ToBitmap();
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            DebugHelper.WriteException(e);
                        }
                    }
                }

                tsmiMonitor.DropDownItems.Clear();

                Screen[] screens = Screen.AllScreens;

                for (int i = 0; i < screens.Length; i++)
                {
                    Screen screen = screens[i];
                    string text = string.Format("{0}. {1}x{2}", i + 1, screen.Bounds.Width, screen.Bounds.Height);
                    ToolStripItem tsi = tsmiMonitor.DropDownItems.Add(text);
                    tsi.Tag = screen.Bounds;
                    tsi.Click += handlerMonitor;
                }

                tsmiWindow.Invalidate();
                tsmiMonitor.Invalidate();
            });
        }

        #region Menu events

        private void tsmiFullscreen_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Screen);
        }

        private void tsddbCapture_DropDownOpening(object sender, EventArgs e)
        {
            PrepareCaptureMenuAsync(tsmiWindow, tsmiWindowItems_Click, tsmiMonitor, tsmiMonitorItems_Click);
        }

        private void tsmiWindowItems_Click(object sender, EventArgs e)
        {
            ToolStripItem tsi = (ToolStripItem)sender;
            WindowInfo wi = tsi.Tag as WindowInfo;
            if (wi != null)
            {
                CaptureWindow(wi.Handle);
            }
        }

        private void tsmiMonitorItems_Click(object sender, EventArgs e)
        {
            ToolStripItem tsi = (ToolStripItem)sender;
            Rectangle rectangle = (Rectangle)tsi.Tag;
            if (!rectangle.IsEmpty)
            {
                DoCapture(() => Screenshot.CaptureRectangle(rectangle), CaptureType.Monitor);
            }
        }

        private void tsmiRectangle_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Rectangle);
        }

        private void tsmiWindowRectangle_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.RectangleWindow);
        }

        private void tsmiRectangleAnnotate_Click(object sender, EventArgs e)
        {
            CaptureRectangleAnnotate();
        }

        private void tsmiRectangleLight_Click(object sender, EventArgs e)
        {
            CaptureRectangleLight();
        }

        private void tsmiRectangleTransparent_Click(object sender, EventArgs e)
        {
            CaptureRectangleTransparent();
        }

        private void tsmiPolygon_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Polygon);
        }

        private void tsmiFreeHand_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Freehand);
        }

        private void tsmiLastRegion_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.LastRegion);
        }

        #endregion Menu events

        #region Tray events

        private void cmsTray_Opened(object sender, EventArgs e)
        {
            if (Program.Settings.TrayAutoExpandCaptureMenu)
            {
                tsmiTrayCapture.Select();
                tsmiTrayCapture.ShowDropDown();
            }
        }

        private void tsmiTrayFullscreen_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Screen, null, false);
        }

        private void tsmiCapture_DropDownOpening(object sender, EventArgs e)
        {
            PrepareCaptureMenuAsync(tsmiTrayWindow, tsmiTrayWindowItems_Click, tsmiTrayMonitor, tsmiTrayMonitorItems_Click);
        }

        private void tsmiTrayWindowItems_Click(object sender, EventArgs e)
        {
            ToolStripItem tsi = (ToolStripItem)sender;
            WindowInfo wi = tsi.Tag as WindowInfo;
            if (wi != null)
            {
                CaptureWindow(wi.Handle, null, false);
            }
        }

        private void tsmiTrayMonitorItems_Click(object sender, EventArgs e)
        {
            ToolStripItem tsi = (ToolStripItem)sender;
            Rectangle rectangle = (Rectangle)tsi.Tag;
            if (!rectangle.IsEmpty)
            {
                DoCapture(() => Screenshot.CaptureRectangle(rectangle), CaptureType.Monitor, null, false);
            }
        }

        private void tsmiTrayRectangle_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Rectangle, null, false);
        }

        private void tsmiTrayWindowRectangle_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.RectangleWindow, null, false);
        }

        private void tsmiTrayRectangleAnnotate_Click(object sender, EventArgs e)
        {
            CaptureRectangleAnnotate(null, false);
        }

        private void tsmiTrayRectangleLight_Click(object sender, EventArgs e)
        {
            CaptureRectangleLight(null, false);
        }

        private void tsmiTrayRectangleTransparent_Click(object sender, EventArgs e)
        {
            CaptureRectangleTransparent(null, false);
        }

        private void tsmiTrayPolygon_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Polygon, null, false);
        }

        private void tsmiTrayFreeHand_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.Freehand, null, false);
        }

        private void tsmiTrayLastRegion_Click(object sender, EventArgs e)
        {
            CaptureScreenshot(CaptureType.LastRegion, null, false);
        }

        #endregion Tray events

        #endregion Hotkey/Capture codes and form events
    }
}