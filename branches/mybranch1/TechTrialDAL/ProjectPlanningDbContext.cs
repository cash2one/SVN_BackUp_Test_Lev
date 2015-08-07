using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Data.Entity;
using System.Data.Entity.ModelConfiguration.Conventions;

namespace TechTrialDAL
{
    public class ProjectPlanningDbContext : DbContext
    {
        public ProjectPlanningDbContext() : base("ProjectPlanning")
        {
            Database.SetInitializer<ProjectPlanningDbContext>(new ProjectPlanningInitializer());
        }

        public DbSet<TechTrialDAL.Model.User> Users { get; set; }
        public DbSet<TechTrialDAL.Model.Role> Roles { get; set; }
        public DbSet<TechTrialDAL.Model.Project> Projects { get; set; }
        public DbSet<TechTrialDAL.Model.Task> Tasks { get; set; }

        protected override void OnModelCreating(DbModelBuilder modelBuilder)
        {
            modelBuilder.Conventions.Remove<PluralizingTableNameConvention>();
        }
    }
}
