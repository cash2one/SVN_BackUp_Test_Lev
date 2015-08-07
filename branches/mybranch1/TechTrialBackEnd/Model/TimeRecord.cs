using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Web;

namespace TechTrialBackEnd.Model
{
    [DataContract]
    public class TimeRecord
    {
        [DataMember]
        public int TimeRecordId { get; set; }

        [DataMember]
        public int TaskId { get; set; }
    }
}