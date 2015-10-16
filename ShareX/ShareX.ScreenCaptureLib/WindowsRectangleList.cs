﻿#region License Information (GPL v3)

/*
    ShareX - A program that allows you to take screenshots and share any file type
    Copyright (c) 2007-2015 ShareX Team

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

    Optionally you can also view the license at <http://www.gnu.org/licenses/>.
*/

#endregion License Information (GPL v3)

using ShareX.HelpersLib;
using System;
using System.Collections.Generic;
using System.Drawing;

namespace ShareX.ScreenCaptureLib
{
    public class WindowsRectangleList
    {
        public IntPtr IgnoreHandle { get; set; }
        public bool IncludeChildWindows { get; set; }

        private List<SimpleWindowInfo> windows;

        public List<SimpleWindowInfo> GetWindowsRectangleList()
        {
            windows = new List<SimpleWindowInfo>();
            NativeMethods.EnumWindowsProc ewp = EvalWindow;
            NativeMethods.EnumWindows(ewp, IntPtr.Zero);

            List<SimpleWindowInfo> result = new List<SimpleWindowInfo>();

            foreach (SimpleWindowInfo window in windows)
            {
                bool rectVisible = true;

                foreach (SimpleWindowInfo window2 in result)
                {
                    if (window2.Rectangle.Contains(window.Rectangle))
                    {
                        rectVisible = false;
                        break;
                    }
                }

                if (rectVisible)
                {
                    result.Add(window);
                }
            }

            return result;
        }

        private bool EvalWindow(IntPtr hWnd, IntPtr lParam)
        {
            return CheckHandle(hWnd, true);
        }

        private bool EvalControl(IntPtr hWnd, IntPtr lParam)
        {
            return CheckHandle(hWnd, false);
        }

        private bool CheckHandle(IntPtr handle, bool isWindow)
        {
            if (handle == IgnoreHandle || !NativeMethods.IsWindowVisible(handle))
            {
                return true;
            }

            SimpleWindowInfo windowInfo = new SimpleWindowInfo(handle);

            if (isWindow)
            {
                windowInfo.Rectangle = CaptureHelpers.GetWindowRectangle(handle);
            }
            else
            {
                windowInfo.Rectangle = NativeMethods.GetWindowRect(handle);
            }

            if (!windowInfo.Rectangle.IsValid())
            {
                return true;
            }

            if (IncludeChildWindows)
            {
                NativeMethods.EnumWindowsProc ewp = EvalControl;
                NativeMethods.EnumChildWindows(handle, ewp, IntPtr.Zero);
            }

            if (isWindow)
            {
                Rectangle clientRect = NativeMethods.GetClientRect(handle);

                if (clientRect.IsValid())
                {
                    windows.Add(new SimpleWindowInfo(handle, clientRect));
                }
            }

            windows.Add(windowInfo);

            return true;
        }
    }
}