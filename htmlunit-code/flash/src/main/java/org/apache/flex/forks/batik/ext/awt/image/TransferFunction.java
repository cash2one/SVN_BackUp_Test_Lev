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

 */package org.apache.flex.forks.batik.ext.awt.image;

/**
 * Defines the interface for all the <tt>ComponentTransferOp</tt> transfer
 * functions, which can all be converted to a lookup table
 *
 * @author <a href="mailto:sheng.pei@eng.sun.com">Sheng Pei</a>
 * @version $Id: TransferFunction.java 7342 2012-09-05 08:57:06Z asashour $ 
 */
public interface TransferFunction {

    /**
     * Returns the lookup table.
     */
    byte [] getLookupTable();

}
