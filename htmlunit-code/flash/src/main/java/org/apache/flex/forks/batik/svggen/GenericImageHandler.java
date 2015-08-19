/*

   Copyright 2001,2003  The Apache Software Foundation 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.flex.forks.batik.svggen;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

import org.w3c.dom.Element;

/**
 * Extends the default ImageHandler interface with calls to
 * allow caching of raster images in generated SVG content.
 *
 * @author <a href="mailto:vincent.hardy@sun.com">Vincent Hardy</a>
 * @version $Id: GenericImageHandler.java 7342 2012-09-05 08:57:06Z asashour $
 */
public interface GenericImageHandler {
    /**
     * Sets the DomTreeManager this image handler may need to 
     * interact with.
     */
    public void setDOMTreeManager(DOMTreeManager domTreeManager);

    /**
     * Creates an Element suitable for referring to images.
     * Note that no assumptions can be made about the name of this Element.
     */
    public Element createElement(SVGGeneratorContext generatorContext);

    /**
     * The handler should set the xlink:href and return a transform
     *
     * @param image             the image under consideration
     * @param imageElement      the DOM Element for this image
     * @param x                 x coordinate
     * @param y                 y coordinate
     * @param width             width for rendering
     * @param height            height for rendering
     * @param generatorContext  the SVGGeneratorContext
     *
     * @return transform converting the image dimension to rendered dimension
     */
    public AffineTransform handleImage(Image image, Element imageElement,
                                       int x, int y,
                                       int width, int height,
                                       SVGGeneratorContext generatorContext);

    /**
     * The handler should set the xlink:href tag and return a transform
     *
     * @param image             the image under consideration
     * @param imageElement      the DOM Element for this image
     * @param x                 x coordinate
     * @param y                 y coordinate
     * @param width             width for rendering
     * @param height            height for rendering
     * @param generatorContext  the SVGGeneratorContext
     *
     * @return transform converting the image dimension to rendered dimension
     */
    public AffineTransform handleImage(RenderedImage image, Element imageElement,
                                       int x, int y,
                                       int width, int height,
                                       SVGGeneratorContext generatorContext);

    /**
     * The handler should set the xlink:href tag and return a transform
     *
     * @param image             the image under consideration
     * @param imageElement      the DOM Element for this image
     * @param x                 x coordinate
     * @param y                 y coordinate
     * @param width             width for rendering
     * @param height            height for rendering
     * @param generatorContext  the SVGGeneratorContext
     *
     * @return transform converting the image dimension to rendered dimension
     */
    public AffineTransform handleImage(RenderableImage image, Element imageElement,
                                       double x, double y,
                                       double width, double height,
                                       SVGGeneratorContext generatorContext);

}
