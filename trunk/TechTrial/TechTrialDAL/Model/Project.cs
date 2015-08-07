using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL.Model
{
    public class Project
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.None)]
        public int ProjectID { get; set; }

        [StringLength(100)]
        [Index("IX_Project_ProjectName", IsUnique = true, Order = 1)]
        [Display(Name="Project Name")]
        public string ProjectName { get; set; }

        public virtual User ProjectManager { get; set; }
    }
}
