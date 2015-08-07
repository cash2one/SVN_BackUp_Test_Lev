using System;
using System.Collections.Generic;
using System.Linq;
using System.ServiceModel;
using System.ServiceModel.Security;
using System.Text;
using TechTrialClient.Client.AttendanceService;
using TechTrialClient.Client.Util;

namespace TechTrialClient.Client.Controller
{
    static class AttendanceServiceController
    {
        private static AttendanceServiceClient client;

        static AttendanceServiceController()
        {
            try
            {
                client = new AttendanceServiceClient();
                PermissiveCertificatePolicy.Enact("CN=TechTrial");                
            }
            catch 
            {

            }
        }

        public static bool CheckCredentials(string username, string password)
        {
            if (client != null)
            {
                client.ClientCredentials.UserName.UserName = username;
                client.ClientCredentials.UserName.Password = password;

                try
                {
                    return client.CheckAuth();
                }
                catch
                {
                    client = new AttendanceServiceClient(); // to allow more authentication attempts
                    throw;
                }
            }
            else
            {
                throw new UnableToConnectException();
            }
        }

        public static List<Task> GetTasks()
        {
            if (client != null || client.State != CommunicationState.Opened)
            {
                return client.GetTaskList().ToList();
            }
            else
            {
                throw new UnableToConnectException();
            }
        }

        public static TimeRecord StartTracking(int taskId)
        {
            if (client != null || client.State != CommunicationState.Opened)
            {
                return client.StartTracking(taskId);
            }
            else
            {
                throw new UnableToConnectException();
            }
        }

        public static void StopTracking(TimeRecord rec)
        {
            if (client != null || client.State != CommunicationState.Opened)
            {
                client.StopTracking(rec);
            }
            else
            {
                throw new UnableToConnectException();
            }
        }

    }
}
