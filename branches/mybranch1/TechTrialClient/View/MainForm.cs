using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using TechTrialClient.Client.AttendanceService;
using TechTrialClient.Client.Controller;
using TechTrialClient.Client.Util;

namespace TechTrialClient.Client.View
{
    public partial class MainForm : Form
    {
        private List<Task> Tasks;
        private TimeRecord CurrentTimeRecord = null;

        private enum WorkMode {  TrackingStarted, TrackingStopped }
        private WorkMode CurrentWorkMode = WorkMode.TrackingStopped;
        private Timer Timer;
        private DateTime TrackingStarted;

        public MainForm(List<Task> tasks)
        {
            InitializeComponent();
            Tasks = tasks;


            Timer = new Timer();
            Timer.Enabled = false;
            Timer.Interval = 1000;
            Timer.Tick += timer_Tick;

            RefreshTasks();
            SwitchGui(WorkMode.TrackingStopped);
        }



        private void btnRefresh_Click(object sender, EventArgs e)
        {
            try
            {
                this.Cursor = Cursors.WaitCursor;
                Tasks = AttendanceServiceController.GetTasks();
                RefreshTasks();
            }
            catch (UnableToConnectException)
            {
                MessageBox.Show("Unable to connect to the service. Please try again later", "Unable to connect", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            catch (Exception ex)
            {
                // here be logging
                MessageBox.Show("Unexpected error while connecting to server: " + ex.ToString(), "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                this.Cursor = Cursors.Default;
            }
        }

        private void RefreshTasks()
        {
            if (Tasks != null && Tasks.Count > 0)
            {
                cbTasks.BeginUpdate();
                cbTasks.Items.Clear();
                cbTasks.Items.AddRange(Tasks.ToArray());
                cbTasks.SelectedIndex = 0;
                cbTasks.EndUpdate();
            }
            else
            {
                cbTasks.Items.Clear();
                MessageBox.Show("You don't have any tasks assigned. Please contact your manager", "No tasks assigned", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
        }

        private void btnStartTracking_Click(object sender, EventArgs e)
        {
            var dlg = new FakeCardReaderForm();
            if (dlg.ShowDialog() != System.Windows.Forms.DialogResult.OK) return;

            try
            {
                this.Cursor = Cursors.WaitCursor;

                Task selectedTask = cbTasks.SelectedItem as Task;

                CurrentTimeRecord = AttendanceServiceController.StartTracking(selectedTask.TaskId);

                SwitchGui(WorkMode.TrackingStarted);
            }
            catch (UnableToConnectException)
            {
                MessageBox.Show("Unable to connect to the service. Please try again later", "Unable to connect", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            catch (Exception ex)
            {
                // here be logging
                MessageBox.Show("Unexpected error while connecting to server: " + ex.ToString(), "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                this.Cursor = Cursors.Default;
            }
        }

        private void btnStopTracking_Click(object sender, EventArgs e)
        {
            var dlg = new FakeCardReaderForm();
            if (dlg.ShowDialog() != System.Windows.Forms.DialogResult.OK) return;

            try
            {
                this.Cursor = Cursors.WaitCursor;

                AttendanceServiceController.StopTracking(CurrentTimeRecord);

                SwitchGui(WorkMode.TrackingStopped);
            }
            catch (UnableToConnectException)
            {
                MessageBox.Show("Unable to connect to the service. Please try again later", "Unable to connect", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            catch (Exception ex)
            {
                // here be logging
                MessageBox.Show("Unexpected error while connecting to server: " + ex.ToString(), "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                this.Cursor = Cursors.Default;
            }
        }

        private void SwitchGui(WorkMode mode)
        {
            CurrentWorkMode = mode;

            if (mode == WorkMode.TrackingStarted)
            {
                btnStartTracking.Enabled = false;
                btnStopTracking.Enabled = true;
                cbTasks.Enabled = false;
                btnRefresh.Enabled = false;
                lblTimer.Text = "00:00:00";
                TrackingStarted = DateTime.Now;
                Timer.Enabled = true;
            }
            else
            {
                Timer.Enabled = false;
                btnStartTracking.Enabled = cbTasks.SelectedItem != null;
                btnStopTracking.Enabled = false;
                cbTasks.Enabled = true;
                btnRefresh.Enabled = true;                
            }
        }

        void timer_Tick(object sender, EventArgs e)
        {
            lblTimer.Text = (DateTime.Now - TrackingStarted).ToString(@"hh\:mm\:ss");
        }

        private void cbTasks_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (CurrentWorkMode == WorkMode.TrackingStopped)
            {
                btnStartTracking.Enabled = cbTasks.SelectedItem != null;
            }
        }

        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            if (CurrentWorkMode == WorkMode.TrackingStarted)
            {
                e.Cancel = true;
            }
        }
    }
}
