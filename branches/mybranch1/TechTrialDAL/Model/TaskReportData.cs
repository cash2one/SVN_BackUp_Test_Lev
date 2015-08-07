using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL.Model
{
    public class TaskReportData
    {
        public string TaskName { get; set; }
        public int TaskID { get; set; }
        public int TotalDuration { get; set; }
        public int CompletedDuration { get; set; }

        public int UserID { get; set; }
    }
}
