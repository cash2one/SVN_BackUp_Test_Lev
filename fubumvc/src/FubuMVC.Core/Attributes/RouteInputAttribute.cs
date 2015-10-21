using System;

namespace FubuMVC.Core
{
    /// <summary>
    /// Marks a property as a route input
    /// </summary>
    [AttributeUsage(AttributeTargets.Property)]
    public class RouteInputAttribute : Attribute
    {
        public RouteInputAttribute()
        {
        }

        public RouteInputAttribute(string defaultValue)
        {
            DefaultValue = defaultValue;
        }

        public object DefaultObject
        {
            set { DefaultValue = value.ToString(); }
        }

        public string DefaultValue { get; set; }
    }
}