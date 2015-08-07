using System.Runtime.Serialization;
using System;

namespace TechTrialClient.Client.AttendanceService
{
    public partial class Task : object, System.Runtime.Serialization.IExtensibleDataObject, System.ComponentModel.INotifyPropertyChanged
    {
        public override string ToString()
        {
            return TaskName;
        }
    }
}