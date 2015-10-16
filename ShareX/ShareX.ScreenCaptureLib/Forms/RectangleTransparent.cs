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
using ShareX.ScreenCaptureLib.Properties;
using System;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Windows.Forms;

namespace ShareX.ScreenCaptureLib
{
    public class RectangleTransparent : LayeredForm
    {
        public static Rectangle LastSelectionRectangle0Based { get; private set; }

        public Rectangle ScreenRectangle { get; private set; }

        public Rectangle ScreenRectangle0Based
        {
            get
            {
                return new Rectangle(0, 0, ScreenRectangle.Width, ScreenRectangle.Height);
            }
        }

        public Rectangle SelectionRectangle { get; private set; }

        public Rectangle SelectionRectangle0Based
        {
            get
            {
                return new Rectangle(SelectionRectangle.X - ScreenRectangle.X, SelectionRectangle.Y - ScreenRectangle.Y, SelectionRectangle.Width, SelectionRectangle.Height);
            }
        }

        private Rectangle PreviousSelectionRectangle { get; set; }

        private Rectangle PreviousSelectionRectangle0Based
        {
            get
            {
                return new Rectangle(PreviousSelectionRectangle.X - ScreenRectangle.X, PreviousSelectionRectangle.Y - ScreenRectangle.Y,
                    PreviousSelectionRectangle.Width, PreviousSelectionRectangle.Height);
            }
        }

        private Timer timer;
        private Bitmap surface;
        private Graphics gSurface;
        private Pen clearPen, borderDotPen, borderDotPen2;
        private Point currentPosition, positionOnClick;
        private bool isMouseDown;
        private Stopwatch penTimer;

        public RectangleTransparent()
        {
            clearPen = new Pen(Color.FromArgb(1, 0, 0, 0));
            borderDotPen = new Pen(Color.Black, 1);
            borderDotPen2 = new Pen(Color.White, 1);
            borderDotPen2.DashPattern = new float[] { 5, 5 };
            penTimer = Stopwatch.StartNew();
            ScreenRectangle = CaptureHelpers.GetScreenBounds();

            surface = new Bitmap(ScreenRectangle.Width, ScreenRectangle.Height);
            gSurface = Graphics.FromImage(surface);
            gSurface.InterpolationMode = InterpolationMode.NearestNeighbor;
            gSurface.SmoothingMode = SmoothingMode.HighSpeed;
            gSurface.CompositingMode = CompositingMode.SourceCopy;
            gSurface.CompositingQuality = CompositingQuality.HighSpeed;
            gSurface.Clear(Color.FromArgb(1, 0, 0, 0));

            StartPosition = FormStartPosition.Manual;
            Bounds = ScreenRectangle;
            Text = "ShareX - " + Resources.RectangleTransparent_RectangleTransparent_Rectangle_capture_transparent;

            Shown += RectangleLight_Shown;
            KeyUp += RectangleLight_KeyUp;
            MouseDown += RectangleLight_MouseDown;
            MouseUp += RectangleLight_MouseUp;

            using (MemoryStream cursorStream = new MemoryStream(Resources.Crosshair))
            {
                Cursor = new Cursor(cursorStream);
            }

            timer = new Timer { Interval = 10 };
            timer.Tick += timer_Tick;
            timer.Start();
        }

        protected override void Dispose(bool disposing)
        {
            if (timer != null) timer.Dispose();
            if (clearPen != null) clearPen.Dispose();
            if (borderDotPen != null) borderDotPen.Dispose();
            if (borderDotPen2 != null) borderDotPen2.Dispose();
            if (gSurface != null) gSurface.Dispose();
            if (surface != null) surface.Dispose();

            base.Dispose(disposing);
        }

        private void RectangleLight_Shown(object sender, EventArgs e)
        {
            this.ShowActivate();
        }

        private void RectangleLight_KeyUp(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Escape)
            {
                Close();
            }
        }

        private void RectangleLight_MouseDown(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                positionOnClick = CaptureHelpers.GetCursorPosition();
                isMouseDown = true;
            }
        }

        private void RectangleLight_MouseUp(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                if (isMouseDown)
                {
                    if (SelectionRectangle0Based.Width > 0 && SelectionRectangle0Based.Height > 0)
                    {
                        LastSelectionRectangle0Based = SelectionRectangle0Based;
                        DialogResult = DialogResult.OK;
                    }

                    Close();
                }
            }
            else
            {
                if (isMouseDown)
                {
                    isMouseDown = false;
                }
                else
                {
                    Close();
                }
            }
        }

        public Image GetAreaImage()
        {
            Rectangle rect = SelectionRectangle0Based;

            if (rect.Width > 0 && rect.Height > 0)
            {
                return Screenshot.CaptureRectangle(SelectionRectangle);
            }

            return null;
        }

        private void timer_Tick(object sender, EventArgs e)
        {
            currentPosition = CaptureHelpers.GetCursorPosition();
            PreviousSelectionRectangle = SelectionRectangle;
            SelectionRectangle = CaptureHelpers.CreateRectangle(positionOnClick.X, positionOnClick.Y, currentPosition.X, currentPosition.Y);

            try
            {
                RefreshSurface();
            }
            catch
            {
            }
        }

        private void RefreshSurface()
        {
            // Clear previous rectangle selection
            gSurface.DrawRectangleProper(clearPen, PreviousSelectionRectangle0Based);

            if (isMouseDown)
            {
                borderDotPen2.DashOffset = (int)(penTimer.Elapsed.TotalMilliseconds / 100) % 10;
                gSurface.DrawRectangleProper(borderDotPen, SelectionRectangle0Based);
                gSurface.DrawRectangleProper(borderDotPen2, SelectionRectangle0Based);
            }

            SelectBitmap(surface);
        }
    }
}