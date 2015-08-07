using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace TechTrialDAL.Model
{
    public class Role
    {
        [DatabaseGenerated(DatabaseGeneratedOption.None)]
        [Key]
        public int RoleID { get; set; }

        [StringLength(100)]
        [Required]
        [Index("IX_Role_RoleName", IsUnique = true, Order = 1)]
        public string RoleName { get; set; }
    }
}
