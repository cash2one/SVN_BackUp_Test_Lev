using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.AspNet.Identity;

namespace TechTrialDAL.Model
{
    public class User
    {
        [DatabaseGenerated(DatabaseGeneratedOption.None)]
        [Key]
        public int UserID { get; set; }
        
        [StringLength(50)]
        [Required]
        [Index("IX_User_UserName", IsUnique=true, Order=1 )]
        [Display(Name="Assignee")]
        public string UserName { get; set; }
        
        [StringLength(1024)]
        [Required]
        public string PasswordHash { get; set; }
        public bool Enabled { get; set; }

        [Required]
        public virtual Role Role { get; set; }
    }
}
