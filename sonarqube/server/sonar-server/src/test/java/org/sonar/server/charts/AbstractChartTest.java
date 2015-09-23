/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.charts;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtilities;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertTrue;

public abstract class AbstractChartTest {
  protected void assertChartSizeGreaterThan(BufferedImage img, int size) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ChartUtilities.writeBufferedImageAsPNG(output, img);
    assertTrue("PNG size in bits=" + output.size(), output.size() > size);
  }

  protected void assertChartSizeLesserThan(BufferedImage img, int size) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ChartUtilities.writeBufferedImageAsPNG(output, img);
    assertTrue("PNG size in bits=" + output.size(), output.size() < size);
  }

  protected void saveChart(BufferedImage img, String name) throws IOException {
    File target = new File("target/tmp-chart", name);
    FileUtils.forceMkdir(target.getParentFile());
    ByteArrayOutputStream imgOutput = new ByteArrayOutputStream();
    ChartUtilities.writeBufferedImageAsPNG(imgOutput, img);
    OutputStream out = new FileOutputStream(target);
    out.write(imgOutput.toByteArray());
    out.close();

  }

  protected static void displayTestPanel(BufferedImage image) {
    ApplicationFrame frame = new ApplicationFrame("testframe");
    BufferedPanel imgPanel = new BufferedPanel(image);
    frame.setContentPane(imgPanel);
    frame.pack();
    RefineryUtilities.centerFrameOnScreen(frame);
    frame.setVisible(true);
  }

  protected static Date stringToDate(String sDate) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh'h'mm");
    return sdf.parse(sDate);
  }

  private static class BufferedPanel extends JPanel {
    private final BufferedImage chartImage;

    public BufferedPanel(BufferedImage chartImage) {
      this.chartImage = chartImage;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      graphics.drawImage(chartImage, 0, 0, null);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(chartImage.getWidth(), chartImage.getHeight());
    }
  }
}
