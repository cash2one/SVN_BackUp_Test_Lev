/*

   Copyright 2001-2003  The Apache Software Foundation 

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

import java.util.HashMap;
import java.util.Map;

/**
 * Generates id for an arbitrary number of prefix
 *
 * @author <a href="mailto:cjolif@ilog.fr">Christophe Jolif</a>
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id: SVGIDGenerator.java 7342 2012-09-05 08:57:06Z asashour $
 */
public class SVGIDGenerator {
    private Map prefixMap = new HashMap();

    public SVGIDGenerator() {
    }

    /**
     * Generates an id for the given prefix. This class keeps
     * track of all invocations to that it generates unique ids
     *
     * @param prefix defines the prefix for which the id should
     *               be generated.
     * @return a value of the form <prefix><n>
     */
    public String generateID(String prefix) {
        Integer maxId = (Integer)prefixMap.get(prefix);
        if (maxId == null) {
            maxId = new Integer(0);
            prefixMap.put(prefix, maxId);
        }

        maxId = new Integer(maxId.intValue()+1);
        prefixMap.put(prefix, maxId);
        return prefix + maxId;
    }
}
