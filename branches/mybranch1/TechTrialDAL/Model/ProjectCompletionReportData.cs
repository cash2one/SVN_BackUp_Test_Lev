using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL.Model
{
    public class ProjectCompletionReportData
    {
        public string ProjectName { get; set; }
        public int TotalDuration { get; set; }
        public int CompletedDuration { get; set; }
        public List<TaskReportData> TaskData { get; set; }
    }    
}
