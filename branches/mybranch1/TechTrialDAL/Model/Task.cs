using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL.Model
{
    public class Task
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int TaskID { get; set; }

        [StringLength(100)]
        [Display(Name="Task Name")]
        [Index("IX_Task_TaskName", IsUnique = true, Order = 1)]
        [Required]
        public string TaskName { get; set; }

        public int ProjectID { get; set; }
        
        public virtual Project Project { get; set; }

        public int UserID { get; set; }

        public virtual User User { get; set; }

        [Required]
        [Range(1, 10000, ErrorMessage="The duration must be between 1 and 10000")]
        public int Duration { get; set; }
    }
}
