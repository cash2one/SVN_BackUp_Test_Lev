using System;
using System.IO;
using FubuCore;

namespace FubuMVC.Core.Runtime.Files
{
    public class FubuFile : IFubuFile
    {
        private string _relativePath;

        public FubuFile(string path)
        {
            Path = path;
            if (!System.IO.Path.IsPathRooted(path))
            {
                RelativePath = path;
            }
        }

        public string Path { get; private set; }

        public string RelativePath
        {
            get { return _relativePath; }
            set { _relativePath = value.IsEmpty() ? string.Empty : value.Replace('\\', '/'); }
        }

        public string ReadContents()
        {
            return new FileSystem().ReadStringFromFile(Path);
        }

        public void ReadContents(Action<Stream> action)
        {
            using (var stream = new FileStream(Path, FileMode.Open, FileAccess.Read))
            {
                action(stream);
            }
        }

        public void ReadLines(Action<string> read)
        {
            new FileSystem().ReadTextFile(Path, read);
        }

        public long Length()
        {
            return new FileInfo(Path).Length;
        }

        public string Etag()
        {
            var length = Length();
            var lastModified = LastModified();

            var hash = lastModified.ToFileTimeUtc() ^ length;

            return Convert.ToString(hash, 16);
        }

        public DateTime LastModified()
        {
            var last = new FileInfo(Path).LastWriteTimeUtc;
            return new DateTime(last.Year, last.Month, last.Day, last.Hour, last.Minute, last.Second, last.Kind);
        }

        protected bool Equals(FubuFile other)
        {
            return string.Equals(RelativePath, other.RelativePath);
        }

        public override bool Equals(object obj)
        {
            if (ReferenceEquals(null, obj)) return false;
            if (ReferenceEquals(this, obj)) return true;
            if (obj.GetType() != this.GetType()) return false;
            return Equals((FubuFile) obj);
        }

        public override int GetHashCode()
        {
            return (RelativePath != null ? RelativePath.GetHashCode() : 0);
        }

        public override string ToString()
        {
            return string.Format("RelativePath: {0}", RelativePath);
        }

        public static IFubuFile Load(string relativePath)
        {
            return new FubuApplicationFiles(FubuRuntime.DefaultApplicationPath()).Find(relativePath);
        }
    }

    public static class FubuFileExtensions
    {
        public static DateTime ExactLastWriteTime(this IFubuFile file)
        {
            return new FileInfo(file.Path).LastWriteTimeUtc;
        }
    }
}
