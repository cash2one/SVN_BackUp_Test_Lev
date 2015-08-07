using System;
using System.Security;
using System.Linq;
using System.IdentityModel.Selectors;
using System.IdentityModel.Tokens;
using TechTrialDAL;
using System.ServiceModel;

namespace TechTrialBackEnd
{
    public class CustomAuthentication : UserNamePasswordValidator
    {
        public CustomAuthentication()
            : base()
        {

        }

        public override void Validate(string userName, string password)
        {
            Security.ValidateUserCredentials(userName, password);
        }
    }
}