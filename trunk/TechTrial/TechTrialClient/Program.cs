using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Net;
using System.ServiceModel;
using System.Text;
using TechTrialClient.Client.Util;
using TechTrialClient.Client.View;
using System.Threading;


namespace TechTrialClient.Client
{
    static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main()
        {
            using (var mutex = new Mutex(false, "Global\\" + appGuid))
            {
                if (!mutex.WaitOne(0, false))
                {
                    MessageBox.Show("Instance already running");
                    return;
                }
                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                Application.Run(new LoginForm());
            }
        }

        private static string appGuid = "5BB5F508-8C32-4CCD-8984-EDEF7371E44F";
    }
}
