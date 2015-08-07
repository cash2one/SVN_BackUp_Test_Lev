using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Entity;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Mvc;
using System.Web.Security;
using TechTrialDAL;
using TechTrialDAL.Model;

namespace TechTrialFrontEnd.Controllers
{
    [Authorize]
    public class ProjectsController : Controller
    {
    

        // GET: Projects
        public ActionResult Index()
        {
            var projects = DatabaseManager.GetProjectsForUser(User.Identity.Name);
            return View(projects);
        }
    }
}
