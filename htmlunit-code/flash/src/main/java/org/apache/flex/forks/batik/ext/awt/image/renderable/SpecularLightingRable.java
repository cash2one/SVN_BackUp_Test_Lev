/*

   Copyright 2001  The Apache Software Foundation 

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
package org.apache.flex.forks.batik.ext.awt.image.renderable;

import java.awt.geom.Rectangle2D;

import org.apache.flex.forks.batik.ext.awt.image.Light;

/**
 * This filter follows the specification of the feSpecularLighting filter in 
 * the SVG 1.0 specification.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id: SpecularLightingRable.java 7342 2012-09-05 08:57:06Z asashour $
 */
public interface SpecularLightingRable extends FilterColorInterpolation {
    /**
     * Returns the source to be filtered
     */
    public Filter getSource();

    /**
     * Sets the source to be filtered
     */
    public void setSource(Filter src);

    /**
     * @return Light object used for the diffuse lighting
     */
    public Light getLight();

    /**
     * @param light New Light object
     */
    public void setLight(Light light);

    /**
     * @return surfaceScale
     */
    public double getSurfaceScale();

    /**
     * Sets the surface scale
     */
    public void setSurfaceScale(double surfaceScale);

    /**
     * @return specular constant, or ks.
     */
    public double getKs();

    /**
     * Sets the specular constant, or ks
     */
    public void setKs(double ks);

    /**
     * @return specular exponent, or kd
     */
    public double getSpecularExponent();

    /**
     * Sets the specular exponent
     */
    public void setSpecularExponent(double specularExponent);

    /**
     * @return the litRegion for this filter
     */
    public Rectangle2D getLitRegion();

    /**
     * Sets the litRegion for this filter
     */
    public void setLitRegion(Rectangle2D litRegion);

    /**
     * Returns the min [dx,dy] distance in user space for evalutation of 
     * the sobel gradient.
     */
    public double [] getKernelUnitLength();

    /**
     * Sets the min [dx,dy] distance in user space for evaluation of the 
     * sobel gradient. If set to zero or null then device space will be used.
     */
    public void setKernelUnitLength(double [] kernelUnitLength);
}

