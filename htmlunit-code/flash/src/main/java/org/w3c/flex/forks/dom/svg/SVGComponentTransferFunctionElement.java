/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.w3c.flex.forks.dom.svg;

public interface SVGComponentTransferFunctionElement extends 
               SVGElement {
  // Component Transfer Types
  public static final short SVG_FECOMPONENTTRANSFER_TYPE_UNKNOWN  = 0;
  public static final short SVG_FECOMPONENTTRANSFER_TYPE_IDENTITY = 1;
  public static final short SVG_FECOMPONENTTRANSFER_TYPE_TABLE    = 2;
  public static final short SVG_FECOMPONENTTRANSFER_TYPE_DISCRETE    = 3;
  public static final short SVG_FECOMPONENTTRANSFER_TYPE_LINEAR   = 4;
  public static final short SVG_FECOMPONENTTRANSFER_TYPE_GAMMA    = 5;

  public SVGAnimatedEnumeration getType( );
  public SVGAnimatedNumberList  getTableValues( );
  public SVGAnimatedNumber      getSlope( );
  public SVGAnimatedNumber      getIntercept( );
  public SVGAnimatedNumber      getAmplitude( );
  public SVGAnimatedNumber      getExponent( );
  public SVGAnimatedNumber      getOffset( );
}
