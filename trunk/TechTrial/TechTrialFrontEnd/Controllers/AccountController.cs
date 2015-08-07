using System;
using System.Globalization;
using System.Linq;
using System.Security.Claims;
using System.Threading.Tasks;
using System.Web;
using System.Web.Mvc;
using Microsoft.AspNet.Identity;
using TechTrialFrontEnd.Models;
using TechTrialDAL;
using System.Web.Security;

namespace TechTrialFrontEnd.Controllers
{
    [Authorize]
    public class AccountController : Controller
    {
        
        public AccountController()
        {
        }        


        //
        // GET: /Account/Login
        [AllowAnonymous]
        public ActionResult Login(string returnUrl)
        {
            ViewBag.ReturnUrl = returnUrl;
            return View();
        }

        //
        // POST: /Account/Login
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> Login(LoginViewModel model, string returnUrl)
        {
            if (!ModelState.IsValid)
            {
                return View(model);
            }

            try
            {
                var user = Security.ValidateManagerCredentials(model.UserName, model.Password);

                if (user != null)
                {
                    FormsAuthentication.SetAuthCookie(user.UserName, createPersistentCookie: false);
                    return RedirectToLocal(returnUrl);
                }
                else
                {
                    ModelState.AddModelError("", "Invalid login attempt.");
                    return View(model);
                }
            }
            catch
            {
                // here be logging
                ModelState.AddModelError("", "Invalid login attempt.");
                return View(model);
            }
        }


        //
        // POST: /Account/LogOff
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult LogOff()
        {
            FormsAuthentication.SignOut();
            HttpContext.Session.Abandon();
            return RedirectToAction("Index", "Home");
        }


        #region Helpers

        private ActionResult RedirectToLocal(string returnUrl)
        {
            if (Url.IsLocalUrl(returnUrl))
            {
                return Redirect(returnUrl);
            }
            return RedirectToAction("Index", "Home");
        }

        #endregion
    }
}