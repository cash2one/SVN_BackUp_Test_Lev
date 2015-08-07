using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL
{
    class ProjectPlanningInitializer : DropCreateDatabaseIfModelChanges <ProjectPlanningDbContext>
    {
        protected override void Seed(ProjectPlanningDbContext context)
        {

            var Role_PM = new TechTrialDAL.Model.Role { RoleID = 1, RoleName = "PM"};
            var Role_USER = new TechTrialDAL.Model.Role { RoleID = 2, RoleName = "USER"};
            var roles = new List<TechTrialDAL.Model.Role> { Role_PM, Role_USER };
            roles.ForEach(r => context.Roles.Add(r));
            context.SaveChanges();

            var User_lev = new TechTrialDAL.Model.User { UserID = 1, Enabled = true, UserName = "lev", PasswordHash = Security.GetHashedPassword("lev", "P@ssw0rd"), Role = Role_USER };
            var User_lena = new TechTrialDAL.Model.User { UserID = 2, Enabled = true, UserName = "lena", PasswordHash = Security.GetHashedPassword("lena", "12345"), Role = Role_USER };
            var PM_lev = new TechTrialDAL.Model.User { UserID = 3, Enabled = true, UserName = "lev_pm", PasswordHash = Security.GetHashedPassword("lev_pm", "P@ssw0rd"), Role = Role_PM };
            var PM_lena = new TechTrialDAL.Model.User { UserID = 4, Enabled = true, UserName = "lena_pm", PasswordHash = Security.GetHashedPassword("lena_pm", "12345"), Role = Role_PM };
            var users = new List<TechTrialDAL.Model.User> { User_lev, User_lena, PM_lev, PM_lena };
            users.ForEach(u => context.Users.Add(u));
            context.SaveChanges();

            var Project_Lev = new TechTrialDAL.Model.Project { ProjectID = 1, ProjectName = "Lev's project", ProjectManager = PM_lev };
            var Project_Lena = new TechTrialDAL.Model.Project { ProjectID = 2, ProjectName = "Lena's project", ProjectManager = PM_lena };
            var Project_Lev2 = new TechTrialDAL.Model.Project { ProjectID = 3, ProjectName = "Lev's second project", ProjectManager = PM_lev };
            var projects = new List<TechTrialDAL.Model.Project> { Project_Lev, Project_Lena, Project_Lev2 };
            projects.ForEach(p => context.Projects.Add(p));
            context.SaveChanges();

            var Task_Lev = new TechTrialDAL.Model.Task { TaskID = 1, TaskName = "Lev's sample task in Lena's project", User = User_lev, Project = Project_Lena, Duration = 1 };
            var Task_Lena = new TechTrialDAL.Model.Task { TaskID = 2, TaskName = "Lena's sample task in Lev's project", User = User_lena, Project = Project_Lev, Duration = 5 };
            var tasks = new List<TechTrialDAL.Model.Task> { Task_Lev, Task_Lena };
            tasks.ForEach(t => context.Tasks.Add(t));
            context.SaveChanges();
        }
    }
}
