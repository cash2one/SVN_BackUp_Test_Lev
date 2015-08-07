using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.ServiceModel;
using System.ServiceModel.Security;
using System.Text;
using System.Windows.Forms;
using TechTrialClient.Client.AttendanceService;
using TechTrialClient.Client.Controller;
using TechTrialClient.Client.Util;

namespace TechTrialClient.Client.View
{
    public partial class LoginForm : Form
    {
        public LoginForm()
        {
            InitializeComponent();
        }

        private void tbs_TextChanged(object sender, EventArgs e)
        {
            btnConnect.Enabled = tbLogin.Text.Length > 0 && tbPassword.Text.Length > 0;
        }

        private void LoginForm_Shown(object sender, EventArgs e)
        {
            tbLogin.Focus();
        }

        private void btnCancel_Click(object sender, EventArgs e)
        {
            Close();
        }

        private void btnConnect_Click(object sender, EventArgs e)
        {
            bool authResult = TryLogin();

            if (authResult)
            {
                InitMainForm();
            }
        }

        private void InitMainForm()
        {
            try
            {
                this.Cursor = Cursors.WaitCursor;

                List<Task> tasks = AttendanceServiceController.GetTasks();
                var main = new MainForm(tasks);

                this.Hide();
                main.ShowDialog();                
                this.Close();
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

        private bool TryLogin()
        {
            try
            {
                this.Cursor = Cursors.WaitCursor;
                return AttendanceServiceController.CheckCredentials(tbLogin.Text, tbPassword.Text);
            }
            catch (UnableToConnectException)
            {
                MessageBox.Show("Unable to connect to the service. Please try again later", "Unable to connect", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            catch (MessageSecurityException ex)
            {
                tbPassword.Text = "";

                if (ex.InnerException != null && !string.IsNullOrWhiteSpace(ex.InnerException.Message))
                {
                    MessageBox.Show("Authentication failed: " + ex.InnerException.Message, "Unable to login", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
                else
                {
                    MessageBox.Show("Authentication failed. Please try again", "Unable to login", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
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

            return false;
        }
    }
}
