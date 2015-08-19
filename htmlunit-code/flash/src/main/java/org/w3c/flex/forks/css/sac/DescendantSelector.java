/*
 * (c) COPYRIGHT 1999 World Wide Web Consortium
 * (Massachusetts Institute of Technology, Institut National de Recherche
 *  en Informatique et en Automatique, Keio University).
 * All Rights Reserved. http://www.w3.org/Consortium/Legal/
 *
 * $Id: DescendantSelector.java 7342 2012-09-05 08:57:06Z asashour $
 */
package org.w3c.flex.forks.css.sac;

/**
 * @version $Revision: 7342 $
 * @author  Philippe Le Hegaret
 * @see Selector#SAC_DESCENDANT_SELECTOR
 * @see Selector#SAC_CHILD_SELECTOR
 */
public interface DescendantSelector extends Selector {
    
    /**
     * Returns the parent selector.
     */    
    public Selector getAncestorSelector();

    /*
     * Returns the simple selector.
     */    
    public SimpleSelector getSimpleSelector();
}
