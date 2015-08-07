using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL
{
    class AttendanceInitializer : DropCreateDatabaseIfModelChanges<AttendanceDbContext>
    {
        protected override void Seed(AttendanceDbContext context)
        {
        }
    }
}
