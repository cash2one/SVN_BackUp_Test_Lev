using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace TechTrialClient.Client.View
{
    public partial class FakeCardReaderForm : Form
    {
        public FakeCardReaderForm()
        {
            InitializeComponent();
        }

        private void FakeCardReaderForm_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = false;
            this.DialogResult = System.Windows.Forms.DialogResult.OK;
            this.Close();
        }

        private void FakeCardReaderForm_Shown(object sender, EventArgs e)
        {
            label1.Focus();
        }
    }
}
