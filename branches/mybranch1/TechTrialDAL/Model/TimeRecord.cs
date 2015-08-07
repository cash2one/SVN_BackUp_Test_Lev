using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TechTrialDAL.Model
{
    public class TimeRecord
    {
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        [Key]
        public int TimeRecordId { get; set; }

        [Required]
        public int UserId { get; set; }

        [Required]
        public int TaskID { get; set; }

        public DateTime? DateStart { get; set; }

        public DateTime? DateEnd { get; set; }
    }
}
