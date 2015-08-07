using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;
using System.Text;

namespace TechTrialBackEnd
{
    // NOTE: You can use the "Rename" command on the "Refactor" menu to change the interface name "IService1" in both code and config file together.
    [ServiceContract(Namespace = "http://TechTrialBackEnd")]
    public interface IAttendanceService
    {
        [OperationContract]
        bool CheckAuth();

        [OperationContract]
        List<TechTrialBackEnd.Model.Task> GetTaskList();

        [OperationContract]
        TechTrialBackEnd.Model.TimeRecord StartTracking(int taskId);

        [OperationContract]
        void StopTracking(TechTrialBackEnd.Model.TimeRecord rec);
    }
}
