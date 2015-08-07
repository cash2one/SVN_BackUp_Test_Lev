using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Mvc;
using TechTrialDAL;
using TechTrialFrontEnd.Models;

namespace TechTrialFrontEnd.Controllers
{
    [Authorize]
    public class ReportsController : Controller
    {
        // GET: Reports
        public ActionResult Index()
        {
            if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            if (!DatabaseManager.GetProjectsForUser(User.Identity.Name).Any(p => p.ProjectID == projectId))
            {
                return RedirectToAction("Index", "Projects");
            }

            return View();
        }


        public ActionResult ProjectCompletionReport()
        {
            if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            var currentProject = DatabaseManager.GetProjectsForUser(User.Identity.Name).SingleOrDefault(p => p.ProjectID == projectId);

            if (currentProject == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            var viewData = DatabaseManager.ProjectCompletionReport(User.Identity.Name, projectId);

            return View(viewData);
        }


        public ActionResult ProjectCompletionReportPDF()
        {
            if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            var currentProject = DatabaseManager.GetProjectsForUser(User.Identity.Name).SingleOrDefault(p => p.ProjectID == projectId);

            if (currentProject == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            var viewData = DatabaseManager.ProjectCompletionReport(User.Identity.Name, projectId);

            return new Rotativa.PartialViewAsPdf("ProjectCompletionReport", viewData);
        }

        public ActionResult EmployeeProductivityReport()
        {
            if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            var currentProject = DatabaseManager.GetProjectsForUser(User.Identity.Name).SingleOrDefault(p => p.ProjectID == projectId);

            if (currentProject == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            var viewData = DatabaseManager.EmployeeProductivityReport(User.Identity.Name, projectId);

            return View(viewData);
        }

        public ActionResult EmployeeProductivityReportPDF()
        {
            if (HttpContext.Session[Constants.SESSION_CURRENT_PROJECT] == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            int projectId = -1;

            if (!int.TryParse(HttpContext.Session[Constants.SESSION_CURRENT_PROJECT].ToString(), out projectId))
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            var currentProject = DatabaseManager.GetProjectsForUser(User.Identity.Name).SingleOrDefault(p => p.ProjectID == projectId);

            if (currentProject == null)
            {
                return RedirectToAction("Index", "Projects");
            }

            var viewData = DatabaseManager.EmployeeProductivityReport(User.Identity.Name, projectId);

            return new Rotativa.PartialViewAsPdf("EmployeeProductivityReport", viewData);
        }
    }
}