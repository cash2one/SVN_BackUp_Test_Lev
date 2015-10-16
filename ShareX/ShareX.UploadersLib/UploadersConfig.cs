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

using CG.Web.MegaApiClient;
using ShareX.HelpersLib;
using ShareX.UploadersLib.FileUploaders;
using ShareX.UploadersLib.HelperClasses;
using ShareX.UploadersLib.ImageUploaders;
using ShareX.UploadersLib.TextUploaders;
using System;
using System.Collections.Generic;

namespace ShareX.UploadersLib
{
    public class UploadersConfig : SettingsBase<UploadersConfig>
    {
        #region Image uploaders

        // Imgur

        public AccountType ImgurAccountType = AccountType.Anonymous;
        public bool ImgurDirectLink = true;
        public ImgurThumbnailType ImgurThumbnailType = ImgurThumbnailType.Large_Thumbnail;
        public bool ImgurUseGIFV = true;
        public OAuth2Info ImgurOAuth2Info = null;
        public bool ImgurUploadSelectedAlbum = false;
        public ImgurAlbumData ImgurSelectedAlbum = null;
        public List<ImgurAlbumData> ImgurAlbumList = null;

        // ImageShack

        public ImageShackOptions ImageShackSettings = new ImageShackOptions();

        // TinyPic

        public AccountType TinyPicAccountType = AccountType.Anonymous;
        public string TinyPicRegistrationCode = string.Empty;
        public string TinyPicUsername = string.Empty;
        public string TinyPicPassword = string.Empty;
        public bool TinyPicRememberUserPass = false;

        // Flickr

        public FlickrAuthInfo FlickrAuthInfo = new FlickrAuthInfo();
        public FlickrSettings FlickrSettings = new FlickrSettings();

        // Photobucket

        public OAuthInfo PhotobucketOAuthInfo = null;
        public PhotobucketAccountInfo PhotobucketAccountInfo = null;

        // Picasa

        public OAuth2Info PicasaOAuth2Info = null;
        public string PicasaAlbumID = string.Empty;

        // Chevereto

        public string CheveretoWebsite = string.Empty;
        public string CheveretoAPIKey = string.Empty;
        public bool CheveretoDirectURL = true;

        #endregion Image uploaders

        #region Text uploaders

        // Pastebin

        public PastebinSettings PastebinSettings = new PastebinSettings();

        // Paste.ee

        public string Paste_eeUserAPIKey = "public";

        // Gist

        public bool GistAnonymousLogin = true;
        public OAuth2Info GistOAuth2Info = null;
        public bool GistPublishPublic = false;

        // uPaste

        public string UpasteUserKey = string.Empty;
        public bool UpasteIsPublic = false;

        // Hastebin

        public string HastebinCustomDomain = "http://hastebin.com";
        public string HastebinSyntaxHighlighting = "hs";

        // OneTimeSecret

        public string OneTimeSecretAPIKey = string.Empty;
        public string OneTimeSecretAPIUsername = string.Empty;

        #endregion Text uploaders

        #region File uploaders

        // Dropbox

        public OAuth2Info DropboxOAuth2Info = null;
        public DropboxAccountInfo DropboxAccountInfo = null;
        public string DropboxUploadPath = "Public/ShareX/%y/%mo";
        public bool DropboxAutoCreateShareableLink = false;
        public DropboxURLType DropboxURLType = DropboxURLType.Default;

        // OneDrive

        public OAuth2Info OneDriveOAuth2Info = null;
        public OneDriveFileInfo OneDriveSelectedFolder = OneDrive.RootFolder;
        public bool OneDriveAutoCreateShareableLink = true;

        // Copy

        public OAuthInfo CopyOAuthInfo = null;
        public CopyAccountInfo CopyAccountInfo = null;
        public string CopyUploadPath = "ShareX/%y/%mo";
        public CopyURLType CopyURLType = CopyURLType.Shortened;

        // Google Drive

        public OAuth2Info GoogleDriveOAuth2Info = null;
        public bool GoogleDriveIsPublic = true;
        public bool GoogleDriveUseFolder = false;
        public string GoogleDriveFolderID = string.Empty;

        // SendSpace

        public AccountType SendSpaceAccountType = AccountType.Anonymous;
        public string SendSpaceUsername = string.Empty;
        public string SendSpacePassword = string.Empty;

        // Minus

        public OAuth2Info MinusOAuth2Info = null;
        public MinusOptions MinusConfig = new MinusOptions();

        // Box

        public OAuth2Info BoxOAuth2Info = null;
        public BoxFileEntry BoxSelectedFolder = Box.RootFolder;
        public bool BoxShare = true;

        // Ge.tt

        public Ge_ttLogin Ge_ttLogin = null;

        // Localhostr

