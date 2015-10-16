﻿namespace ShareX
{
    partial class AfterCaptureForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(AfterCaptureForm));
            this.lvAfterCaptureTasks = new System.Windows.Forms.ListView();
            this.chAfterCapture = ((System.Windows.Forms.ColumnHeader)(new System.Windows.Forms.ColumnHeader()));
            this.btnContinue = new System.Windows.Forms.Button();
            this.btnCancel = new System.Windows.Forms.Button();
            this.pbImage = new ShareX.HelpersLib.MyPictureBox();
            this.btnCopy = new System.Windows.Forms.Button();
            this.tcTasks = new System.Windows.Forms.TabControl();
            this.tpAfterCapture = new System.Windows.Forms.TabPage();
            this.tpBeforeUpload = new System.Windows.Forms.TabPage();
            this.ucBeforeUpload = new ShareX.BeforeUploadControl();
            this.tcTasks.SuspendLayout();
            this.tpAfterCapture.SuspendLayout();
            this.tpBeforeUpload.SuspendLayout();
            this.SuspendLayout();
            // 
            // lvAfterCaptureTasks
            // 
            this.lvAfterCaptureTasks.Columns.AddRange(new System.Windows.Forms.ColumnHeader[] {
            this.chAfterCapture});
            resources.ApplyResources(this.lvAfterCaptureTasks, "lvAfterCaptureTasks");
            this.lvAfterCaptureTasks.FullRowSelect = true;
            this.lvAfterCaptureTasks.HeaderStyle = System.Windows.Forms.ColumnHeaderStyle.None;
            this.lvAfterCaptureTasks.MultiSelect = false;
            this.lvAfterCaptureTasks.Name = "lvAfterCaptureTasks";
            this.lvAfterCaptureTasks.Scrollable = false;
            this.lvAfterCaptureTasks.UseCompatibleStateImageBehavior = false;
            this.lvAfterCaptureTasks.View = System.Windows.Forms.View.Details;
            this.lvAfterCaptureTasks.ItemSelectionChanged += new System.Windows.Forms.ListViewItemSelectionChangedEventHandler(this.lvAfterCaptureTasks_ItemSelectionChanged);
            this.lvAfterCaptureTasks.MouseDown += new System.Windows.Forms.MouseEventHandler(this.lvAfterCaptureTasks_MouseDown);
            // 
            // chAfterCapture
            // 
            resources.ApplyResources(this.chAfterCapture, "chAfterCapture");
            // 
            // btnContinue
            // 
            resources.ApplyResources(this.btnContinue, "btnContinue");
            this.btnContinue.Name = "btnContinue";
            this.btnContinue.UseVisualStyleBackColor = true;
            this.btnContinue.Click += new System.EventHandler(this.btnContinue_Click);
            // 
            // btnCancel
            // 
            resources.ApplyResources(this.btnCancel, "btnCancel");
            this.btnCancel.Name = "btnCancel";
            this.btnCancel.UseVisualStyleBackColor = true;
            this.btnCancel.Click += new System.EventHandler(this.btnCancel_Click);
            // 
            // pbImage
            // 
            resources.ApplyResources(this.pbImage, "pbImage");
            this.pbImage.BackColor = System.Drawing.Color.White;
            this.pbImage.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.pbImage.Cursor = System.Windows.Forms.Cursors.Default;
            this.pbImage.DrawCheckeredBackground = true;
            this.pbImage.EnableRightClickMenu = true;
            this.pbImage.FullscreenOnClick = true;
            this.pbImage.Name = "pbImage";
            // 
            // btnCopy
            // 
            resources.ApplyResources(this.btnCopy, "btnCopy");
            this.btnCopy.Name = "btnCopy";
            this.btnCopy.UseVisualStyleBackColor = true;
            this.btnCopy.Click += new System.EventHandler(this.btnCopy_Click);
            // 
            // tcTasks
            // 
            resources.ApplyResources(this.tcTasks, "tcTasks");
            this.tcTasks.Controls.Add(this.tpAfterCapture);
            this.tcTasks.Controls.Add(this.tpBeforeUpload);
            this.tcTasks.Name = "tcTasks";
            this.tcTasks.SelectedIndex = 0;
            // 
            // tpAfterCapture
            // 
            this.tpAfterCapture.Controls.Add(this.lvAfterCaptureTasks);
            resources.ApplyResources(this.tpAfterCapture, "tpAfterCapture");
            this.tpAfterCapture.Name = "tpAfterCapture";
            this.tpAfterCapture.UseVisualStyleBackColor = true;
            // 
            // tpBeforeUpload
            // 
            this.tpBeforeUpload.Controls.Add(this.ucBeforeUpload);
            resources.ApplyResources(this.tpBeforeUpload, "tpBeforeUpload");
            this.tpBeforeUpload.Name = "tpBeforeUpload";
            this.tpBeforeUpload.UseVisualStyleBackColor = true;
            // 
            // ucBeforeUpload
            // 
            resources.ApplyResources(this.ucBeforeUpload, "ucBeforeUpload");
            this.ucBeforeUpload.Name = "ucBeforeUpload";
            // 
            // AfterCaptureForm
            // 
            this.AcceptButton = this.btnContinue;
            resources.ApplyResources(this, "$this");
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.Controls.Add(this.tcTasks);
            this.Controls.Add(this.btnCopy);
            this.Controls.Add(this.btnCancel);
            this.Controls.Add(this.btnContinue);
            this.Controls.Add(this.pbImage);
            this.Name = "AfterCaptureForm";
            this.SizeGripStyle = System.Windows.Forms.SizeGripStyle.Hide;
            this.TopMost = true;
            this.tcTasks.ResumeLayout(false);
            this.tpAfterCapture.ResumeLayout(false);
            this.tpBeforeUpload.ResumeLayout(false);
            this.ResumeLayout(false);

        }

        #endregion

        private HelpersLib.MyPictureBox pbImage;
        private System.Windows.Forms.ListView lvAfterCaptureTasks;
        private System.Windows.Forms.ColumnHeader chAfterCapture;
        private System.Windows.Forms.Button btnContinue;
        private System.Windows.Forms.Button btnCancel;
        private System.Windows.Forms.Button btnCopy;
        private System.Windows.Forms.TabControl tcTasks;
        private System.Windows.Forms.TabPage tpAfterCapture;
        private System.Windows.Forms.TabPage tpBeforeUpload;
        private BeforeUploadControl ucBeforeUpload;
    }
}