using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Web;

namespace TechTrialBackEnd.Model
{
    [DataContract]
    public class Task
    {
        [DataMember]
        public int TaskId { get; set; }

        [DataMember]
        public string TaskName { get; set; }
    }
}