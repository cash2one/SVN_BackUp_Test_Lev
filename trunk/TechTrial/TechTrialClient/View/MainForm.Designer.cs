namespace TechTrialClient.Client.View
{
    partial class MainForm
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
            this.label1 = new System.Windows.Forms.Label();
            this.cbTasks = new System.Windows.Forms.ComboBox();
            this.btnRefresh = new System.Windows.Forms.Button();
            this.btnStartTracking = new System.Windows.Forms.Button();
            this.btnStopTracking = new System.Windows.Forms.Button();
            this.lblTimer = new System.Windows.Forms.Label();
            this.SuspendLayout();
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(12, 9);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(34, 13);
            this.label1.TabIndex = 0;
            this.label1.Text = "Task:";
            // 
            // cbTasks
            // 
            this.cbTasks.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.cbTasks.FormattingEnabled = true;
            this.cbTasks.Location = new System.Drawing.Point(52, 6);
            this.cbTasks.Name = "cbTasks";
            this.cbTasks.Size = new System.Drawing.Size(308, 21);
            this.cbTasks.TabIndex = 1;
            this.cbTasks.SelectedIndexChanged += new System.EventHandler(this.cbTasks_SelectedIndexChanged);
            // 
            // btnRefresh
            // 
            this.btnRefresh.Location = new System.Drawing.Point(366, 4);
            this.btnRefresh.Name = "btnRefresh";
            this.btnRefresh.Size = new System.Drawing.Size(75, 23);
            this.btnRefresh.TabIndex = 2;
            this.btnRefresh.Text = "Refresh";
            this.btnRefresh.UseVisualStyleBackColor = true;
            this.btnRefresh.Click += new System.EventHandler(this.btnRefresh_Click);
            // 
            // btnStartTracking
            // 
            this.btnStartTracking.Location = new System.Drawing.Point(133, 47);
            this.btnStartTracking.Name = "btnStartTracking";
            this.btnStartTracking.Size = new System.Drawing.Size(75, 44);
            this.btnStartTracking.TabIndex = 3;
            this.btnStartTracking.Text = "Start Tracking";
            this.btnStartTracking.UseVisualStyleBackColor = true;
            this.btnStartTracking.Click += new System.EventHandler(this.btnStartTracking_Click);
            // 
            // btnStopTracking
            // 
            this.btnStopTracking.Location = new System.Drawing.Point(214, 47);
            this.btnStopTracking.Name = "btnStopTracking";
            this.btnStopTracking.Size = new System.Drawing.Size(75, 44);
            this.btnStopTracking.TabIndex = 4;
            this.btnStopTracking.Text = "Stop Tracking";
            this.btnStopTracking.UseVisualStyleBackColor = true;
            this.btnStopTracking.Click += new System.EventHandler(this.btnStopTracking_Click);
            // 
            // lblTimer
            // 
            this.lblTimer.AutoSize = true;
            this.lblTimer.Font = new System.Drawing.Font("Microsoft Sans Serif", 48F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.lblTimer.ForeColor = System.Drawing.Color.DarkGreen;
            this.lblTimer.Location = new System.Drawing.Point(76, 116);
            this.lblTimer.Name = "lblTimer";
            this.lblTimer.Size = new System.Drawing.Size(284, 73);
            this.lblTimer.TabIndex = 5;
            this.lblTimer.Text = "00:00:00";
            // 
            // MainForm
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(452, 205);
            this.Controls.Add(this.lblTimer);
            this.Controls.Add(this.btnStopTracking);
            this.Controls.Add(this.btnStartTracking);
            this.Controls.Add(this.btnRefresh);
            this.Controls.Add(this.cbTasks);
            this.Controls.Add(this.label1);
            this.DoubleBuffered = true;
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.Name = "MainForm";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "TechTrial Attendance Tracker";
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.MainForm_FormClosing);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.ComboBox cbTasks;
        private System.Windows.Forms.Button btnRefresh;
        private System.Windows.Forms.Button btnStartTracking;
        private System.Windows.Forms.Button btnStopTracking;
        private System.Windows.Forms.Label lblTimer;
    }
}