using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Entity;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Mvc;
using TechTrialDAL;
using TechTrialDAL.Model;

namespace TechTrialFrontEnd.Controllers
{
    [Authorize]
    public class TasksController : Controller
    {
        // GET: Tasks
        public ActionResult Index(int? ProjectId)
        {
            if (ProjectId == null)
            {                
                if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
                {
                    return RedirectToAction("Index", "Projects");
                }

                int tmp = -1;

                if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out tmp))
                {
                    return RedirectToAction("Index", "Projects");
                }
                else 
                {
                    ProjectId = tmp;
                }
            }

            var CurrentProject = DatabaseManager.GetProjectsForUser(User.Identity.Name).Where(p => p.ProjectID == ProjectId).SingleOrDefault();

            if (CurrentProject == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            HttpContext.Session.Add(Constants.SESSION_CURRENT_PROJECT, CurrentProject.ProjectID);
            ViewBag.ProjectName = CurrentProject.ProjectName;
            var tasks = DatabaseManager.GetTaskForProject(User.Identity.Name, CurrentProject.ProjectID);

            return View(tasks);
        }

        // GET: Tasks/Details/5
        public ActionResult Details(int? id)
        {
            if (id == null || HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return RedirectToAction("Index", "Projects");
            }

            Task task = DatabaseManager.GetTaskForProject(User.Identity.Name, projectId).Where(t => t.TaskID == id).SingleOrDefault();

            if (task == null)
            {
                return HttpNotFound();
            }

            return View(task);
        }

        // GET: Tasks/Create
        public ActionResult Create()
        {
            PopulateUserDropDownList();
            PopulateProjectDropDownList();
            return View();
        }

        // POST: Tasks/Create
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Create([Bind(Include = "UserID,TaskName,Duration,ProjectID")] Task task)
        {
            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return RedirectToAction("Index", "Projects");
            }            

            if (ModelState.IsValid)
            {
                // additional validation
                if (!DatabaseManager.GetAllTaskUsers().Any(u => u.UserID == task.UserID))
                {
                    return RedirectToAction("Index", "Projects");
                }

                if (!DatabaseManager.GetProjectsForUser(User.Identity.Name).Any(p => p.ProjectID == task.ProjectID))
                {
                    return RedirectToAction("Index", "Projects");
                }

                DatabaseManager.CreateTask(task);

                return RedirectToAction("Index");
            }

            PopulateUserDropDownList();
            PopulateProjectDropDownList();
            return View(task);
        }

        private void PopulateUserDropDownList(object selectedUser = null)
        {
            ViewBag.UserID = new SelectList(DatabaseManager.GetAllTaskUsers(), "UserID", "UserName", selectedUser);
        }

        private void PopulateProjectDropDownList(object selectedProject = null)
        {
            if (selectedProject == null && HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] != null)
            {
                selectedProject = HttpContext.Session[Constants.SESSION_CURRENT_PROJECT];
            }

            ViewBag.ProjectID = new SelectList(DatabaseManager.GetProjectsForUser(User.Identity.Name), "ProjectID", "ProjectName", selectedProject);
        }

        // GET: Tasks/Edit/5
        public ActionResult Edit(int? id)
        {
            if (id == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return RedirectToAction("Index", "Projects");
            }

            Task task = DatabaseManager.GetTaskForProject(User.Identity.Name, projectId).Where(t => t.TaskID == id).SingleOrDefault();

            if (task == null)
            {
                return HttpNotFound();
            }

            PopulateUserDropDownList(task.UserID);
            PopulateProjectDropDownList(task.ProjectID);
            return View(task);
        }

        // POST: Tasks/Edit/5
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Edit([Bind(Include = "TaskID,TaskName,Duration,UserID,ProjectID")] Task task)
        {
            if (ModelState.IsValid)
            {
                if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
                {
                    return RedirectToAction("Index", "Projects");
                }

                // additional validation
                int projectId = -1;

                if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
                {
                    return RedirectToAction("Index", "Projects");
                }     

                if (!DatabaseManager.GetTaskForProject(User.Identity.Name, projectId).Any(t => t.TaskID == task.TaskID))
                {
                    return RedirectToAction("Index", "Projects");
                }

                if (!DatabaseManager.GetAllTaskUsers().Any(u => u.UserID == task.UserID))
                {
                    return RedirectToAction("Index", "Projects");
                }

                if (!DatabaseManager.GetProjectsForUser(User.Identity.Name).Any(p => p.ProjectID == task.ProjectID))
                {
                    return RedirectToAction("Index", "Projects");
                }

                DatabaseManager.UpdateTask(task);
                return RedirectToAction("Index");
            }
            return View(task);
        }

        // GET: Tasks/Delete/5
        public ActionResult Delete(int? id)
        {
            if (id == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return RedirectToAction("Index", "Projects");
            }

            Task task = DatabaseManager.GetTaskForProject(User.Identity.Name, projectId).Where(t => t.TaskID == id).SingleOrDefault();

            if (task == null)
            {
                return HttpNotFound();
            }
            return View(task);
        }

        // POST: Tasks/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public ActionResult DeleteConfirmed(int id)
        {
            // additional validation
            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return RedirectToAction("Index", "Projects");
            }

            if (!DatabaseManager.GetTaskForProject(User.Identity.Name, projectId).Any(t => t.TaskID == id))
            {
                return HttpNotFound();
            }

            DatabaseManager.DeleteTask(id);

            return RedirectToAction("Index");
        }
    }
}
