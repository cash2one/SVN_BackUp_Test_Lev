using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using System.IdentityModel.Selectors;
using System.IdentityModel.Tokens;
using System.ServiceModel;

namespace TechTrialDAL
{
    public static class Security
    {
        public static readonly string ROLE_USER = "USER";
        public static readonly string ROLE_PM = "PM";
        
        public static string GetHashedPassword(string username, string password)
        {
            HashAlgorithm algorithm = new SHA256Managed();

            byte[] plainText = Encoding.UTF8.GetBytes(password);
            byte[] salt = Encoding.UTF8.GetBytes(username);

            byte[] plainTextWithSaltBytes = new byte[plainText.Length + salt.Length];

            for (int i = 0; i < plainText.Length; i++)
            {
                plainTextWithSaltBytes[i] = plainText[i];
            }
            for (int i = 0; i < salt.Length; i++)
            {
                plainTextWithSaltBytes[plainText.Length + i] = salt[i];
            }

            var result = algorithm.ComputeHash(plainTextWithSaltBytes);

            return Convert.ToBase64String(result);
        }

        public static TechTrialDAL.Model.User ValidateUserCredentials(string userName, string password)
        {
            return ValidateCredentials(userName, password, ROLE_USER);
        }

        public static TechTrialDAL.Model.User ValidateManagerCredentials(string userName, string password)
        {
            return ValidateCredentials(userName, password, ROLE_PM);
        }

        private static TechTrialDAL.Model.User ValidateCredentials(string userName, string password, string role)
        {
            string hash = Security.GetHashedPassword(userName, password);

            var user = DatabaseManager.GetUser(userName, hash);

            if (user == null)
            {
                throw new FaultException("User name or password is invalid");
            }

            if (!user.Enabled)
            {
                throw new FaultException("User is disabled");
            }

            if (user.Role.RoleName != role)
            {
                throw new FaultException("User doesn not have the required role");
            }

            return user;
        }
    }
}
