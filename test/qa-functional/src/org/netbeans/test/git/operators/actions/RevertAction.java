/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.test.git.operators.actions;

import org.netbeans.jellytools.actions.ActionNoBlock;

/**
 *
 * @author alexbcoles
 */
public class RevertAction extends ActionNoBlock {
    
     /** "Mercurial" menu item. */
    public static final String HG_ITEM = "Mercurial";
            
    /** "Revert" menu item. */
    public static final String REVERT_ITEM = "Revert";
    
    /** Creates a new instance of RevertAction */
    public RevertAction() {
        super(HG_ITEM + "|" + REVERT_ITEM, HG_ITEM + "|" + REVERT_ITEM);
    }
    

}