        public string LocalhostrEmail = string.Empty;
        public string LocalhostrPassword = string.Empty;
        public bool LocalhostrDirectURL = true;

        // FTP Server

        public List<FTPAccount> FTPAccountList = new List<FTPAccount>();
        public int FTPSelectedImage = 0;
        public int FTPSelectedText = 0;
        public int FTPSelectedFile = 0;

        // Shared Folder

        public List<LocalhostAccount> LocalhostAccountList = new List<LocalhostAccount>();
        public int LocalhostSelectedImages = 0;
        public int LocalhostSelectedText = 0;
        public int LocalhostSelectedFiles = 0;

        // Email

        public string EmailSmtpServer = "smtp.gmail.com";
        public int EmailSmtpPort = 587;
        public string EmailFrom = "...@gmail.com";
        public string EmailPassword = string.Empty;
        public bool EmailRememberLastTo = true;
        public bool EmailConfirmSend = true;
        public string EmailLastTo = string.Empty;
        public string EmailDefaultSubject = "Sending email from ShareX";
        public string EmailDefaultBody = "Screenshot is attached.";

        // Jira

        public string JiraHost = "http://";
        public string JiraIssuePrefix = "PROJECT-";
        public OAuthInfo JiraOAuthInfo = null;

        // Mega

        public MegaApiClient.AuthInfos MegaAuthInfos = null;
        public string MegaParentNodeId = null;

        // Amazon S3

        public AmazonS3Settings AmazonS3Settings = new AmazonS3Settings()
        {
            ObjectPrefix = "ShareX/%y/%mo",
            UseReducedRedundancyStorage = true
        };

        // ownCloud

        public string OwnCloudHost = "";
        public string OwnCloudUsername = "";
        public string OwnCloudPassword = "";
        public string OwnCloudPath = "/";
        public bool OwnCloudCreateShare = true;
        public bool OwnCloudDirectLink = false;
        public bool OwnCloudIgnoreInvalidCert = false;
        public bool OwnCloud81Compatibility = false;

        // MediaFire

        public string MediaFireUsername = "";
        public string MediaFirePassword = "";
        public string MediaFirePath = "";
        public bool MediaFireUseLongLink = false;

        // Pushbullet

        public PushbulletSettings PushbulletSettings = new PushbulletSettings();

        // Up1

        public string Up1Host = "https://up1.ca";
        public string Up1Key = "c61540b5ceecd05092799f936e27755f";

        // Lambda

        public LambdaSettings LambdaSettings = new LambdaSettings();

        // Pomf

        public PomfUploader PomfUploader = Pomf.DefaultUploader;

        // Seafile

        public string SeafileAPIURL = "";
        public string SeafileAuthToken = "";
        public string SeafileRepoID = "";
        public string SeafilePath = "/";
        public bool SeafileIsLibraryEncrypted = false;
        public string SeafileEncryptedLibraryPassword = "";
        public bool SeafileCreateShareableURL = true;
        public bool SeafileIgnoreInvalidCert = false;
        public int SeafileShareDaysToExpire = 0;
        public string SeafileSharePassword = "";
        public string SeafileAccInfoEmail = "";
        public string SeafileAccInfoUsage = "";

        #endregion File uploaders

        #region URL shorteners

        // bit.ly

        public OAuth2Info BitlyOAuth2Info = null;
        public string BitlyDomain = string.Empty;

        // Google URL Shortener

        public AccountType GoogleURLShortenerAccountType = AccountType.Anonymous;
        public OAuth2Info GoogleURLShortenerOAuth2Info = null;

        // yourls.org

        public string YourlsAPIURL = "http://yoursite.com/yourls-api.php";
        public string YourlsSignature = string.Empty;
        public string YourlsUsername = string.Empty;
        public string YourlsPassword = string.Empty;

        // adf.ly
        public string AdFlyAPIKEY = String.Empty;
        public string AdFlyAPIUID = String.Empty;

        // coinurl.com
        public string CoinURLUUID = string.Empty;

        // polr
        public string PolrAPIHostname = string.Empty;
        public string PolrAPIKey = string.Empty;

        #endregion URL shorteners

        #region URL sharing services

        // Twitter

        public List<OAuthInfo> TwitterOAuthInfoList = new List<OAuthInfo>();
        public int TwitterSelectedAccount = 0;
        public bool TwitterSkipMessageBox = false;
        public string TwitterDefaultMessage = string.Empty;

        #endregion URL sharing services

        #region Custom Uploaders

        public List<CustomUploaderItem> CustomUploadersList = new List<CustomUploaderItem>();
        public int CustomImageUploaderSelected = 0;
        public int CustomTextUploaderSelected = 0;
        public int CustomFileUploaderSelected = 0;
        public int CustomURLShortenerSelected = 0;

