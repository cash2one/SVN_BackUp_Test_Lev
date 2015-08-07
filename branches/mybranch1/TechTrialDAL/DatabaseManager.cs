using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Data.Entity.Core.Objects;
using System.Data.Entity.Infrastructure;
using System.Linq;
using System.Text;

namespace TechTrialDAL
{
    public static class DatabaseManager
    {
        private static ProjectPlanningDbContext db;
        private static AttendanceDbContext db2;

        static DatabaseManager()
        {
            db = new ProjectPlanningDbContext();
            db2 = new AttendanceDbContext();
        }

        public static TechTrialDAL.Model.User GetUser(string userName, string passwordHash)
        {
            return db.Users.Where(u => u.UserName == userName && u.PasswordHash == passwordHash).SingleOrDefault(); 
        }

        public static TechTrialDAL.Model.User GetUser(string userId)
        {
            return db.Users.Where(u => u.UserID.ToString() == userId).SingleOrDefault();
        }

        public static TechTrialDAL.Model.User GetUserByName(string name)
        {
            return db.Users.Where(u => u.UserName == name).SingleOrDefault();
        }

        public static List<TechTrialDAL.Model.Task> GetUserTasks(string userName)
        {
            return db.Tasks.Where(t => t.User.UserName == userName).ToList();
        }

        public static TechTrialDAL.Model.TimeRecord StartTracking(string userName, int taskId)
        {
            var user = db.Users.Where(u => u.UserName == userName).SingleOrDefault();
            if (user == null || !user.Enabled || user.Role.RoleName != Security.ROLE_USER) throw new ArgumentException("Invalid user");

            var task = GetUserTasks(userName).Where(t => t.TaskID == taskId).SingleOrDefault();
            if (task == null) throw new ArgumentException("Invalid task id");

            var record = new TechTrialDAL.Model.TimeRecord { UserId = user.UserID, TaskID = task.TaskID, DateStart = DateTime.Now, DateEnd = null };
            
            db2.TimeRecords.Add(record);
            db2.SaveChanges();

            return record;
        }

        public static void StopTracking(string userName, int taskId, int RecordId)
        {
            var user = db.Users.Where(u => u.UserName == userName).SingleOrDefault();
            if (user == null || !user.Enabled || user.Role.RoleName != Security.ROLE_USER) throw new ArgumentException("Invalid user");

            var record = db2.TimeRecords.Where(r => r.UserId == user.UserID && r.TaskID == taskId && r.TimeRecordId == RecordId).SingleOrDefault();
            if (record == null || record.DateEnd.HasValue) throw new ArgumentException("Invalid time record");

            record.DateEnd = DateTime.Now;
            
            db2.SaveChanges();
        }

        public static List<TechTrialDAL.Model.Project> GetProjectsForUser(string userName)
        {
            return db.Projects.Include("ProjectManager").Where(p => p.ProjectManager.UserName == userName).ToList();
        }

        public static List<TechTrialDAL.Model.Task> GetTaskForProject(string userName, int projectId)
        {
            var context = ((IObjectContextAdapter)db).ObjectContext;
            context.Refresh(RefreshMode.StoreWins, db.Tasks);
            return db.Tasks.Where(t => t.Project.ProjectManager.UserName == userName && t.Project.ProjectID == projectId).ToList();
        }

        public static List<TechTrialDAL.Model.User> GetAllTaskUsers()
        {
            return db.Users.Where(u => u.Role.RoleName == Security.ROLE_USER).OrderBy(u => u.UserName).ToList();
        }

        public static void UpdateTask(TechTrialDAL.Model.Task task)
        {
            TechTrialDAL.Model.Task currentTask = db.Tasks.Where(t => t.TaskID == task.TaskID).SingleOrDefault();
            if (currentTask == null) throw new ArgumentException("Invalid task");

            currentTask.Duration = task.Duration;
            currentTask.ProjectID = task.ProjectID;
            currentTask.UserID = task.UserID;
            currentTask.TaskName = task.TaskName;

            db.SaveChanges();
        }

        public static void DeleteTask(int id)
        {
            TechTrialDAL.Model.Task task = db.Tasks.Find(id);
            db.Tasks.Remove(task);
            db.SaveChanges();
        }

        public static void CreateTask(Model.Task task)
        {
            db.Tasks.Add(task);
            db.SaveChanges();
        }

        public static TechTrialDAL.Model.ProjectCompletionReportData ProjectCompletionReport(string userName, int projectId)
        {
            var context = ((IObjectContextAdapter)db).ObjectContext;
            context.Refresh(RefreshMode.StoreWins, db.Tasks);

            var context2 = ((IObjectContextAdapter)db2).ObjectContext;
            context2.Refresh(RefreshMode.StoreWins, db2.TimeRecords);

            var tasks = GetTaskForProject(userName, projectId).ToList();
            var taskIds = tasks.Select(t => t.TaskID).Distinct();

            var data = db2.TimeRecords.Where(r => taskIds.Contains(r.TaskID));

            var result = new TechTrialDAL.Model.ProjectCompletionReportData();

            result.ProjectName = db.Projects.Where(p => p.ProjectID == projectId).Single().ProjectName;
            result.TaskData = new List<Model.TaskReportData>();

            foreach (var task in tasks)
            {
                result.TaskData.Add(new Model.TaskReportData() 
                { 
                    TaskName = task.TaskName, 
                    TaskID = task.TaskID, 
                    UserID = task.UserID, 
                    TotalDuration = task.Duration, 
                    CompletedDuration = 0 
                });
            }

            foreach (var record in data)
            {
                int add = (int)((record.DateEnd.HasValue ? record.DateEnd.Value : DateTime.Now) - record.DateStart.Value).TotalSeconds;
                
                var task = result.TaskData.Where(t => t.TaskID == record.TaskID).Single();

                if (task.CompletedDuration + add > task.TotalDuration)
                {
                    task.CompletedDuration = task.TotalDuration;
                }
                else
                {
                    task.CompletedDuration += add;
                }
            }

            result.TotalDuration = result.TaskData.Sum(t => t.TotalDuration);
            result.CompletedDuration = result.TaskData.Sum(t => t.CompletedDuration);

            return result;
        }

        public static Model.EmployeeProductivityReportData EmployeeProductivityReport(string userName, int projectId)
        {
            var rep1 = ProjectCompletionReport(userName, projectId);

            var result = new Model.EmployeeProductivityReportData();
            result.ProjectName = rep1.ProjectName;
            result.TotalDuration = rep1.TotalDuration;
            result.CompletedDuration = rep1.CompletedDuration;
            result.UserTaskData = new Dictionary<string, List<Model.TaskReportData>>();

            Dictionary<int, string> user_ref = db.Users.ToDictionary(u => u.UserID, u => u.UserName);

            foreach (var task in rep1.TaskData)
            {
                string key = user_ref[task.UserID];

                if (!result.UserTaskData.ContainsKey(key))
                {
                    result.UserTaskData.Add(key, new List<Model.TaskReportData>());
                }

                result.UserTaskData[key].Add(task);
            }

            return result;
        }
    }
}
