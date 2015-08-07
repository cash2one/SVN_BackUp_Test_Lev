using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Data.Entity;
using System.Data.Entity.ModelConfiguration.Conventions;

namespace TechTrialDAL
{
    public class AttendanceDbContext : DbContext
    {
        public AttendanceDbContext()
            : base("Attendance")
        {
            Database.SetInitializer<AttendanceDbContext>(new AttendanceInitializer());
        }

        public DbSet<TechTrialDAL.Model.TimeRecord> TimeRecords { get; set; }

        protected override void OnModelCreating(DbModelBuilder modelBuilder)
        {
            modelBuilder.Conventions.Remove<PluralizingTableNameConvention>();
        }
    }
}