        #endregion Custom Uploaders

        #region Helper Methods

        public bool IsValid<T>(int index)
        {
            Enum destination = (Enum)Enum.ToObject(typeof(T), index);

            if (destination is ImageDestination)
            {
                return IsValid((ImageDestination)destination);
            }

            if (destination is TextDestination)
            {
                return IsValid((TextDestination)destination);
            }

            if (destination is FileDestination)
            {
                return IsValid((FileDestination)destination);
            }

            if (destination is UrlShortenerType)
            {
                return IsValid((UrlShortenerType)destination);
            }

            if (destination is URLSharingServices)
            {
                return IsValid((URLSharingServices)destination);
            }

            return true;
        }

        public bool IsValid(ImageDestination destination)
        {
            switch (destination)
            {
                case ImageDestination.Imgur:
                    return ImgurAccountType == AccountType.Anonymous || OAuth2Info.CheckOAuth(ImgurOAuth2Info);
                case ImageDestination.ImageShack:
                    return ImageShackSettings != null && !string.IsNullOrEmpty(ImageShackSettings.Auth_token);
                case ImageDestination.TinyPic:
                    return TinyPicAccountType == AccountType.Anonymous || !string.IsNullOrEmpty(TinyPicRegistrationCode);
                case ImageDestination.Flickr:
                    return !string.IsNullOrEmpty(FlickrAuthInfo.Token);
                case ImageDestination.Photobucket:
                    return PhotobucketAccountInfo != null && OAuthInfo.CheckOAuth(PhotobucketOAuthInfo);
                case ImageDestination.Picasa:
                    return OAuth2Info.CheckOAuth(PicasaOAuth2Info);
                case ImageDestination.Twitter:
                    return TwitterOAuthInfoList != null && TwitterOAuthInfoList.IsValidIndex(TwitterSelectedAccount) && OAuthInfo.CheckOAuth(TwitterOAuthInfoList[TwitterSelectedAccount]);
                case ImageDestination.Chevereto:
                    return !string.IsNullOrEmpty(CheveretoWebsite) && !string.IsNullOrEmpty(CheveretoAPIKey);
                case ImageDestination.CustomImageUploader:
                    return CustomUploadersList != null && CustomUploadersList.IsValidIndex(CustomImageUploaderSelected);
            }

            return true;
        }

        public bool IsValid(TextDestination destination)
        {
            switch (destination)
            {
                case TextDestination.CustomTextUploader:
                    return CustomUploadersList != null && CustomUploadersList.IsValidIndex(CustomTextUploaderSelected);
            }

            return true;
        }

        public bool IsValid(FileDestination destination)
        {
            switch (destination)
            {
                case FileDestination.Dropbox:
                    return OAuth2Info.CheckOAuth(DropboxOAuth2Info);
                case FileDestination.FTP:
                    return FTPAccountList != null && FTPAccountList.IsValidIndex(FTPSelectedFile);
                case FileDestination.OneDrive:
                    return OAuth2Info.CheckOAuth(OneDriveOAuth2Info);
                case FileDestination.GoogleDrive:
                    return OAuth2Info.CheckOAuth(GoogleDriveOAuth2Info);
                case FileDestination.Copy:
                    return OAuthInfo.CheckOAuth(CopyOAuthInfo);
                case FileDestination.Box:
                    return OAuth2Info.CheckOAuth(BoxOAuth2Info);
                case FileDestination.Mega:
                    return MegaAuthInfos != null && MegaAuthInfos.Email != null && MegaAuthInfos.Hash != null && MegaAuthInfos.PasswordAesKey != null;
                case FileDestination.AmazonS3:
                    return AmazonS3Settings != null && !string.IsNullOrEmpty(AmazonS3Settings.AccessKeyID) && !string.IsNullOrEmpty(AmazonS3Settings.SecretAccessKey) &&
                        !string.IsNullOrEmpty(AmazonS3Settings.Bucket) && AmazonS3.GetCurrentRegion(AmazonS3Settings) != AmazonS3.UnknownEndpoint;
                case FileDestination.OwnCloud:
                    return !string.IsNullOrEmpty(OwnCloudHost) && !string.IsNullOrEmpty(OwnCloudUsername) && !string.IsNullOrEmpty(OwnCloudPassword);
                case FileDestination.MediaFire:
                    return !string.IsNullOrEmpty(MediaFireUsername) && !string.IsNullOrEmpty(MediaFirePassword);
                case FileDestination.Pushbullet:
                    return PushbulletSettings != null && !string.IsNullOrEmpty(PushbulletSettings.UserAPIKey) && PushbulletSettings.DeviceList != null &&
                        PushbulletSettings.DeviceList.IsValidIndex(PushbulletSettings.SelectedDevice);
                case FileDestination.SendSpace:
                    return SendSpaceAccountType == AccountType.Anonymous || (!string.IsNullOrEmpty(SendSpaceUsername) && !string.IsNullOrEmpty(SendSpacePassword));
                case FileDestination.Minus:
                    return MinusConfig != null && MinusConfig.MinusUser != null;
                case FileDestination.Ge_tt:
                    return Ge_ttLogin != null && !string.IsNullOrEmpty(Ge_ttLogin.AccessToken);
                case FileDestination.Localhostr:
                    return !string.IsNullOrEmpty(LocalhostrEmail) && !string.IsNullOrEmpty(LocalhostrPassword);
                case FileDestination.Jira:
                    return OAuthInfo.CheckOAuth(JiraOAuthInfo);
                case FileDestination.Lambda:
                    return LambdaSettings != null && !string.IsNullOrEmpty(LambdaSettings.UserAPIKey);
                case FileDestination.Pomf:
                    return PomfUploader != null && !string.IsNullOrEmpty(PomfUploader.UploadURL);
                case FileDestination.Seafile:
                    return !string.IsNullOrEmpty(SeafileAPIURL) && !string.IsNullOrEmpty(SeafileAuthToken) && !string.IsNullOrEmpty(SeafileRepoID);
                case FileDestination.SharedFolder:
                    return LocalhostAccountList != null && LocalhostAccountList.IsValidIndex(LocalhostSelectedFiles);
                case FileDestination.Email:
                    return !string.IsNullOrEmpty(EmailSmtpServer) && EmailSmtpPort > 0 && !string.IsNullOrEmpty(EmailFrom) && !string.IsNullOrEmpty(EmailPassword);
                case FileDestination.CustomFileUploader:
                    return CustomUploadersList != null && CustomUploadersList.IsValidIndex(CustomFileUploaderSelected);
            }

            return true;
        }

