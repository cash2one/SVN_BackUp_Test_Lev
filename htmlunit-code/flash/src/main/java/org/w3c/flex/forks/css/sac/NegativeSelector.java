/*
 * (c) COPYRIGHT 1999 World Wide Web Consortium
 * (Massachusetts Institute of Technology, Institut National de Recherche
 *  en Informatique et en Automatique, Keio University).
 * All Rights Reserved. http://www.w3.org/Consortium/Legal/
 *
 * $Id: NegativeSelector.java 7342 2012-09-05 08:57:06Z asashour $
 */
package org.w3c.flex.forks.css.sac;

/**
 * @version $Revision: 7342 $
 * @author  Philippe Le Hegaret
 * @see Selector#SAC_NEGATIVE_SELECTOR
 */
public interface NegativeSelector extends SimpleSelector {

    /**
     * Returns the simple selector.
     */    
    public SimpleSelector getSimpleSelector();
}
