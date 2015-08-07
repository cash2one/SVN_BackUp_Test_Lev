using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL.Model
{
    public class EmployeeProductivityReportData
    {
        public string ProjectName { get; set; }
        public int TotalDuration { get; set; }
        public int CompletedDuration { get; set; }
        public Dictionary<string, List<TaskReportData>> UserTaskData { get; set; }
    }
}