        public bool IsValid(UrlShortenerType destination)
        {
            switch (destination)
            {
                case UrlShortenerType.BITLY:
                    return OAuth2Info.CheckOAuth(BitlyOAuth2Info);
                case UrlShortenerType.Google:
                    return GoogleURLShortenerAccountType == AccountType.Anonymous || OAuth2Info.CheckOAuth(GoogleURLShortenerOAuth2Info);
                case UrlShortenerType.YOURLS:
                    return !string.IsNullOrEmpty(YourlsAPIURL) && (!string.IsNullOrEmpty(YourlsSignature) || (!string.IsNullOrEmpty(YourlsUsername) && !string.IsNullOrEmpty(YourlsPassword)));
                case UrlShortenerType.AdFly:
                    return !string.IsNullOrEmpty(AdFlyAPIKEY) && !string.IsNullOrEmpty(AdFlyAPIUID);
                case UrlShortenerType.CoinURL:
                    return !string.IsNullOrEmpty(CoinURLUUID);
                case UrlShortenerType.Polr:
                    return !string.IsNullOrEmpty(PolrAPIKey);
                case UrlShortenerType.CustomURLShortener:
                    return CustomUploadersList != null && CustomUploadersList.IsValidIndex(CustomURLShortenerSelected);
            }

            return true;
        }

        public bool IsValid(URLSharingServices destination)
        {
            switch (destination)
            {
                case URLSharingServices.Email:
                    return !string.IsNullOrEmpty(EmailSmtpServer) && EmailSmtpPort > 0 && !string.IsNullOrEmpty(EmailFrom) && !string.IsNullOrEmpty(EmailPassword);
                case URLSharingServices.Twitter:
                    return TwitterOAuthInfoList != null && TwitterOAuthInfoList.IsValidIndex(TwitterSelectedAccount) && OAuthInfo.CheckOAuth(TwitterOAuthInfoList[TwitterSelectedAccount]);
                case URLSharingServices.Pushbullet:
                    return PushbulletSettings != null && !string.IsNullOrEmpty(PushbulletSettings.UserAPIKey) && PushbulletSettings.DeviceList != null &&
                        PushbulletSettings.DeviceList.IsValidIndex(PushbulletSettings.SelectedDevice);
            }

            return true;
        }

        public int GetFTPIndex(EDataType dataType)
        {
            switch (dataType)
            {
                case EDataType.Image:
                    return FTPSelectedImage;
                case EDataType.Text:
                    return FTPSelectedText;
                default:
                case EDataType.File:
                    return FTPSelectedFile;
            }
        }

        public int GetLocalhostIndex(EDataType dataType)
        {
            switch (dataType)
            {
                case EDataType.Image:
                    return LocalhostSelectedImages;
                case EDataType.Text:
                    return LocalhostSelectedText;
                default:
                case EDataType.File:
                    return LocalhostSelectedFiles;
            }
        }

        #endregion Helper Methods
    }
}