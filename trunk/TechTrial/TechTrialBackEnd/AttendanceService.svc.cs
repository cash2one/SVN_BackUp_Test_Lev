using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;
using System.Text;
using TechTrialDAL;

namespace TechTrialBackEnd
{
    public class AttendanceService : IAttendanceService
    {
        public bool CheckAuth()
        {
            CheckAuthInternal();
            return true;
        }

        private static void CheckAuthInternal()
        {
            if (!ServiceSecurityContext.Current.PrimaryIdentity.IsAuthenticated)
            {
                throw new FaultException("Not authorized");
            }
        }

        public List<Model.Task> GetTaskList()
        {
            CheckAuthInternal();

            var tasks_dal = DatabaseManager.GetUserTasks(ServiceSecurityContext.Current.PrimaryIdentity.Name);
            var tasks_wcf = new List<Model.Task>();
            tasks_dal.ForEach(t => tasks_wcf.Add(new Model.Task() { TaskId = t.TaskID, TaskName = t.TaskName }));
            return tasks_wcf;
        }


        public Model.TimeRecord StartTracking(int taskId)
        {
            CheckAuthInternal();

            var record_dal = DatabaseManager.StartTracking(ServiceSecurityContext.Current.PrimaryIdentity.Name, taskId);

            return new Model.TimeRecord { TimeRecordId = record_dal.TimeRecordId, TaskId = record_dal.TaskID };
        }

        public void StopTracking(Model.TimeRecord rec)
        {
            CheckAuthInternal();

            DatabaseManager.StopTracking(ServiceSecurityContext.Current.PrimaryIdentity.Name, rec.TaskId, rec.TimeRecordId);
        }
    }
}
